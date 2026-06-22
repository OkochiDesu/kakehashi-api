# Dev Container リビルド時に shellcheck feature が GLIBC エラーで失敗する

## 概要

`.devcontainer/devcontainer.json` に `ghcr.io/devcontainers-extra/features/shellcheck:1` を追加した後、
Dev Container のリビルドが GLIBC バージョン不足により失敗する現象が発生した。

## 発生環境（対応前）

- ベースイメージ: `mcr.microsoft.com/devcontainers/java:21-bullseye`（Debian 11 / bullseye）
  ※ 現在は bookworm に変更済み（本ドキュメントの「対応」セクション参照）
- 追加 feature: `ghcr.io/devcontainers-extra/features/shellcheck:1`

## 発生したエラー

リビルド時に以下のエラーが表示される。

```
/usr/local/bin/nanolayer: /lib/x86_64-linux-gnu/libc.so.6: version `GLIBC_2.36' not found
(required by /usr/local/bin/nanolayer)
```

## 原因

`devcontainers-extra/features/shellcheck` は内部で **nanolayer** を使用してツールをインストールする。
nanolayer のバイナリは GLIBC 2.36 以上を要求するが、Debian 11 (bullseye) の GLIBC は 2.31 であり、要件を満たさない。

| Debian バージョン | コード名 | GLIBC バージョン | nanolayer 互換性 |
|---|---|---|---|
| 11 | bullseye | 2.31 | **非対応** |
| 12 | bookworm | 2.36 | 対応 |

## 対応

`.devcontainer/docker-compose.yml` のベースイメージを `bullseye` から `bookworm` に変更する。

```yaml
# 変更前
image: mcr.microsoft.com/devcontainers/java:21-bullseye

# 変更後
image: mcr.microsoft.com/devcontainers/java:21-bookworm
```

変更後、Dev Container を「Rebuild Container」で再起動すれば shellcheck feature が正常にインストールされる。

## 再発防止メモ

- `ghcr.io/devcontainers-extra/features/` 配下の feature は nanolayer を使うものが多く、bullseye では動作しない
- 新しい feature を追加する際は、ベースイメージが bookworm（GLIBC 2.36 以上）であることを確認する
- `devcontainer-lock.json` に feature のハッシュが記録されるため、feature 追加後は同ファイルのコミットも忘れずに含める

## 関連

- [Dev Container Compose Compatibility](devcontainer-compose-compatibility.md)
- [Dev Container ClaudeCode Extension Missing](devcontainer-claude-code-extension-missing.md)
