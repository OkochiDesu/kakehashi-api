# AI-ADR-0014: AIのgit/gh操作権限を3層モデル（自動・確認・ブロック）に整理

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-06-23

## 関連

- Supersedes: なし
- Superseded by: なし
- 関連: AI-ADR-0008（AIのcommit権限をCLAUDE.md確認制御+pre-commit fail-closedで管理）。本ADRはその拡張・補完であり、commitに限定されていた確認制御モデルを git push / git merge へ拡張し、git/gh操作全体を3層に整理する。

## 背景

従来は `git push` を `.claude/settings.json` の deny リストでハードブロックしていた。これは AI への信頼度が低い段階での保守的な設定だった。

しかし AI-ADR-0008 により commit について「対象を提示して確認してから実行する」フロー（コミットメッセージと `git diff --cached` を提示し、明示的な確認後にのみ実行）が整備された。同等の確認制御を push にも適用できると判断したため、push を deny からはずし確認制御へ移行する。

同時に、`git merge`（ローカルマージ）と `gh pr merge`（GitHub PR マージ）の扱いも整理する必要が生じた。前者はローカルの状態変更にとどまるが、後者は GitHub 上での永続的・公開的なマージ行為であり、両者を同列に扱うべきではない。

## 決定

AI（Claude Code）が実行できる git / gh 操作を以下の**3層モデル**に整理する。

### 層1: 自動実行可（`.claude/settings.json` allow リスト）

読み取り・ステージング系。確認不要。

- `git add`, `git status`, `git diff`, `git log`, `git branch`, `git stash`, `git show`
- `gh api repos/*/pulls/*/comments/*/replies --method POST`

### 層2: 確認後に実行（`CLAUDE.md` 確認ルール）

実行前に対象・影響範囲をユーザーに提示し、明示的な「OK」を得てから実行する。Auto Mode も例外なし。

- `git commit`: コミットメッセージと `git diff --cached` を提示（AI-ADR-0008 の方針を継承）
- `git push`: プッシュ先リモートブランチ・対象コミット一覧を提示
- `git merge`: マージ元ブランチ・影響範囲を提示

### 層3: ハードブロック（`.claude/settings.json` deny リスト）

破壊的・不可逆・外部公開に直結する操作。即時ブロック。

- `git reset --hard`, `git clean`
- `gh pr merge`（GitHub 上での永続的なマージ行為）
- `rm`, `rmdir`, `find -delete`, `rsync --delete`
- `curl`, `wget`（外部ダウンロード）

## 代替案

- **案A（push を引き続き deny）**: 従来方針の維持。commit と同等の確認フローが整備済みであり、push のみをハードブロックし続けるのは過剰規制になるため不採用。
- **案B（push / merge / gh pr merge すべて層2で確認後実行）**: `gh pr merge` は GitHub 上の公開行為であり、AI が単独で実行すべきではないため不採用。`gh pr merge` は層3に残す。
- **案C（push / merge を層1の自動実行可に格上げ）**: ユーザーが関与するタイミングを残すべきであり、確認ゲートを撤去するのは時期尚早のため不採用。

## 影響

- `.claude/settings.json`: deny リストから `Bash(git push*)` を削除し、`Bash(gh pr merge*)` を追加する。
- `CLAUDE.md` の commit運用セクション: `git push` / `git merge` の確認ルール（実行前に対象・影響範囲を提示し、Auto Mode も例外なく確認を得る）を追記する。
- 確認制御モデルの適用範囲が commit から push / merge へ拡張され、AI-ADR-0008 と本ADRが git/gh 操作権限設計の対をなす。
- 実装コミット: `2229c40`

## 今後の見直しポイント

- AI への信頼度がさらに高まった場合、`git push` を層1（自動実行可）へ格上げするかをADRで再検討する。
- `gh pr merge` をワークフロー上で AI に委ねる必要が生じた場合は、確認制御（層2）への移行を本ADRのSupersedeとして検討する。
