---
globs:
  - "**/*Test.kt"
---

# テストコード規約

`*Test.kt` ファイルを編集・作成するときに適用するルール。
詳細なテスト方針は [kdoc-and-test-policy.md](../../docs/conventions/kdoc-and-test-policy.md) を参照。

## アサーション

- **`assert()` を使わない**: JVM の `-ea` フラグが無効のとき評価されず常に成功する。`assertEquals` / `assertTrue` / `assertThrows` / `assertNull` 等 JUnit5 アサーションを使うこと（pre-commit でも検出する）
- **`assertNull()` より `assertNull(actual)` の形式を使う**: メッセージ引数は省略してよいが、対象は明示する

## TDD（テスト先行）

新規実装・バグ修正ともに**テストを先に書いてから実装する**。
- バグ修正: バグを再現する失敗テストを書く → 修正する → テストが通ることを確認する
- 機能追加: 期待動作を定義するテストを書く → 実装する

## 正常系テストの観点

- 状態遷移・戻り値が仕様通りであることを確認する
- **状態を変更する UseCase の正常系テストでは、`updatedAt` が更新されていること・`updatedBy` が操作者 ID になっていることを同じテストケース内で検証すること**（別テストに分離しない）
- バグ修正で「正常系の仕様を最初から検証しきれていなかった」と判明した場合は、新規テストを追加するのではなく既存の正常系テストを強化する

## 異常系テストの観点

- 楽観ロック競合: `update()` が 0件 → `OptimisticLockException`。`currentVersion` の値が正しいことまで検証すること（`findById()` 2回目呼び出しに別バージョンを返す `returnsMany` を使用）
- 権限エラー: `isAdmin = false` / `operatorIsAdmin = false` → `ForbiddenOperationException`
- ステータス遷移不正: `canTransitionTo()` が false → `InvalidStatusTransitionException`
- Not Found: 対象が存在しない → `AccountNotFoundException`
- 不正な入力値: 未定義の `roleCode` など → `IllegalArgumentException`

## バリデーション変更時

バリデーション挙動を変更した diff（`runCatching.getOrNull()` 廃止・例外スロー追加・型変換ロジック変更等）がある場合は、**同じ diff 内**にエラーパステストを追加・更新すること。

## Testcontainers 統合テスト

Testcontainers を使う `@SpringBootTest` 統合テストでは **`@ServiceConnection` を使う**こと。
Spring Boot が DataSource・Flyway・MyBatis を自動設定する（APP-ADR-0013）。

**注意**: `@JdbcTest` / `@AutoConfigureTestDatabase` は Spring Boot 4.x で削除済み。`@DynamicPropertySource` / `ContainerDatabaseDriver`（JDBC URL 方式）は使わない。

### テストクラス

```kotlin
@Testcontainers
@SpringBootTest
@ActiveProfiles("integration-test")
@Transactional
class MyIntegrationTest {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired
    lateinit var repository: MyRepositoryImpl
    // tests...
}
```

### 設定ファイル（`application-integration-test.properties`）

```properties
# @ServiceConnection が DataSource・Flyway・MyBatis を自動設定するため接続設定は不要
spring.flyway.enabled=true
```

- `@ActiveProfiles("integration-test")` を付与し devcontainer の DB に接続しないようにすること
- `@Transactional` を付与してテスト間のデータ汚染を防ぐこと
- **ローカル実行制約**: devcontainer では Docker socket が `root:root` 権限のため Testcontainers が起動できない。DB 系統合テストは CI（GitHub Actions）で確認すること（詳細: [testcontainers-jvmstatic-kotlin.md](../../docs/troubleshooting/testcontainers-jvmstatic-kotlin.md)）

詳細: [testcontainers-jvmstatic-kotlin.md](../../docs/troubleshooting/testcontainers-jvmstatic-kotlin.md)

## テストクラス KDoc（テストケース目次）

**新規テストクラス作成時・既存テストクラスへのテスト追加時**、クラスの KDoc に以下のフォーマットで
テストケース一覧を記載・更新すること。**テスト追加と同一コミットで KDoc を更新すること**（drift 防止）。

### フォーマット

```
/**
 * XxxUseCase 単体テスト
 *
 * 設計書No：UC-XX
 * ADRNo：APP-ADR-XXXX
 *
 * ★★全体観点★★
 * このテストクラスが何を保証するか・なぜ存在するかを1〜3文で説明する。
 * UI直結・権限の根幹・楽観ロック等、重要度が伝わるように書く。
 *
 * 《観　点》[何の動作・仕様を確認するか]
 * 《テスト》正常系： [正常ケースのテストケース名]
 * 《テスト》異常系： [同じ観点の異常ケース（あれば）]
 *
 * 《観　点》[別の観点（異常系のみでもよい）]
 * 《テスト》異常系： [テストケース名]
 */
```

### ルール
- `★★正常系★★` / `★★異常系★★` のセクション分割は**使わない**。`《観　点》` 単位でグループ化し、同一観点内は正常系を先に、異常系を後に並べる
- 1つの `《観　点》` に複数の `《テスト》` をまとめてよい（同一観点の複数ケース）
- **`《テスト》` の記述はテストメソッド名（backtick 内）と完全一致させること**。`正常系：` / `異常系：` プレフィックスを含む場合はそのまま含める
  - 良い例: `《テスト》正常系： grantAdminRole=true で admin ロールが付与される`
  - 悪い例: `《テスト》grantAdminRole=true で admin ロールが付与される`（プレフィックス抜け）
  - 悪い例: `《テスト》正常系: grantAdminRole=true で admin ロールが付与される`（コロン種別違い）
- 正常系・異常系の区別がない場合（ArchUnit 等）は `★★ルール★★` 等の適切なヘッダーを使う

## テスト命名

テスト名は「`正常系/異常系： 条件 → 期待結果`」の形式で書く。
- 良い例: `` `正常系： grantAdminRole=true で admin ロールが付与される` ``
- 良い例: `` `異常系： operatorIsAdmin=false は ForbiddenOperationException` ``
