package com.kakehashi.domain.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoleCodeTest {
    @Test
    fun `fromCode - adminを変換するとADMINを返す`() {
        assertEquals(RoleCode.ADMIN, RoleCode.fromCode("admin"))
    }

    @Test
    fun `fromCode - view_personal_infoを変換するとVIEW_PERSONAL_INFOを返す`() {
        assertEquals(RoleCode.VIEW_PERSONAL_INFO, RoleCode.fromCode("view_personal_info"))
    }

    @Test
    fun `fromCode - 未知のcodeはIllegalArgumentExceptionをスローする`() {
        assertThrows<IllegalArgumentException> { RoleCode.fromCode("unknown_role") }
    }
}
