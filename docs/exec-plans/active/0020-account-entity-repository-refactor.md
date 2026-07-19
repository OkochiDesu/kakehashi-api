# 0020: Accountエンティティのclass化・Repository実装のMyBatis統一

## 完了条件（Definition of Done）

- `Account`エンティティが`data class`ではなく通常`class`として実装され、`private constructor` + `companion object`ファクトリ（`reconstruct()`等）・ID基準の`equals()`/`hashCode()`手書き実装・PII安全な`toString()`・`withChanges()`privateヘルパーを備えている（APP-ADR-0015準拠）
- `AccountRepositoryImpl`がMyBatis経由で実装され、MyBatisのリフレクション対象がエンティティ本体ではなく中間DTO（`AccountRow`等）に限定されている（APP-ADR-0016準拠）
- 既存の単体テスト・統合テストが新設計に追随し、全件PASSしている

## 目的・スコープ

APP-ADR-0015（DDDエンティティは通常classとして実装）・APP-ADR-0016（Repository実装をMyBatis統一しリフレクション対象を中間DTOに限定）で決定した方針を、実際に`Account`エンティティおよび`AccountRepositoryImpl`へ適用する。両ADRとも「方針決定のみで実装はスコープ外、別ブランチで後日実施」と明記しており、本exec-planがその「後日」を追跡する。

対象は`Account`ドメイン（`src/main/kotlin/com/kakehashi/domain/account/Account.kt`・`src/main/kotlin/com/kakehashi/infrastructure/account/AccountRepositoryImpl.kt`）。Resume・Skill等、今後実装するエンティティ（exec-plan 0009・0010）は、実装時点でAPP-ADR-0015/0016の方針に最初から従うため、本exec-planの対象外（追いかけリファクタリング不要）。

## 進捗状況

- [x] `Account.kt`を`data class`から通常`class`へリファクタリング（`private constructor`・`companion object.reconstruct()`・ID基準`equals()`/`hashCode()`・PII安全`toString()`・`withChanges()`privateヘルパー）。新規メソッド`assignRoles(updatedBy)`（ロール変更に伴うversion bump専用）・`provision()`ファクトリ（新規発行用）も追加
- [x] `AccountRepositoryImpl`をMyBatis化: 中間DTO（`AccountRow`・`AccountRoleInsertRow`）を新設し、`AccountMapper`（`@Mapper`）経由でMyBatisがリフレクションで触れる対象を中間DTOに限定する。読み取り方向は`AccountRow`→`Account.reconstruct(...)`、書き込み方向は`Account`のフィールド→`AccountRow`への詰め替え
- [x] 既存の単体テスト（`AccountTest.kt`・`AccountTestFixtures.kt`等）・統合テスト（`AccountRepositoryImplIntegrationTest.kt`）を新設計に追随させる。equals/hashCode・toString・assignRolesの新規テストも追加
- [x] `.claude/agents/kotlin-implementer.md`を更新（APP-ADR-0015のエンティティ実装方針・APP-ADR-0016のMyBatis統一方針を反映）。`account-domain-agent.md`は業務ルール専用で実装スタイルに触れないため変更不要と判断
- [x] `docs/architecture/package-structure.md`にエンティティ実装方針（通常class、APP-ADR-0015）・Repository実装方針（MyBatis統一、APP-ADR-0016）を補記
- [x] コードレビュー（`code-reviewer` → `test-reviewer`）: 両者ともAPPROVED（FAIL項目なし）
- [ ] PR作成・マージ

## 意思決定ログ

- 2026-07-17: exec-plan 0006（Google SSO実装）のPRレビュー中の議論から、`Account`エンティティの`data class`実装への懸念が提起され、APP-ADR-0015（エンティティ実装方針）・APP-ADR-0016（Repository実装方針）として方針を確定。実装はスコープを分離し、本exec-planとして別途起票した。
- 2026-07-19: exec-plan 0006（PR #18/#19/#21）が全項目完了・マージ済みとなり`completed/`へ移動。次に着手するexec-planとして本0020をユーザーが選定し、`pending/`から`active/`へ移動して着手を決定した。
- 2026-07-19: 実装中、TDDのred確認（テスト追加後・実装前に実行して失敗を確認する）を怠っていたとユーザーから指摘を受けた。再発防止として`.claude/rules/test-rules.md`のTDDセクションにred確認の明示的ステップ・コンパイルレベル変更時の扱い・Testcontainers統合テストのCI依存の扱いを追記した。
- 2026-07-19: `AssignRolesUseCase`の`copy()`呼び出し（version bumpのみ、ロール自体はAccountのフィールドではない）は、新規ドメインメソッド`assignRoles(updatedBy)`として`Account`に追加する設計とした。ガード不要な単純なversion bumpのため`check()`は付与していない。
- 2026-07-19: 実装・テストともにcode-reviewer/test-reviewerでAPPROVED（FAIL項目なし）。作業中に発見した`usecase/account/README.md`の既存ドリフト（`AssignRolesUseCase_Input`のフィールド名不一致等、本exec-plan起因ではない）も同一コミットで解消した。

## 残課題・引き継ぎ事項

- APP-ADR-0016の「単一集約読み取り vs 集約をまたぐ読み取り」の区別に注意すること。`GetAccountQuery`（`AccountDetailRow`、roles結合）・`ListAccountsQuery`（`AccountSummaryRow`、部分射影）は本exec-planの対象外（既存のQuery側DTOパターンを維持する）。リファクタリング対象は`AccountRepositoryImpl`（Command側の単一集約読み取り・書き込み）のみ
