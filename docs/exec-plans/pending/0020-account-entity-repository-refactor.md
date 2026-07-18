# 0020: Accountエンティティのclass化・Repository実装のMyBatis統一

## 完了条件（Definition of Done）

- `Account`エンティティが`data class`ではなく通常`class`として実装され、`private constructor` + `companion object`ファクトリ（`reconstruct()`等）・ID基準の`equals()`/`hashCode()`手書き実装・PII安全な`toString()`・`withChanges()`privateヘルパーを備えている（APP-ADR-0015準拠）
- `AccountRepositoryImpl`がMyBatis経由で実装され、MyBatisのリフレクション対象がエンティティ本体ではなく中間DTO（`AccountRow`等）に限定されている（APP-ADR-0016準拠）
- 既存の単体テスト・統合テストが新設計に追随し、全件PASSしている

## 目的・スコープ

APP-ADR-0015（DDDエンティティは通常classとして実装）・APP-ADR-0016（Repository実装をMyBatis統一しリフレクション対象を中間DTOに限定）で決定した方針を、実際に`Account`エンティティおよび`AccountRepositoryImpl`へ適用する。両ADRとも「方針決定のみで実装はスコープ外、別ブランチで後日実施」と明記しており、本exec-planがその「後日」を追跡する。

対象は`Account`ドメイン（`src/main/kotlin/com/kakehashi/domain/account/Account.kt`・`src/main/kotlin/com/kakehashi/infrastructure/account/AccountRepositoryImpl.kt`）。Resume・Skill等、今後実装するエンティティ（exec-plan 0009・0010）は、実装時点でAPP-ADR-0015/0016の方針に最初から従うため、本exec-planの対象外（追いかけリファクタリング不要）。

## 進捗状況

- [ ] `Account.kt`を`data class`から通常`class`へリファクタリング（`private constructor`・`companion object.reconstruct()`・ID基準`equals()`/`hashCode()`・PII安全`toString()`・`withChanges()`privateヘルパー）
- [ ] `AccountRepositoryImpl`をMyBatis化: 中間DTO（`AccountRow`等）を新設し、`AccountMapper`（`@Mapper`）経由でMyBatisがリフレクションで触れる対象を中間DTOに限定する。読み取り方向は`AccountRow`→`Account.reconstruct(...)`、書き込み方向は`Account`のフィールド→`AccountRow`への詰め替え
- [ ] 既存の単体テスト（`AccountTest.kt`・`GoogleSsoCallbackUseCaseTest.kt`等）・統合テスト（`AccountRepositoryImplIntegrationTest.kt`）を新設計に追随させる
- [ ] `.claude/agents/kotlin-implementer.md`・`.claude/agents/account-domain-agent.md`がAPP-ADR-0015/0016の方針を反映しているか確認し、必要なら更新する
- [ ] `docs/architecture/package-structure.md`にエンティティ実装方針（通常class、APP-ADR-0015）の補記を検討する
- [ ] コードレビュー（`code-reviewer` → `test-reviewer`）
- [ ] PR作成・マージ

## 意思決定ログ

- 2026-07-17: exec-plan 0006（Google SSO実装）のPRレビュー中の議論から、`Account`エンティティの`data class`実装への懸念が提起され、APP-ADR-0015（エンティティ実装方針）・APP-ADR-0016（Repository実装方針）として方針を確定。実装はスコープを分離し、本exec-planとして別途起票した。

## 残課題・引き継ぎ事項

- APP-ADR-0016の「単一集約読み取り vs 集約をまたぐ読み取り」の区別に注意すること。`GetAccountQuery`（`AccountDetailRow`、roles結合）・`ListAccountsQuery`（`AccountSummaryRow`、部分射影）は本exec-planの対象外（既存のQuery側DTOパターンを維持する）。リファクタリング対象は`AccountRepositoryImpl`（Command側の単一集約読み取り・書き込み）のみ
