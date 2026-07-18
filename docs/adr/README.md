# ADR 索引

このディレクトリは Architecture Decision Record (ADR) を管理する。
ADR の作成・更新・Supersede は `adr-governance` サブエージェントまたは `/adr-governance` スキルが行う。
命名規則・テンプレート・運用ルールの詳細は [.claude/rules/adr-rules.md](../../.claude/rules/adr-rules.md) を参照。

## ADR一覧（カテゴリ別索引）

ADRが増えてもファイル・連番は分割せず、この表でカテゴリ別に把握する。
新規ADRを追加・Supersedeした際は、この表にも行を追加・更新すること。
AI-ADR（`AI-ADR-` プレフィックス）は [docs/agents/README.md](../agents/README.md) の索引で一元管理するため、この表には含めない。

| ADR | タイトル | カテゴリ | ステータス |
|---|---|---|---|
| [CICD-ADR-0001](CICD-ADR-0001-CI品質ゲートとDependabot運用方針.md) | CI品質ゲートとDependabot運用方針 | CI/CD | Superseded → CICD-ADR-0002 |
| [CICD-ADR-0002](CICD-ADR-0002-CIトリガー分離とWorkflow検証運用方針.md) | CIトリガー分離とWorkflow検証運用方針 | CI/CD | Accepted |
| [CICD-ADR-0003](CICD-ADR-0003-複雑度しきい値によるCIフェイル条件導入.md) | 複雑度しきい値によるCIフェイル条件導入 | CI/CD | Accepted |
| [CICD-ADR-0004](CICD-ADR-0004-コミットメッセージベースのPRサマリー自動コメント導入.md) | コミットメッセージベースのPRサマリー自動コメント導入 | CI/CD | Accepted（決定4のみCICD-ADR-0005で置換） |
| [CICD-ADR-0005](CICD-ADR-0005-PR本文への変更内容自動反映方式への変更.md) | PR本文への変更内容自動反映方式への変更 | CI/CD | Accepted |
| [CICD-ADR-0006](CICD-ADR-0006-CIテストレポート方式にGradle-testLoggingを採用.md) | CIテストレポート方式にGradle testLoggingを採用 | CI/CD | Accepted |
| [APP-ADR-0004](APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md) | 永続化技術スタックの導入（Flyway / MyBatis / PostgreSQL） | コーディング | Accepted |
| [APP-ADR-0008](APP-ADR-0008-DDD-CQRSアーキテクチャ原則の採用.md) | DDD + CQRS アーキテクチャ原則の採用 | コーディング | Accepted |
| [APP-ADR-0009](APP-ADR-0009-APIパスにバージョンプレフィックスを含めない.md) | API パスにバージョンプレフィックスを含めない | コーディング | Accepted |
| [APP-ADR-0010](APP-ADR-0010-UseCaseのInputOutputをネストしたdataclassで定義しBuilderを使わない.md) | UseCase の Input/Output 設計パターン（ネスト data class） | コーディング | Accepted |
| [APP-ADR-0011](APP-ADR-0011-Testcontainersコアを1.20.4に固定しSpringBoot4xのBOM管理2xを回避.md) | Testcontainers コアを 1.20.4 に固定し Spring Boot 4.x の BOM 管理（2.x）を回避 | コーディング | Superseded → APP-ADR-0012 |
| [APP-ADR-0012](APP-ADR-0012-Testcontainersを2.0.5へ移行しTestConfiguration直接起動方式を採用.md) | Testcontainers を 2.0.5 へ移行し @TestConfiguration 直接起動方式を採用 | コーディング | Superseded → APP-ADR-0013 |
| [APP-ADR-0013](APP-ADR-0013-Testcontainers統合テストをServiceConnection方式へ移行.md) | Testcontainers 統合テストを @ServiceConnection 方式へ移行 | コーディング | Accepted |
| [APP-ADR-0014](APP-ADR-0014-JWT戦略-自前JWT発行を採用.md) | JWT戦略（自前JWT発行を採用） | コーディング | Accepted |
| [APP-ADR-0015](APP-ADR-0015-DDDエンティティは振る舞いを持つ通常classとして実装し値オブジェクトのdataclassと区別する.md) | DDD エンティティは振る舞いを持つ通常 class として実装し、値オブジェクトの data class と区別する | コーディング | Accepted |
| [APP-ADR-0016](APP-ADR-0016-Repository実装をMyBatis統一しリフレクション対象を中間DTOに限定する.md) | Repository 実装を MyBatis に統一し、リフレクション対象をエンティティ本体ではなく中間 DTO に限定する | コーディング | Accepted |
| [APP-ADR-0001](APP-ADR-0001-テーブル設計共通方針.md) | テーブル設計共通方針 | 業務仕様 | Accepted |
| [APP-ADR-0002](APP-ADR-0002-星取表マスタと経歴書のデータ連携方針.md) | 星取表マスタと経歴書のデータ連携方針 | 業務仕様 | Accepted |
| [APP-ADR-0003](APP-ADR-0003-経歴書のマスク範囲-コンタクト経路-ファイル出力範囲のスコープ判断.md) | 経歴書のマスク範囲・コンタクト経路・ファイル出力範囲のスコープ判断 | 業務仕様 | Accepted（決定4のみAPP-ADR-0007で置換） |
| [APP-ADR-0005](APP-ADR-0005-楽観ロックにversionカラム整数カウンタを採用.md) | 楽観ロックにversionカラム（整数カウンタ）を採用 | 業務仕様 | Accepted |
| [APP-ADR-0006](APP-ADR-0006-accounts.statusに4値設計（deactivated追加）と非adminからのsuspended-deactivated除外.md) | accounts.statusに4値設計（deactivated追加）と非adminからのsuspended/deactivated除外 | 業務仕様 | Accepted |
| [APP-ADR-0007](APP-ADR-0007-rolesをpermissionベースに再定義しvisibility_rulesを廃止.md) | `roles` を権限（Permission）ベースに再定義し `visibility_rules` を廃止 | 業務仕様 | Accepted |
| [DOC-ADR-0001](DOC-ADR-0001-ドキュメントにchangelogセクションを持たない.md) | ドキュメントにchangelogセクションを持たない（git + ADRリンクで代替） | ドキュメント方針（DOC-ADR） | Accepted |
| [DOC-ADR-0002](DOC-ADR-0002-docs-READMEをフォルダ索引粒度に整理する.md) | docs/README.md をフォルダ索引粒度（1行説明付き）に整理する | ドキュメント方針（DOC-ADR） | Accepted |
| [DOC-ADR-0003](DOC-ADR-0003-ADR運用ルールをclauderulesに移管しREADMEを索引に特化させる.md) | ADR 運用ルールを .claude/rules/ に移管し README を索引に特化させる | ドキュメント方針（DOC-ADR） | Accepted |

## ADRエージェント運用

ADR 更新（影響分析・ドラフト作成・Supersede 処理・命名検証）は ClaudeCode の `/adr-governance` スキルまたは `adr-governance` サブエージェントが支援する。
エージェント定義・詳細な運用ルールは [.claude/agents/adr-governance.md](../../.claude/agents/adr-governance.md) を参照。
