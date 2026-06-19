# AI-ADR-0009: CLAUDE.md @importによるコンテキスト常時注入（hook廃止）

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-06-19

## 関連

- Supersedes: なし
- Superseded by: なし
- 関連: [AI-ADR-0007](AI-ADR-0007-AGENTS.mdを目次型マップとして採用.md)

## 背景

AGENTS.mdをAIエージェントのコンテキストに常時保持する方法として2つの手段を検討した。

- **hook方式**: `.claude/hooks/session-start-compact.sh`（SessionStart, matcher: compact）を用いて、新セッション開始時・`/compact` 実行後にAGENTS.md全文をadditionalContextとして再注入する
- **import方式**: CLAUDE.mdの冒頭に `@AGENTS.md` を記述し、CLAUDE.mdがロードされるたびにAGENTS.md全文が自動的に展開される

import方式を試験し、新セッション開始時・`/compact` 実行後の両方でAGENTS.md全文がコンテキストに保持されることを確認した。

## 決定

**import方式を採用** し、hookによる再注入を廃止する。CLAUDE.md冒頭に `@AGENTS.md` を記述する。

理由:

- importとhook再注入はコンテキストサイズの面で同等のコストだが、importは常時・自動で機能する
- hookはCLAUDE.md/AGENTS.mdの内容変更のたびにメンテナンス（追従）が必要になる
- hookファイル自体の管理コスト（`.claude/hooks/` への登録・実行権限設定）が不要になる

## 代替案

- **hook方式（SessionStart, matcher: compact）**: 条件付きで発火するため「いつ注入されたか」が不透明。CLAUDE.md/AGENTS.md変更時にhook内容の更新も必要になる二重管理が生じるため不採用。

## 影響

- `.claude/hooks/session-start-compact.sh` と `.claude/settings.json` のhook登録を削除する
- CLAUDE.md先頭の `@AGENTS.md` がコンテキスト注入の唯一の仕組みとなる
- AGENTS.mdの内容変更はCLAUDE.mdの変更なしに自動反映される

## 今後の見直しポイント

- AGENTS.mdが大幅に長くなり（500行超など）コンテキストコストが問題になった場合は、分割またはhook方式への部分的な切り替えを検討する
