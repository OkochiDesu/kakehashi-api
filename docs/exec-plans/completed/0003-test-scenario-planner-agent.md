# 0003: test-scenario-planner エージェントの追加

## 目的・スコープ

TDD サイクルを実装フローに組み込み、テストシナリオを実装前に明示化する。
`test-scenario-planner` エージェントを新設し、`kotlin-implementer` 呼び出し前に
シナリオ KDoc を人間が承認するゲートを設ける。

背景:
- 現在の実装フローはテストシナリオを明示化せずにコードを書くため、テスト観点の漏れが起きやすい
- 正常系・異常系の網羅性を「テスト前」に確認することで、実装後の手戻りを減らす
- `kotlin-implementer` 内部で TDD サイクル（赤→緑）を完結させ、エージェント数を最小限に保つ

## 進捗状況

### Phase 1: test-scenario-planner エージェント作成
- [x] `.claude/agents/test-scenario-planner.md` 新規作成

### Phase 2: 既存エージェント・スキル更新
- [x] `implement-review-loop` スキルに test-scenario-planner ステップを追加（kotlin-implementer の前）
- [x] `kotlin-implementer.md` をシナリオ KDoc を入力として使うよう更新

### Phase 3: 索引・ドキュメント更新
- [x] `AGENTS.md` に `test-scenario-planner` を追記
- [x] `docs/agents/README.md` に `test-scenario-planner` を追記

## 意思決定ログ

- 2026-06-23: 4 エージェント案（planner / test-writer / implementer / checker）は過剰と判断。
  シナリオ作成+観点チェックを `test-scenario-planner` 1 エージェントに統合。
  「失敗確認」ステップは独立エージェント化せず `kotlin-implementer` 内部ステップとして実装する。
  理由: 失敗確認はコンテキストが本体実装と同一のため分離メリットがない。

## 残課題・引き継ぎ事項

- `kotlin-implementer` 内部での `./gradlew test` 実行（赤確認→緑確認）の安定性は動作確認後に評価する
