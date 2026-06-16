# GitHub PR「Checking for the ability to merge automatically...」が終わらない

## 概要

GitHub のPR画面で「Checking for the ability to merge automatically...」が永続的に表示され、
マージボタンが押せない状態になることがある。

## 原因

`mergeable: UNKNOWN` は、GitHubがマージ可否を計算中またはブランチ保護要件が未充足の
いずれかの状態で発生する。

主な要因：

1. **Approve 未取得**: ブランチ保護に `required_approving_review_count: 1` が設定されている場合、
   Approve が 0 件だとマージ状態が解決できず UNKNOWN のままになりやすい。
   Copilot などのボットによる COMMENTED レビューは Approve としてカウントされない。
2. **GitHub バックエンドの一時的な遅延**: pushや新規コミット直後は、GitHubが計算を開始するまで
   UNKNOWN が続く場合がある。

## 確認方法

```bash
# マージ可否・CI状態を確認
gh pr view <PR番号> --json state,mergeable,mergeStateStatus,statusCheckRollup

# ブランチ保護ルールを確認
gh api repos/<owner>/<repo>/branches/main/protection

# レビュー承認状況を確認
gh api repos/<owner>/<repo>/pulls/<PR番号>/reviews \
  --jq '.[] | {user: .user.login, state: .state}'
```

## 対処手順

### 1. リポジトリオーナーが管理者権限でマージする（推奨）

`enforce_admins: false`（デフォルト設定）の場合、オーナーは `--admin` フラグで
ブランチ保護をバイパスしてマージできる。

```bash
# squash merge（required_linear_history: true に対応）
gh pr merge <PR番号> --squash --admin

# rebase merge の場合
gh pr merge <PR番号> --rebase --admin
```

### 2. ブランチ保護ルールを一時的に緩和する

Settings → Branches → mainの保護ルール編集 →
「Require a pull request before merging」の「Required approvals」を一時的に 0 にし、
マージ後に元の値に戻す。

## 空コミットによる再計算トリガーについて

`git commit --allow-empty` で空コミットをpushして再計算をトリガーする方法があるが、
GitHub Actionsがワークフロー設定によって空コミットをスキップする場合があり、
効果がないことがある。Approveが根本原因の場合はCI再実行よりも上記の対処手順を優先する。
