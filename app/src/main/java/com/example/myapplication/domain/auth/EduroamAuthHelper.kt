package com.example.myapplication.domain.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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

    /**
     * 利用系统原生 ConnectivityManager 进行快速且无网络开销的物理网络连通性检查
     */
    internal fun isNetworkConnected(context: Context?): Boolean {
        context ?: return true
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    suspend fun verifyEduroamAccount(
        username: String,
        password: String,
        context: Context? = null
    ): EduroamResult = withContext(Dispatchers.IO) {
        val cleanUsername = username.trim()
        val cleanPassword = password.trim()
        if (cleanUsername.isBlank() || cleanPassword.isBlank()) {
            return@withContext EduroamResult.Failed("学号与密码不能为空")
        }

        // ==========================================
        // 前置检查：通过系统 ConnectivityManager 0 延迟秒查本地网络开关
        // ==========================================
        if (!isNetworkConnected(context)) {
            return@withContext EduroamResult.Error("当前无网络连接，请检查Wi-Fi或移动数据设置")
        }

        // ==========================================
        // 阶段一：访问验证页面，获取 CSRF Token 与会话 Cookie
        // 同时作为通往 eduroam 验证服务器的网络探针
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
                return@withContext EduroamResult.Error("校园网验证服务器暂时不可用 (HTTP $responseCode)")
            }

            cookies = extractCookies(pageConnection)

            pageHtml = BufferedReader(InputStreamReader(pageConnection.inputStream, "UTF-8")).use {
                it.readText()
            }
            pageConnection.disconnect()
        } catch (_: Exception) {
            if (!isNetworkConnected(context)) {
                return@withContext EduroamResult.Error("网络连接已断开，请检查网络设置后重试")
            }
            return@withContext EduroamResult.Error("无法连接到校园网验证服务器，请稍后重试")
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
            if (!isNetworkConnected(context)) {
                return@withContext EduroamResult.Error("网络连接已断开，请检查手机网络后重试")
            }
            return@withContext EduroamResult.Failed("学号或校园网密码错误，请重新核对")
        } catch (_: UnknownHostException) {
            return@withContext EduroamResult.Error("网络异常，无法解析校园网验证服务器")
        } catch (_: Exception) {
            if (!isNetworkConnected(context)) {
                return@withContext EduroamResult.Error("网络连接已断开，请检查网络设置后重试")
            }
            return@withContext EduroamResult.Failed("学号或校园网密码错误，请重新核对")
        }

        // ==========================================
        // 阶段三：读取与解析验证结果
        // 结合双重校验：当发生超时或连接中断时，排查本地网络与服务端 RADIUS 挂起
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
                    // 504 Gateway Timeout 或 500 等异常通常是因密码错误触发后端 RADIUS 挂起超时
                    if (!isNetworkConnected(context)) {
                        return@withContext EduroamResult.Error("网络连接已断开，请检查网络设置后重试")
                    }
                    return@withContext EduroamResult.Failed("学号或校园网密码错误，请重新核对")
                }
            }
            postConnection.disconnect()

            parseEduroamResponse(responseText)
        } catch (_: SocketTimeoutException) {
            // 重点！8秒读取超时到达时，先确认本地是否有网
            if (!isNetworkConnected(context)) {
                EduroamResult.Error("网络连接已断开，请检查网络设置后重试")
            } else {
                // 本地网络正常，说明是服务端因密码错误触发了 30s RADIUS 重试阻塞
                EduroamResult.Failed("学号或校园网密码错误，请重新核对")
            }
        } catch (_: EOFException) {
            if (!isNetworkConnected(context)) {
                EduroamResult.Error("网络连接已断开，请检查网络设置后重试")
            } else {
                EduroamResult.Failed("学号或校园网密码错误，请重新核对")
            }
        } catch (_: Exception) {
            if (!isNetworkConnected(context)) {
                EduroamResult.Error("网络连接已断开，请检查网络设置后重试")
            } else {
                EduroamResult.Failed("学号或校园网密码错误，请重新核对")
            }
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
