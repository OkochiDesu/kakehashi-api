# ADR-0001: CI品質ゲートとDependabot運用方針

## ステータス

- [ ] Proposed
- [ ] Accepted
- [x] Superseded ([ADR-0002: CIトリガー分離とWorkflow検証運用方針](ADR-0002-CIトリガー分離とWorkflow検証運用方針.md))
- [ ] Rejected

## 日付

2026-05-03

## 背景

本プロジェクトでは以下の課題があった。

- PR前後で品質確認を自動化し、マージ前に不具合混入を防ぎたい
- カバレッジと複雑度の可視化を行いたい
- GitHub Actions / ツール更新を安全側で運用したい
- 更新自動化はしたいが、互換性破壊リスクは抑えたい

## 決定

### 1. CIワークフローの責務分離

`.github/workflows/ci.yml` を2ジョブ構成とする。

- `verify`:
  - `spotlessCheck`, `test`, `jacocoTestReport` を実行
  - マージ可否を判定する必須品質ゲート
- `reports`:
  - `verify` 成功後に実行（`needs: verify`）
  - 複雑度レポート生成（lizard）
  - PRカバレッジコメント投稿
  - 複雑度コメント投稿（上位N件の高複雑度関数）
  - レポートをArtifactとして保存

### 2. Branch protectionの必須チェック

GitHub Branch protection の Required checks は `CI / Verify` を必須とする。

- 理由: マージ可否を決めるチェックを最小・明確にするため
- `Reports` は可視化用途のため必須にはしない

### 3. カバレッジ運用

- JaCoCo XML はCI実行時に生成されたものを使用する
- コミット済みレポートを参照しない
- PR時に自動でカバレッジコメントを投稿する

### 4. 複雑度運用

- lizard で複雑度レポート（XML/TXT）を生成
- PRコメントは高複雑度の上位N件のみ表示し、レビューしやすさを優先
- しきい値による fail 判定は現時点では未導入
  - しきい値定義は TODO で管理し、後日導入する
  - 補足: しきい値による fail 判定は [ADR-0003](ADR-0003-複雑度しきい値によるCIフェイル条件導入.md) で導入済み（本項の記述はその範囲で更新されている）

### 5. サプライチェーン対策

- 外部ActionはSHA固定で利用する
- 公式Actionは当面メジャータグ運用（段階的強化方針）
- lizard はバージョン固定（`1.22.1`）

### 6. Dependabot方針（GitHub Actions）

`.github/dependabot.yml` で以下を採用する。

- 対象: `github-actions`
- 実行: 週次
- 同時PR上限: 2
- 自動マージ: しない
- 更新範囲: パッチ更新のみ（minor/majorは除外）

## 代替案

### 代替案A: 1ジョブに集約

- 長所: 設定が単純
- 短所: 失敗原因の切り分けが難しく、必須チェック設定も粗くなる

### 代替案B: Dependabotでminor/majorも提案

- 長所: 更新追従が速い
- 短所: 互換性破壊の可能性が上がり、試験運用段階ではリスクが高い

## 影響

- 品質ゲート（Verify）と可視化（Reports）が分離され、運用が明確化
- PRレビュー時にカバレッジ/複雑度が確認しやすくなる
- 依存更新は安全側（パッチ中心）で進める運用になる

## 今後の見直しポイント

- 複雑度しきい値（CCN上限）の定義と fail 条件導入
  - [ADR-0003](ADR-0003-複雑度しきい値によるCIフェイル条件導入.md) で仮値（CCN上限10）として導入済み。運用しながら見直す
- Dependabotの対象拡張（Gradle依存）可否
- 公式ActionのSHA固定への段階的移行
