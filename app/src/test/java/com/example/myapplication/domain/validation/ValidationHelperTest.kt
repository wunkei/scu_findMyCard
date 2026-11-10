package com.example.myapplication.domain.validation

import org.junit.Assert.*
import org.junit.Test

class ValidationHelperTest {

    // --- 学号验证测试 ---

    @Test
    fun validateStudentId_validCases() {
        assertNull(ValidationHelper.validateStudentId("2023141240001"))
        assertNull(ValidationHelper.validateStudentId(" 2024141520123 "))
        assertNull(ValidationHelper.validateStudentId("2022141520099@scu.edu.cn"))
        assertNull(ValidationHelper.validateStudentId("2021141520088@SCU.EDU.CN"))
    }

    @Test
    fun validateStudentId_blank_returnsError() {
        val error = ValidationHelper.validateStudentId("")
        assertNotNull(error)
        assertTrue(error!!.contains("请输入四川大学学号"))

        val spaceError = ValidationHelper.validateStudentId("   ")
        assertNotNull(spaceError)
    }

    @Test
    fun validateStudentId_wrongLength_returnsError() {
        val shortError = ValidationHelper.validateStudentId("202314124000") // 12 digits
        assertNotNull(shortError)
        assertTrue(shortError!!.contains("13位纯数字"))

        val longError = ValidationHelper.validateStudentId("20231412400011") // 14 digits
        assertNotNull(longError)
        assertTrue(longError!!.contains("13位纯数字"))
    }

    @Test
    fun validateStudentId_nonDigits_returnsError() {
        val error = ValidationHelper.validateStudentId("202314124000A")
        assertNotNull(error)
        assertTrue(error!!.contains("13位纯数字"))
    }

    @Test
    fun validateStudentId_invalidYear_returnsError() {
        val pastYearError = ValidationHelper.validateStudentId("1998141240001")
        assertNotNull(pastYearError)
        assertTrue(pastYearError!!.contains("入学年份须在2000至"))

        val futureYearError = ValidationHelper.validateStudentId("2099141240001")
        assertNotNull(futureYearError)
        assertTrue(futureYearError!!.contains("入学年份须在2000至"))
    }

    // --- 身份证号码验证测试 ---

    @Test
    fun validateIdCard_validStandardCases() {
        // 110101199003072375: 194 % 11 = 7 -> checkChar is '5'
        assertNull(ValidationHelper.validateIdCard("110101199003072375"))
        assertNull(ValidationHelper.validateIdCard(" 110101199003072375 "))
        // 110101199003072383: 196 % 11 = 9 -> checkChars[9] is '3'
        assertNull(ValidationHelper.validateIdCard("110101199003072383"))
    }

    @Test
    fun validateIdCard_wrongCheckChar_returnsError() {
        val error = ValidationHelper.validateIdCard("110101199003072376") // expected 5, given 6
        assertNotNull(error)
        assertTrue(error!!.contains("身份证校验码不匹配"))
    }

    @Test
    fun validateIdCard_invalidLength_returnsError() {
        val error = ValidationHelper.validateIdCard("11010119900307237")
        assertNotNull(error)
        assertTrue(error!!.contains("必须为完整的18位"))
    }

    @Test
    fun validateIdCard_invalidDate_returnsError() {
        // Month 13
        val monthError = ValidationHelper.validateIdCard("110101199013072375")
        assertNotNull(monthError)

        // Day 32
        val dayError = ValidationHelper.validateIdCard("110101199003322375")
        assertNotNull(dayError)
    }

    @Test
    fun validateIdCard_isOptionalFlag() {
        assertNull(ValidationHelper.validateIdCard("", isOptional = true))
        assertNull(ValidationHelper.validateIdCard("   ", isOptional = true))
        assertNotNull(ValidationHelper.validateIdCard("", isOptional = false))
    }

    // --- 姓名验证测试 ---

    @Test
    fun validateName_validCases() {
        assertNull(ValidationHelper.validateName("张三"))
        assertNull(ValidationHelper.validateName("诸葛孔明"))
        assertNull(ValidationHelper.validateName("买买提·吐尔逊"))
        assertNull(ValidationHelper.validateName("John Doe"))
    }

    @Test
    fun validateName_invalidCases() {
        assertNotNull(ValidationHelper.validateName(""))
        assertNotNull(ValidationHelper.validateName(" "))
        assertNotNull(ValidationHelper.validateName("张")) // 少于2个字符
        assertNotNull(ValidationHelper.validateName("张三123")) // 包含数字
        assertNotNull(ValidationHelper.validateName("李四@#!")) // 包含非法字符
    }

    // --- 手机号码验证测试 ---

    @Test
    fun validatePhone_validCases() {
        assertNull(ValidationHelper.validatePhone("13800138000"))
        assertNull(ValidationHelper.validatePhone("15812345678"))
        assertNull(ValidationHelper.validatePhone("18688889999"))
        assertNull(ValidationHelper.validatePhone("19912345678"))
    }

    @Test
    fun validatePhone_invalidCases() {
        assertNotNull(ValidationHelper.validatePhone("12800138000")) // 第二位必须是3-9
        assertNotNull(ValidationHelper.validatePhone("1380013800")) // 10位
        assertNotNull(ValidationHelper.validatePhone("138001380000")) // 12位
        assertNotNull(ValidationHelper.validatePhone("abcdefghijk"))
    }

    @Test
    fun validatePhone_isOptional() {
        assertNull(ValidationHelper.validatePhone("", isOptional = true))
        assertNotNull(ValidationHelper.validatePhone("", isOptional = false))
    }

    // --- 脱敏逻辑测试 ---

    @Test
    fun maskPersonName_variousLengths() {
        assertEquals("", ValidationHelper.maskPersonName(""))
        assertEquals("*", ValidationHelper.maskPersonName("张"))
        assertEquals("*四", ValidationHelper.maskPersonName("李四"))
        assertEquals("张*丰", ValidationHelper.maskPersonName("张三丰"))
        assertEquals("诸**明", ValidationHelper.maskPersonName("诸葛孔明"))
        assertEquals("买*****逊", ValidationHelper.maskPersonName("买买提·吐尔逊"))
    }

    @Test
    fun maskDocumentNumber_idCard() {
        // 身份证显示前6位以及最后2位
        val id = "110101199003072375"
        val masked = ValidationHelper.maskDocumentNumber("居民身份证", id)
        assertEquals("110101**********75", masked)
        assertTrue(masked.startsWith("110101"))
        assertTrue(masked.endsWith("75"))
        assertEquals(18, masked.length)
    }

    @Test
    fun maskDocumentNumber_campusCard() {
        // 校园卡只显示前面4位和最后3位
        val card = "2023141240001"
        val masked = ValidationHelper.maskDocumentNumber("校园卡", card)
        assertEquals("2023******001", masked)
        assertTrue(masked.startsWith("2023"))
        assertTrue(masked.endsWith("001"))
        assertEquals(13, masked.length)
    }

    @Test
    fun maskDocumentNumber_otherDocuments() {
        // 其他证件只显示前面2位以及最后2位
        val doc = "EB1234567"
        val masked = ValidationHelper.maskDocumentNumber("港澳通行证", doc)
        assertEquals("EB*****67", masked)
        assertTrue(masked.startsWith("EB"))
        assertTrue(masked.endsWith("67"))
        assertEquals(9, masked.length)
    }

    @Test
    fun maskDocumentNumber_blankInput() {
        assertEquals("", ValidationHelper.maskDocumentNumber("校园卡", ""))
        assertEquals("", ValidationHelper.maskDocumentNumber("身份证", "   "))
    }
}
