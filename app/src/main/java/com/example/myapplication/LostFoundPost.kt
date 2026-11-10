package com.example.myapplication

import java.util.UUID

data class LostFoundPost(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val status: String,
    val title: String,
    val campus: String,
    val location: String,
    val date: String,
    val note: String,
    val isDemo: Boolean = false,
    val personName: String = "",
    val documentNumber: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor(
        type: String,
        status: String,
        title: String,
        campus: String,
        location: String,
        date: String,
        note: String,
        isDemo: Boolean,
        personName: String = "",
        documentNumber: String = ""
    ) : this(
        id = UUID.randomUUID().toString(),
        type = type,
        status = status,
        title = title,
        campus = campus,
        location = location,
        date = date,
        note = note,
        isDemo = isDemo,
        personName = personName,
        documentNumber = documentNumber,
        createdAt = System.currentTimeMillis()
    )
}
