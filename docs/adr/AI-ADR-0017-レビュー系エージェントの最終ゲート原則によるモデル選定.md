# AI-ADR-0017: レビュー系エージェントの最終ゲート原則によるモデル選定

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-06-26

## 関連

- Supersedes: なし
- Superseded by: なし

## 背景

`.claude/agents/` 配下のエージェントはモデルを個別に指定している。
これまでレビュー系エージェント（`code-reviewer`、`test-reviewer`、`design-impl-checker` 等）は
すべて `sonnet` に統一されていたが、役割の性質によってモデルの適正が異なることが判明した。

`code-reviewer` と `test-reviewer` は実装パイプラインの最終ゲートであり、
ここで見落とした問題はそのまま実装コードに残る。下流での修正コストが高いため、
判断精度を最優先にすべきという議論が行われた。

## 決定

**最終ゲート原則**を導入する：パイプライン末端で人間確認前の最後の自動チェックを担うエージェントは `opus` を使用する。

- `code-reviewer` のモデルを `sonnet` → `opus` に変更する
- `test-reviewer` のモデルを `sonnet` → `opus` に変更する
- 機械的比較が主体のエージェント（`design-impl-checker`、`src-doc-maintainer`、`doc-maintainer-structure`、`doc-maintainer-content`）は `sonnet` のまま

## 代替案

1. **全レビュー系を opus にする**: `design-impl-checker` 等はパス・フィールドの機械的比較が主体で深い判断を要さない。opus は過剰でコスト増に見合わない → 却下
2. **すべて sonnet のまま**: 最終ゲートでの見落としリスクを許容することになる → 却下
3. **人間承認ゲートがあれば sonnet で十分とする**: `api-designer`・`test-scenario-planner` 等、後段に人間承認ゲートがあるエージェントは sonnet が妥当。本ADRの対象外として sonnet を維持する

## 影響

- `code-reviewer` と `test-reviewer` の呼び出しコストが増加する
- 今後エージェントを追加する際、「最終ゲートか否か」でモデルを判断する基準が明確になる
- `adr-governance` は引き続き `opus`（オーケストレーターとして最も複雑な判断を要する。本ADRとは独立した選定）
- `adr-search`・`adr-validator` は引き続き `haiku`（読み取り専用・機械的チェック）

## 今後の見直しポイント

- `sonnet` のモデルバージョンが向上し `opus` との品質差が縮まった場合
- パイプラインに新たな最終ゲートエージェントが追加された場合
