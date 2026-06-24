# APP-ADR-0010: UseCase の Input/Output をネストした data class で定義し Builder を使わない

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-06-23

## 関連

- Supersedes: なし
- Superseded by: なし
- 補完: APP-ADR-0008（DDD + CQRS アーキテクチャ原則）— 本 ADR は UseCase の入出力境界の設計パターンを補足する

## 背景

APP-ADR-0008 で UseCase 層を POJO として `usecase/{context}/` に UC 単位で配置する方針を定めたが、UseCase の入出力（Input / Output）をどう表現するかは規定していなかった。`AssignRolesUseCase` の実装レビューで、入出力 DTO の定義方式（Builder パターン / ファクトリメソッド / data class）と、UseCase 内部のみで使う定数の可視性について方針を統一する必要が生じた。

## 決定

UseCase の入出力境界を次の方針で実装する。

1. **Input / Output はネストした data class で定義する**: 各 UseCase クラスの内部に `Input` / `Output`（必要に応じて `RoleOutput` 等の補助型）をネストした `data class` として定義する。
2. **Builder パターン・ファクトリメソッドは使わない**: Kotlin の名前付き引数・デフォルト引数で生成意図が表現できるため、Builder やファクトリメソッドは導入しない。コンストラクタを直接呼び出す。
3. **UseCase 内部の実装詳細は private にする**: companion object 等に置く内部定数（V1 マイグレーションで固定した DB の UUID 等）は、呼び出し側が参照する必要がないため private 修飾子を付与する。

## 代替案

### 代替案A: Builder パターン

Java 由来の Builder を入出力 DTO に導入する。Kotlin では名前付き引数・デフォルト引数で同等の可読性が得られ、Builder は冗長なボイラープレートになるため却下。

### 代替案B: ファクトリメソッド

`Input.of(...)` 等のファクトリメソッドを介して生成する。data class のプライマリコンストラクタと表現力に差がなく、間接層が増えるだけのため却下。

## 影響

- **入出力 DTO の実装**: `AssignRolesUseCase.kt` の `Input` / `Output` / `RoleOutput` は本方針に沿ったネスト data class として既に実装済み。今後追加する UseCase も同方針に従う。
- **可視性の修正**: `AssignRolesUseCase` の companion object にある `ADMIN_ROLE_ID` / `VIEW_PERSONAL_INFO_ROLE_ID` は現状 public な `val` だが、Controller 等の呼び出し側が参照しない内部実装詳細のため `private val` に変更する（本ブランチで実施）。
- **エージェント定義**: 実装方針の決定のため、`kotlin-implementer` / `code-reviewer` の関連記述が陳腐化していないか合わせて確認する。

## 今後の見直しポイント

- 複数 UseCase で同一の Input/Output 構造が再利用される必要が生じた場合（共有 DTO へ切り出すか、ネスト維持か）。
- Kotlin に Builder を要する新たな制約（ネスト深い不変オブジェクトの段階的構築等）が現れた場合。
