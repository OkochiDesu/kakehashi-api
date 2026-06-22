# Project "Kakehashi" - プロジェクトコンテキスト & アーキテクチャ方針

要件定義フェーズで作成するドキュメント群（[ui-flows.md](ui-flows.md) / [data-models.md](data-models.md) / [quality-standards.md](quality-standards.md)）の前提となる、プロジェクトの目的・技術スタック・アーキテクチャ原則をまとめる。

重厚長大な仕様書ではなく、開発者が即座に実装へ落とし込める粒度を保つことを優先する。

## 目次

- [1. プロジェクト概要](#1-プロジェクト概要)
- [2. スコープ](#2-スコープ)
- [3. 技術スタック](#3-技術スタック)
- [4. アーキテクチャ原則](#4-アーキテクチャ原則)
- [5. このディレクトリの構成](#5-このディレクトリの構成)
- [6. フロントエンド関連ドキュメントの移行方針](#6-フロントエンド関連ドキュメントの移行方針)
- [7. イベントストーミング結果と業務ルール](#7-イベントストーミング結果と業務ルール)
- [8. 意思決定ログ](#8-意思決定ログ)

## 1. プロジェクト概要

- **目的**: 社内エンジニアのスキルシート更新の手間削減、技術スタックの棚卸し、スムーズなアサインを実現する社内ツール。
- **主要機能**:
  - Step1（今回の要件定義の対象）
    1. 経歴書のWeb化
    2. 星取表（技術スタック棚卸）
    3. 検索画面
  - Step2（Step1完成後に追加）
    4. AIレコメンド
- **現在の進捗**: Miroを用いた「イベントストーミング（カラーパズル）」による業務要件と集約（Aggregate）の整理が完了。ここから具体的な画面フロー、データモデル、非機能要件をMarkdownに落とし込むフェーズ。

## 2. スコープ

本リポジトリ（kakehashi-api）は**バックエンドAPI**を対象とする。Nuxt 3によるフロントエンドは別リポジトリで管理予定のため、本ディレクトリの要件定義ではAPI観点（エンドポイント、データモデル、認証・認可、非機能要件）を中心に記述する。画面フロー（[ui-flows.md](ui-flows.md)）はAPI設計のインプットとして必要な範囲に限定する。

## 3. 技術スタック

実装済みのバージョンは `build.gradle.kts` を正とする。

| 項目 | 内容 | 状態 |
|---|---|---|
| 言語/ランタイム | Kotlin 2.2.21 (Java 21 toolchain) | 導入済み |
| フレームワーク | Spring Boot 4.0.6 | 導入済み |
| Lint/Format | ktlint 1.5.0 (Spotless) | 導入済み |
| データベース | PostgreSQL 15 (JSONBを積極利用) | **導入済み**（[APP-ADR-0004](../adr/APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md)） |
| マイグレーション | Flyway | **導入済み**（[APP-ADR-0004](../adr/APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md)） |
| O/Rマッパー | MyBatis | **導入済み**（[APP-ADR-0004](../adr/APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md)） |
| フロントエンド | Nuxt 3, TypeScript | 別リポジトリで管理予定（本リポジトリのスコープ外） |
| インフラ/CI・CD | Dev Containers (VSCode), GitHub Actions | 導入済み |

> Spring Boot 4.x系を前提とするため、ライブラリ選定時はSpring Boot 3.x向け情報との互換性に注意する（例: `spring-boot-starter-webmvc` 等の新パッケージ構成）。

## 4. アーキテクチャ原則

以下は設計の前提とする方針。DDD/Clean Architecture と CQRS の原則は [APP-ADR-0008](../adr/APP-ADR-0008-DDD-CQRSアーキテクチャ原則の採用.md) として確定済み。

1. **DDD & Clean Architecture**
   ドメイン層（エンティティ、値オブジェクト）はSpring/MyBatisから完全に独立させる。
2. **CQRSの徹底**
   - 【Command(更新系)】: ドメインモデル（集約）を経由し、状態の変更（イベント）を伴う。
   - 【Query(参照系)】: ドメイン層をバイパスし、MyBatisからJSONBを直接DTOにマッピングして高速に画面へ返す。検索に「イベント」や「集約」は介在させない。
3. **認証・認可**
   - Google SSO（`sub` クレーム）を識別子としたJIT（Just-In-Time）プロビジョニング。
   - フロントエンドでのUI非表示に加え、バックエンドのControllerで厳格なロール制御（`@PreAuthorize`）を実施。
4. **単一情報源（Single Source of Truth）**
   - キャリアシートはバージョン管理の複雑化を避けるため、フォーマットはJSONスキーマ、データはPostgreSQLの `JSONB` カラムに格納。
   - Excel出力（Jxls-poi）を正とし、PDFはそれをヘッドレスLibreOfficeで変換出力する。
   - 設計ドキュメントはソースコードを正とし、MarkdownでGit管理する。

## 5. このディレクトリの構成

- [README.md](README.md)（本ファイル）: プロジェクトコンテキスト・アーキテクチャ方針
- [ui-flows.md](ui-flows.md): ユースケースと画面遷移（Mermaid）
- [data-models.md](data-models.md): RDB厳密カラム / JSONB柔軟カラムの境界整理
- [quality-standards.md](quality-standards.md): ISO/IEC 25010:2023に基づく品質目標
- [inputs/miro/README.md](inputs/miro/README.md): イベントストーミング結果（Miroエクスポート・生データ）

## 6. フロントエンド関連ドキュメントの移行方針

`docs/requirements/` 配下のドキュメントは、現在はバックエンドリポジトリで一元管理している。フロントエンドプロジェクト着手後、内容がフロントエンド領域に強く依存するもの（画面遷移、UI仕様など）は、フロント側プロジェクトとの二重管理を避けるため以下の方針で移行する。

- フロントエンドに関係する要件定義ドキュメントはフロント側プロジェクトへ移行する
- CI/CDでPDF化しNotebookLMで管理する
- MCPサーバを用意し、バックエンド側からフロントエンド関連情報が必要な場合はMCP経由で参照する

移行対象の例（フロントエンド着手時に再評価）:

- [ui-flows.md](ui-flows.md): 画面遷移図（移行対象濃厚）
- [data-models.md](data-models.md): JSONBスキーマ等、フロントエンドの型定義と重複する部分は移行を検討
- [quality-standards.md](quality-standards.md): UI/UX観点の品質目標は移行を検討

詳細な移行手順・MCPサーバ仕様は、フロントエンドプロジェクトの構成が決まった時点で具体化する。

## 7. イベントストーミング結果と業務ルール

Miroのイベントストーミング結果（[docs/requirements/inputs/miro/](inputs/miro/)）から判明した、ドキュメント化が必要な業務ルール。

- **アカウント本登録は自動処理**: 「アカウントの本登録を申し込む」（本人）が実行されたら、システムは必ず「アカウントを本登録する」処理を自動実行する。管理者の承認は介在しない（イベントハンドラ/ポリシーとして実装）。
- **経歴書とキャリアシートは別物**:
  - 経歴書 = エンジニアのスキルシート（主要機能①「経歴書のWeb化」に対応）
  - キャリアシート = エンジニアの目標設定シート（毎年内容・フォーマットが更新される）
  - キャリアシートコンテキスト（フォーマット管理・キャリアシート本体）はStep1の要件定義対象外とし、別項で検討する
- **フォーマットのバージョニング要件**: キャリアシートはフォーマットが年次更新されるが、過去フォーマット・そのフォーマット利用当時に登録されたデータは閲覧可能にする必要がある（JSONBスキーマによるバージョン管理、[data-models.md](data-models.md)で詳細化）。経歴書はフォーマット変更の頻度・粒度がキャリアシートと異なるため、同じバージョニング機構は適用せず、テーブルへのカラム追加マイグレーションで対応する（[data-models.md 4章](data-models.md#4-経歴書コンテキスト)）。
- **一括登録系コマンドはAPI化しない**: 「優先度低い（SQL直入れ想定）」とマークされたコマンド（星取表マスタ一括登録、星取表一括登録、キャリアシート一括登録等）は、Step1ではAPIを作らずSQL直接投入で対応する。
- **アカウント承認・コンタクトは営業経由を検討**: エンジニア間の直接コミュニケーションが難しいケースを想定し、メッセージコンテキストの「コンタクトをとる」は営業担当を介する運用を検討中。

## 8. 意思決定ログ

- 2026-06-14: 技術スタック記述を実態（Spring Boot 4.0.6, Kotlin 2.2.21）に合わせて更新し、MyBatis/PostgreSQL/Flywayは当時「導入予定」の方針として明記（その後2026-06-16に導入済みへ更新）。Nuxt 3フロントエンドは別リポジトリ管理前提とし、本リポジトリの要件定義はAPI観点に絞る。ドキュメント配置は新設の `docs/requirements/` 配下に統一。
- 2026-06-14: 主要機能を Step1（経歴書のWeb化・星取表・検索画面）と Step2（AIレコメンド）に分割。今回の要件定義はStep1を対象とし、AIレコメンドはStep1完成後に追加検討する。
- 2026-06-14: 画面遷移系ドキュメント（[ui-flows.md](ui-flows.md)）はフロントエンド領域が強く、フロントエンドプロジェクト作成時に二重管理となる懸念があるため、将来的にフロント側プロジェクトへ移行し、CI/CDでPDF化しNotebookLM管理＋MCPサーバ経由でバックエンドから参照する方針を記録（詳細はフロントエンド着手時に具体化）。
- 2026-06-14: Miroイベントストーミング結果（[inputs/miro/](inputs/miro/)）を取り込み、7章の業務ルールを記録。一括登録系コマンド（星取表マスタ/星取表/キャリアシートの一括登録）はStep1ではAPI化せずSQL直接投入で対応する方針を確定。
- 2026-06-14: キャリアシートコンテキスト（フォーマット管理・キャリアシート本体）はStep1の要件定義対象外とし、別途検討する。Step1は認証・アカウント・メッセージ・星取表・経歴書・ファイルの各コンテキストを対象とする。
- 2026-06-14: 「アカウント承認依頼を送信する」（承認待ちフロー）は、アカウント本登録が自動処理のため現状発生しない。Step1のメッセージコンテキストからは除外し、エンジニアへのコンタクト（UC-M1）のみを対象とする。
- 2026-06-14: `data-models.md`は永続化層（RDB/JSONB）のスキーマ設計を対象とし、ドメインモデル（エンティティ・値オブジェクト・集約の振る舞い）は実装フェーズで別途設計する「たたき台」と位置づけることを明記。
- 2026-06-14: `accounts.role`（単一enum）を廃止し、`roles`（ロール・区分マスタ）＋`account_roles`（多対多）＋`visibility_rules`（ロール別の項目可視性ルール）に拡張。経歴書のマスク制御（4章の未確定事項）と権限変更（UC-A6）を同じ仕組みで扱う方針とした。
- 2026-06-14: `accounts.google_sub`は平文を保持せず、`google_sub_hash`（決定的ハッシュ）として保存する方針に変更。ログイン時はGoogleの`sub`をハッシュ化して比較する。
- 2026-06-14: `skill_master_items` / `level_master_items`の`category`（区分）を、専用マスタテーブル（`skill_categories` / `level_categories`）に切り出してFK化。区分・項目それぞれの`display_order`は疎な整数（10/100刻み等）で運用し、並び替え時の更新範囲を抑える方針とした。`level_categories`により、レベル定義が領域（インフラ／アプリケーション等）ごとに異なる場合にも対応する。
- 2026-06-14: quality-standards.mdで識別した未確定事項を`data-models.md`へ反映。`accounts`に`suspended_at`（長期停止判定用）、編集対象テーブル（`accounts` / `skill_master_items` / `level_master_items` / `user_skills` / `user_skill_levels` / `resumes`）に`version`（楽観ロック）・`updated_by`（編集者記録）を追加。変更履歴は汎用の`entity_change_logs`で記録する方針とした（対象テーブル・粒度は実装フェーズで確定）。
- 2026-06-14: 検索画面に新たな検索条件を追加: 経歴書は「案件名×言語」、星取表は「言語（ツール）×レベル」で検索可能とし、いずれも単体検索も可能とする（[quality-standards.md 1章](quality-standards.md#1-機能適合性functional-suitability)）。星取表のエンジニアページ表示は、全項目を一画面に出すのではなくレイアウト（区分）で切替可能とし、切替速度を性能目標に追加する（[quality-standards.md 2章](quality-standards.md#2-性能効率性performance-efficiency)）。
- 2026-06-14: 経歴書のデータモデルを`resumes.content`（JSONB集約）から正規化テーブル（`resumes`=基本情報+集約、`resume_qualifications`=資格情報、`resume_projects`=案件経歴）に変更し、`resume_formats`テーブルを廃止。`resume_projects.languages_tools`等は`text[]` + GINインデックスで「案件名×言語」検索に対応する（[data-models.md 4章](data-models.md#4-経歴書コンテキスト)）。これに伴い、7章の「フォーマットのバージョニング要件」をキャリアシートのみに適用する記述に修正し、経歴書のフォーマット変更はカラム追加マイグレーションで対応する方針とした。
- 2026-06-15: マスク済み経歴書（UC-R2）のマスク対象を`resumes.nearest_station`（最寄り駅）・`resumes.final_education`（最終学歴）の2列に確定し、`visibility_rules.target_category = resume_personal_info`として管理する方針とした。年齢・自己PR・資格情報・案件経歴は本人以外にも公開する。マスク解除の条件は、閲覧者が本人、または`resume_personal_info`を`can_view: true`とするロール（管理者等）を持つ場合とする（[data-models.md 4章](data-models.md#4-経歴書コンテキスト) / [ui-flows.md 4章](ui-flows.md#4-経歴書コンテキスト)）。
- 2026-06-15: UC-M1（エンジニアへのコンタクト）は、Step1ではエンジニア間の直接コンタクト（Googleメッセージ起点、永続化なし）のみとし、営業担当を介する運用・`contacts`テーブルは対象外とする。営業経由ルーティングの検討はStep2（AIレコメンド検討時）にまとめて行う（[ui-flows.md 2章](ui-flows.md#2-メッセージコンテキスト) / [data-models.md 2章](data-models.md#2-メッセージコンテキスト)）。
- 2026-06-15: `data-models.md`のテーブル設計方針（0章）を以下の通り更新。
  - 全テーブルに監査カラム`created_at` / `updated_at` / `created_by` / `updated_by`（後2者はtext型、`accounts.account_id`またはバッチのリクエストID）を付与する。
  - PKカラム名を`id`から`<エンティティ名（単数形）>_id`（例: `account_id`, `role_id`, `skill_category_id`）に統一する。
  - `accounts.account_id`は`AZ0000`形式（仮フォーマット）のtext型とし、社員コード相当の識別子としてアプリ側で採番する。他テーブルのPKはUUID（v7想定）でアプリ側採番とする（DDDの集約ルートIDをドメイン層で確定させる方針と整合）。
  - `display_order`は疎な整数（10/100刻み）ではなく1始まりの連番とする（暗黙的な意味を持たせないため）。
- 2026-06-15: `user_skills`と`user_skill_levels`を1テーブル（`user_skills`）に統合。1ユーザー・1スキル項目に対しレベルは最大1つ（1:1）であるため、`user_skills.level_master_item_id`をnullable列とし、「スキルのみ登録（レベル未設定）」をNULL行で表現する。これに伴いUC-S3（星取表スキル編集）とUC-S4（星取表レベル編集）を統合し、UC-S3「星取表（スキル・レベル）編集」とする（[data-models.md 3章](data-models.md#3-星取表コンテキスト) / [ui-flows.md 3章](ui-flows.md#3-星取表コンテキスト)）。
- 2026-06-15: `skill_categories` / `skill_master_items` / `level_categories` / `level_master_items`（3章）を星取表専用ではなく経歴書（4章）からも参照される共通のスキル・レベルマスタとして位置づけた（テーブル名・配置章は変更なし）。これに伴い`resume_projects.languages_tools`（`text[]` + GINインデックス）を廃止し、案件経歴×使用スキルの中間テーブル`resume_project_skills`（`resume_project_id` × `skill_master_item_id`）を新設。経歴書検索（案件名×言語）は`resumes` / `resume_projects` / `resume_project_skills` / `skill_master_items`のJOINで行う。さらに、イベントストーミング上の論点「経歴書を更新するタイミングで星取表にスキルを反映できないか？」を解決: UC-R1（経歴書保存、保存処理自体はブロックしない）後、`resume_project_skills`に登録されたスキル項目のうち、保存者本人の`user_skills`に未登録のものがあれば`level_master_item_id = NULL`の行をUPSERTし、レベル入力の確認ダイアログを表示する（スキップ可）。スキップ等で残った「レベル未設定」行はマイページに件数バッジ等でリマインダー表示し、星取表の陳腐化を防ぐ（既存行は上書きしない。レベル設定はUC-S3で別途行う）（[data-models.md 3章・4章](data-models.md#3-星取表コンテキスト) / [ui-flows.md 0章・4章](ui-flows.md#0-全体画面構成)）。
- 2026-06-15: UC-F1（経歴書ファイル出力）の出力対象は経歴書（`resumes` / `resume_qualifications` / `resume_projects`）のみとし、星取表（`user_skills`）は対象外とする。また出力履歴（`resume_export_logs`等）はStep1では設けない（[data-models.md 5章](data-models.md#5-ファイルコンテキスト) / [ui-flows.md 5章](ui-flows.md#5-ファイルコンテキスト)）。なお、検索条件・利用者属性・星取表のスキル傾向をログ化して改善サイクルに活用するアイデアは、[docs/TODO.md](../TODO.md)の「利用状況分析（Step2 AIレコメンド連携）」に検討観点として追記した。
- 2026-06-16: PostgreSQL / Flyway / MyBatis を `build.gradle.kts` および `application.properties` へ導入。技術スタック表の状態を「導入済み」に更新。詳細は [APP-ADR-0004](../adr/APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md) を参照。
