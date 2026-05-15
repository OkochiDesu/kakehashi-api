# ADR-0002: CIトリガー分離とWorkflow検証運用方針

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-05-15

## 関連

- Supersedes: ADR-0001
- Superseded by: (none)

## 背景

ADR-0001 では CI を verify/reports に責務分離したが、次の運用課題が残っていた。

- Markdown 更新だけでも重い CI が起動し、実行コストが増える
- Workflow 定義の更新時に、重い CI を止めると検証経路が弱くなる
- コード変更と docs/workflow 変更が同時に入る PR で、どのワークフローを優先するかを明確化したい

## 決定

### 1. 重いCIの起動対象をアプリコード変更に限定する

対象ファイル: .github/workflows/ci.yml

- push / pull_request に paths-ignore を設定する
- `**/*.md` と `.github/workflows/**` だけの変更では重い CI を起動しない
- 重い CI は品質ゲートとして、アプリコード変更時の検証に集中させる

### 2. 軽量なWorkflow検証ワークフローを分離する

対象ファイル: .github/workflows/workflow-lint.yml

- docs/workflow 変更時に起動する軽量ワークフローを新設する
- actionlint により Workflow 記述の妥当性を検証する
- 実行時間と消費リソースを抑えるため、重いビルドやテストは実行しない

### 3. 変更が混在する場合は重いCIを優先する

- docs/workflow に加えてソースコード変更が含まれる場合:
  - 重い CI は通常どおり起動する
  - 軽量 Workflow 検証は変更判定ステップでスキップする
- これにより、混在変更で二重実行を避けつつ、品質ゲートの一貫性を保つ

### 4. Branch protection の必須チェックは維持する

- Required checks は引き続き `CI / Verify` を必須とする
- `Workflow Lint` は補助的チェックとして運用し、必須にはしない

## 代替案

### 代替案A: 単一CIのまま paths-ignore だけ追加する

- 長所: 設定変更が最小
- 短所: workflow 更新の検証経路が弱くなる

### 代替案B: 混在変更でも重いCIと軽量CIを常に両方実行する

- 長所: 検証の網羅性が高い
- 短所: 実行時間とコストが増え、運用意図に反する

## 影響

- docs のみ更新時に重い CI が起動せず、CI コストを削減できる
- workflow 更新時に軽量検証が実行され、設定破損を検知しやすくなる
- 混在変更時は重い CI を優先するため、マージ判定フローが単純になる

## 今後の見直しポイント

- Workflow Lint を必須チェックにするかどうか
- docs のうち設計文書更新を別ワークフローで追加検証するかどうか
- CI のパス条件をモノレポ化やモジュール分割に合わせて再設計するかどうか
