# パッケージ構成規約

根拠ADR: [APP-ADR-0008](../adr/APP-ADR-0008-DDD-CQRSアーキテクチャ原則の採用.md)（DDD + Clean Architecture + CQRS）

---

## 目次

- [レイヤー構成](#レイヤー構成)
- [パッケージツリー（アカウント・ロールドメイン例）](#パッケージツリーアカウントロールドメイン例)
- [各レイヤーの責務](#各レイヤーの責務)
- [UseCase の DI 登録（@Configuration）](#usecase-の-di-登録configuration)
- [Enum の配置と活用](#enum-の配置と活用)
- [命名規則](#命名規則)

---

## レイヤー構成

```
presentation/   ← Spring MVC（Controller）。HTTP の入出力のみ担当
usecase/        ← ユースケース（POJO）。業務フローの調整役
domain/         ← エンティティ・値オブジェクト・集約・Enum・リポジトリインターフェース
infrastructure/ ← リポジトリ実装・MyBatis Mapper・外部サービスアダプタ
```

依存方向: `presentation → usecase → domain ← infrastructure`

`infrastructure` は `domain` のインターフェース（Repository）を実装する。  
`usecase` は `domain` のインターフェースのみを参照し、`infrastructure` の具象クラスを直接参照しない。

---

## パッケージツリー（アカウント・ロールドメイン例）

```
src/main/kotlin/com/kakehashi/
├── domain/
│   └── account/
│       ├── Account.kt              # 集約（エンティティ）
│       ├── AccountId.kt            # 値オブジェクト（AZ0000 形式）
│       ├── AccountStatus.kt        # Enum（ステータス遷移ルールを持つ）
│       ├── RoleCode.kt             # Enum（権限コード）
│       └── AccountRepository.kt   # リポジトリインターフェース（ポート）
├── usecase/
│   └── account/
│       ├── RegisterAccountUseCase.kt    # UC-A3: 本登録申込み（Command）
│       ├── EditAccountUseCase.kt        # UC-A4: アカウント情報編集（Command）
│       ├── AssignRolesUseCase.kt        # UC-A6: 権限付与・変更（Command）
│       ├── SuspendAccountUseCase.kt     # UC-A7: 停止（Command）
│       ├── UnsuspendAccountUseCase.kt   # UC-A7: 停止解除（Command）
│       ├── ListAccountsQuery.kt         # UC-A5: 一覧検索（Query）
│       └── GetAccountQuery.kt           # 詳細取得（Query）
├── infrastructure/
│   └── account/
│       ├── AccountRepositoryImpl.kt     # AccountRepository の実装（Command 用）
│       └── AccountMapper.kt             # MyBatis Mapper（Query 用 DTO マッピング）
├── presentation/
│   └── account/
│       └── AccountController.kt        # 全エンドポイント（@RestController）
└── config/
    └── AccountUseCaseConfig.kt          # UseCase の @Bean 定義（@Configuration）
```

---

## 各レイヤーの責務

### domain/

- フレームワーク非依存（Spring / MyBatis のアノテーション・型を持ち込まない）
- **エンティティ**: 識別子（`AccountId`）を持ち、状態遷移のルールを持つ
- **値オブジェクト**: 不変。等価性は値で判断（`AccountId` 等）
- **Enum**: ドメイン概念を型で表現し、業務ルールをメソッドとして実装する（後述）
- **リポジトリインターフェース**: `save` / `findById` 等を定義。実装は `infrastructure` 層

### usecase/

- 1クラス = 1ユースケース（UC-A3 なら `RegisterAccountUseCase`）
- `@Service` は付与しない（POJO）。DI 登録は `config/` の `@Configuration` で行う
- Command 系: ドメイン集約を経由して状態変更する
- Query 系: `AccountMapper`（MyBatis）を直接呼び出し DTO を返す（集約を経由しない）

### infrastructure/

- `AccountRepositoryImpl`: `AccountRepository` インターフェースを実装する Spring Bean
- `AccountMapper`: MyBatis の `@Mapper`。Query 系 UseCase から直接呼ばれる

### presentation/

- `@RestController` で HTTP リクエスト/レスポンスの変換のみを担当
- UseCase を呼び出し、結果を HTTP レスポンスに変換する
- バリデーション（`@Valid`）はここで行う

---

## UseCase の DI 登録（@Configuration）

UseCase クラスには Spring アノテーションを付与しない。`config/` に集約した `@Configuration` クラスで `@Bean` として登録する。

```kotlin
// usecase/account/RegisterAccountUseCase.kt
// Spring に依存しない純粋な Kotlin クラス
class RegisterAccountUseCase(
    private val accountRepository: AccountRepository
) {
    fun execute(accountId: AccountId): Account {
        // ...
    }
}
```

```kotlin
// config/AccountUseCaseConfig.kt
@Configuration
class AccountUseCaseConfig {

    @Bean
    fun registerAccountUseCase(repo: AccountRepository): RegisterAccountUseCase =
        RegisterAccountUseCase(repo)

    @Bean
    fun editAccountUseCase(repo: AccountRepository): EditAccountUseCase =
        EditAccountUseCase(repo)

    // ... 他の UseCase も同様に登録
}
```

**メリット**:
- UseCase の単体テストで Spring Context が不要（`RegisterAccountUseCase(mockRepo)` で直接生成できる）
- ドメイン・ユースケース層が Spring に依存しないことが型システムで保証される

---

## Enum の配置と活用

ドメイン概念を表す Enum は `domain/{context}/` に配置し、業務ルールをメソッドとして実装する。

```kotlin
// domain/account/AccountStatus.kt
enum class AccountStatus {
    PROVISIONAL, ACTIVE, SUSPENDED, DEACTIVATED;

    /** ログイン可否 */
    fun canLogin(): Boolean = this == ACTIVE

    /** 指定ステータスへの遷移が有効か */
    fun canTransitionTo(next: AccountStatus): Boolean = when (this) {
        PROVISIONAL  -> next == ACTIVE
        ACTIVE       -> next == SUSPENDED
        SUSPENDED    -> next == ACTIVE || next == DEACTIVATED
        DEACTIVATED  -> false
    }

    /** 一般検索の対象か（active のみ返す） */
    fun isSearchable(): Boolean = this == ACTIVE
}
```

```kotlin
// domain/account/RoleCode.kt
enum class RoleCode(val code: String) {
    ADMIN("admin"),
    VIEW_PERSONAL_INFO("view_personal_info");

    companion object {
        fun fromCode(code: String): RoleCode =
            entries.firstOrNull { it.code == code }
                ?: throw IllegalArgumentException("Unknown role code: $code")
    }
}
```

UseCase や Controller での分岐を Enum 側に集約することで、業務ルールの変更を1箇所に閉じ込められる。

---

## 命名規則

| 種別 | 命名パターン | 例 |
|---|---|---|
| エンティティ | `{エンティティ名}` | `Account` |
| 値オブジェクト | `{概念名}` | `AccountId` |
| Enum | `{概念名}` | `AccountStatus`, `RoleCode` |
| リポジトリI/F | `{エンティティ名}Repository` | `AccountRepository` |
| リポジトリ実装 | `{エンティティ名}RepositoryImpl` | `AccountRepositoryImpl` |
| Command UseCase | `{動詞}{対象}UseCase` | `RegisterAccountUseCase` |
| Query UseCase | `{動詞}{対象}Query` | `ListAccountsQuery`, `GetAccountQuery` |
| MyBatis Mapper | `{エンティティ名}Mapper` | `AccountMapper` |
| Controller | `{エンティティ名}Controller` | `AccountController` |
| DI 設定クラス | `{エンティティ名}UseCaseConfig` | `AccountUseCaseConfig` |
