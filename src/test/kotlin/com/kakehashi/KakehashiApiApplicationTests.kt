package com.kakehashi

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

// DB 接続が必要なため、CI/Testcontainers 環境なしでは実行できない
// Repository テスト追加時（Testcontainers 導入後）に有効化する
@Disabled("DB接続が必要なため Testcontainers 導入まで無効化")
@SpringBootTest
class KakehashiApiApplicationTests {
    @Test
    fun contextLoads() {
    }
}
