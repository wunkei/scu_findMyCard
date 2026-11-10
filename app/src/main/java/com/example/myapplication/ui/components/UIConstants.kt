package com.example.myapplication.ui.components

import androidx.compose.ui.graphics.Color

data class DocumentTypeOption(
    val name: String,
    val identifierLabel: String,
    val identifierPlaceholder: String,
    val personNameLabel: String,
    val personNamePlaceholder: String
)

val documentTypeOptions = listOf(
    DocumentTypeOption("校园卡", "学号", "请输入13位学号", "姓名", "请输入卡面姓名"),
    DocumentTypeOption("居民身份证", "身份证号码", "请输入18位身份证号码", "证件姓名", "请输入证件上的姓名"),
    DocumentTypeOption("港澳居民来往内地通行证", "通行证号码", "请输入完整通行证号码", "证件姓名", "请输入证件上的姓名"),
    DocumentTypeOption("护照", "护照号码", "请输入完整护照号码", "护照姓名", "请输入证件上的姓名"),
    DocumentTypeOption("其他证件", "证件号码", "请输入完整证件号码", "证件姓名", "请输入证件上的姓名")
)

val ScreenInk = Color(0xFF1E2D3D)
val ScreenMuted = Color(0xFF718093)
val BrandBlue = Color(0xFF2878C8)
val SuccessGreen = Color(0xFF278566)
val WarmOrange = Color(0xFFDE8958)
val CardWhite = Color.White
