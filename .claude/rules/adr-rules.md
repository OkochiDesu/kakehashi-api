---
globs:
  - "**/*-ADR-*.md"
---

# ADR 編集規約

`*-ADR-*.md` ファイルを編集・作成するときに適用するルール。
ADR の作成・更新・Supersede は必ず `adr-governance` サブエージェントに委譲すること。

## ファイル命名規則

| プレフィックス | 対象 | 例 |
|---|---|---|
| `CICD-ADR-XXXX-` | CI/CDパイプライン・PR自動化・品質ゲート | `CICD-ADR-0001-CI品質ゲート.md` |
| `APP-ADR-XXXX-` | バックエンド・DB設計・業務仕様・アーキテクチャ | `APP-ADR-0001-テーブル設計共通方針.md` |
| `DOC-ADR-XXXX-` | ドキュメントの記述方式・構成方針 | `DOC-ADR-0001-changelogセクションを持たない.md` |
| `AI-ADR-XXXX-` | エージェント設計・マルチエージェント構成 | `AI-ADR-0001-マルチエージェント構成採用.md` |

共通ルール:
- 連番は各プレフィックス独自の 4 桁通し番号（0001 から）
- ファイル名に日本語可。スペース不可・記号はハイフン（`-`）のみ
- 命名規則は CI（`.github/workflows/workflow-lint.yml`）で自動検証

## ADR の標準構成（9 見出し）

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

## 目次（ToC）の書き方

`## 目次` セクションはこのリポジトリの標準9見出しには含まれない（各ADRファイルが任意で付与する慣習）。ToCを付与する場合、以下のルールに従う。

- **「日付」セクションへのリンクは省略してよい**。日付は本文冒頭（`## ステータス` の直後）にすぐ表示され、リンクで辿る必要性が低いため（既存慣習: `docs/adr/` 配下でToCを持つADRの過半数が日付リンクを省略している）
- 上記以外のセクション（背景・決定・代替案・影響・今後の見直しポイント等）は網羅することが望ましいが、必須ではない
- ToCのリンク網羅性は `adr-validator` の検証対象外（[.claude/agents/adr-validator.md](../agents/adr-validator.md) 参照）。日付リンクの欠落を「漏落」として指摘しないこと

## ステータス運用

- 現在の状態のみ `[x]`、それ以外は `[ ]`
- Superseded の場合は `[x] Superseded → <後継ADRへのリンク>` と記載
- ステータス欄と「関連」セクションは必ず整合させる

## ADR 対象か exec-plan 対象か

- **ADR**: kakehashi-api（製品・システム）の恒久的な決定
- **AI-ADR**: エージェント設計・マルチエージェント構成に関するアーキテクチャ決定
- **exec-plan 意思決定ログ**: CLAUDE.md・`.claude/`・`.githooks/`・devcontainer など AIエージェントの運用設定・開発プロセスに関する決定

判断軸の詳細は [core-beliefs.md 原則8](../../docs/design-docs/core-beliefs.md#8-adrとexec-plan意思決定ログの使い分け) を参照。

## 既存 ADR 修正 vs 新規 ADR 追加

- **軽微な誤字・表現補足** → 既存 ADR を直接修正してよい
- **意思決定の内容・運用ルールが変わる** → 新規 ADR を追加し旧 ADR を Supersede

Supersede 時の必須作業:
1. 旧 ADR: ステータスを `[x] Superseded` に更新し `Superseded by:` を追記
2. 新 ADR: `Supersedes:` に旧 ADR のリンクを追記

## 索引の更新

ADR を作成・Supersede した際は必ず索引を更新すること。

- 通常 ADR（CICD/APP/DOC）: `docs/adr/README.md` の「ADR一覧（カテゴリ別索引）」
- AI-ADR: `docs/agents/README.md` の「エージェント設計ADR（AI-ADR）索引」
