# ADR-0004: コミットメッセージベースのPRサマリー自動コメント導入

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-06-13

## 関連

- Supersedes: なし
- Superseded by: なし

## 背景

ADR-0001ではカバレッジコメント、ADR-0003では複雑度コメントが `reports` ジョブから自動投稿されるようになっており、PRレビュー時の可視化情報が `ci.yml` に集約されてきた。

一方で、PRの「変更内容」をコミットメッセージから整形してレビュアーに提示する仕組みは未整備であり、レビュー前のPR概要作成はレビュアー・作成者の手動作業に依存していた。

本プロジェクトでは [Git Prefixes](../conventions/git-prefixes.md) によりConventional Commits（`feat:` / `fix:` / `docs:` / `refactor:` / `test:` / `ci:` / `chore:`）が既に規約として定められており、このプレフィックスを機械的に分類するだけでPRサマリーの下書きを生成できる。

## 決定

### 1. 新規ワークフロー `pr-summary.yml` を追加する

対象ファイル: [`.github/workflows/pr-summary.yml`](../../.github/workflows/pr-summary.yml)

- `ci.yml`（ADR-0001/ADR-0002のverify/reports構成）には組み込まず、独立したワークフローとする
- 既存の `verify`/`reports` ジョブとは責務が異なる（品質ゲート・可視化レポートではなく、コミット履歴の整形）ため、分離して管理する

### 2. トリガーはmainへのPRに限定し、ドキュメントのみのPRも対象にする

- `pull_request` の `opened` / `synchronize` / `reopened` で起動する
- ADR-0002の paths-ignore（`**/*.md` と `.github/workflows/**` のみの変更で重いCIを止める方針）とは独立に運用する
  - 本ワークフローは軽量（チェックアウトとシェルスクリプトのみ）であり、ドキュメントのみのPRでもCIコストへの影響は小さいため、対象から除外しない
  - これによりドキュメントのみのPRでもサマリーコメントが投稿される

### 3. AIモデル（LLM API / Copilot premium request）は使用しない

- PRのbase..head間のコミットメッセージを `git log` で取得し、シェルスクリプトでプレフィックスごとに分類するのみ
- 規約に従っていないコミットは「その他（Conventional Commits形式に従っていないコミット）」セクションに集約する
- 背景・意図の要約（「なぜ変更したか」）は対象外とし、[PRテンプレート](../../.github/pull_request_template.md)の「概要」「動作確認」「気になる部分」は引き続き手動記載とする

### 4. コメント投稿は既存パターンと同じ `marocchino/sticky-pull-request-comment` を利用する

- header: `pr-summary` で他のコメント（カバレッジ・複雑度）と区別する
- ADR-0001/ADR-0003と同様、外部ActionはSHA固定で利用する
- マージ可否には影響しない（コメント投稿のみ。Required checksには含めない）

## 代替案

### 代替案A: `ci.yml` の `reports` ジョブに統合する

- 長所: ワークフローファイルが増えず、既存の可視化コメント群と一元管理できる
- 短所: `reports` ジョブはADR-0002のpaths-ignoreによりドキュメントのみのPRでは起動しない。ドキュメントのみのPRでもサマリーを出したい本決定の目的と矛盾する

### 代替案B: AIモデル（Copilot premium request等）でPR概要を要約する

- 長所: 「なぜ変更したか」を含む高品質な要約が得られる可能性がある
- 短所: トークン・利用料が発生し、ADR-0001で示された「安全側でのコスト運用」方針と整合しにくい。コミットメッセージの規約（Git Prefixes）が既に整備されており、機械的分類で十分な価値が得られる

## 影響

- PRオープン・更新時に、コミットメッセージのプレフィックス別整理コメントが自動投稿されるようになる
- ドキュメントのみのPRでも本ワークフローは起動する（ADR-0002のpaths-ignore対象外）
- コミットメッセージがGit Prefixes規約に従っていないPRでは、「その他」セクションに集約され、サマリーの精度が下がる

## 今後の見直しポイント

- Conventional Commits規約に従わないコミットが多い場合、PR作成時のテンプレートやlintでの規約強制を検討する
- 本ワークフローと `ci.yml` の `reports` ジョブとの統合可否（運用上の手間が増えた場合に再検討）
- 「概要」「動作確認」セクションの自動化（AIモデル活用）の必要性が高まった場合、コスト影響を踏まえて別途ADRで検討する
