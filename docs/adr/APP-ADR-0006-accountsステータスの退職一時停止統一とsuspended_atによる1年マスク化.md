# APP-ADR-0006: accounts.statusに4値設計（deactivated追加）と非adminからのsuspended/deactivated除外

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-06-20

## 関連

- Supersedes: なし
- Superseded by: なし
- 関連: [APP-ADR-0001](APP-ADR-0001-テーブル設計共通方針.md)（`suspended_at` カラムのテーブル設計共通方針との整合）

## 背景

`accounts.status` の `suspended` は「管理者による一時停止（可逆的）」として設計していた（UC-A7: アカウント停止・停止解除）。しかし以下の課題が浮上した。

1. **退職ユーザーの扱いが未定義**: 「一時停止（問題行動・不備対応）」と「退職による退会（恒久的）」を同じ `suspended` に混在させると、管理者が退職者を誤って復活させるリスクがある。
2. **廃止ユーザーが通常検索に出る**: `suspended_at` から1年経過したアカウントをマスクするだけでは、管理者の通常検索に廃止ユーザーが混在し続ける。明示的に絞り込まない限り廃止ユーザーを見えないようにしたい。
3. **非adminからの可視性**: `suspended` アカウントは非admin（`general` / `sales`）の検索・一覧に表示すべきでない。

## 決定

`accounts.status` を以下の4値設計とする。

| 値 | 意味 | 遷移元 |
|---|---|---|
| `provisional` | 仮登録 | Google SSO初回ログイン時（自動） |
| `active` | 本登録済み | `provisional` → 本登録申込みで遷移 |
| `suspended` | 停止中 | `active` → 管理者操作（停止解除で `active` に戻る） |
| `deactivated` | 廃止 | `suspended` → 1年経過後に自動遷移 |

具体的な決定事項:

1. **`suspended` に退職と一時停止を一本化する**: 「管理者による一時停止」と「退職による退会」を区別しない。いずれも管理者が `POST /api/v1/accounts/{accountId}/suspend` で停止する。
2. **`suspended` → `deactivated` の自動遷移**: Spring Boot の `@Scheduled` アノテーションを用いた日次タスクで `suspended_at < NOW() - INTERVAL '1 year'` な行を `status = 'deactivated'` に更新する。独立したバッチ基盤は不要。
3. **非adminからは `suspended` / `deactivated` を除外する**: `general` / `sales` ロールの検索・一覧クエリは `WHERE status = 'active'` のみを対象とする。他コンテキスト（経歴書・星取表）の検索でも同様に `active` のみを対象とする。
4. **adminのデフォルト検索は `suspended` まで**: admin の一覧・検索クエリのデフォルトは `WHERE status IN ('active', 'suspended')` とし、`deactivated` は除外する。
5. **adminが明示的に `status=deactivated` を指定した場合のみ表示**: `GET /api/v1/accounts?status=deactivated` のように明示的にフィルタした場合のみ `deactivated` アカウントを返す。
6. **`deactivated` アカウントの `name` / `email` は常にマスク**: `deactivated` 状態のアカウントは個人情報フィールド（`name` / `email`）を `"***"` 等で伏せて返す。

## 代替案

### 代替案A: `suspended_at` からリアルタイム判定のみ（APP-ADR-0006 初版）

- 短所: 廃止ステータスがDB上で明示されないため、検索除外フィルタの実装が複雑になる（`WHERE NOT (status = 'suspended' AND suspended_at < ...)` のような条件が必要）。廃止状態がAPIレスポンスにしか現れない。
- 変更理由: `deactivated` を DB 上の値とすることで、フィルタを単純な `status IN (...)` に統一できる。

### 代替案B: 廃止ステータス名として `withdrawn` を採用する

- 不採用理由: `withdrawn`（退会・撤回）は自発的な離脱を連想しやすいが、今回の遷移はシステムが自動的に行う。`active` の対語として `deactivated`（無効化）がより意味的に適切なため、`deactivated` を採用した。

### 代替案C: バッチ専用基盤（Quartz / Spring Batch）

- 短所: 日次1クエリのためだけに別途スケジューラ基盤を導入するのは過剰。Spring `@Scheduled` で十分。

### 代替案D: lazy update（読み取り時にDB更新）

- 短所: Read 操作が Write を引き起こす設計は副作用が大きく、テストやトランザクション管理が複雑になるため不採用。

## 影響

- `accounts.status` が4値に拡張: `provisional` / `active` / `suspended` / `deactivated`
- V1 マイグレーションのコメント・CHECK 制約を更新し、`deactivated` を有効値として追加する
- 非admin 向け query は `WHERE status = 'active'` でフィルタする
- admin 向けデフォルト query は `WHERE status IN ('active', 'suspended')` でフィルタする
- `deactivated` アカウントを返す場合は `name` / `email` を常にマスクする
- `@Scheduled` タスクを `AccountService`（または専用の `AccountDeactivationService`）に実装する（日次実行、`@Transactional` を付与）

## 今後の見直しポイント

- `deactivated` → 物理削除（GDPRや社内規定等での要件化）が必要になった場合は本ADRをSupersedeし、削除ポリシーを新ADRで定義する。
- `@Scheduled` タスクのタイムゾーン・実行時刻・リトライ要件が問題になった場合（チーム規模拡大・マルチインスタンス運用等）は、Quartz 等の専用スケジューラへの移行を検討する。
- マスク対象フィールド（現在は `name` / `email`）の範囲を変更する場合は本ADRを更新する。
