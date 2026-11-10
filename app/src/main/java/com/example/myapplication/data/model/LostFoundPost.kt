package com.example.myapplication.data.model

import java.util.UUID

data class LostFoundPost(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val status: String,
    val title: String,
    val campus: String,
    val location: String,
    val date: String,
    val note: String = "",
    val isDemo: Boolean = false,
    val personName: String = "",
    val documentNumber: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUri: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor(
        type: String,
        status: String,
        title: String,
        campus: String,
        location: String,
        date: String,
        note: String = "",
        isDemo: Boolean,
        personName: String = "",
        documentNumber: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        imageUri: String = ""
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
        latitude = latitude,
        longitude = longitude,
        imageUri = imageUri,
        createdAt = System.currentTimeMillis()
    )
}
