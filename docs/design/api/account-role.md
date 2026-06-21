# REST API 設計書 — アカウント・ロールドメイン

> ステータス: 初版作成済み

対象ユースケース: UC-A1〜A7（[ui-flows.md 1章](../../requirements/ui-flows.md#1-認証アカウントコンテキスト)）
根拠テーブル: `accounts` / `roles` / `account_roles`（[data-models.md 1章](../../requirements/data-models.md#1-認証アカウントコンテキスト)）
参照ADR: [APP-ADR-0001](../../adr/APP-ADR-0001-テーブル設計共通方針.md) / [APP-ADR-0003](../../adr/APP-ADR-0003-経歴書のマスク範囲-コンタクト経路-ファイル出力範囲のスコープ判断.md) / [APP-ADR-0007](../../adr/APP-ADR-0007-rolesをpermissionベースに再定義しvisibility_rulesを廃止.md)

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
  - [UC-A6: ロール付与・変更（管理者）](#uc-a6-ロール付与変更管理者)
  - [UC-A7: アカウント停止・停止解除（管理者）](#uc-a7-アカウント停止停止解除管理者)
- [Query（参照系）エンドポイント](#query参照系エンドポイント)
  - [UC-A5: アカウント一覧・検索（管理者）](#uc-a5-アカウント一覧検索管理者)
  - [アカウント詳細取得（管理者）](#アカウント詳細取得管理者)
  - [自分のアカウント情報取得（本人）](#自分のアカウント情報取得本人)

---

## 設計方針

### CQRSの適用

[docs/requirements/README.md 4章](../../requirements/README.md#4-アーキテクチャ原則adr化予定の方針)の方針に従い、Command と Query を明確に分離する。

| 分類 | 処理の流れ | 主な用途 |
|---|---|---|
| Command（更新系） | Controller → ドメイン集約（`Account`）→ Repository → DB | アカウント状態遷移・ロール変更等の状態変化を伴う操作 |
| Query（参照系） | Controller → MyBatis Mapper（JOINクエリ）→ DTO → レスポンス | アカウント一覧・詳細の参照（ドメイン層をバイパス） |

### 認証・認可

- Spring Security + Google OAuth2（OIDC）を前提とする。`sub` クレームをSHA-256等でハッシュ化し `accounts.google_sub_hash` と照合する（平文の `sub` はサーバー側で保持しない）。
- 認証が必要なエンドポイントは `Authorization: Bearer <JWT>` ヘッダーを要求する。
- ロール制御は Spring Security の `@PreAuthorize` で実施する。

### accounts.account_id の形式

`AZ0000` 形式（プレフィックス付き連番）の text 型。パスパラメータとして使用する（APP-ADR-0001 決定3）。

### アカウントステータス設計と可視性制御

`accounts.status` は以下の4値とする（[APP-ADR-0006](../../adr/APP-ADR-0006-accountsステータスの退職一時停止統一とsuspended_atによる1年マスク化.md)）。

| status | 意味 | 遷移 |
|---|---|---|
| `provisional` | 仮登録 | Google SSO初回ログイン時（自動） |
| `active` | 本登録済み | 本登録申込みで遷移 |
| `suspended` | 停止中 | 管理者操作（停止解除で `active` に戻る）。退職も含む |
| `deactivated` | 廃止 | `suspended_at` から1年後に `@Scheduled` 日次タスクで自動遷移 |

**検索可視性:**

| 権限 | 表示される status |
|---|---|
| 権限なし（デフォルト） | `active` のみ |
| `admin` 権限あり（デフォルト） | `active` / `suspended` |
| `admin` 権限あり（`status=deactivated` 明示） | `deactivated`（`name`/`email` は常にマスク） |

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

- **メソッド・パス**: `POST /api/v1/auth/google/callback`
- **認証**: 不要（Google OAuthコールバック受信）
- **アクセス制御**: 全ロール（未登録含む）
- **処理概要（JITプロビジョニング）**:
  1. Googleから受け取った `id_token`（または `code`）を検証し、`sub` クレームをハッシュ化して `accounts.google_sub_hash` と照合する。
  2. 一致するアカウントが存在しない場合（初回ログイン）は `accounts.status = 'provisional'` でアカウントを仮登録する（UC-A2 相当の内部処理）。
  3. アカウント状態（`status`）に応じてフロントエンドへのリダイレクト先を返す。
  - `provisional`: 本登録申込み画面
  - `active`: マイページ
  - `suspended`: エラー（ログイン拒否）。退職者も同じ扱い
  - `deactivated`: エラー（ログイン拒否）

- **リクエスト（ボディ）**:
  ```
  {
    "idToken": string   // Google id_token
  }
  ```

- **レスポンス 200**:
  ```
  {
    "accountId": string,          // AZ0000 形式（新規仮登録時は採番済み値）
    "status": "provisional" | "active" | "suspended" | "deactivated",
    "redirectTo": string          // フロントエンドがリダイレクトすべきパス
  }
  ```

- **レスポンス 4xx**:
  - `401 Unauthorized`: id_token の署名検証失敗
  - `403 Forbidden`: `status = 'suspended'` のアカウントのログイン試行
  - `422 Unprocessable Entity`: id_token のフォーマット不正、ドメインチェック失敗（環境変数で指定した会社ドメイン以外のGoogleアカウント）

- **根拠 UC**: UC-A1, UC-A2（内部処理）
- **備考**: UC-A2（仮登録）はこのコールバック内のシステム内部処理であり、独立したエンドポイントは持たない。

---

### UC-A3: 本登録申込み

- **メソッド・パス**: `POST /api/v1/accounts/me/registration`
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

- **メソッド・パス**: `PATCH /api/v1/accounts/me`
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

- **メソッド・パス**: `PUT /api/v1/accounts/{accountId}/roles`
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

- **メソッド・パス**: `POST /api/v1/accounts/{accountId}/suspend`
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

- **メソッド・パス**: `POST /api/v1/accounts/{accountId}/unsuspend`
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

Query 側は MyBatis でドメイン層をバイパスし、JOIN クエリ結果を直接 DTO にマッピングして返す（[docs/requirements/README.md 4章 CQRS](../../requirements/README.md#4-アーキテクチャ原則adr化予定の方針)）。

---

### UC-A5: アカウント一覧・検索（管理者）

- **メソッド・パス**: `GET /api/v1/accounts`
- **認証**: 必要（`admin` 権限のみ）
- **アクセス制御**: `admin` 権限保持者のみ
- **処理概要**: `accounts` に `account_roles` / `roles` を JOIN し、検索条件に応じてフィルタしてアカウント一覧を返す。MyBatis でドメイン層をバイパスして直接 DTO にマッピングする（Query 側）。

- **クエリパラメータ**:
  ```
  name:     string  （任意）表示名の部分一致
  email:    string  （任意）メールアドレスの部分一致
  status:   string  （任意）"active" | "suspended" | "deactivated"
             ※ 未指定時は active + suspended のみ返す。deactivated は明示指定が必要
  roleCode: string  （任意）"admin" | "view_personal_info"
  page:     integer （任意、デフォルト 0）ページ番号（0始まり）
  size:     integer （任意、デフォルト 20）1ページの件数
  ```

- **レスポンス 200**:
  ```
  {
    "content": [
      {
        "accountId": string,
        "name": string,
        "email": string,
        "status": string,
        "roles": [
          {
            "code": string,
            "name": string
          }
        ],
        "suspendedAt": string | null,
        "createdAt": string,
        "updatedAt": string
      }
    ],
    "totalElements": integer,
    "totalPages": integer,
    "page": integer,
    "size": integer
  }
  ```

  > マスク制御: `deactivated` アカウントは `name` / `email` を `"***"` でマスクして返す。`deactivated` はデフォルトでは返さず、`status=deactivated` を明示した場合のみ返す。

- **レスポンス 4xx**:
  - `401 Unauthorized`: 未認証
  - `403 Forbidden`: `admin` ロール以外のアクセス

- **根拠 UC**: UC-A5

---

### アカウント詳細取得（管理者）

UC-A5 の詳細画面（`AccountDetail`）に対応。UC-A6・UC-A7 の操作前に現在の `version` 値を取得する用途も兼ねる。

- **メソッド・パス**: `GET /api/v1/accounts/{accountId}`
- **認証**: 必要（`admin` 権限のみ）
- **アクセス制御**: `admin` 権限保持者のみ
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
    "version": integer,     // 楽観ロック用。UC-A6/A7 のリクエストボディに使用する
    "createdAt": string,
    "updatedAt": string,
    "updatedBy": string     // 最終更新者の accountId
  }
  ```

  > マスク制御: `deactivated` アカウントは `name` / `email` を `"***"` でマスクして返す。

- **レスポンス 4xx**:
  - `401 Unauthorized`: 未認証
  - `403 Forbidden`: `admin` ロール以外のアクセス
  - `404 Not Found`: 指定した `accountId` が存在しない

- **根拠 UC**: UC-A5（詳細画面）、UC-A6・UC-A7 の操作前の version 取得

---

### 自分のアカウント情報取得（本人）

マイページの表示や、本登録後のアカウント状態確認に使用する。

- **メソッド・パス**: `GET /api/v1/accounts/me`
- **認証**: 必要（全ユーザー、本人のみ）
- **アクセス制御**: 認証済みアカウント（権限不問）
- **処理概要**: 認証トークンから本人の `account_id` を特定し、`accounts` に `account_roles` / `roles` を JOIN して取得する（MyBatis DTO マッピング、Query 側）。

- **レスポンス 200**:
  ```
  {
    "accountId": string,
    "name": string,
    "email": string,
    "status": string,
    "roles": [
      {
        "code": string,
        "name": string
      }
    ],
    "version": integer,     // UC-A4（アカウント情報編集）のリクエストボディに使用する
    "createdAt": string,
    "updatedAt": string
  }
  ```

  > マスク制御: 本人情報のため全フィールドをマスクなしで返す。`suspendedAt` は本人には非公開（自分が停止されているとログインできないため、このエンドポイントでは返さない）。

- **レスポンス 4xx**:
  - `401 Unauthorized`: 未認証

- **根拠 UC**: UC-A4（編集前の情報取得）、UC-A3（本登録後の状態確認）

---

## エンドポイント一覧（サマリ）

| 分類 | メソッド | パス | 認証 | アクセス制御 | 根拠 UC |
|---|---|---|---|---|---|
| Command | POST | `/api/v1/auth/google/callback` | 不要 | — | UC-A1, UC-A2 |
| Command | POST | `/api/v1/accounts/me/registration` | 必要 | 仮登録状態の全ユーザー | UC-A3, UC-A4 |
| Command | PATCH | `/api/v1/accounts/me` | 必要 | 認証済み（権限不問） | UC-A4 |
| Command | PUT | `/api/v1/accounts/{accountId}/roles` | 必要 | admin 権限 | UC-A6 |
| Command | POST | `/api/v1/accounts/{accountId}/suspend` | 必要 | admin 権限 | UC-A7 |
| Command | POST | `/api/v1/accounts/{accountId}/unsuspend` | 必要 | admin 権限 | UC-A7 |
| Query | GET | `/api/v1/accounts` | 必要 | admin 権限 | UC-A5 |
| Query | GET | `/api/v1/accounts/{accountId}` | 必要 | admin 権限 | UC-A5, UC-A6, UC-A7 |
| Query | GET | `/api/v1/accounts/me` | 必要 | 認証済み（権限不問） | UC-A3, UC-A4 |
