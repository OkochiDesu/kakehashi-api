# exec-plans 索引

実行計画（exec-plan）を管理するディレクトリ。
運用ルール・テンプレート・昇格基準の詳細は [.claude/rules/exec-plan-rules.md](../../.claude/rules/exec-plan-rules.md) を参照。

## ディレクトリ構成

- [pending/](pending/) — 計画済みだが未着手（PR バックログ）
- [active/](active/) — 現在進行中（原則1件）
- [completed/](completed/) — 完了してアーカイブ済み
- [tech-debt-tracker.md](tech-debt-tracker.md) — 既知の技術的負債・後回し課題

## ファイル命名規則

`NNNN-内容を表す短い名前.md`（4桁連番）。次に使用できる番号は **0022**。
