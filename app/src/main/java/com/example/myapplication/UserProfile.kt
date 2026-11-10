package com.example.myapplication

data class UserProfile(
    val studentId: String,
    val name: String,
    val password: String = "",
    val idCard: String = "",
    val phone: String = "",
    val campus: String = "江安校区",
    val isVerified: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
