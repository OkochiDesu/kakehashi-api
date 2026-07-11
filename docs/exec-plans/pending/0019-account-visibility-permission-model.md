# 0019: アカウント検索・閲覧権限モデルの刷新

## 完了条件（Definition of Done）

- 検索経由（ユーザ検索・案件検索・スキル検索）でのアカウント情報閲覧が、権限マトリクス（`admin` / `view_personal_info` / 権限なし・本人 / 権限なし・他人）に沿って動作する
- `GET /api/accounts`（一覧・検索）が管理者専用から全ユーザー向けに変更されている
- `GET /api/accounts/{accountId}` が全ユーザー向けに開放され、`SecurityContextHolder` の `accountId`・権限に応じてマスク有無が判定される
- `PATCH /api/accounts/me` が `PATCH /api/accounts/{accountId}` に統合され、`admin` による他アカウント編集が可能になっている
- 経歴書・星取表（exec-plan 0009・0010 が実装する UC-S/UC-R）に同じ権限モデルを適用する前提が `ui-flows.md`・`data-models.md` に明記されている（実装自体は 0009・0010 側で行う）

## 目的・スコープ

PR #19（exec-plan 0006）のレビューで、アカウント情報の閲覧権限モデルについて「検索経由の閲覧は誰でも可能・権限に応じてマスク」という業務ルールが明確化された。現行の `ui-flows.md`・APP-ADR-0007・`account-role.md` は「アカウント一覧・詳細は管理者専用（他人は403）」という異なるモデルを規定しており、この差分を解消する。

対象はアカウントドメイン（`GET /api/accounts`, `GET /api/accounts/{accountId}`, `PATCH /api/accounts/me` → `{accountId}`）のドキュメント更新・実装変更まで。経歴書・星取表ドメインへの同モデル適用は、対応する exec-plan（0009: 星取表CRUD, 0010: 経歴書CRUD）が未着手のため、本 exec-plan では要件定義への反映（`ui-flows.md`/`data-models.md` への権限モデル明記）に留め、実装はそれぞれの exec-plan に引き継ぐ。

exec-plan 0007（認可・アクセス制御基盤）が提供する認可の仕組み（Controller引数解決・`provisional` ガード等）を利用する前提のため、**0007 完了後の着手を推奨**する。

## 進捗状況

- [ ] ADR作成: `view_personal_info` 権限のスコープをアカウント情報にも拡張するか、新規ADRとするかを決定（`adr-governance` 経由。既存 APP-ADR-0007 との関係整理を含む）
- [ ] `ui-flows.md` 更新: 検索フロー（ユーザ検索・案件検索・スキル検索の3画面）の画面遷移・アクセス制御表を新権限マトリクスに合わせて修正
- [ ] `data-models.md` 更新: アカウント情報のマスク方針（`view_personal_info`/`admin` による解除ルール）を追記
- [ ] `account-role.md` 更新: `GET /api/accounts` のアクセス制御を全ユーザー向けに変更、`GET /api/accounts/{accountId}` のマスクロジックを明記、`PATCH /api/accounts/me` → `{accountId}` へのパス変更を反映
- [ ] 実装変更: `GetAccountQuery`・`ListAccountsQuery`・`EditAccountUseCase`・`AccountController` を新モデルに対応
- [ ] テストシナリオ再設計（`test-scenario-planner`）・実装（`kotlin-implementer`）
- [ ] コードレビュー（`code-reviewer` → `test-reviewer`）
- [ ] PR作成・マージ

## 意思決定ログ

- 2026-07-11: PR #19 のレビューコメントを起点に、`account-domain-agent` で既存ADR（APP-ADR-0007等）との整合性を検証。`view_personal_info` は経歴書ドメイン限定の決定であり、アカウント情報への拡張は範囲外と確認。ユーザーとの対話で権限マトリクス（`admin`/`view_personal_info`/本人/他人）を確定。
- 2026-07-11: 経歴書・星取表への同モデル適用は、対応exec-plan（0009・0010）が未着手のため実装はそちらに委ね、本exec-planは要件定義文書への明記とアカウントドメインの実装までとする。

## 残課題・引き継ぎ事項

- `ListAccountsQuery` が設計書の403を実装していない既存の乖離は、本exec-planとは別に `tech-debt-tracker.md` で管理する（対応時に本exec-planと合わせて解消してもよい）
- 経歴書（UC-R1〜R4）・星取表（UC-S1〜S5）への同モデル適用は exec-plan 0009・0010 側のタスクに追記が必要
