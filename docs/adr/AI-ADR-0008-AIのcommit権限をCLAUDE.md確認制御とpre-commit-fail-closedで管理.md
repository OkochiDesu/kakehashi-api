# AI-ADR-0008: AIのcommit権限をCLAUDE.md確認制御+pre-commit fail-closedで管理

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
- 関連: なし

## 背景

AIエージェント（Claude Code）がgit commitを実行できるかどうかの権限設計として2つの方針を検討した。

- **案A**: `.claude/settings.json` のdenyリストに `Bash(git commit*)` を追加し、AIによるcommitを完全禁止する
- **案B**: denyリストからgit commitを外し、CLAUDE.mdの「commit運用」セクションでヒューマンインザループを制度化する（コミットメッセージと `git diff --cached` をユーザーに提示し、明示的な確認を得た場合のみ実行）

案Bを支持する追加背景として、以下の判断があった。

- **意味のある単位への分割**: 変更を関心事の単位に分けてcommitに整理する作業は、AIが全差分を一度に把握しているため人間より精度が高い
- **コミット内容の抜け漏れ**: 人間がステージングする場合に生じがちなファイル漏れや関連ファイルの取りこぼしを、AIはdiffを網羅的に確認することで防ぎやすい

これらの観点から、commitの「実行」をAIに委ねつつ「承認」は人間が行うモデルが最もコスト効率が高いと判断した。

また、`squash-merge` 運用のため「mainにマージ済みのブランチへの誤commit」を防ぐ仕組みとして、`.githooks/pre-commit` にPRマージ済みチェックを組み込んだ。未インストール・未認証でチェック自体が実行できない場合もcommitをブロックするfail-closed方式とした。

## 決定

- **案Bを採用**: CLAUDE.mdの確認制御方式。AIはユーザーの明示的な確認後にcommitを実行できる。`git push` は引き続きdenyリストに残し人間のみが実行できる。
- **pre-commit fail-closed**: `.githooks/pre-commit` でPR状態を確認し、MERGEDならcommitをブロック。`gh` コマンドが利用不可な場合もfail-closedでブロックする。

## 代替案

- **案A（AI commit完全禁止）**: 安全だが、人間が毎回コピーペーストしてcommitする手間が生じ、作業効率が低下するため不採用。
- **pre-commit fail-open（hookエラー時は通す）**: `gh` が使えない環境では誤commitを見逃すリスクがあるため不採用。

## 影響

- AIはCLAUDE.mdの確認制御に従いcommitを実行できる（`git push` は不可）
- commitのauthor/committerはユーザーのgit configに紐づき、誰が確認・実行したかはcommitメタデータに残る
- `.githooks/pre-commit` の導入により、AIが確認を怠った場合でも自動でブロックされる二重防護が働く

## 今後の見直しポイント

- チームメンバーが増えた場合、AI commitの確認権限を限定するロールベース制御の導入を検討する
- fail-closed方式が開発体験を著しく損なう場合は、fail-openへの変更をADRでSupersedeする
