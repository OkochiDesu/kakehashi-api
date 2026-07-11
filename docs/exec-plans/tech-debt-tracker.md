# 技術的負債トラッカー

対応を見送った課題・将来検討する項目を記録する。
運用ルールは [docs/exec-plans/README.md](README.md) を参照。

## 一覧

| 項目 | 内容 | 起票日 | 状態 |
|------|------|--------|------|
| SSHエージェント転送の設定 | GitHubへの認証をHTTPS（gh auth）からSSHエージェント転送方式に切り替える。秘密鍵をコンテナにコピーせず、ホスト側`ssh-agent`を転送する方式を想定。手順は[devcontainer-ssh-agent-forwarding.md](../troubleshooting/devcontainer-ssh-agent-forwarding.md)に記載。VS Code再起動・コンテナ再アタッチ後の転送確認・リモートURL切り替えが残作業。 | 2026-06-13 | 対応中 |
| PostgreSQL を 15 → 16 へ移行 | devcontainer（`docker-compose.yml`）・APP-ADR-0004・テストコンテナ・docs・rules の全参照を 16 系に統一。devcontainer リビルドが必要。 | 2026-06-24 | 対応済み（feature/planning-and-test-docs） |
| UC-A1 `redirectTo` のバックエンド決定を見直す | `GoogleSsoCallbackUseCase`（`redirectTo`）・`account-role.md`（UC-A1レスポンス）で、バックエンドが `/registration` `/mypage` `/error/suspended` 等のフロントエンドルーティングパスを決定して返している。SSR構成（Thymeleaf等）ならバックエンドが画面遷移を握るのは自然だが、本プロジェクトはフロントエンド/バックエンドが分離した構成のため、バックエンドの応答（`accountId`・`status`等）を根拠にフロントエンド側でリダイレクト先を判断する設計の方が適切ではないかとPR #19でレビュー指摘あり。既存実装（PR #10）由来のため今回のPRでは対応せず、別PRで設計を再検討する。 | 2026-07-11 | 未対応 |
| `ListAccountsQuery` の403未実装 | `docs/design/api/account-role.md`（UC-A5）は「`admin`権限なしのアクセスは403 Forbidden」と明記しているが、`ListAccountsQuery`/`AccountController.listAccounts` の実装は403を返さず、`isAdmin`フラグに応じて`status`を`active`に強制するのみになっている。設計書と実装の乖離。exec-plan 0019（アカウント検索・閲覧権限モデルの刷新）でUC-A5自体のアクセス制御方針が変わる見込みのため、そちらと合わせて解消するか判断する。 | 2026-07-11 | 未対応 |
