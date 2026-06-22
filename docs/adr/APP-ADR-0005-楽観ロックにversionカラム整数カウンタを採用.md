# APP-ADR-0005: 楽観ロックにversionカラム（整数カウンタ）を採用

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-06-19

## 関連

- Supersedes: なし
- Superseded by: なし
- 関連ADR: [APP-ADR-0001](APP-ADR-0001-テーブル設計共通方針.md)（テーブル設計共通方針。楽観ロックは同ADRの対象外とされ、本ADRで補完する）

## 背景

`accounts` / `skill_master_items` / `level_master_items` / `user_skills` / `resumes` など、ユーザーが編集する主要テーブルでは、複数ユーザーの同時更新による競合を検出する必要がある。

[data-models.md 0章](../requirements/data-models.md#0-設計方針)では「楽観ロック用の`version`（integer、更新ごとにインクリメント）を付与する」と記述されているが、**なぜ**その方式を選んだかの記録がなかった。

[APP-ADR-0001](APP-ADR-0001-テーブル設計共通方針.md)は「編集系テーブルの楽観ロック（`version`）・編集履歴ログ（`entity_change_logs`）は本ADRの対象外」と明示しており、本ADRはその空白を補完する位置づけである。

## 決定

編集系テーブルの楽観ロックに、**`version`（integer、更新ごとにインクリメント）カラムを採用する。**

クライアントは読み取り時の`version`を更新リクエストに含め、サーバー側の現在値と一致しなければ `409 Conflict` を返す。

採用理由:

- タイムスタンプ精度に依存しない。monotonically increasing な整数のため競合検出が確実である。
- MyBatis の UPDATE 文の WHERE 句に `version = #{version}` を加えることで実装でき、更新件数が 0 の場合に `409 Conflict` を返すアプリ側ハンドリングと組み合わせられる（APP-ADR-0004: MyBatis + JDBC スタック）。
- 409 レスポンスでクライアントに競合を明示でき、「再取得して再試行」フローを組みやすい。

## 代替案

1. **`updated_at`（timestamp）で照合する**: クライアントが読んだ時点の`updated_at`をリクエストに含め、サーバー側の現在値と比較する。
   - 却下理由: タイムスタンプの精度（ミリ秒・マイクロ秒）に依存し、同一ミリ秒内の2回更新を検出できないケースがある。アプリクロックとDBクロックのズレ（分散環境）で false negative が起こりうる。
2. **悲観ロック（SELECT FOR UPDATE）**: 読み取り時にレコードをロックする。
   - 却下理由: 読み込み〜編集〜保存の間ずっと DB 接続・ロックを保持するためスループットが下がる。本システムの編集頻度に対しては過剰である。
3. **`version`（integer）カウンタ（採用）**: 更新のたびに +1 する整数値。クライアントが読んだ`version`をリクエストに含め、不一致なら 409 Conflict を返す。

## 影響

- [data-models.md 0章](../requirements/data-models.md#0-設計方針)の楽観ロック記述に本ADRへのリンクを追加する。
- 対象テーブル（`accounts` / `skill_master_items` / `level_master_items` / `user_skills` / `resumes`）のDDLに `version integer not null default 0` を含める（V1 SQL（`src/main/resources/db/migration/V1__create_accounts_and_roles.sql`）では `accounts` に実装済み）。
- 全更新系エンドポイントのリクエストボディに `version` を必須フィールドとして含める。
- 更新時の `version` 不一致は `409 Conflict` としてクライアントに返す。
- 事前 version チェック通過後に別トランザクションで更新が割り込んだ場合（UPDATE 0件）は、`findById` で currentVersion を再取得して `OptimisticLockException` に渡す。クライアントは最新 version を受け取ることで再試行の判断ができる。

## 今後の見直しポイント

- 更新リクエストの頻度・競合発生率が想定を大きく超え、楽観ロックの再試行コストが問題化した場合は、対象テーブル単位で悲観ロック併用を再検討する。
- `version`（integer）のオーバーフローが現実的に問題となる規模に達した場合は型の見直しを検討する（通常は想定されない）。
