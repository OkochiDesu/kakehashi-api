# APP-ADR-0001: テーブル設計共通方針

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-06-15

## 関連

- Supersedes: なし
- Superseded by: なし

## 背景

要件定義フェーズで `docs/requirements/data-models.md` の各コンテキスト（認証・アカウント／メッセージ／星取表／経歴書／ファイル）のテーブル設計を進める中で、テーブルごとに監査カラムの有無・PKカラム名・PKの型と採番方式・表示順カラムの運用が個別に決まると、実装時の一貫性が損なわれる懸念があった。

これを避けるため、`data-models.md` 0章「設計方針」として全テーブルに共通する設計ルールを先に確定し、各コンテキストのテーブル定義（1〜5章）はこのルールに従う形で記述した（[README.md 8章 2026-06-15の意思決定](../requirements/README.md#8-意思決定ログ)）。

## 決定

`docs/requirements/data-models.md` 0章で、以下を全テーブル共通の設計方針として確定する。

1. **監査カラムの付与**: 全テーブルに `created_at` / `updated_at`（timestamp）、`created_by` / `updated_by`（text）を付与する。`created_by` / `updated_by` には、ユーザー操作時は `accounts.account_id`、バッチ処理（SQL直接投入等）時は処理を識別するリクエストIDなどの文字列を格納する。値の種類が混在するためFK制約は持たせない。
2. **PKカラム名の統一**: 各テーブルのPKカラム名は `id` という単独名を使わず、`<エンティティ名（単数形）>_id`（例: `account_id`, `role_id`, `skill_category_id`）に統一する。
3. **PKの型・採番方式**:
   - `accounts.account_id` は `AZ0000` 形式（仮フォーマット、確定はマイグレーション実装時のADRで行う）のtext型とし、社員コード相当の識別子としてアプリ側で採番する。
   - 上記以外のテーブルのPKはUUID（v7想定）とし、アプリ側で採番する（DDDの集約ルートIDをドメイン層で確定させる方針と整合）。
4. **display_orderの運用**: 区分・項目の表示順は1始まりの連番とする（疎な整数刻みは使わない）。並び替え時は対象範囲内の複数行の `display_order` を更新する。

対象ファイル: [docs/requirements/data-models.md 0章](../requirements/data-models.md#0-設計方針)

## 代替案

### 代替案A: テーブルごとに個別に設計ルールを決める

- 長所: テーブルの特性に応じた最適化が可能
- 短所: 監査カラムの有無やPK命名がテーブル間でばらつき、実装・レビューのコストが増える。MyBatisのマッパー・共通処理（楽観ロック、編集履歴ログ）の横展開も難しくなる。

### 代替案B: display_orderを疎な整数（10/100刻み）で運用する

- 長所: 並び替え時に更新対象が局所化される（間に挿入する場合、周辺の値だけ変更すれば済む）
- 短所: 刻み幅という暗黙的な意味を持つ値が運用上の制約になり、刻みを使い切った場合の再採番が必要になる。Step1の規模では恩恵が小さいと判断し、1始まりの連番（並び替え時は対象範囲内の複数行を更新）を採用した。

### 代替案C: PKをすべてUUIDで統一する（accountsも含む）

- 長所: PK型・採番ルールが完全に統一される
- 短所: `accounts.account_id` は社員コード相当の識別子として人間が識別・参照する用途があり、`AZ0000` 形式のtext型が運用上望ましいと判断した。

## 影響

- 1〜5章の全テーブル定義（`accounts`, `roles`, `account_roles`, `visibility_rules`, `skill_categories`, `skill_master_items`, `level_categories`, `level_master_items`, `user_skills`, `resumes`, `resume_qualifications`, `resume_projects`, `resume_project_skills` 等）は、本方針に従った監査カラム・PK命名・PK型で記述されている。
- `accounts.account_id` の `AZ0000` 形式は「仮フォーマット」であり、確定フォーマット（桁数・連番の境界処理等）はFlywayマイグレーション実装時に別ADRで確定する。
- UUID v7はライブラリ選定・生成方法（DB側 `gen_random_uuid()` 系か、アプリ側ライブラリか）を実装フェーズで確定する必要がある。
- 編集系テーブルの楽観ロック（`version`）・編集履歴ログ（`entity_change_logs`）は本ADRの対象外とし、`data-models.md` 0章の別項目として記述されている（[quality-standards.md 1章・6章](../requirements/quality-standards.md#1-機能適合性functional-suitability)参照）。

## 今後の見直しポイント

- `accounts.account_id` の `AZ0000` 形式を確定する際（マイグレーション実装時）に、本ADRを更新または新規ADRでSupersedeする。
- UUID v7の生成方式（DB関数 / アプリ側ライブラリ）を確定した際は、本ADRまたは実装関連の別ADRに記録する。
- 実装を進める中で監査カラム・PK命名規則の例外が必要になった場合は、本ADRを見直す。
