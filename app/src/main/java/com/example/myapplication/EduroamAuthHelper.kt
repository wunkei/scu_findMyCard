package com.example.myapplication

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
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
    private const val TIMEOUT_MS = 15000

    private val CSRF_META_PATTERN_1 = Pattern.compile(
        """<meta\s+name=["']csrf-token["']\s+content=["']([^"']+)["']""",
        Pattern.CASE_INSENSITIVE
    )
    private val CSRF_META_PATTERN_2 = Pattern.compile(
        """<meta\s+content=["']([^"']+)["']\s+name=["']csrf-token["']""",
        Pattern.CASE_INSENSITIVE
    )

    suspend fun verifyEduroamAccount(username: String, password: String): EduroamResult = withContext(Dispatchers.IO) {
        if (username.isBlank() || password.isBlank()) {
            return@withContext EduroamResult.Failed("学号与密码不能为空")
        }

        try {
            // 1. GET page & collect cookies and CSRF token
            val pageConnection = (URL(PAGE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                instanceFollowRedirects = true
            }

            val responseCode = pageConnection.responseCode
            if (responseCode !in 200..299) {
                return@withContext EduroamResult.Error("无法连接到 eduroam 验证服务器 (HTTP $responseCode)")
            }

            // Extract Set-Cookie headers
            val cookies = extractCookies(pageConnection)

            val pageHtml = BufferedReader(InputStreamReader(pageConnection.inputStream, "UTF-8")).use {
                it.readText()
            }
            pageConnection.disconnect()

            val csrfToken = extractCsrfToken(pageHtml)
                ?: return@withContext EduroamResult.Error("无法从验证页面获取安全凭证 (CSRF Token)")

            // 2. Generate random 16-digit sid: "0.xxxxxxxxxxxxxxxx"
            val randomDigits = Random.nextLong(0, 10_000_000_000_000_000L)
            val sid = "0.%016d".format(Locale.ROOT, randomDigits)

            // 3. POST authentication payload
            val postConnection = (URL(POST_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
                doInput = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                setRequestProperty("Referer", PAGE_URL)
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01")
                setRequestProperty("X-Requested-With", "XMLHttpRequest")
                if (cookies.isNotBlank()) {
                    setRequestProperty("Cookie", cookies)
                }
            }

            val cleanUsername = username.trim()
            val eduroamUsername = if (cleanUsername.contains("@")) {
                cleanUsername
            } else {
                "$cleanUsername@scu.edu.cn"
            }

            val formParams = listOf(
                "username" to eduroamUsername,
                "passwd" to password,
                "sid" to sid,
                "_csrf-f" to csrfToken
            ).joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }

            OutputStreamWriter(postConnection.outputStream, "UTF-8").use { writer ->
                writer.write(formParams)
                writer.flush()
            }

            val postResponseCode = postConnection.responseCode
            val responseText = if (postResponseCode in 200..299) {
                BufferedReader(InputStreamReader(postConnection.inputStream, "UTF-8")).use { it.readText() }
            } else {
                val errorStream = postConnection.errorStream
                if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
                } else {
                    return@withContext EduroamResult.Error("验证请求返回异常 (HTTP $postResponseCode)")
                }
            }
            postConnection.disconnect()

            // 4. Parse JSON result
            parseEduroamResponse(responseText)

        } catch (_: Exception) {
            EduroamResult.Error("网络连接失败，请检查手机网络设置后重试")
        }
    }

    private fun extractCookies(connection: HttpURLConnection): String {
        val cookieList = mutableListOf<String>()
        val headerFields = connection.headerFields
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

    private fun extractCsrfToken(html: String): String? {
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

    private fun parseEduroamResponse(jsonStr: String): EduroamResult {
        return try {
            val json = JSONObject(jsonStr)
            val logtitle = json.optJSONArray("logtitle")
            val isSuccess = if (logtitle != null) {
                (0 until logtitle.length()).any { index ->
                    logtitle.optString(index).contains("SUCCESS", ignoreCase = true)
                }
            } else {
                false
            }

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
