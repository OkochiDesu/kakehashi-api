# KDoc・テスト方針

根拠ADR: [APP-ADR-0008](../adr/APP-ADR-0008-DDD-CQRSアーキテクチャ原則の採用.md)

## 目次

- [KDoc フォーマット](#kdoc-フォーマット)
  - [ルール](#ルール)
  - [`@throws` の記述ルール](#throws-の記述ルール)
  - [エラーメッセージのルール](#エラーメッセージのルール)
- [コメント方針](#コメント方針)
- [テスト方針](#テスト方針)
  - [原則: TDD（テスト先行）](#原則-tddテスト先行)
  - [テスト種別と対象](#テスト種別と対象)
  - [UseCase テストの観点](#usecase-テストの観点)
  - [ファイル配置](#ファイル配置)

---

## KDoc フォーマット

公開メソッド・関数には以下のフォーマットで KDoc を付与する。

```kotlin
/**
 * （このメソッドが行うこと・一文で）
 *
 * 設計書No：UC-XX（なければ `-`）
 * ADRNo：APP-ADR-XXXX（なければ `-`）
 *
 * @param xxx 説明
 * @return 説明
 */
```

### ルール

- 1行目はメソッドが「何をするか」を動詞で始める（「取得する」「更新する」「遷移させる」等）
- 設計書No・ADRNo は必ず記載する（該当なしは `-`）
- `@param` / `@return` は自明でない場合のみ記載する（`id: AccountId` に「アカウントID」とだけ書くのは不要）。ただし **`interface` のメソッドおよびリポジトリ系の公開メソッドは `@param` を省略しない**（実装クラスとの対応追跡を容易にするため）
- クラス・インターフェース自体にも概要 KDoc を付与する

### `@throws` の記述ルール

- **説明文は実装の分岐条件と正確に一致させる**
  - 悪い例: `@throws InvalidStatusTransitionException ACTIVE以外の場合`
  - 良い例: `@throws InvalidStatusTransitionException canTransitionTo(ACTIVE) が false の場合`
- **実際にスローされる例外のみ列挙する**（漏れ・過剰記載に注意）

### エラーメッセージのルール

- **エラーメッセージは日本語で記述する**（`require()` / `check()` / RuntimeException のメッセージ文字列、`GlobalExceptionHandler` のフォールバック文字列すべてに適用）
  - 悪い例: `"Cannot transition from $status to ACTIVE"`
  - 良い例: `"${status} から ACTIVE への遷移は許可されていません"`
- 英語のエラーメッセージが残っている場合は REQUIRES_CHANGES として指摘する

---

## コメント方針

- **書くべきコメント**: なぜそう実装したか（非自明な制約・ADR起因のトレードオフ・意図的な例外処理）、複雑なロジックの説明
- **書かないコメント**: コードを読めば分かること（`// null チェック`、`// 更新する` 等）
- レビューは人間が行うため、レビュアーが文脈を理解するのに必要な情報を残す
- **インラインコメントは条件式ベースで書く**（列挙ではなく条件を書く）
  - 悪い例: `// active / suspended の場合は 409`
  - 良い例: `// canTransitionTo(ACTIVE) が false の場合は 409`

---

## テスト方針

### 原則: TDD（テスト先行）

新規実装時はテストを先に書いてから実装する。

### テスト種別と対象

| 種別 | 対象 | フレームワーク | Spring Context |
|---|---|---|---|
| **UseCase 単体テスト** | 全 UseCase クラス | JUnit5 + MockK | 不要（POJO） |
| **Controller テスト** | `AccountController` | `@WebMvcTest` + MockK | WebMvc 層のみ |
| **Repository テスト** | `AccountRepositoryImpl` / `AccountMapper.xml` | Testcontainers（PostgreSQL） | 必要 |

### UseCase テストの観点

各 UseCase に対して以下を最低限テストする。

- 正常系: 期待通りの状態遷移・戻り値
- 楽観ロック競合: `update()` が 0件 → `OptimisticLockException`
- 権限エラー: `isAdmin = false` → `ForbiddenOperationException`
- ステータス遷移不正: `canTransitionTo()` が false → `InvalidStatusTransitionException`
- Not Found: 対象アカウントが存在しない → `AccountNotFoundException`
- 不正な入力値: 未定義の `roleCode` など → `IllegalArgumentException` → `GlobalExceptionHandler` で 400 変換

### ファイル配置

```
src/test/kotlin/com/kakehashi/
├── domain/account/          # Enum・値オブジェクトのテスト
├── usecase/account/         # UseCase 単体テスト
└── presentation/account/    # Controller テスト（@WebMvcTest）
```
