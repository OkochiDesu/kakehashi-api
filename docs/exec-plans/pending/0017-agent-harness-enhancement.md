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

- [ ] アカウントドメイン専用エージェント（`account-domain-agent.md`）を設計・作成する
- [ ] AGENTS.md / docs/agents/README.md の索引に追記する

### ② Qiita 記事セキュリティ設定の適用判断

- [ ] `allowUnsandboxedCommands: false` の現状確認 → 適用有無を決定・実施
- [ ] ネットワークホワイトリストの要否をユーザーと相談 → 決定に応じて実施
- [ ] PreToolUse フックによる Bash 事前検証の要否をユーザーと相談 → 決定に応じて実施
- [ ] 決定事項を AI-ADR または CLAUDE.md に記録する

### ③ Zenn 記事 grill-me スキルの適用判断

- [ ] grill-me スキル（`/grill-me`）の導入有無をユーザーと相談
- [ ] 導入する場合: `.claude/skills/grill-me/SKILL.md` を作成し AGENTS.md に追記する

### ④ 指摘対応ハーネス化エージェント

- [ ] 設計・必要性をユーザーと相談（ユーザー指摘 / GitHub Copilot 指摘を受けて再発防止を自動化するエージェント）
- [ ] 作成する場合: エージェント定義・AGENTS.md 追記・AI-ADR 作成まで完結させる

### ⑤ 仕上げ

- [ ] doc-maintainer（structure + content）による全体チェック
- [ ] PR 作成・マージ

## 意思決定ログ

- 2026-06-28: Qiita/Zenn 記事を参照し、適用済み項目を確認。残りは本 exec-plan でユーザーと相談しながら判断する

## 残課題・引き継ぎ事項

- Qiita 記事 URL: https://qiita.com/miruky/items/51db293a7a7d0d277a5d
- Zenn 記事 URL: https://zenn.dev/ryonakae/articles/8783c6b3ead2cb
