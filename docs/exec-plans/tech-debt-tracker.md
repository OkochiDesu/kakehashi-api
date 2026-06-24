# 技術的負債トラッカー

対応を見送った課題・将来検討する項目を記録する。
運用ルールは [docs/exec-plans/README.md](README.md) を参照。

## 一覧

| 項目 | 内容 | 起票日 | 状態 |
|------|------|--------|------|
| SSHエージェント転送の設定 | GitHubへの認証をHTTPS（gh auth）からSSHエージェント転送方式に切り替える。秘密鍵をコンテナにコピーせず、ホスト側`ssh-agent`を転送する方式を想定。手順は[devcontainer-ssh-agent-forwarding.md](../troubleshooting/devcontainer-ssh-agent-forwarding.md)に記載。VS Code再起動・コンテナ再アタッチ後の転送確認・リモートURL切り替えが残作業。 | 2026-06-13 | 対応中 |
| PostgreSQL を 15 → 16 へ移行 | devcontainer（`docker-compose.yml`）・APP-ADR-0004・テストコンテナ（`AccountRepositoryImplIntegrationTest`）を一括で 16 系に上げる。16 は安定版であり互換性も高いため、新しいバージョンを使う方針に沿って対応する。PR #13 では 15 に揃える最小修正を実施済み。移行時は devcontainer リビルドと APP-ADR-0004 の更新（`adr-governance` 経由）が必要。 | 2026-06-24 | 未着手 |
