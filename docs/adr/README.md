# ADR 運用ルール

このディレクトリは Architecture Decision Record (ADR) を管理する。

## 目次

- [目的](#目的)
- [ADR一覧（カテゴリ別索引）](#adr一覧カテゴリ別索引)
- [ファイル命名規則](#ファイル命名規則)
- [ADR の標準構成](#adr-の標準構成)
- [ステータス運用](#ステータス運用)
- [ADR対象か、exec-plan対象か](#adr対象か-exec-plan意思決定ログ対象か)
- [既存ADRの修正 vs 新規ADR追加](#既存adrを修正するか新規adrを追加するか)
- [運用フロー](#運用フロー)
- [レビュー観点](#レビュー観点)
- [ADRエージェント運用](#adrエージェント運用)
- [ADR テンプレート](#adr-テンプレート)

## 目的

- 重要な技術判断を時系列で残す
- なぜその判断に至ったかを後から説明できる状態を保つ
- 実装変更と運用方針の関係を追跡できるようにする

## ADR一覧（カテゴリ別索引）

ADRが増えてもファイル・連番は分割せず、この表でカテゴリ別に把握する。
新規ADRを追加・Supersedeした際は、この表にも行を追加・更新すること。
なお、AI-ADR（エージェント設計ADR、`AI-ADR-` プレフィックス）は [docs/agents/README.md](../agents/README.md) の索引で一元管理するため、この表には含めない。

利用するカテゴリ:

- **CI/CD**: ビルド・テスト・デプロイパイプライン、品質ゲート、PR自動化
- **コーディング**: 実装規約・アーキテクチャ・命名規則
- **セキュリティ**: 認証・認可・シークレット管理
- **業務仕様**: ドメインモデル・業務ルール・データ設計
- **ドキュメント/運用**: ドキュメント体系・エージェント運用・開発フロー
- **ドキュメント方針（DOC-ADR）**: ドキュメントの記述方式・構成方針に関する決定（`DOC-ADR-` プレフィックスを使用）
- **エージェント設計**: エージェント構成・役割分担・マルチエージェント方針（`AI-ADR-` プレフィックスを使用）

| ADR | タイトル | カテゴリ | ステータス |
|---|---|---|---|
| [CICD-ADR-0001](CICD-ADR-0001-CI品質ゲートとDependabot運用方針.md) | CI品質ゲートとDependabot運用方針 | CI/CD | Superseded → CICD-ADR-0002 |
| [CICD-ADR-0002](CICD-ADR-0002-CIトリガー分離とWorkflow検証運用方針.md) | CIトリガー分離とWorkflow検証運用方針 | CI/CD | Accepted |
| [CICD-ADR-0003](CICD-ADR-0003-複雑度しきい値によるCIフェイル条件導入.md) | 複雑度しきい値によるCIフェイル条件導入 | CI/CD | Accepted |
| [CICD-ADR-0004](CICD-ADR-0004-コミットメッセージベースのPRサマリー自動コメント導入.md) | コミットメッセージベースのPRサマリー自動コメント導入 | CI/CD | Accepted（決定4のみCICD-ADR-0005で置換） |
| [CICD-ADR-0005](CICD-ADR-0005-PR本文への変更内容自動反映方式への変更.md) | PR本文への変更内容自動反映方式への変更 | CI/CD | Accepted |
| [APP-ADR-0001](APP-ADR-0001-テーブル設計共通方針.md) | テーブル設計共通方針 | 業務仕様 | Accepted |
| [APP-ADR-0002](APP-ADR-0002-星取表マスタと経歴書のデータ連携方針.md) | 星取表マスタと経歴書のデータ連携方針 | 業務仕様 | Accepted |
| [APP-ADR-0003](APP-ADR-0003-経歴書のマスク範囲-コンタクト経路-ファイル出力範囲のスコープ判断.md) | 経歴書のマスク範囲・コンタクト経路・ファイル出力範囲のスコープ判断 | 業務仕様 | Accepted（決定4のみAPP-ADR-0007で置換） |
| [APP-ADR-0004](APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md) | 永続化技術スタックの導入（Flyway / MyBatis / PostgreSQL） | コーディング | Accepted |
| [APP-ADR-0005](APP-ADR-0005-楽観ロックにversionカラム整数カウンタを採用.md) | 楽観ロックにversionカラム（整数カウンタ）を採用 | 業務仕様 | Accepted |
| [APP-ADR-0006](APP-ADR-0006-accountsステータスの退職一時停止統一とsuspended_atによる1年マスク化.md) | accounts.statusに4値設計（deactivated追加）と非adminからのsuspended/deactivated除外 | 業務仕様 | Accepted |
| [APP-ADR-0007](APP-ADR-0007-rolesをpermissionベースに再定義しvisibility_rulesを廃止.md) | `roles` を権限（Permission）ベースに再定義し `visibility_rules` を廃止 | 業務仕様 | Accepted |
| [DOC-ADR-0001](DOC-ADR-0001-ドキュメントにchangelogセクションを持たない.md) | ドキュメントにchangelogセクションを持たない（git + ADRリンクで代替） | ドキュメント方針（DOC-ADR） | Accepted |

## ファイル命名規則

CI/CD ADR（CICD-ADR）:

- 形式: `CICD-ADR-連番4桁-日本語タイトル.md`
- 例: `CICD-ADR-0002-CIトリガー分離とWorkflow検証運用方針.md`
- 対象: CI/CDパイプライン・PR自動化・品質ゲートに関する決定
- 連番は CICD-ADR 独自の通し番号（0001 から始まる）

アプリケーションADR（APP-ADR）:

- 形式: `APP-ADR-連番4桁-日本語タイトル.md`
- 例: `APP-ADR-0001-テーブル設計共通方針.md`
- 対象: バックエンド・フロントエンド・DB設計・業務仕様に関する決定（ドメイン横断の場合は影響セクションに明記）
- 連番は APP-ADR 独自の通し番号（0001 から始まる）

ドキュメント方針ADR（DOC-ADR）:

- 形式: `DOC-ADR-連番4桁-日本語タイトル.md`
- 例: `DOC-ADR-0001-ドキュメントにchangelogセクションを持たない.md`
- 対象: ドキュメントの記述方式・構成方針に関する決定
- 連番は DOC-ADR 独自の通し番号（ADR / AI-ADR の連番と独立して 0001 から始まる）
- `docs/adr/` 配下に配置し、本READMEの「ADR一覧（カテゴリ別索引）」に含める

エージェント設計ADR（AI-ADR）:

- 形式: `AI-ADR-連番4桁-日本語タイトル.md`
- 例: `AI-ADR-0001-マルチエージェント構成採用方針.md`
- 連番は AI-ADR 独自の通し番号（ADR の連番と独立して 0001 から始まる）

共通ルール:

- 可読性を優先し、ファイル名に日本語を使用する
- スペースは使わず記号はハイフン（`-`）のみを使用する
- 命名規則は CI（`.github/workflows/workflow-lint.yml`）で検証し、違反時はチェックを失敗させる

## ADR の標準構成

各 ADR は原則として次の見出しを含める。

1. タイトル
2. ステータス
3. 日付
4. 関連（Supersedes / Superseded by）
5. 背景
6. 決定
7. 代替案
8. 影響
9. 今後の見直しポイント

## ステータス運用

使用するステータスは次の4種類とし、各ADRでは「現在の状態」をマークで表現する。

- [ ] Proposed
- [ ] Accepted
- [ ] Superseded
- [ ] Rejected

運用ルール:

- 現在の状態のみ `[x]`、それ以外は `[ ]` とする
- Superseded の場合は、`[x] Superseded` の後ろに後継ADRへのリンクを付ける
- ステータス欄の表記と、関連セクション（Supersedes / Superseded by）は整合させる

## ADR対象か、exec-plan意思決定ログ対象か

ADRは「kakehashi-api（製品・システム）に関する恒久的な決定」を対象とする。

例外として **エージェント設計に関するアーキテクチャ決定**（どのエージェントを採用したか、分割方針、役割分担など）は
`AI-ADR-`（Agent Architecture Decision Record）プレフィックスで `docs/adr/` に記録する。
AI-ADR は `docs/agents/README.md` からも索引する（他のメンバーがエージェント構成の背景を辿れるようにするため）。

一方、CLAUDE.md・`.claude/`・`.githooks/`・devcontainerなど、**AIエージェントの運用設定・開発プロセス**に関する決定は
ADR/AI-ADR ではなく対応する exec-plan の意思決定ログに記録する。
判断軸の詳細は [core-beliefs.md 原則7](../design-docs/core-beliefs.md#7-adrとexec-plan意思決定ログの使い分け) を参照。

## 既存ADRを修正するか、新規ADRを追加するか

原則として次の基準で判断する。

- 軽微な誤字修正、表現補足: 既存 ADR を修正してよい
- 意思決定の内容が変わる、運用ルールが変わる: 新規 ADR を追加する

意思決定が変わる場合は次を必ず実施する。

1. 旧 ADR のステータスを Superseded に更新
2. 旧 ADR に Superseded by を追記
3. 新 ADR に Supersedes を追記

## 運用フロー

1. 変更内容を整理し、既存 ADR の対象か新規判断かを決める
2. 新規 ADR が必要なら次番号で作成する
3. 関連する workflow / 設計文書 / 実装ファイルとの対応を本文に明記する
4. PR でレビューし、合意後にステータスを最終化する

## レビュー観点

- 背景に課題が具体的に書かれているか
- 決定内容が運用可能な粒度で書かれているか
- 代替案とトレードオフが示されているか
- 影響範囲と見直し条件が明確か
- Supersedes / Superseded by が双方向で正しいか

## ADRエージェント運用

このリポジトリでは、ADR更新を支援するエージェント/スキルを Git 管理している。
以下を自動化できる。

- git diff からの影響分析
- 新規 ADR ドラフトの作成
- 既存 ADR の Supersede 処理
- 命名・ステータス・リンク整合の検証

- **ClaudeCode**: `/adr-governance` スキルを実行する、または `adr-governance` サブエージェントを呼び出す。
- **GitHub Copilot**: Copilot チャットで `@ADR Governance` を呼び出す。

## ADR テンプレート

新しい ADR を追加する際は、プレフィックスを選択（`CICD-ADR-` / `APP-ADR-` / `DOC-ADR-` / `AI-ADR-`）してコピーする。

```markdown
# <PREFIX>-NNNN: タイトル

## ステータス

- [ ] Proposed
- [ ] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

YYYY-MM-DD

## 関連

- Supersedes: なし
- Superseded by: なし

## 背景

なぜこの決定が必要になったか。解決したい問題や制約を記述する。

## 決定

何を決めたか。選択した手段を簡潔に記述する。

## 代替案

検討したが採用しなかった選択肢と、却下した理由。

## 影響

この決定によって生じる制約・副作用・今後の課題。

## 今後の見直しポイント

どのような状況になったら再検討するか。
```

- ClaudeCode用エージェント:
  - [.claude/agents/adr-governance.md](../../.claude/agents/adr-governance.md)
  - [.claude/agents/adr-search.md](../../.claude/agents/adr-search.md)
  - [.claude/agents/adr-validator.md](../../.claude/agents/adr-validator.md)
- ClaudeCode用スキル:
  - [.claude/skills/adr-governance/SKILL.md](../../.claude/skills/adr-governance/SKILL.md)
- GitHub Copilot用エージェント（互換のため維持）:
  - `.github/agents/adr/adr-governance.agent.md`
  - `.github/agents/adr/adr-search.agent.md`
  - `.github/agents/adr/adr-validator.agent.md`
- GitHub Copilot用スキル（互換のため維持）:
  - `.github/skills/adr-governance/SKILL.md`

### 基本フロー

1. ClaudeCodeでは `/adr-governance` スキル（または `adr-governance` サブエージェント）を起動する。Copilotでは `@ADR Governance` を起動する。
2. 差分と関連ADRの確認結果を提示し、更新方針（既存修正/新規追加/置換）を人間が承認する。
3. 承認後にADRを編集し、命名・ステータス・関連リンクを検証する。

### 低コスト運用ルール

- まず `git diff` と変更ファイル判定などの軽量チェックを実施する。
- サブエージェント（検索・検証）は必要時のみ呼び出す。
- 同一依頼内でのサブエージェント呼び出しは原則1回ずつに制限する。

### ハルシネーション抑止ルール

- 根拠はリポジトリ内ファイルの内容のみを使う。
- 不明点がある場合は推測せず、ユーザーへ確認する。
- Superseded の判定時は後継ADRリンクを必須にする。
