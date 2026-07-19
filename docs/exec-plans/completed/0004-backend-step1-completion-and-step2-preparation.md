# 0004: バックエンド Step1 完了 〜 Step2 準備ロードマップ（解体済み）

> **このexec-planは解体済みです。**
> PR単位の個別exec-planに分割しました:
> Step1: [0005](../completed/0005-ci-integration-test-environment.md) / [0006](../completed/0006-authentication-google-sso-jwt.md) / [0007](../pending/0007-authorization-access-control.md) / [0008](../pending/0008-deactivated-auto-transition-batch.md) / [0009](../pending/0009-skill-matrix-crud.md) / [0010](../pending/0010-resume-crud.md) / [0011](../pending/0011-resume-export-excel-pdf.md) / [0012](../pending/0012-test-strategy-step1-gate.md)
> Step2: [0013](../pending/0013-career-sheet-dynamic-form.md) / [0014](../pending/0014-usage-analytics.md) / [0015](../pending/0015-contact-route-revision.md)

## 目次

- [完了条件（Definition of Done）](#完了条件definition-of-done)
- [目的・スコープ](#目的スコープ)
- [フェーズ構成と依存関係](#フェーズ構成と依存関係)
- [進捗状況](#進捗状況)
  - [Phase 1: CI 統合テスト環境（マイグレーション実行）](#phase-1-ci-統合テスト環境マイグレーション実行)
  - [Phase 2: 認証基盤（Google SSO + JWT）](#phase-2-認証基盤google-sso--jwt)
  - [Phase 3: 認可・アクセス制御](#phase-3-認可アクセス制御)
  - [Phase 4: deactivated 自動遷移バッチ](#phase-4-deactivated-自動遷移バッチ)
  - [Phase 5: テスト戦略確定・カバレッジルール整備（Step1 完了ゲート）](#phase-5-テスト戦略確定カバレッジルール整備step1-完了ゲート)
  - [Phase 6: 経歴書帳票出力（Excel/PDF）【Step2 開始】](#phase-6-経歴書帳票出力excelpdfstep2-開始)
  - [Phase 7: キャリアシート・ダイナミックフォーム基盤](#phase-7-キャリアシートダイナミックフォーム基盤)
  - [Phase 8: 利用状況分析基盤（Step2 後半）](#phase-8-利用状況分析基盤step2-後半)
- [意思決定ログ](#意思決定ログ)
- [残課題・引き継ぎ事項](#残課題引き継ぎ事項)

## 完了条件（Definition of Done）

このexec-planが完了したとき、以下の状態になっている:

- Step1 スコープ（アカウント・認証・認可・deactivated バッチ・テスト戦略確定）が実装・テスト済みでマージ済み
- Step2 スコープ（帳票出力・キャリアシート基盤）の設計が着手可能な状態（API設計書・DBマイグレーション設計が揃っている）
- 各フェーズの完了後に AI エージェントが次フェーズを提案できるよう、フェーズ境界が明確に定義されている

## 目的・スコープ

`docs/TODO.md` のバックエンドセクションに残る未着手タスクを優先順位・依存関係つきで整理し、
「次に何をするか」をセッション跨ぎで引き継げるようにする。

対象: `kakehashi-api` バックエンドのみ（フロントエンドは別リポジトリのため対象外）。

---

## フェーズ構成と依存関係

```
[Phase 1] CI 統合テスト環境
         ↓（他フェーズと並列実施可能）
[Phase 2] 認証基盤（Google SSO + JWT）
         ↓
[Phase 3] 認可・アクセス制御
         ↓
[Phase 4] deactivated 自動遷移バッチ
         ↓
[Phase 5] テスト戦略確定・カバレッジルール整備
         ↓（Step1 完了ゲート）
[Phase 6] 経歴書帳票出力（Excel/PDF）   ← Step2 開始
[Phase 7] キャリアシート・ダイナミックフォーム基盤
[Phase 8] 利用状況分析基盤（Step2 後半・AIレコメンド連携）
```

---

## 進捗状況

### Phase 1: CI 統合テスト環境（マイグレーション実行）

**完了条件**: GitHub Actions の CI で PostgreSQL コンテナを起動し Flyway マイグレーションが自動検証される

- [x] `ci.yml` に PostgreSQL サービスコンテナ追加（`postgres:16`）
- [x] Flyway マイグレーション実行ステップを CI に追加（`./gradlew flywayMigrate` or 統合テストで代替）
- [x] Testcontainers 統合テスト（`AccountRepositoryImplIntegrationTest`）を CI で実行できることを確認

> **注意**: devcontainer の DoD（Docker-outside-of-Docker）環境と CI の native Docker 環境は構成が異なる。
> `TESTCONTAINERS_RYUK_DISABLED` / `TESTCONTAINERS_HOST_OVERRIDE` は devcontainer 専用設定であり CI では不要。[APP-ADR-0012](../../adr/APP-ADR-0012-Testcontainersを2.0.5へ移行しTestConfiguration直接起動方式を採用.md) 参照。

---

### Phase 2: 認証基盤（Google SSO + JWT）

**完了条件**: `/api/auth/google/callback`（UC-A1）が実装され、Google ID トークンの検証と accountId の発行が動作する

- [x] ADR 作成: JWT 戦略（自前発行 vs Google id_token Bearer）の決定（`adr-governance` 経由）
- [x] `spring-boot-starter-oauth2-resource-server` or カスタムフィルタの設計
- [x] `GoogleSsoCallbackUseCase` の認証フロー実装（JIT プロビジョニング含む）
- [x] `SecurityContextHolder` から `accountId` を取得する仕組みの確定（`@AuthenticationPrincipal` 等）
- [x] API 設計書: `POST /api/auth/google/callback`（`api-designer` → `test-scenario-planner` → `kotlin-implementer`）
- [x] テスト: 正常系（新規ユーザー自動登録）・異常系（無効トークン・無効アカウント）

---

### Phase 3: 認可・アクセス制御

**完了条件**: `provisional` アカウントが保護エンドポイントに到達できず、`admin` ロールが必要なエンドポイントが認可エラーを返す

- [x] ADR 作成: `provisional` 状態アクセス制御の実装レイヤー決定（Spring Security フィルタ vs `@PreAuthorize`）
- [x] `HandlerMethodArgumentResolver` で `@AuthenticatedAccountId` 等のアノテーション実装
- [x] Controller 引数をドメインモデルのみに保つ（UseCase 層に Web 概念を持ち込まない）
- [x] テスト: `provisional` → 403、`active` → 200、`admin` ロール不足 → 403

> 根拠: [APP-ADR-0008](../../adr/APP-ADR-0008-DDD-CQRSアーキテクチャ原則の採用.md)（Controller 引数をドメインモデルのみにする依存方向維持）

---

### Phase 4: deactivated 自動遷移バッチ

**完了条件**: `suspended_at` から1年経過したアカウントが日次で `deactivated` に更新される

- [x] `@Scheduled` バッチの実装（`AccountDeactivationBatch.kt`）
- [x] `@ConditionalOnProperty` でテスト時無効化
- [x] テスト: 1年経過アカウントが `deactivated` に変わること（`@TestPropertySource` で有効化して検証）

> 根拠: [APP-ADR-0006](../../adr/APP-ADR-0006-accounts.statusに4値設計（deactivated追加）と非adminからのsuspended-deactivated除外.md)

---

### Phase 5: テスト戦略確定・カバレッジルール整備（Step1 完了ゲート）

**完了条件**: カバレッジ閾値が CI で強制されており、Step1 の全ドメイン・ユースケースが閾値を満たす

- [x] レイヤー別カバレッジ閾値を決定（ドメイン層 80% / インフラ層除外 等）
- [x] `jacocoCoverageVerification` に閾値・除外パターンを設定
- [x] CI で `jacocoTestCoverageVerification` タスクを実行
- [x] Controller 統合テストの要否を決定（MockMvc vs Testcontainers）

---

### Phase 6: 経歴書帳票出力（Excel/PDF）【Step2 開始】

**完了条件**: エンジニアの経歴書を Excel ダウンロード・PDF ダウンロードできる

- [x] DB 設計: `skill_sheets`（経歴書）テーブル設計（`db-designer` 経由）
- [x] Flyway マイグレーション作成
- [x] API 設計: `GET /api/engineers/{id}/skill-sheet/excel`、`GET /api/engineers/{id}/skill-sheet/pdf`（`api-designer`）
- [x] `Jxls-poi` 導入・Excel テンプレート実装（`infrastructure/report/JxlsSkillSheetExporter.kt`）
- [x] LibreOffice headless 導入（devcontainer Dockerfile に `fonts-noto-cjk` + `libreoffice` 追加）
- [x] `SkillSheetExporter` インターフェース定義（UseCase 層はインターフェースのみ依存）
- [x] テスト・実装（`kotlin-implementer` → `code-reviewer` → `test-reviewer`）

---

### Phase 7: キャリアシート・ダイナミックフォーム基盤

**完了条件**: キャリアシートのレイアウト定義（JSON マスタ）と入力データ（JSONB）が CRUD できる

- [x] DB 設計: `career_sheet_formats`（レイアウト定義）・`career_sheets`（入力データ JSONB）テーブル
- [x] API 設計: フォーマット管理・キャリアシート CRUD（`api-designer`）
- [x] バージョン移行ポリシーの ADR 作成
- [x] テスト・実装（`kotlin-implementer` → `code-reviewer` → `test-reviewer`）

---

### Phase 8: 利用状況分析基盤（Step2 後半）

**完了条件**: 主要 API のアクセスログが収集・集計可能な状態になっている

- [x] ログ設計 ADR: ログ粒度・保存先・個人情報方針の決定
- [x] アクセスログ収集の実装（Spring AOP / フィルタ）
- [x] 分析クエリ基盤の整備

---

## 意思決定ログ

- 2026-06-24: exec-plan 作成。Phase 1（CI 環境）を他フェーズと独立して先行実施可能と判断。Phase 2〜3 は認証→認可の順序依存がある。Step1 完了ゲート（Phase 5）を設けることで、Step2 着手前に品質を担保する構成とした。

## 残課題・引き継ぎ事項

- フロントエンド（Nuxt 3）の開発タイミングと Phase 2〜3 の API 完成スケジュールの調整が必要
- Phase 6 以降は別 exec-plan として分割することを検討する（規模が大きい場合）
- CI 統合テスト環境（Phase 1）は `.github/workflows/` 変更を含むため、着手前にユーザー確認必須（CLAUDE.md 参照）
- コンタクト経路の見直し（Step2）は `docs/TODO.md` に残存しており、Phase 6〜8 と並列して検討が必要
