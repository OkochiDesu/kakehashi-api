package com.kakehashi.domain.account

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * RoleCode 値オブジェクトの単体テスト
 *
 * 設計書No：-
 * ADRNo：APP-ADR-0001
 *
 * ★観点
 * DB 保存文字列から RoleCode enum への変換（fromCode）が正確であることを保証する。
 * 未定義ロールコードが混入した場合に早期失敗で検出し、権限判定の不整合を防ぐ。
 *
 * ★★正常系★★
 * 《観　点》定義済みロールコードが正しく enum に変換されることの確認
 * 《テスト》"admin" を変換すると ADMIN を返す
 * 《テスト》"view_personal_info" を変換すると VIEW_PERSONAL_INFO を返す
 *
 * ★★異常系★★
 * 《観　点》未定義のロールコードが混入した場合に即座に検出できることの確認
 * 《テスト》未知の code は IllegalArgumentException をスローする
 */
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
