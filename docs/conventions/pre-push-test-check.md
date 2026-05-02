# pre-push テストコードチェック 備忘録

## 目的

push 時にテストコードが存在しないクラスを検出し、漏れを防ぐ。

## 仕組み

`.githooks/pre-push` スクリプトが push 前に自動実行される。
push 対象のコミット差分に含まれる `src/main/kotlin` 配下の Kotlin ファイルに対し、
対応するテストファイルが `src/test/kotlin` 配下に存在するかチェックする。

対応テストファイルの命名規則:

- `FooTest.kt`
- `FooTests.kt`

## 有効化手順

### コンテナ再作成時

`devcontainer.json` の `postCreateCommand` に設定済みのため自動で有効化される。

### 手動で有効化する場合

```bash
git config core.hooksPath .githooks
```

## 動作

| 状況 | 結果 |
|------|------|
| 対応テストファイルが存在する | push 通過 |
| 対応テストファイルが存在しない | push ブロック＋該当ファイル一覧を表示 |
| スキップしたい場合 | `git push --no-verify` |

## 関連ファイル

- `.githooks/pre-push` — フックスクリプト本体
- `.devcontainer/devcontainer.json` — `postCreateCommand` で自動有効化設定

## 再発防止

- 新しいクラスを追加したら同時にテストファイルも作成する。
- フックをスキップした場合は、速やかにテストコードを追加する。
