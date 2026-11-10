package com.example.myapplication

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ValidationHelper {

    /**
     * 学号验证：
     * 1. 纯数字且长度为13位
     * 2. 前4位代表入学年份，必须在 2000 至当前年份之间
     */
    fun validateStudentId(studentId: String): String? {
        val clean = studentId.replace(" ", "").trim().removeSuffix("@scu.edu.cn").removeSuffix("@SCU.EDU.CN")
        if (clean.isBlank()) {
            return "请输入四川大学学号"
        }
        if (clean.length != 13 || !clean.all { it.isDigit() }) {
            return "学号必须为13位纯数字（您输入了 ${clean.length} 位）"
        }
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val yearPrefix = clean.take(4).toIntOrNull()
        if (yearPrefix == null || yearPrefix < 2000 || yearPrefix > currentYear) {
            return "学号前4位入学年份须在2000至${currentYear}之间（当前为 $yearPrefix）"
        }
        return null
    }

    /**
     * 身份证号码验证：
     * 1. 完整18位
     * 2. 前17位为数字，第18位为数字或大写X
     * 3. 出生日期需合法且不晚于当前年份
     * 4. 符合 ISO 7064:1983.MOD 11-2 校验码规则
     */
    fun validateIdCard(idCard: String): String? {
        val clean = idCard.replace(" ", "").trim().uppercase()
        if (clean.isBlank()) {
            return "请输入完整的18位居民身份证号码"
        }
        if (clean.length != 18) {
            return "身份证号码长度必须为完整的18位（当前输入了 ${clean.length} 位）"
        }

        val formatRegex = Regex("^[1-9]\\d{5}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9X]$")
        if (!formatRegex.matches(clean)) {
            return "请输入规范的18位居民身份证号码（前17位数字，末位数字或X）"
        }

        // 验证出生日期有效性
        val birthDateStr = clean.substring(6, 14)
        try {
            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).apply {
                isLenient = false
            }
            val birthDate = sdf.parse(birthDateStr) ?: return "身份证包含的出生日期不合法"
            val now = Calendar.getInstance().time
            if (birthDate.after(now)) {
                return "身份证出生日期不能晚于当前日期"
            }
        } catch (_: Exception) {
            return "身份证包含的出生日期不合法"
        }

        // 校验码 (ISO 7064:1983.MOD 11-2)
        val weights = intArrayOf(7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
        val checkChars = charArrayOf('1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2')
        var sum = 0
        for (i in 0 until 17) {
            sum += (clean[i] - '0') * weights[i]
        }
        val expectedCheckChar = checkChars[sum % 11]
        if (clean[17] != expectedCheckChar) {
            return "身份证校验码不匹配，请仔细核对末位字符（标准校验位应为 $expectedCheckChar）"
        }

        return null
    }

    /**
     * 姓名验证：不少于2个字符，合法中文或英文名字
     */
    fun validateName(name: String): String? {
        val clean = name.trim()
        if (clean.isBlank()) {
            return "请输入真实姓名"
        }
        if (clean.length < 2) {
            return "姓名长度不能少于2个字符"
        }
        val regex = Regex("^[\\u4e00-\\u9fa5a-zA-Z·\\s]{2,20}$")
        if (!regex.matches(clean)) {
            return "请输入规范的中文或英文姓名"
        }
        return null
    }

    /**
     * 手机号码格式验证（选填项，但若输入则需符合11位格式）
     */
    fun validatePhone(phone: String, isOptional: Boolean = true): String? {
        val clean = phone.replace(" ", "").trim()
        if (clean.isBlank()) {
            return if (isOptional) null else "请输入手机号码"
        }
        val regex = Regex("^1[3-9]\\d{9}$")
        if (!regex.matches(clean)) {
            return "手机号格式不正确，请输入11位中国大陆手机号码"
        }
        return null
    }
}
