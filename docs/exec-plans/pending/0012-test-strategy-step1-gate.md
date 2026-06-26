# 0012: テスト戦略確定・カバレッジルール整備（Step1 完了ゲート）

## 完了条件（Definition of Done）

- カバレッジ閾値が CI で強制されており、Step1 全スコープ（UC-A1〜A7, UC-S1〜S5, UC-R1〜R4, UC-F1）が閾値を満たす
- exec-plan 0005〜0011 がすべてマージ済みであること

## 目的・スコープ

Step1 の最終 PR。exec-plan 0005〜0011 がすべて完了した後に着手する。Step2 着手前の品質ゲートとして機能する。

## 進捗状況

- [ ] exec-plan 0005〜0011 がすべてマージ済みであることを確認
- [ ] レイヤー別カバレッジ閾値の決定（ドメイン層 80% / インフラ層除外 等）
- [ ] `jacocoCoverageVerification` に閾値・除外パターンを設定
- [ ] CI で `jacocoTestCoverageVerification` タスクを実行
- [ ] Controller 統合テストの要否を決定（MockMvc vs Testcontainers）
- [ ] PR 作成・マージ

## 意思決定ログ

## 残課題・引き継ぎ事項
