package com.example.myapplication.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

object NetworkTimeHelper {

    private const val TENCENT_TIME_URL_HTTP = "http://vv.video.qq.com/checktime?otype=json"
    private const val TENCENT_TIME_URL_HTTPS = "https://vv.video.qq.com/checktime?otype=json"
    private const val TIMEOUT_MS = 3000

    private val TIME_REGEX = Pattern.compile(""""t"\s*:\s*(\d+)""")

    /**
     * 获取腾讯服务器提供的网络标准时间（毫秒），若请求失败则回退至设备当前系统时间
     */
    suspend fun getNetworkTimeMillis(): Long = withContext(Dispatchers.IO) {
        val tencentSec = fetchTencentEpochSeconds()
        if (tencentSec != null && tencentSec > 0) {
            tencentSec * 1000L
        } else {
            System.currentTimeMillis()
        }
    }

    /**
     * 解析腾讯时间接口返回的数据
     * 典型格式：QZOutputJson={"s":"o","t":1592663920,"ip":"113.246.187.221","pos":"---","rand":"HTYY3wMs_y5_3F_XTi-KKg=="};
     */
    internal fun parseTencentResponse(response: String): Long? {
        val matcher = TIME_REGEX.matcher(response)
        if (matcher.find()) {
            return matcher.group(1)?.toLongOrNull()
        }
        return try {
            val jsonStr = response.substringAfter("QZOutputJson=").substringBeforeLast(";").trim()
            val json = JSONObject(jsonStr)
            val t = json.optLong("t", -1L)
            if (t > 0) t else null
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchTencentEpochSeconds(): Long? {
        // 先尝试 HTTP（匹配接口默认协议），若失败则尝试 HTTPS
        for (targetUrl in listOf(TENCENT_TIME_URL_HTTP, TENCENT_TIME_URL_HTTPS)) {
            try {
                val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                if (conn.responseCode in 200..299) {
                    val body = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { it.readText() }
                    conn.disconnect()
                    val parsed = parseTencentResponse(body)
                    if (parsed != null && parsed > 0) {
                        return parsed
                    }
                }
                conn.disconnect()
            } catch (_: Exception) {
                // 尝试备用协议
            }
        }
        return null
    }

    /**
     * 校验指定的时间戳（毫秒）是否不晚于当前网络时间（默认允许容忍 60 秒的网络与时钟偏差）
     */
    suspend fun validateNotLaterThanNow(targetTimeMillis: Long, toleranceSeconds: Long = 60): Boolean {
        val currentNetworkTime = getNetworkTimeMillis()
        return targetTimeMillis <= (currentNetworkTime + toleranceSeconds * 1000L)
    }
}
