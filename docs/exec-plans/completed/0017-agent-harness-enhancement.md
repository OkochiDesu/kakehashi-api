# 0017: エージェント整備・ハーネス強化

## 完了条件（Definition of Done）

- ドメイン専用サブエージェント（アカウントドメイン）が `.claude/agents/` に作成済み
- Qiita 記事のセキュリティ設定 10 選のうち未適用項目の適用有無が決定・実施済み
- Zenn 記事の grill-me スキルの適用有無が決定・実施済み
- ユーザー/GitHub Copilot 指摘時の自動ハーネス化エージェントの設計・作成有無が決定・実施済み
- 上記で作成したエージェント・スキル・設定が AGENTS.md / docs/agents/README.md に反映済み

## 目的・スコープ

エージェント構成をさらに成熟させる。
- ドメイン責任者エージェントを置くことで、ドメイン知識のガイドラインをエージェント単位で分離管理する
- セキュリティ設定の棚卸しで既存の隙間を埋める
- grill-me スキルで実装前の要件定義フェーズの品質を高める
- 指摘対応の自動ハーネス化で再発防止の仕組みをさらに強化する

## 進捗状況

### ① ドメイン専用サブエージェント

- [x] アカウントドメイン専用エージェント（`account-domain-agent.md`）を設計・作成する
- [x] AGENTS.md / docs/agents/README.md の索引に追記する

### ② Qiita 記事セキュリティ設定の適用判断

- [x] `allowUnsandboxedCommands: false` の現状確認 → **見送り**（deny リストで代替済み、AI-ADR-0019 に記録）
- [x] ネットワークホワイトリストの要否をユーザーと相談 → **見送り**（`Bash(curl *http*)` 等の deny で実質済み）
- [x] PreToolUse フックによる Bash 事前検証の要否をユーザーと相談 → **見送り**（threat model 的に過剰）
- [x] 決定事項を AI-ADR-0019 に記録済み

### ③ Zenn 記事 grill-me スキルの適用判断

- [x] grill-me スキル（`/grill-me`）の導入有無をユーザーと相談
- [x] **最終決定**: 独立した SKILL.md は不要。各エージェントに「不明点確認プロセス」セクションとして埋め込む方式を採用
  - 対象エージェント: `api-designer` / `db-designer` / `test-scenario-planner` / `feedback-harness-agent` / `kotlin-implementer`
  - 理由: スタンドアロンスキルは別 HITL ステップを追加してしまう。既存の設計レビュー HITL の中で確認できる形が望ましい
  - 5行の作法（共通認識まで徹底質問・ツリーを順番に解決・推奨回答付き・一度に一つ・コードベース探索で解決できるなら質問しない）を各エージェントに直接記載した

### ④ 指摘対応ハーネス化エージェント

- [x] 設計・必要性をユーザーと相談（決定: 作成する）
- [x] `feedback-harness-agent.md` を作成（分類5カテゴリ・実装ガイドライン含む）
- [x] AGENTS.md に追記済み
- [x] AI-ADR-0020 として記録済み

### ⑤ 仕上げ

- [x] AGENTS.md / docs/agents/README.md 索引更新済み
- [x] doc-maintainer チェック実施済み
- [x] PR 作成・マージ

## 意思決定ログ

- 2026-06-28: Qiita/Zenn 記事を参照し、適用済み項目を確認。残りは本 exec-plan でユーザーと相談しながら判断する
- 2026-06-28: セキュリティ設定3項目すべて見送り決定。AI-ADR-0019 に記録
- 2026-06-28: grill-me はスタンドアロン SKILL.md ではなく「不明点確認プロセス」セクションとして5エージェント（api-designer/db-designer/test-scenario-planner/feedback-harness-agent/kotlin-implementer）に直接埋め込む形式に変更。独立 HITL を追加せず既存フロー内で確認できる設計に
- 2026-06-28: feedback-harness-agent 作成・AI-ADR-0020 記録
- 2026-06-28: CLAUDE.md の分類ロジックをインライン記述から `feedback-harness-agent` への委譲に変更（`adr-governance` と同じハーネスパターンを適用）

## 残課題・引き継ぎ事項

- なし（grill-me の役割は各エージェントの不明点確認プロセスに吸収済み）
