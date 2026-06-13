# pre-commit シークレットチェック 備忘録

## 目的

commit に APIキー・トークン・秘密鍵などのシークレットらしき文字列が
誤って含まれていないかを、commit 前に簡易チェックする。

AI（ClaudeCode等）が `git commit` を実行できる運用（[CLAUDE.md](../../CLAUDE.md) の commit運用）に対する
追加の防御（[design-docs/core-beliefs.md](../design-docs/core-beliefs.md) 原則2 二重の防御）として導入。

## 仕組み

`.githooks/pre-commit` スクリプトが commit 前に自動実行される。
ステージされた変更（`git diff --cached`）に対して、以下のような形式の文字列が含まれていないかを正規表現でチェックする。

- AWSアクセスキーID
- 秘密鍵（`-----BEGIN ... PRIVATE KEY-----`）
- Google APIキー
- GitHubトークン
- Slackトークン

## 有効化手順

### コンテナ再作成時

`devcontainer.json` の `postCreateCommand` で `git config core.hooksPath .githooks` が設定済みのため自動で有効化される（[pre-push-test-check.md](pre-push-test-check.md) と共通）。

### 手動で有効化する場合

```bash
git config core.hooksPath .githooks
```

## 動作

| 状況 | 結果 |
|------|------|
| シークレットらしき文字列が見つからない | commit 通過 |
| シークレットらしき文字列が見つかる | commit ブロック＋該当ファイル一覧を表示 |
| スキップしたい場合 | `git commit --no-verify` |

## 限界・注意

- 正規表現による簡易チェックであり、すべてのシークレット形式を検出できるわけではない。
- 誤検知・検知漏れが起こり得るため、`.env` 等の機密ファイルは `.gitignore` で管理することが大前提。
- より網羅的なチェックが必要な場合は、gitleaks 等の専用ツール導入を検討する（devcontainerイメージへの追加が必要なため別途対応）。

## 関連ファイル

- `.githooks/pre-commit` — フックスクリプト本体
- `.devcontainer/devcontainer.json` — `postCreateCommand` で自動有効化設定
