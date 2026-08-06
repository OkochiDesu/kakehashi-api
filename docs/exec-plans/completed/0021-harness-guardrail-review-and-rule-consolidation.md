# 0021: ハーネス・ガードレール見直しとコーディングルール集約

## 完了条件（Definition of Done）

- ハーネス・ガードレール（`.claude/agents`・`.claude/rules`・`.claude/hooks`・`.claude/skills`・CLAUDE.md/AGENTS.md・docs/配下の運用ドキュメント）の全体チェックが完了し、指摘事項が棚卸しされている
- 棚卸しで洗い出された改善項目に順次対応済み。少なくとも以下を含む:
  - 実装・レビューエージェント（`kotlin-implementer`/`code-reviewer`等）が参照する実装ルール（KDoc規約・エラーメッセージ規約・命名規則等）が単一のコーディングルールドキュメントに集約され、各エージェント定義は参照のみで実体を持たない
- 対応結果が AGENTS.md / docs/agents/README.md 等の索引に反映済み
- PR 作成・マージ済み

## 目的・スコープ

開発が進み `.claude/agents`・`.claude/rules`・docs/ 配下の md ファイルの情報量が増えてきたため、AIエージェントが働きやすい環境（ハーネス・ガードレール）になっているかを一度全体点検する。点検で見つかった改善項目に順次対応し、その中に「実装で使うコーディングルールをエージェントごとに重複させず単一ドキュメントに集約する」という具体的な改善を含める。

この集約の必要性は、exec-plan 0006 の PR #21 対応中に実際に発生した事象（override メソッドの KDoc 省略例外というルール1つを `docs/conventions/kdoc-and-test-policy.md`・`.claude/agents/kotlin-implementer.md`・`.claude/agents/code-reviewer.md` の3ファイルへ同時に反映する必要が生じた）で具体的に確認済みの課題である。

exec-plan 0020（Account エンティティ・Repository リファクタリング）完了後に着手する。

## 進捗状況

### ① 全体チェック（監査フェーズ）

- [x] `doc-maintainer-structure` / `doc-maintainer-content` を全体スコープ（diffスコープではなくリポジトリ全体）で実行し、陳腐化・不整合・索引漏れを洗い出す
- [x] `.claude/agents/` 配下の各エージェント定義に、実装ルール（KDoc・エラーメッセージ・命名規則等）の重複記載がないか棚卸しする（`kotlin-implementer.md` / `code-reviewer.md` / `test-scenario-planner.md` 等）
- [x] `docs/agents/navigation-metrics.md` のナビゲーション指標を確認し、閾値超過があれば対応要否を判断する（直近5件は探索コスト1,0,1,2,1で閾値未超過。対応不要と判断）
- [x] 監査結果を本 exec-plan の「意思決定ログ」に一覧化し、対応する/しない項目をユーザーと合意する

### ② コーディングルール集約（プラン2）

- [x] 実装ルールの集約先ドキュメントを決定する（新規作成 or 既存 `docs/conventions/kdoc-and-test-policy.md` の拡張、どちらが良いかユーザーと相談）→ 既存ドキュメントを正本化する方針で合意
- [x] `kotlin-implementer.md` / `code-reviewer.md` 等に重複記載されている実装ルールを集約先への参照に置き換える
- [x] `mybatis-rules.md` / `test-rules.md` 等、既存 glob ルールとの役割分担を整理する（調査の結果、`kotlin-implementer.md`/`code-reviewer.md`はどちらも本文転記なしで3項目程度の短い要約付きリンクに留まっており、KDocルールのような重複はなし。追加対応不要と判断）
- [x] doc-maintainer チェックで重複・矛盾が解消されたことを確認する（`doc-maintainer-structure` をdiffスコープで再実行し指摘0件を確認）

### ③ その他監査で見つかった改善項目

- [x] S1: `README.md#4-アーキテクチャ原則adr化予定の方針` のリンク切れ（5ファイル・7箇所）を修正
- [x] S2: `docs/references/harness-engineering/openai-harness-engineering.md`（196行）に目次がなかった問題を、見出し11箇所の`##`化＋ToC追加で解消
- [x] C1/C2: APP-ADR-0015/0016の「影響」欄が exec-plan 0020 完了前提の未来形記述のまま陳腐化していた問題を修正
- [x] C3: `navigation-metrics.md` の「チェック項目8」が doc-maintainer 分割後の現行番号（項目6）とずれていた問題を修正
- [x] N1/N2: `DOC-ADR-0001`・`AI-ADR-0003`に残っていた分割前の`.claude/agents/doc-maintainer.md`（AI-ADR-0011で`doc-maintainer-structure.md`/`doc-maintainer-content.md`に分割済み）へのリンク切れを修正
- [x] N3: `.claude/rules/exec-plan-rules.md`（99行）に目次がなかった問題をToC追加で解消

（T1: TODO.md「冪等性キーチェック基盤」のexec-plan昇格は本exec-planのスコープ外と判断し見送り。詳細は「残課題・引き継ぎ事項」参照）

### ④ 仕上げ

- [x] AGENTS.md / docs/agents/README.md 等の索引更新（今回の変更はエージェント定義内部のルール記述圧縮のみで役割・責務は不変のため、索引側の更新は不要と判断）
- [x] doc-maintainer チェック実施（全体スコープ2回・diffスコープ2回、最終的に指摘0件を確認）
- [x] PR 作成・マージ: [PR #24](https://github.com/OkochiDesu/kakehashi-api/pull/24) 作成・マージ済み（2026-07-20）

## 意思決定ログ

- 2026-07-19: ユーザーから「①ハーネス全体チェック→②その中でコーディングルール集約（プラン2）を含め順次対応」という進め方の合意を得て、1つの exec-plan として起票した。exec-plan 0020 完了後に着手する順序で合意した。
- 2026-07-19: exec-plan 0020（PR #23）のレビュー対応完了に伴い、`pending/`から`active/`へ移動し着手可能な状態にした。exec-plan 0020対応中に、KDoc規約（`@property`タグ・非自明なoverrideの説明）を`kdoc-and-test-policy.md`・`kotlin-implementer.md`・`code-reviewer.md`の3ファイルへ同時反映する事象が再度発生しており（②コーディングルール集約の必要性を裏付ける追加事例）、①監査フェーズで参照すること。
- 2026-07-20: PR #23 マージ済み確認後、`feature/harness-guardrail-review` ブランチを origin/main から新規作成し着手。
- 2026-07-20: ①監査フェーズを `doc-maintainer-structure` / `doc-maintainer-content` の全体スコープ並列実行で完了。指摘7件（structure 2件・content 3件・TODO昇格候補1件・KDocルール三重重複1件）をユーザーに提示し、対応方針を個別に合意した:
  - S1（リンク切れ5ファイル）・S2（ToC欠落1件）・C1/C2（APP-ADR-0015/0016の影響欄陳腐化）・C3（navigation-metrics.mdのチェック項目番号ずれ）→ 全件今回対応
  - T1（TODO.md「冪等性キーチェック基盤」のexec-plan昇格）→ 0021のスコープ外のため今回は見送り、TODO.mdのまま据え置き
  - R1（KDocルールの三重重複）→ 既存 `docs/conventions/kdoc-and-test-policy.md` を正本とする方針で合意（新規ドキュメント作成は不採用）
- 2026-07-20: R1対応として `kotlin-implementer.md`「KDoc・コメントルール」を全面圧縮し `kdoc-and-test-policy.md` への参照に置き換えた。合わせて `code-reviewer.md` 側で同じ内容が復元されていた「KDoc品質」「型安全・null安全」「エラーハンドリング」「ステータスチェック特定性」の各チェック項目も、`kotlin-implementer.md` / `kdoc-and-test-policy.md` への参照形式に圧縮した（R1で報告した3ファイル重複に加え、kotlin-implementer.md↔code-reviewer.md間の非KDoc実装ルール重複（正規表現アンカー・Output DTOのNothing?回避・runCatching.getOrNull()回避・ステータスチェック特定性）も同一パターンとして合わせて解消）。
- 2026-07-20: C1/C2（ADR「影響」欄の鮮度修正）は、決定内容自体を変更するものではなく既存記述に完了事実を追記する性質のため、[adr-rules.md](../../.claude/rules/adr-rules.md) の「軽微な誤字・表現補足 → 既存ADRを直接修正してよい」の適用範囲と判断し、`adr-governance` を呼ばず直接編集した。
- 2026-07-20: `mybatis-rules.md` / `test-rules.md` を調査した結果、`kotlin-implementer.md`/`code-reviewer.md`からの参照は本文転記のない短い要約リンクに留まっており、KDocルールで発生したような3ファイル間の重複はないと判断。追加対応なしで②を完了とした。
- 2026-07-20: コミット f953f30 後、④仕上げの一環としてPR作成前の定期チェック（doc-maintainer-structure/content 全体スコープ）を実施。今回のコミット自体には新たな不整合はなかったが、コミット対象外の既存ファイルで新規3件（N1: DOC-ADR-0001のリンク切れ、N2: AI-ADR-0003のリンク切れ、N3: exec-plan-rules.mdのToC欠落）を検出。ユーザーと合意の上、いずれも今回のセッションで対応した（N1/N2はAI-ADR-0011で分割済みの`doc-maintainer.md`への参照修正のため、決定内容を変更しない軽微な修正としてadr-governanceを呼ばず直接編集）。
- 2026-07-20: N1〜N3をコミット（f72526d）後、`feature/harness-guardrail-review`をpushし [PR #24](https://github.com/OkochiDesu/kakehashi-api/pull/24) を作成した。ソースコード変更を含まないため「動作確認（手動）」セクションは省略。①〜③および④の索引確認・doc-maintainerチェックは全項目完了、残るはPRレビュー・マージのみ。
- 2026-07-20: PR #24 マージ済み（`main` の `3d4181d`）を確認。全進捗項目が `[x]` になったため `completed/` へ移動する。

## 残課題・引き継ぎ事項

- T1: TODO.md「冪等性キーチェック基盤」（Spring Interceptor + AOP + Redis）が exec-plan 昇格基準（DoD・主要タスク3件以上・PR目的）を満たしていると `doc-maintainer-content` の全体監査で指摘された。本exec-planのスコープ外のため対応は見送り、TODO.mdに残したまま。着手判断は別セッションで行う
- TODO候補: 「VS Code上でCopilotが実装ルールを守れるよう設定ファイルを追加」（[docs/TODO.md:94-101](../../docs/TODO.md)）。前提条件「アーキテクチャが固まってから」は、Step1（アカウントドメイン）範囲ではAPP-ADR-0008/0015/0016確定・exec-plan 0020実装完了・ArchUnit稼働により実質的に満たされていると`doc-maintainer-content`の④仕上げチェックで指摘された。ただしStep2（Resume/Skill等）は未確定のため、スコープをStep1に限定するか待つかはユーザー判断が必要。着手するかは別セッションで判断する
