package com.example.myapplication.data.model

import org.junit.Assert.*
import org.junit.Test

class LostFoundPostTest {

    @Test
    fun createPost_primaryConstructor() {
        val post = LostFoundPost(
            type = "拾卡",
            status = "还在原地",
            title = "校园卡",
            campus = "江安校区",
            location = "第一教学楼",
            date = "11月10日",
            note = "在二楼连廊拾获",
            personName = "张三",
            documentNumber = "2023141240001",
            latitude = 30.557,
            longitude = 104.002,
            imageUri = "content://media/123"
        )

        assertNotNull(post.id)
        assertEquals("拾卡", post.type)
        assertEquals("还在原地", post.status)
        assertEquals("校园卡", post.title)
        assertEquals("江安校区", post.campus)
        assertEquals("第一教学楼", post.location)
        assertEquals("张三", post.personName)
        assertEquals("2023141240001", post.documentNumber)
        assertEquals(30.557, post.latitude!!, 0.001)
        assertEquals(104.002, post.longitude!!, 0.001)
        assertEquals("content://media/123", post.imageUri)
        assertFalse(post.isDemo)
        assertTrue(post.createdAt > 0)
    }

    @Test
    fun postCopy_updatesSpecificFields() {
        val original = LostFoundPost(
            type = "拾卡",
            status = "还在原地",
            title = "校园卡",
            campus = "江安校区",
            location = "一教",
            date = "今天"
        )
        val updated = original.copy(status = "已被失主认领", imageUri = "newUri")

        assertEquals(original.id, updated.id)
        assertEquals("已被失主认领", updated.status)
        assertEquals("newUri", updated.imageUri)
        assertEquals(original.title, updated.title)
    }
}
