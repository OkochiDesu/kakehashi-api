# AI-ADR-0016: GitHub Copilot と ClaudeCode の役割分担明確化と Copilot エージェントファイルの削除

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-06-26

## 関連

- Supersedes: AI-ADR-0002 の「GitHub Copilot版（`.github/agents/adr/`）は互換維持のため削除せず並存する」という決定
- Superseded by: なし

## 背景

AI-ADR-0002 において、`.github/agents/adr/` の GitHub Copilot 用エージェントは「互換維持のため削除せず並存する」と決定した。

その後の運用を通じて、ツールの役割分担が明確になった。

- **GitHub Copilot**: GitHub PR レビュー専用（プレミアムリクエスト節約のため実装には使わない）
- **ClaudeCode（VSCode 拡張）**: VSCode 上の実装・ドキュメント整備すべてを担当

この役割分担のもとでは、`.github/agents/adr/` の Copilot 用エージェントは実質的に使われておらず、ClaudeCode 版の `.claude/agents/` と二重管理になっていた。
CI ワークフロー（`.github/workflows/`）からの参照もなく、維持コスト（陳腐化リスク・索引更新負担）に見合わないと判断した。

## 決定

GitHub Copilot と ClaudeCode の役割分担を以下の通り明確化する。

| ツール | 用途 |
|---|---|
| GitHub Copilot | GitHub PR レビューのみ |
| ClaudeCode | VSCode 上の実装・ドキュメント・ADR 整備すべて |

この決定に基づき、`.github/agents/adr/`（3ファイル）および `.github/skills/adr-governance/`（2ファイル）を削除する。
今後このリポジトリでは、エージェント定義は `.claude/agents/` のみで管理する。

## 代替案

- **並存を継続（AI-ADR-0002 の方針維持）**: Copilot のスポット利用に備えて残す。しかし実際には使われておらず、ClaudeCode 版との二重管理が陳腐化を招く。却下。
- **Copilot 版も随時更新する**: 両者を同期して維持する。更新負担が倍増し、どちらが正なのか混乱を招く。却下。

## 影響

- `.github/agents/adr/`（3ファイル）と `.github/skills/adr-governance/`（2ファイル）を削除
- `AGENTS.md` の「GitHub Copilot 用エージェント」セクションを削除
- ADR の作成・更新は `.claude/agents/adr-governance.md` または `/adr-governance` スキルに一本化
- ファイル削除は git 管理のため、必要があれば git 履歴から復元可能

## 今後の見直しポイント

- Copilot を VSCode 実装でも使う必要が生じた場合は、`.github/agents/` を再整備する（git 履歴から復元可能）
- ClaudeCode 以外の IDE ツールを導入した場合は、役割分担を再評価する
