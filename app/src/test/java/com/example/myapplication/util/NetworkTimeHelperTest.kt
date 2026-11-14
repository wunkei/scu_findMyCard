package com.example.myapplication.util

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NetworkTimeHelperTest {

    @Test
    fun parseTencentResponse_standardFormat_extractsTimestamp() {
        val raw = """QZOutputJson={"s":"o","t":1592663920,"ip":"113.246.187.221","pos":"---","rand":"HTYY3wMs_y5_3F_XTi-KKg=="};"""
        val timestamp = NetworkTimeHelper.parseTencentResponse(raw)
        assertNotNull(timestamp)
        assertEquals(1592663920L, timestamp)
    }

    @Test
    fun parseTencentResponse_withSpaces_extractsTimestamp() {
        val raw = """QZOutputJson={"s": "o", "t": 1700000000, "ip": "1.1.1.1"};"""
        val timestamp = NetworkTimeHelper.parseTencentResponse(raw)
        assertNotNull(timestamp)
        assertEquals(1700000000L, timestamp)
    }

    @Test
    fun parseTencentResponse_invalidFormat_returnsNull() {
        val raw = """{"error": "not found"}"""
        val timestamp = NetworkTimeHelper.parseTencentResponse(raw)
        assertNull(timestamp)
    }

    @Test
    fun validateNotLaterThanNow_pastTime_returnsTrue() = runBlocking {
        val oneHourAgo = System.currentTimeMillis() - 3600 * 1000L
        val isValid = NetworkTimeHelper.validateNotLaterThanNow(oneHourAgo)
        assertTrue(isValid)
    }

    @Test
    fun validateNotLaterThanNow_farFutureTime_returnsFalse() = runBlocking {
        val tomorrow = System.currentTimeMillis() + 86400 * 1000L
        val isValid = NetworkTimeHelper.validateNotLaterThanNow(tomorrow)
        assertFalse(isValid)
    }
}
