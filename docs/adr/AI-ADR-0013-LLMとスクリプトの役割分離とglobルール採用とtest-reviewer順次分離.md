# AI-ADR-0013: LLMとスクリプトの役割分離・globルール採用・test-reviewer順次分離

## 目次

- [ステータス](#ステータス)
- [日付](#日付)
- [関連](#関連)
- [背景](#背景)
- [決定](#決定)
  - [決定1: LLM/スクリプトの役割分離原則](#決定1-llmスクリプトの役割分離原則)
  - [決定2: .claude/rules/ glob ルールパターンの採用](#決定2-clauderules-glob-ルールパターンの採用)
  - [決定3: test-reviewer を code-reviewer から分離し順次呼び出し](#決定3-test-reviewer-を-code-reviewer-から分離し順次呼び出し)
- [代替案](#代替案)
- [影響](#影響)
- [今後の見直しポイント](#今後の見直しポイント)

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
- 関連: [AI-ADR-0001](AI-ADR-0001-Step1実装サポート用マルチエージェントパイプライン構成の採用.md)（マルチエージェントパイプライン構成）
- 関連: [AI-ADR-0005](AI-ADR-0005-スキルの救済措置パターンと設計原則.md)（スキルの救済措置パターン）
- 関連: [AI-ADR-0011](AI-ADR-0011-doc-maintainerの構造チェックと内容チェックへの分割.md)（doc-maintainer分割、同様の「役割に応じてエージェントを分割する」方針）
- exec-plan: [0002-agent-rules-refactoring](../../docs/exec-plans/active/0002-agent-rules-refactoring.md)

## 背景

`code-reviewer.md` / `kotlin-implementer.md` にテスト観点・MyBatis 観点・スクリプト化可能な観点が蓄積し、以下の問題が顕在化した。

1. **LLM/スクリプトの役割が未分離**: 決定論的パターン検出（英語メッセージ・assert() 誤使用）と意味的チェック（ADR 準拠・設計整合）が同一エージェントに混在していた。スクリプトで確実・高速に検出できるものを LLM に任せることは非効率かつ信頼性が低い。
2. **ファイルタイプ限定規約の全体読み込み**: テスト規約・MyBatis 規約はそれぞれ `*Test.kt` / `*Mapper.xml` を編集するときにしか使わないが、全セッションで読み込まれていた。対象外ファイルのセッションでのコンテキスト浪費・エージェントの注意分散が問題だった。
3. **テストレビューと本体レビューが同一エージェント**: 本体コードに REQUIRES_CHANGES が出た場合、テストも書き直しになることが多いため、本体コードレビュー完了前にテストをレビューする構造は非効率だった。

## 決定

### 決定1: LLM/スクリプトの役割分離原則

決定論的パターンチェックは `.githooks/pre-commit` スクリプトに移管し、意味的・設計的チェックのみ LLM エージェントが担う。

| 役割 | 担当 | 理由 |
|------|------|------|
| 英語エラーメッセージ検出（`require/check/Exception` の引数） | `.githooks/pre-commit` スクリプト | grep で確実・高速に検出可能 |
| `assert()` 誤使用検出（JVM `-ea` 依存の危険なアサーション） | `.githooks/pre-commit` スクリプト | 正規表現で確実に検出可能 |
| ADR 準拠・アーキテクチャ整合・KDoc 正確性・設計適合 | LLM エージェント（code-reviewer 等） | 文脈・意図の理解が必要 |

### 決定2: `.claude/rules/` glob ルールパターンの採用

ファイルタイプ限定のコンテキストを `.claude/rules/` 配下の glob ルールファイルとして管理し、`globs:` フロントマターで適用対象を限定する。

| ファイル | 適用対象 | 内容 |
|----------|----------|------|
| `.claude/rules/test-rules.md` | `**/*Test.kt` | TDD・アサーション種別・updatedAt検証・楽観ロック競合テスト・テスト命名 |
| `.claude/rules/mybatis-rules.md` | `**/*Mapper.xml`, `**/*Mapper.kt` | `<id>`タグ・`notNullColumn`・`#{}`使用・`@param`必須 |

対象外ファイルのセッションではこれらのルールファイルが読み込まれず、コンテキスト効率が向上する。

### 決定3: test-reviewer を code-reviewer から分離し順次呼び出し

テストコードレビューを `test-reviewer` 専用エージェント（`.claude/agents/test-reviewer.md`）に切り出し、`code-reviewer` が **APPROVED** を返した後に**順次（直列）**呼び出す。

呼び出し順序: `kotlin-implementer` → `code-reviewer` → (APPROVED) → `test-reviewer` → (APPROVED) → 人間確認

## 代替案

### 決定1の代替案

**全パターンチェックを code-reviewer に残す**: LLM による確認は確率的であり、英語メッセージを見落とす可能性がある。スクリプトに移管することで確実性が上がるため採用しない。

### 決定2の代替案

**全規約を `CLAUDE.md` や各エージェントに記述する**: ファイル数は減るが、エージェントの肥大化と関心事の混在が発生する。対象外ファイルを扱うセッションでも不要なルールを読み込む非効率が残るため採用しない。

### 決定3の代替案

**code-reviewer と test-reviewer を並列で呼び出す**: 本体コードに REQUIRES_CHANGES が出た場合、テストも書き直しになることが多く、並列実行ではテストレビューが無駄になる。また code-reviewer の指摘をテストレビューに反映できないため採用しない。

## 影響

- `.githooks/pre-commit` に英語メッセージ検出・assert() 誤使用検出のスクリプトブロックを追加済み（commit: 794647f）
- `.claude/rules/test-rules.md` / `.claude/rules/mybatis-rules.md` を新規作成済み
- `.claude/agents/test-reviewer.md` を新規作成済み
- `code-reviewer.md` / `kotlin-implementer.md` から移管済み観点を削除し cross-reference に置換済み（commit: 1f86e85）
- `implement-review-loop` スキルに test-reviewer 呼び出しステップを追加済み
- `AGENTS.md` / `docs/agents/README.md` に test-reviewer・glob ルールセクションを追記済み

## 今後の見直しポイント

- 新たな決定論的チェックが必要になった場合は pre-commit に追加する（LLM エージェントには追加しない）
- glob ルールは `.claude/rules/` に追加・管理する（エージェント定義には埋め込まない）
- Controller 層専用など他のファイルタイプに限定した規約が増えてきたら同様に glob ルール化を検討する
