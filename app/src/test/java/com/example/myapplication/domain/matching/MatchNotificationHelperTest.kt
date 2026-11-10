package com.example.myapplication.domain.matching

import com.example.myapplication.data.model.LostFoundPost
import com.example.myapplication.data.model.UserProfile
import org.junit.Assert.*
import org.junit.Test

class MatchNotificationHelperTest {

    private val testUser = UserProfile(
        studentId = "2023141240001",
        name = "张三",
        idCard = "110101199003072375"
    )

    @Test
    fun isPostMatchingUser_matchingStudentId_returnsTrue() {
        val post = LostFoundPost(
            type = "拾卡",
            status = "还在原地",
            title = "校园卡",
            campus = "江安校区",
            location = "一教A座",
            date = "今天",
            documentNumber = "2023141240001"
        )
        assertTrue(MatchNotificationHelper.isPostMatchingUser(post, testUser))
    }

    @Test
    fun isPostMatchingUser_matchingIdCard_returnsTrue() {
        val post = LostFoundPost(
            type = "拾卡",
            status = "代为保管",
            title = "身份证",
            campus = "望江校区",
            location = "基础教学楼",
            date = "今天",
            documentNumber = "110101199003072375"
        )
        assertTrue(MatchNotificationHelper.isPostMatchingUser(post, testUser))
    }

    @Test
    fun isPostMatchingUser_postTypeNotFound_returnsFalse() {
        // 寻卡帖子不触发匹配通知，只有拾卡帖子才通知失主
        val post = LostFoundPost(
            type = "寻卡",
            status = "正在寻找",
            title = "校园卡",
            campus = "江安校区",
            location = "一教A座",
            date = "今天",
            documentNumber = "2023141240001"
        )
        assertFalse(MatchNotificationHelper.isPostMatchingUser(post, testUser))
    }

    @Test
    fun isPostMatchingUser_differentDocNumber_returnsFalse() {
        val post = LostFoundPost(
            type = "拾卡",
            status = "还在原地",
            title = "校园卡",
            campus = "江安校区",
            location = "一教A座",
            date = "今天",
            documentNumber = "2023141249999"
        )
        assertFalse(MatchNotificationHelper.isPostMatchingUser(post, testUser))
    }

    @Test
    fun isPostMatchingUser_blankDocNumber_returnsFalse() {
        val post = LostFoundPost(
            type = "拾卡",
            status = "还在原地",
            title = "校园卡",
            campus = "江安校区",
            location = "一教A座",
            date = "今天",
            documentNumber = ""
        )
        assertFalse(MatchNotificationHelper.isPostMatchingUser(post, testUser))
    }

    @Test
    fun isPostMatchingUser_caseAndWhitespaceInsensitive() {
        val post = LostFoundPost(
            type = "拾卡",
            status = "代为保管",
            title = "身份证",
            campus = "望江校区",
            location = "基础教学楼",
            date = "今天",
            documentNumber = " 110101199003072375 "
        )
        assertTrue(MatchNotificationHelper.isPostMatchingUser(post, testUser))
    }
}
