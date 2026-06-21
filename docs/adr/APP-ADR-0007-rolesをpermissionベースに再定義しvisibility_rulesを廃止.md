# APP-ADR-0007: `roles` を権限（Permission）ベースに再定義し `visibility_rules` を廃止

## 目次

[ステータス](#ステータス) / [関連](#関連) / [背景](#背景) / [決定](#決定) / [代替案](#代替案) / [影響](#影響) / [今後の見直しポイント](#今後の見直しポイント)

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-06-20

## 関連

- Supersedes: [APP-ADR-0003](APP-ADR-0003-経歴書のマスク範囲-コンタクト経路-ファイル出力範囲のスコープ判断.md) の決定4（ロール別可視範囲）のみ。決定1〜3は引き続き有効
- Superseded by: なし

## 背景

APP-ADR-0003 決定4では `roles` テーブルに職種ベースのロール（`general` / `sales` / `admin`）を定義し、`visibility_rules` テーブルでロールごとの可視範囲を管理する設計を採用していた。

この設計には以下の問題が判明した。

1. **職種と権限の混在**: `general`（一般エンジニア）と `sales`（営業）は職種であり、`admin` は権限である。これらを同一テーブルに並べると「誰がどの操作をできるか」が不明確になる。たとえば「営業職の管理者」や「エンジニア職の管理者」を表現できない。
2. **`visibility_rules` の冗長性**: `visibility_rules` は「ロールが決まれば可視範囲も一意に決まる」静的マッピングであり、JOIN が増えるだけで設計の柔軟性メリットがなかった。個人情報の閲覧可否は権限の有無（`view_personal_info` が `account_roles` に存在するか）で直接判定できる。
3. **拡張性の制限**: 職種ベースでは「どの職種でも付与できる横断的な権限」を表現しにくい。

## 決定

### 1. `roles` テーブルを権限（Permission）マスタとして再定義する

`roles` テーブルのコードは職種ではなく「できること（権限）」を表す。Step1の初期データは以下の2件のみとする。

| `roles.code` | `roles.name` | 概要 |
|---|---|---|
| `admin` | 管理業務 | 権限付与・停止・復活・他ユーザー情報変更 |
| `view_personal_info` | 個人情報表示 | 他ユーザーの `nearest_station` / `final_education` の閲覧 |

### 2. `account_roles` でアカウントと権限を紐づける

`account_roles` はアカウントに付与された権限の集合とする。認可判定は「対象操作に必要な権限が `account_roles` に存在するか」で行う。

- XX管理画面を操作するとき → `admin` 権限が `account_roles` に存在すれば許可
- 他ユーザーの個人情報を閲覧するとき → `view_personal_info` 権限が `account_roles` に存在すれば許可

### 3. `visibility_rules` テーブルを廃止する

個人情報（`resumes.nearest_station` / `resumes.final_education`）の閲覧可否は `view_personal_info` 権限の有無で直接判定する。`visibility_rules` テーブル・関連クエリ・初期データは削除する。

### 4. 本登録時（UC-A3）のデフォルト権限付与はなし

旧設計では本登録時に `general` ロールを自動付与していたが、新設計ではデフォルト権限付与は行わない。権限は管理者が明示的に UC-A6 で付与する。

### 5. UC-A6 のリクエスト形式を権限フラグ方式に変更する

ロール配列（`roleCodes: [...]`）ではなく、権限ごとのブールフラグを明示的に指定する形式に変更する。

```json
{
  "admin": boolean,
  "viewPersonalInfo": boolean,
  "version": integer
}
```

## 代替案

### 代替案A: 職種ベース設計を維持する（旧設計）

- 不採用理由: `general` / `sales` は職種であり `admin` は権限であるという概念的な不整合が残る。「営業職の管理者」など職種と権限を組み合わせるユースケースを表現できない。

### 代替案B: `visibility_rules` を存続させ、`roles` のみ権限ベースに変える

- 不採用理由: `visibility_rules` はロールが確定すれば可視範囲も一意に決まるため、JOIN の増加に対して得られる柔軟性がない。将来 `target_category` が増える場合は `roles` テーブルに権限コードを追加する運用で同等に対応できる。

### 代替案C: `account_roles` を廃止し `accounts` に権限フラグを持たせる

- 不採用理由: 権限が増えるたびに `accounts` テーブルにカラムを追加する必要があり、スキーマ変更コストが高い。`account_roles` + `roles` 方式の方が追加権限に柔軟に対応できる。

## 影響

- `roles` テーブルの初期データが `general` / `sales` / `admin` → `admin` / `view_personal_info` に変わる
- `visibility_rules` テーブルを削除（Flyway マイグレーションで反映）
- UC-A3（本登録）でのデフォルトロール付与処理を削除
- UC-A6（ロール付与・変更）のリクエスト形式を変更（`roleCodes` 配列 → フラグ方式）
- 経歴書の個人情報マスク判定ロジックが `visibility_rules` JOIN から `account_roles` の `view_personal_info` 有無チェックに変わる
- `docs/requirements/data-models.md` の roles / account_roles / visibility_rules 説明を更新
- `docs/design/api/account-role.md` のロール設計・ロール一覧テーブルを更新
- APP-ADR-0003 決定1のマスク解除判定ロジック（`visibility_rules` 参照部分）は本ADRの決定3に従い置き換わる

## 今後の見直しポイント

- 権限の種類が増えた場合は `roles` テーブルに新しいコードを追加し、`account_roles` で付与する運用とする
- 「職種（エンジニア / 営業）」の情報が将来必要になった場合（例: 職種ごとの集計、UI表示）は `accounts` テーブルへの `job_type` カラム追加を別途 ADR で検討する
