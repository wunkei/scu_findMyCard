package com.example.myapplication.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EduroamAuthHelperTest {

    @Test
    fun parseEduroamResponse_withSuccessJson_returnsSuccess() {
        val successJson = """
            {
                "logtitle": [
                    "User: 2023123456@scu.edu.cn",
                    "Radius Server: auth.scu.edu.cn",
                    "Result: SUCCESS Access-Accept"
                ],
                "logdetail": "Authentication succeeded"
            }
        """.trimIndent()

        val result = EduroamAuthHelper.parseEduroamResponse(successJson)
        assertTrue(result is EduroamResult.Success)
    }

    @Test
    fun parseEduroamResponse_withFailureJson_returnsFailed() {
        val failureJson = """
            {
                "logtitle": [
                    "User: 2023123456@scu.edu.cn",
                    "Radius Server: auth.scu.edu.cn",
                    "Result: REJECT Access-Reject: Wrong password"
                ],
                "logdetail": "Authentication failed"
            }
        """.trimIndent()

        val result = EduroamAuthHelper.parseEduroamResponse(failureJson)
        assertTrue(result is EduroamResult.Failed)
        assertEquals("学号或校园网密码错误，请重新核对", (result as EduroamResult.Failed).message)
    }

    @Test
    fun parseEduroamResponse_withRawSuccessString_returnsSuccess() {
        val rawStr = "<html><body>SUCCESS verification</body></html>"
        val result = EduroamAuthHelper.parseEduroamResponse(rawStr)
        assertTrue(result is EduroamResult.Success)
    }

    @Test
    fun parseEduroamResponse_withRawFailureString_returnsFailed() {
        val rawStr = "<html><body>Gateway Timeout 504</body></html>"
        val result = EduroamAuthHelper.parseEduroamResponse(rawStr)
        assertTrue(result is EduroamResult.Failed)
        assertEquals("学号或校园网密码错误，请重新核对", (result as EduroamResult.Failed).message)
    }

    @Test
    fun extractCsrfToken_standardPattern_extractsToken() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="csrf-param" content="_csrf-f">
                <meta name="csrf-token" content="abc123xyzToken==">
            </head>
            <body></body>
            </html>
        """.trimIndent()

        val token = EduroamAuthHelper.extractCsrfToken(html)
        assertEquals("abc123xyzToken==", token)
    }

    @Test
    fun extractCsrfToken_invertedAttributes_extractsToken() {
        val html = """
            <head>
                <meta content="invertedToken456==" name="csrf-token">
            </head>
        """.trimIndent()

        val token = EduroamAuthHelper.extractCsrfToken(html)
        assertEquals("invertedToken456==", token)
    }

    @Test
    fun extractCsrfToken_notFound_returnsNull() {
        val html = "<html><head><title>No Token</title></head></html>"
        val token = EduroamAuthHelper.extractCsrfToken(html)
        assertNull(token)
    }
}
