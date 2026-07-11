# REST API 設計書 — アカウント・ロールドメイン

> ステータス: 初版作成済み

対象ユースケース: UC-A1〜A7（[ui-flows.md 1章](../../requirements/ui-flows.md#1-認証アカウントコンテキスト)）
根拠テーブル: `accounts` / `roles` / `account_roles`（[data-models.md 1章](../../requirements/data-models.md#1-認証アカウントコンテキスト)）
参照ADR: [APP-ADR-0001](../../adr/APP-ADR-0001-テーブル設計共通方針.md) / [APP-ADR-0003](../../adr/APP-ADR-0003-経歴書のマスク範囲-コンタクト経路-ファイル出力範囲のスコープ判断.md) / [APP-ADR-0007](../../adr/APP-ADR-0007-rolesをpermissionベースに再定義しvisibility_rulesを廃止.md) / [APP-ADR-0008](../../adr/APP-ADR-0008-DDD-CQRSアーキテクチャ原則の採用.md) / [APP-ADR-0009](../../adr/APP-ADR-0009-APIパスにバージョンプレフィックスを含めない.md) / [APP-ADR-0014](../../adr/APP-ADR-0014-JWT戦略-自前JWT発行を採用.md)

---

## 目次

- [設計方針](#設計方針)
  - [CQRSの適用](#cqrsの適用)
  - [認証・認可](#認証認可)
  - [accounts.account_id の形式](#accountsaccount_id-の形式)
  - [アカウントステータス設計と可視性制御](#アカウントステータス設計と可視性制御)
  - [楽観ロック](#楽観ロック)
- [ロール・アクセス制御の概要](#ロールアクセス制御の概要)
- [Command（更新系）エンドポイント](#command更新系エンドポイント)
  - [UC-A1: Google SSOログイン（仮登録・自動プロビジョニング）](#uc-a1-google-ssoログイン仮登録自動プロビジョニング)
  - [UC-A3: 本登録申込み](#uc-a3-本登録申込み)
  - [UC-A4: アカウント情報編集（本人）](#uc-a4-アカウント情報編集本人)
  - [UC-A6: 権限付与・変更（管理者）](#uc-a6-権限付与変更管理者)
  - [UC-A7: アカウント停止・停止解除（管理者）](#uc-a7-アカウント停止停止解除管理者)
- [Query（参照系）エンドポイント](#query参照系エンドポイント)
  - [UC-A5: アカウント一覧・検索（管理者）](#uc-a5-アカウント一覧検索管理者)
  - [アカウント詳細取得](#アカウント詳細取得)
- [エンドポイント一覧（サマリ）](#エンドポイント一覧サマリ)

---

## 設計方針

### CQRSの適用

[APP-ADR-0008](../../adr/APP-ADR-0008-DDD-CQRSアーキテクチャ原則の採用.md) に従い、Command と Query を明確に分離する（[要件定義 4章](../../requirements/README.md#4-アーキテクチャ原則) 参照）。

| 分類 | 処理の流れ | 主な用途 |
|---|---|---|
| Command（更新系） | Controller → ドメイン集約（`Account`）→ Repository → DB | アカウント状態遷移・ロール変更等の状態変化を伴う操作 |
| Query（参照系） | Controller → MyBatis Mapper（JOINクエリ）→ DTO → レスポンス | アカウント一覧・詳細の参照（ドメイン層をバイパス） |

### 認証・認可

自前JWT発行方式を採用する（[APP-ADR-0014](../../adr/APP-ADR-0014-JWT戦略-自前JWT発行を採用.md)）。認証フローは以下の3段構成:

1. `POST /api/auth/google/callback` で Google ID トークン（`idToken`）を受け取り、バックエンドが Google JWKS を用いて署名検証する。
2. 検証後、`sub` クレームを SHA-256 等でハッシュ化し `accounts.google_sub_hash` と照合する（平文の `sub` はサーバー側で保持しない）。一致するアカウントが存在しない場合は JIT プロビジョニング（仮登録）を行う。
3. バックエンドが `accountId` クレームを埋め込んだ**自前JWT**を発行し、レスポンスで返す（UC-A1 のレスポンス参照）。

これ以降のリクエストは、この自前JWTを `Authorization: Bearer <JWT>` ヘッダーで送信する（Google ID トークンそのものは Bearer トークンとして使用しない。この点は [APP-ADR-0014](../../adr/APP-ADR-0014-JWT戦略-自前JWT発行を採用.md) が却下した代替案「Google id_token Bearer 方式」との違いに留意すること）。

Spring Security のカスタムフィルターが自前JWTを検証し、`SecurityContextHolder` に `accountId` をセットする。ロール制御は Spring Security の `@PreAuthorize` で実施する（`accountId` から権限を解決する具体的な認可ロジックは exec-plan 0007 のスコープ）。

### accounts.account_id の形式

`AZ0000` 形式（プレフィックス付き連番）の text 型。パスパラメータとして使用する（APP-ADR-0001 決定3）。

### アカウントステータス設計と可視性制御

`accounts.status` は以下の4値とする（[APP-ADR-0006](../../adr/APP-ADR-0006-accounts.statusに4値設計（deactivated追加）と非adminからのsuspended-deactivated除外.md)）。

| status | 意味 | 遷移 |
|---|---|---|
| `provisional` | 仮登録 | Google SSO初回ログイン時（自動） |
| `active` | 本登録済み | 本登録申込みで遷移 |
| `suspended` | 停止中 | 管理者操作（停止解除で `active` に戻る）。退職も含む |
| `deactivated` | 廃止 | `suspended_at` から1年後に `@Scheduled` 日次タスクで自動遷移 |

**検索可視性:**

| ケース | 返される status |
|---|---|
| デフォルト（status 未指定） | `active` のみ |
| `admin` 権限あり + `status=suspended` 明示 | `suspended` のみ |
| `admin` 権限あり + `status=active,suspended` 明示 | `active` + `suspended` |
| `admin` 権限あり + `status=deactivated` 明示 | `deactivated`（`name`/`email` は常にマスク） |

`suspended` / `deactivated` は `admin` 権限なしでは明示指定しても返さない（サーバー側で強制 `active` フィルタ）。

**`deactivated` アカウントのマスク**: `name` / `email` を `"***"` 等で伏せて返す。

### 楽観ロック

`accounts.version` を使用する。更新系リクエストのボディには `version` を必須とし、サーバー側の現在値と不一致の場合は `409 Conflict` を返す。

---

## ロール・アクセス制御の概要

`roles` テーブルは「できること（Permission）」のマスタ（[APP-ADR-0007](../../adr/APP-ADR-0007-rolesをpermissionベースに再定義しvisibility_rulesを廃止.md)）。`account_roles` でアカウントと権限を紐づけ、エンドポイントごとに必要な権限の有無で認可判定を行う。

| `roles.code` | `roles.name` | 概要 |
|---|---|---|
| `admin` | 管理業務 | 権限付与・停止・復活・他ユーザー情報変更 |
| `view_personal_info` | 個人情報表示 | 他ユーザーの `nearest_station` / `final_education` 閲覧 |

権限なしのアカウントは自分のアカウント情報の参照・編集のみ可能。権限は管理者（`admin` 権限保持者）が UC-A6 で明示的に付与する。

---

## Command（更新系）エンドポイント

---

### UC-A1: Google SSOログイン（仮登録・自動プロビジョニング）

- **メソッド・パス**: `POST /api/auth/google/callback`
- **認証**: 不要（Google OAuthコールバック受信）
- **アクセス制御**: 全ロール（未登録含む）
- **処理概要（JITプロビジョニング・自前JWT発行、[APP-ADR-0014](../../adr/APP-ADR-0014-JWT戦略-自前JWT発行を採用.md)）**:
  1. Googleから受け取った `idToken` をバックエンドで Google JWKS を用いて署名検証する。
  2. 検証成功後、`sub` クレームをハッシュ化して `accounts.google_sub_hash` と照合する。一致するアカウントが存在しない場合（初回ログイン）は `accounts.status = 'provisional'` でアカウントを仮登録する（UC-A2 相当の内部処理）。
  3. アカウント状態（`status`）に応じて処理を分岐する。
     - `provisional` / `active`: `accountId` クレームを埋め込んだ自前JWTを発行し、フロントエンドへのリダイレクト先とあわせて返す（`provisional` は本登録申込み画面、`active` はマイページ）。
     - `suspended` / `deactivated`: 自前JWTは発行せず、ログインを拒否する（エラー応答）。退職者は `suspended` 扱いで同様に拒否する。

- **リクエスト（ボディ）**:
  ```
  {
    "idToken": string   // Google id_token
  }
  ```

- **レスポンス 200**（`status` が `provisional` または `active` の場合のみ。`suspended` / `deactivated` はログイン拒否のため 4xx を返す）:
  ```
  {
    "accountId": string,          // AZ0000 形式（新規仮登録時は採番済み値）
    "status": "provisional" | "active",
    "accessToken": string,        // バックエンド発行の自前JWT（accountIdクレームを含む）。
                                   // 以降のリクエストは Authorization: Bearer <accessToken> ヘッダーで送信する
    "redirectTo": string          // フロントエンドがリダイレクトすべきパス
  }
  ```

- **レスポンス 4xx**:
  - `401 Unauthorized`: `idToken` の署名検証失敗（Google JWKS 照合失敗）
  - `403 Forbidden`: `status = 'suspended'` または `status = 'deactivated'` のアカウントのログイン試行（自前JWTは発行しない）
  - `422 Unprocessable Entity`: `idToken` のフォーマット不正、ドメインチェック失敗（環境変数で指定した会社ドメイン以外のGoogleアカウント）

- **根拠 UC**: UC-A1, UC-A2（内部処理）
- **備考**: UC-A2（仮登録）はこのコールバック内のシステム内部処理であり、独立したエンドポイントは持たない。自前JWTの署名鍵・有効期限・リフレッシュ方針は本設計書のスコープ外（[APP-ADR-0014](../../adr/APP-ADR-0014-JWT戦略-自前JWT発行を採用.md)「影響」参照、認証基盤実装時に決定）。

---

### UC-A3: 本登録申込み

- **メソッド・パス**: `POST /api/accounts/me/registration`
- **認証**: 必要（`status = 'provisional'` のアカウントのみ）
- **アクセス制御**: 全ロール（ただし仮登録状態のアカウントのみ実行可能）
- **処理概要**: 本登録申込みを受けてシステムが自動的に `accounts.status` を `provisional` から `active` へ遷移させる（UC-A4 相当の自動処理、管理者承認なし）。デフォルト権限の付与はなし（権限は管理者が UC-A6 で明示的に付与する）。

- **リクエスト（ボディ）**: なし（認証トークンから本人を特定）

- **レスポンス 200**:
  ```
  {
    "accountId": string,    // AZ0000 形式
    "status": "active"
  }
  ```

- **レスポンス 4xx**:
  - `401 Unauthorized`: 未認証
  - `409 Conflict`: すでに `active` または `suspended` の場合（二重申込み防止）

- **根拠 UC**: UC-A3, UC-A4（自動処理）

---

### UC-A4: アカウント情報編集（本人）

- **メソッド・パス**: `PATCH /api/accounts/me`
- **認証**: 必要（全ユーザー、本人のみ）
- **アクセス制御**: 認証済みアカウント（権限不問）
- **処理概要**: 本人が自分のアカウント表示名を編集する。`accounts.name` を更新する。`version` による楽観ロックを適用する。

- **リクエスト（ボディ）**:
  ```
  {
    "name": string,      // 表示名（空文字列は不可）
    "version": integer   // 楽観ロック用バージョン（現在値と一致しない場合は 409）
  }
  ```

- **レスポンス 200**:
  ```
  {
    "accountId": string,
    "name": string,
    "email": string,
    "status": string,
    "version": integer   // 更新後のバージョン値
  }
  ```

- **レスポンス 4xx**:
  - `400 Bad Request`: `name` が空文字列、または `version` が未指定
  - `401 Unauthorized`: 未認証
  - `409 Conflict`: `version` 不一致（楽観ロック競合）

- **根拠 UC**: UC-A4

---

### UC-A6: 権限付与・変更（管理者）

- **メソッド・パス**: `PUT /api/accounts/{accountId}/roles`
- **認証**: 必要（`admin` 権限のみ）
- **アクセス制御**: `admin` 権限保持者のみ
- **処理概要**: 対象アカウントの `account_roles` を全置換する。リクエストで `true` を指定した権限を `account_roles` に挿入し、`false` を指定した権限の行を削除する。`accounts.version` による楽観ロックを適用する。

- **パスパラメータ**:
  - `accountId`: string（AZ0000 形式）

- **リクエスト（ボディ）**:
  ```
  {
    "admin":           boolean,  // 管理業務権限の付与（true）/ 剥奪（false）
    "viewPersonalInfo": boolean, // 個人情報表示権限の付与（true）/ 剥奪（false）
    "version": integer           // accounts.version の現在値
  }
  ```
  > 全権限を必ず指定する。省略不可（全置換のため）。

- **レスポンス 200**:
  ```
  {
    "accountId": string,
    "roles": [
      {
        "roleId": string,    // UUID
        "code": string,      // "admin" | "view_personal_info"
        "name": string
      }
    ],
    "version": integer       // 更新後のバージョン値
  }
  ```

- **レスポンス 4xx**:
  - `400 Bad Request`: `admin` または `viewPersonalInfo` が未指定
  - `401 Unauthorized`: 未認証
  - `403 Forbidden`: `admin` 権限なしのアカウントによるアクセス
  - `404 Not Found`: 指定した `accountId` が存在しない
  - `409 Conflict`: `version` 不一致（楽観ロック競合）

- **根拠 UC**: UC-A6

---

### UC-A7: アカウント停止・停止解除（管理者）

#### アカウント停止

- **メソッド・パス**: `POST /api/accounts/{accountId}/suspend`
- **認証**: 必要（`admin` 権限のみ）
- **アクセス制御**: `admin` 権限保持者のみ
- **処理概要**: `accounts.status` を `suspended`、`accounts.suspended_at` に現在日時を設定する。`accounts.version` による楽観ロックを適用する。

- **パスパラメータ**:
  - `accountId`: string（AZ0000 形式）

- **リクエスト（ボディ）**:
  ```
  {
    "version": integer   // accounts.version の現在値
  }
  ```

- **レスポンス 200**:
  ```
  {
    "accountId": string,
    "status": "suspended",
    "suspendedAt": string,   // ISO 8601 形式
    "version": integer
  }
  ```

- **レスポンス 4xx**:
  - `401 Unauthorized`: 未認証
  - `403 Forbidden`: `admin` ロール以外のアクセス
  - `404 Not Found`: 指定した `accountId` が存在しない
  - `409 Conflict`: `version` 不一致（楽観ロック競合）、または対象アカウントが既に `suspended` 状態

- **根拠 UC**: UC-A7

#### アカウント停止解除

- **メソッド・パス**: `POST /api/accounts/{accountId}/unsuspend`
- **認証**: 必要（`admin` 権限のみ）
- **アクセス制御**: `admin` 権限保持者のみ
- **処理概要**: `accounts.status` を `active`、`accounts.suspended_at` を NULL に設定する。`accounts.version` による楽観ロックを適用する。

- **パスパラメータ**:
  - `accountId`: string（AZ0000 形式）

- **リクエスト（ボディ）**:
  ```
  {
    "version": integer   // accounts.version の現在値
  }
  ```

- **レスポンス 200**:
  ```
  {
    "accountId": string,
    "status": "active",
    "suspendedAt": null,
    "version": integer
  }
  ```

- **レスポンス 4xx**:
  - `401 Unauthorized`: 未認証
  - `403 Forbidden`: `admin` ロール以外のアクセス
  - `404 Not Found`: 指定した `accountId` が存在しない
  - `409 Conflict`: `version` 不一致（楽観ロック競合）、または対象アカウントが `suspended` 状態でない

- **根拠 UC**: UC-A7

---

## Query（参照系）エンドポイント

Query 側は MyBatis でドメイン層をバイパスし、JOIN クエリ結果を直接 DTO にマッピングして返す（[APP-ADR-0008](../../adr/APP-ADR-0008-DDD-CQRSアーキテクチャ原則の採用.md)）。

---

### UC-A5: アカウント一覧・検索（管理者）

- **メソッド・パス**: `GET /api/accounts`
- **認証**: 必要（`admin` 権限のみ）
- **アクセス制御**: `admin` 権限保持者のみ
- **処理概要**: `accounts` テーブルを検索条件でフィルタし、アカウント一覧を返す（軽量）。MyBatis でドメイン層をバイパスして直接 DTO にマッピングする（Query 側）。`roleCode` 指定時のみ `account_roles` / `roles` を JOIN する（MyBatis `<if>` で動的に追加）。

- **クエリパラメータ**:
  ```
  name:     string   （任意）表示名の部分一致
  status:   string[] （任意）"active" | "suspended" | "deactivated" の複数指定可（カンマ区切り）
                      未指定時は active のみ。suspended / deactivated は admin 権限なしでは無効
  roleCode: string   （任意）"admin" | "view_personal_info"（account_roles JOIN が発生）
  page:     integer  （任意、デフォルト 0）ページ番号（0始まり）
  size:     integer  （任意、デフォルト 20）1ページの件数
  ```

- **レスポンス 200**:
  ```
  {
    "content": [
      {
        "accountId": string,
        "name": string,
        "status": string
      }
    ],
    "totalElements": integer,
    "totalPages": integer,
    "page": integer,
    "size": integer
  }
  ```

  > 詳細情報（email / roles / suspendedAt / version 等）は `GET /api/accounts/{accountId}` で取得する。
  > `deactivated` アカウントの `name` は `"***"` でマスクして返す。

- **レスポンス 4xx**:
  - `401 Unauthorized`: 未認証
  - `403 Forbidden`: `admin` 権限なしのアクセス

- **根拠 UC**: UC-A5

---

### アカウント詳細取得

UC-A5 の詳細画面、マイページ表示、UC-A6・UC-A7 の操作前 `version` 取得を兼ねる単一エンドポイント。

- **メソッド・パス**: `GET /api/accounts/{accountId}`
- **認証**: 必要（認証済みアカウント）
- **アクセス制御**:
  - `admin` 権限あり: 任意の `accountId` にアクセス可
  - `admin` 権限なし: JWT から特定した本人の `accountId` のみ可（他人は `403`）
- **処理概要**: `accounts` に `account_roles` / `roles` を JOIN して1件取得する（MyBatis DTO マッピング、Query 側）。

- **パスパラメータ**:
  - `accountId`: string（AZ0000 形式）

- **レスポンス 200**:
  ```
  {
    "accountId": string,
    "name": string,
    "email": string,
    "status": string,
    "roles": [
      {
        "roleId": string,   // UUID
        "code": string,
        "name": string
      }
    ],
    "suspendedAt": string | null,
    "version": integer,     // 楽観ロック用。UC-A4/A6/A7 のリクエストボディに使用する
    "createdAt": string,
    "updatedAt": string,
    "updatedBy": string     // 最終更新者の accountId
  }
  ```

  > `deactivated` アカウントは `name` / `email` を `"***"` でマスクして返す。

- **レスポンス 4xx**:
  - `401 Unauthorized`: 未認証
  - `403 Forbidden`: 他人の `accountId` に `admin` 権限なしでアクセス
  - `404 Not Found`: 指定した `accountId` が存在しない

- **根拠 UC**: UC-A3（本登録後の状態確認）、UC-A4（編集前の情報取得）、UC-A5（詳細画面）、UC-A6・UC-A7 の操作前 version 取得

---

## エンドポイント一覧（サマリ）

| 分類 | メソッド | パス | 認証 | アクセス制御 | 根拠 UC |
|---|---|---|---|---|---|
| Command | POST | `/api/auth/google/callback` | 不要 | — | UC-A1, UC-A2 |
| Command | POST | `/api/accounts/me/registration` | 必要 | 仮登録状態の全ユーザー | UC-A3, UC-A4 |
| Command | PATCH | `/api/accounts/me` | 必要 | 認証済み（権限不問） | UC-A4 |
| Command | PUT | `/api/accounts/{accountId}/roles` | 必要 | admin 権限 | UC-A6 |
| Command | POST | `/api/accounts/{accountId}/suspend` | 必要 | admin 権限 | UC-A7 |
| Command | POST | `/api/accounts/{accountId}/unsuspend` | 必要 | admin 権限 | UC-A7 |
| Query | GET | `/api/accounts` | 必要 | admin 権限 | UC-A5 |
| Query | GET | `/api/accounts/{accountId}` | 必要 | 本人 or admin 権限 | UC-A3, UC-A4, UC-A5, UC-A6, UC-A7 |

> 上表の「認証」欄における `Authorization: Bearer <JWT>` は、いずれもバックエンドが UC-A1 で発行する自前JWTを指す（[APP-ADR-0014](../../adr/APP-ADR-0014-JWT戦略-自前JWT発行を採用.md)）。
