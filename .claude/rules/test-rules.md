---
globs:
  - "**/*Test.kt"
---

# テストコード規約

`*Test.kt` ファイルを編集・作成するときに適用するルール。
詳細なテスト方針は [kdoc-and-test-policy.md](../../docs/conventions/kdoc-and-test-policy.md) を参照。

## アサーション

- **`assert()` を使わない**: JVM の `-ea` フラグが無効のとき評価されず常に成功する。`assertEquals` / `assertTrue` / `assertThrows` / `assertNull` 等 JUnit5 アサーションを使うこと（pre-commit でも検出する）
- **`assertNull()` より `assertNull(actual)` の形式を使う**: メッセージ引数は省略してよいが、対象は明示する

## TDD（テスト先行）

新規実装・バグ修正ともに**テストを先に書いてから実装する**。
- バグ修正: バグを再現する失敗テストを書く → 修正する → テストが通ることを確認する
- 機能追加: 期待動作を定義するテストを書く → 実装する

## 正常系テストの観点

- 状態遷移・戻り値が仕様通りであることを確認する
- **状態を変更する UseCase の正常系テストでは、`updatedAt` が更新されていること・`updatedBy` が操作者 ID になっていることを同じテストケース内で検証すること**（別テストに分離しない）
- バグ修正で「正常系の仕様を最初から検証しきれていなかった」と判明した場合は、新規テストを追加するのではなく既存の正常系テストを強化する

## 異常系テストの観点

- 楽観ロック競合: `update()` が 0件 → `OptimisticLockException`。`currentVersion` の値が正しいことまで検証すること（`findById()` 2回目呼び出しに別バージョンを返す `returnsMany` を使用）
- 権限エラー: `isAdmin = false` / `operatorIsAdmin = false` → `ForbiddenOperationException`
- ステータス遷移不正: `canTransitionTo()` が false → `InvalidStatusTransitionException`
- Not Found: 対象が存在しない → `AccountNotFoundException`
- 不正な入力値: 未定義の `roleCode` など → `IllegalArgumentException`

## バリデーション変更時

バリデーション挙動を変更した diff（`runCatching.getOrNull()` 廃止・例外スロー追加・型変換ロジック変更等）がある場合は、**同じ diff 内**にエラーパステストを追加・更新すること。

## テスト命名

テスト名は「`正常系/異常系： 条件 → 期待結果`」の形式で書く。
- 良い例: `` `正常系： grantAdminRole=true で admin ロールが付与される` ``
- 良い例: `` `異常系： operatorIsAdmin=false は ForbiddenOperationException` ``
