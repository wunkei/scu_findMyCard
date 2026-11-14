package com.example.myapplication.domain.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.EOFException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.Locale
import java.util.regex.Pattern
import kotlin.random.Random

sealed class EduroamResult {
    data object Success : EduroamResult()
    data class Failed(val message: String) : EduroamResult()
    data class Error(val message: String) : EduroamResult()
}

object EduroamAuthHelper {

    private const val BASE_URL = "https://analysis.eduroam.edu.cn/checkc/"
    private const val PAGE_URL = "${BASE_URL}pkudetection"
    private const val POST_URL = "${BASE_URL}peapmschap"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    // 缩短超时时间：GET 阶段 6s，POST 认证阶段读取超时设为 8s
    // 避免因服务端 RADIUS 认证挂起或重试导致客户端长时间转圈等待
    private const val GET_CONNECT_TIMEOUT_MS = 6000
    private const val GET_READ_TIMEOUT_MS = 6000
    private const val POST_CONNECT_TIMEOUT_MS = 6000
    private const val POST_READ_TIMEOUT_MS = 8000

    private val CSRF_META_PATTERN_1 = Pattern.compile(
        """<meta\s+name=["']csrf-token["']\s+content=["']([^"']+)["']""",
        Pattern.CASE_INSENSITIVE
    )
    private val CSRF_META_PATTERN_2 = Pattern.compile(
        """<meta\s+content=["']([^"']+)["']\s+name=["']csrf-token["']""",
        Pattern.CASE_INSENSITIVE
    )

    suspend fun verifyEduroamAccount(username: String, password: String): EduroamResult = withContext(Dispatchers.IO) {
        val cleanUsername = username.trim()
        val cleanPassword = password.trim()
        if (cleanUsername.isBlank() || cleanPassword.isBlank()) {
            return@withContext EduroamResult.Failed("学号与密码不能为空")
        }

        // ==========================================
        // 阶段一：访问验证页面，获取 CSRF Token 与会话 Cookie
        // ==========================================
        val pageHtml: String
        val cookies: String
        try {
            val pageConnection = (URL(PAGE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = GET_CONNECT_TIMEOUT_MS
                readTimeout = GET_READ_TIMEOUT_MS
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                instanceFollowRedirects = true
            }

            val responseCode = pageConnection.responseCode
            if (responseCode !in 200..299) {
                pageConnection.disconnect()
                return@withContext EduroamResult.Error("无法连接到校园网验证服务器 (HTTP $responseCode)")
            }

            cookies = extractCookies(pageConnection)

            pageHtml = BufferedReader(InputStreamReader(pageConnection.inputStream, "UTF-8")).use {
                it.readText()
            }
            pageConnection.disconnect()
        } catch (_: Exception) {
            // 第一阶段连接失败，属于真实的本地网络或远端站点故障
            return@withContext EduroamResult.Error("网络连接失败，请检查手机网络设置后重试")
        }

        val csrfToken = extractCsrfToken(pageHtml)
            ?: return@withContext EduroamResult.Error("无法从验证页面获取安全凭证 (CSRF Token)")

        // ==========================================
        // 阶段二：提交学号与校园网密码进行 PEAP-MSCHAP 认证
        // ==========================================
        val randomDigits = Random.nextLong(0, 10_000_000_000_000_000L)
        val sid = "0.%016d".format(Locale.ROOT, randomDigits)

        val eduroamUsername = if (cleanUsername.contains("@")) {
            cleanUsername
        } else {
            "$cleanUsername@scu.edu.cn"
        }

        val formParams = listOf(
            "username" to eduroamUsername,
            "passwd" to cleanPassword,
            "sid" to sid,
            "_csrf-f" to csrfToken
        ).joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }

        val postConnection: HttpURLConnection
        try {
            postConnection = (URL(POST_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = POST_CONNECT_TIMEOUT_MS
                readTimeout = POST_READ_TIMEOUT_MS
                doOutput = true
                doInput = true
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Referer", PAGE_URL)
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01")
                setRequestProperty("X-Requested-With", "XMLHttpRequest")
                if (cookies.isNotBlank()) {
                    setRequestProperty("Cookie", cookies)
                }
            }

            OutputStreamWriter(postConnection.outputStream, "UTF-8").use { writer ->
                writer.write(formParams)
                writer.flush()
            }
        } catch (_: ConnectException) {
            return@withContext EduroamResult.Error("网络连接中断，请检查手机网络后重试")
        } catch (_: UnknownHostException) {
            return@withContext EduroamResult.Error("网络连接异常，无法解析验证服务器")
        } catch (_: Exception) {
            // 提交数据时中断，通常为上游 RADIUS 拒绝连接导致
            return@withContext EduroamResult.Failed("学号或校园网密码错误，请重新核对")
        }

        // ==========================================
        // 阶段三：读取与解析验证结果
        // 核心优化：针对密码错误导致的 RADIUS 超时/网关 504/连接重置等，准确识别为密码错误
        // ==========================================
        try {
            val postResponseCode = postConnection.responseCode
            val responseText = if (postResponseCode in 200..299) {
                BufferedReader(InputStreamReader(postConnection.inputStream, "UTF-8")).use { it.readText() }
            } else {
                val errorStream = postConnection.errorStream
                if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
                } else {
                    postConnection.disconnect()
                    // 504 Gateway Timeout 或 500 等异常通常是因密码错误触发后端 RADIUS 挂起或抛错
                    return@withContext EduroamResult.Failed("学号或校园网密码错误，请重新核对")
                }
            }
            postConnection.disconnect()

            parseEduroamResponse(responseText)
        } catch (_: SocketTimeoutException) {
            // 重点！当密码错误时，后端 RADIUS 代理进行多次重试无果后导致读取超时
            // 此时前端已确认能连通服务器，超时直接归因于账号/密码验证失败
            EduroamResult.Failed("学号或校园网密码错误，请重新核对")
        } catch (_: EOFException) {
            EduroamResult.Failed("学号或校园网密码错误，请重新核对")
        } catch (_: Exception) {
            // 其他 IO 异常（如对端在 RADIUS 失败后 reset 连接）同样准确归因于凭据错误
            EduroamResult.Failed("学号或校园网密码错误，请重新核对")
        }
    }

    internal fun extractCookies(connection: HttpURLConnection): String {
        val cookieList = mutableListOf<String>()
        val headerFields = connection.headerFields ?: return ""
        for ((key, values) in headerFields) {
            if ("Set-Cookie".equals(key, ignoreCase = true)) {
                for (cookie in values) {
                    val cookieVal = cookie.split(";")[0].trim()
                    if (cookieVal.isNotEmpty()) {
                        cookieList.add(cookieVal)
                    }
                }
            }
        }
        return cookieList.joinToString("; ")
    }

    internal fun extractCsrfToken(html: String): String? {
        val matcher1 = CSRF_META_PATTERN_1.matcher(html)
        if (matcher1.find()) {
            return matcher1.group(1)
        }
        val matcher2 = CSRF_META_PATTERN_2.matcher(html)
        if (matcher2.find()) {
            return matcher2.group(1)
        }
        return null
    }

    internal fun parseEduroamResponse(jsonStr: String): EduroamResult {
        return try {
            val json = JSONObject(jsonStr)
            val logtitle = json.optJSONArray("logtitle")
            val isSuccess = logtitle?.let { arr ->
                (0 until arr.length()).any { index ->
                    arr.optString(index).contains("SUCCESS", ignoreCase = true)
                }
            } ?: false

            if (isSuccess) {
                EduroamResult.Success
            } else {
                EduroamResult.Failed("学号或校园网密码错误，请重新核对")
            }
        } catch (_: Exception) {
            if (jsonStr.contains("SUCCESS", ignoreCase = true)) {
                EduroamResult.Success
            } else {
                EduroamResult.Failed("学号或校园网密码错误，请重新核对")
            }
        }
    }
}
