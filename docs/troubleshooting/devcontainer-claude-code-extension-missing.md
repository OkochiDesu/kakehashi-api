# Dev Container リビルド後に ClaudeCode 拡張機能が消える

## 概要

`.devcontainer/devcontainer.json` の `customizations.vscode.extensions` に ClaudeCode拡張（`anthropic.claude-code`）を明示していなかったため、
Dev Containerをリビルドすると拡張機能が再インストールされず、VS Code上からClaudeCodeが消える現象が発生した。

## 原因

Dev Containersは、コンテナ作成・リビルド時に `extensions` リストに列挙された拡張機能のみを自動インストールする。
手動でインストールした拡張機能はリビルド時に保持されない。

## 対応

`.devcontainer/devcontainer.json` の `extensions` に `anthropic.claude-code` を追加し、リビルド後も自動インストールされるようにした。

```jsonc
"extensions": [
    "fwcd.kotlin",
    "vscjava.vscode-java-pack",
    "ryanluker.vscode-coverage-gutters",
    "anthropic.claude-code"
]
```

## 関連

- [Dev Container Compose Compatibility](devcontainer-compose-compatibility.md)
