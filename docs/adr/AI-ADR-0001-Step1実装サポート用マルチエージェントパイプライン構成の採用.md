# AI-ADR-0001: Step1実装サポート用マルチエージェントパイプライン構成の採用

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-06-16

## 関連

- Supersedes: なし
- Superseded by: なし
- 関連: [AI-ADR-0010](AI-ADR-0010-src配下README自動生成によるHITL可視性確保.md)（クラス図生成エージェント追加による拡張）

## 背景

Step1の要件定義（A〜E確定・ADR化）が概ね完了し、実装フェーズへ移行する段階に入った。
実装フェーズ（Flywayマイグレーション → API設計 → Kotlin実装 → レビュー）を効率化し、かつ品質を担保するためのエージェント構成が必要になった。

当初のexec-plan（[0001-requirements-definition-multiagent.md](../exec-plans/active/0001-requirements-definition-multiagent.md)）では、Phase 2のサブエージェントを「要件定義用」（コンテキスト収集・ドメイン分析・要件ドラフト・レビュー）として計画していたが、要件定義が概ね完了したため、実装サポートの優先度が高くなった。

## 決定

Step1実装サポート用として、以下の4エージェントによる順次パイプライン構成を採用する。

```
[db-designer] → [api-designer] → [kotlin-implementer] → [class-diagram-updater] → [src-doc-maintainer] → [code-reviewer] → 人間確認 → commit
```

| エージェント | 役割 | 入力 | 出力 |
|---|---|---|---|
| [db-designer](../../.claude/agents/db-designer.md) | FlywayマイグレーションSQL設計・作成 | data-models.md / ADR | `V*.sql` |
| [api-designer](../../.claude/agents/api-designer.md) | REST APIエンドポイント設計書生成 | ui-flows.md / data-models.md | `docs/design/api/*.md` |
| [kotlin-implementer](../../.claude/agents/kotlin-implementer.md) | Spring Boot (Kotlin) 実装 | API設計書 / data-models.md | Kotlinコード + テスト |
| [class-diagram-updater](../../.claude/agents/class-diagram-updater.md) | src/ 配下 README.md のクラス図自動生成・更新 | 実装コード | `src/***/README.md` |
| [src-doc-maintainer](../../.claude/agents/src-doc-maintainer.md) | src/ 内 README.md とコードの整合性チェック（読み取り専用） | 実装コード + README.md | OK / REQUIRES_FIX レポート |
| [code-reviewer](../../.claude/agents/code-reviewer.md) | ADR・セキュリティ・仕様適合レビュー | 実装コード | APPROVED / REQUIRES_CHANGES |

加えて、ヒューマンインザループを以下のとおり維持する。

- api-designer の設計書を人間がレビュー・承認してから実装に進む。
- code-reviewer が **APPROVED** を出した後、人間が最終確認したうえで commit する。
- AIレビューでAPPROVEDになった実装のみを人間に上げることで、人間の確認負荷を抑えつつ品質を担保する。

## 代替案

- **シングルエージェントによる全工程実施**: 1エージェントが設計から実装・レビューまでを担う案。役割分担がなくなり、各工程の専門性（DB設計方針・API設計・レビュー観点）を個別のプロンプトで保証できず、自己レビューによる見落としリスクが高いため不採用。
- **全自動commit**: code-reviewer がAPPROVEDを出した時点でAIが自動的にcommitまで実施する案。1機能・1PR単位でのヒューマンインザループが失われ、誤った変更がそのままリポジトリに入るリスクがあるため不採用。commit前の人間確認は本リポジトリのcommit運用（[CLAUDE.md](../../CLAUDE.md)）とも整合する。

## 影響

- 実装フェーズは「設計 → 人間承認 → 実装 → クラス図生成・整合確認 → レビュー → 人間確認 → commit」という固定フローに従う。
- 各エージェントは入力ドキュメント（data-models.md / ui-flows.md / API設計書 / ADR）を唯一の根拠とするため、これらのドキュメントの整備状態が成果物の品質を左右する。
- code-reviewer → kotlin-implementer の差し戻しループが発生しうる。これが自動実行できない場合の救済措置として `/implement-review-loop` スキルを用意している（[AI-ADR-0004](AI-ADR-0004-implement-review-loopスキルの救済措置としての位置づけ.md)）。

## 今後の見直しポイント

- Step2開始時または手戻り発生時には、要件定義用エージェント（コンテキスト収集・ドメイン分析・要件ドラフト・レビュー）の追加を別途検討する。
- 試運転（小さなタスクでの動作確認）の結果、パイプラインの順序や役割分担に課題が見つかった場合は本構成を見直す。
