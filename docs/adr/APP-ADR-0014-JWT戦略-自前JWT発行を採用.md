# APP-ADR-0014: JWT戦略（自前JWT発行を採用）

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-07-01

## 関連

- Supersedes: なし
- Superseded by: なし

## 背景

認証基盤（exec-plan 0006）で Google SSO ログイン（UC-A1）を実装する。
`POST /api/auth/google/callback` で Google ID トークンを受け取る仕様（account-role.md）が確定している。

後続の認可（exec-plan 0007）では、各 API が `SecurityContextHolder` から `accountId` を取得する設計になっている。
したがって認証段階で「リクエスト主体を `accountId` として一意に確定し、以降のリクエストで安価に取り出せる」仕組みが必要になる。

現状の依存は Spring Boot 4.x・spring-boot-starter-webmvc であり、Spring Security は未導入である。
認証基盤の導入にあたり、リクエストごとに送るトークンとして「自前 JWT を発行する」か「Google ID トークンをそのまま Bearer として使い続ける」かを決める必要がある。

## 決定

**自前 JWT 発行方式を採用する。**

- `POST /api/auth/google/callback` で Google ID トークンを受け取り、バックエンドで Google JWKS を用いて検証する。
- 検証後、未登録の Google アカウントは JIT プロビジョニング（仮登録）を行う。
- バックエンドが自前の JWT（`accountId` クレームを埋め込む）を発行し、レスポンスで返す。
- 以降のリクエストは、この自前 JWT を `Authorization: Bearer` ヘッダーで送信する。
- Spring Security のカスタムフィルターで自前 JWT を検証し、`SecurityContextHolder` に `accountId` をセットする。

追加予定の依存:

- `spring-boot-starter-security`
- `spring-boot-starter-oauth2-resource-server`（Google JWKS 検証用）
- JWT ライブラリ（jjwt-api 等）

## 代替案

**Google id_token Bearer（検証のみ）方式を却下した。**

毎リクエストで Google ID トークンを `Bearer` として送り、バックエンドは検証のみを行う方式。

却下理由:

- Google sub（UID）→ `accountId` の DB ルックアップが毎リクエスト必要になり、認可のたびにコストが発生する。
- Google ID トークンの有効期限が短い（約 1 時間）ため、フロントエンド側のトークンリフレッシュ実装が複雑になる。

## 影響

- Spring Security・oauth2-resource-server・JWT ライブラリを新規依存として導入する。
- 認証フローは「Google ID トークン検証 → JIT プロビジョニング → 自前 JWT 発行」の 3 段構成になる。
- 自前 JWT の署名鍵の管理・有効期限・リフレッシュ方針を別途定める必要がある（本 ADR のスコープ外。認証基盤実装時に決定する）。
- カスタムフィルターが `SecurityContextHolder` に `accountId` をセットするため、後続の認可（exec-plan 0007）は Google sub ではなく `accountId` を前提に設計できる。
- `accountId` を JWT クレームに埋め込むことで、認可時の DB ルックアップを回避できる。

## 今後の見直しポイント

- 自前 JWT の鍵ローテーション・失効管理の運用負荷が高くなった場合、外部 IdP のセッション管理・トークンイントロスペクションへの移行を再検討する。
- 複数の IdP（Google 以外）に対応する要件が生じた場合、トークン発行・検証の抽象化方針を見直す。
- リフレッシュトークンの導入可否を認証基盤実装時に別途決定する（本 ADR では自前アクセス JWT の発行方針のみを確定する）。
