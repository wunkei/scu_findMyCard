package com.example.myapplication.data.model

import org.junit.Assert.*
import org.junit.Test

class UserProfileTest {

    @Test
    fun createUserProfile_defaultValues() {
        val user = UserProfile(
            studentId = "2023141240001",
            name = "李四"
        )

        assertEquals("2023141240001", user.studentId)
        assertEquals("李四", user.name)
        assertEquals("", user.password)
        assertEquals("", user.idCard)
        assertEquals("", user.phone)
        assertEquals("江安校区", user.campus)
        assertTrue(user.isVerified)
        assertTrue(user.createdAt > 0)
    }

    @Test
    fun userProfileCopy_preservesImmutableFields() {
        val user = UserProfile(
            studentId = "2023141240001",
            name = "李四",
            campus = "望江校区",
            phone = "13800138000"
        )
        val modified = user.copy(phone = "13900139000", campus = "华西校区")

        assertEquals("2023141240001", modified.studentId)
        assertEquals("李四", modified.name)
        assertEquals("13900139000", modified.phone)
        assertEquals("华西校区", modified.campus)
    }
}
