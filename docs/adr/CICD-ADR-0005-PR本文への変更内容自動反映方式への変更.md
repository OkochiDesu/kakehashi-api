# CICD-ADR-0005: PR本文への変更内容自動反映方式への変更

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-06-13

## 関連

- Supersedes: [CICD-ADR-0004](CICD-ADR-0004-コミットメッセージベースのPRサマリー自動コメント導入.md)（決定4「コメント投稿方式」のみを置き換え。決定1〜3は維持）
- Superseded by: なし

## 背景

ADR-0004では、コミットメッセージから生成したPRサマリーを `marocchino/sticky-pull-request-comment` でPRコメントとして投稿する方式を採用していた。

この方式では、PRの「変更内容」セクション（[PRテンプレート](../../.github/pull_request_template.md)）は引き続き作成者が手動で記載する必要があり、サマリーコメントとPR本文の「変更内容」が別々に存在することになっていた。

本プロジェクトの目的は「PRの変更内容記載という人間の作業を減らす」ことであり、サマリーをコメントとして分離するのではなく、PR本文の「変更内容」セクションそのものを自動生成・更新できれば、作成者の手動記載作業を削減できる。

## 決定

### PR本文の「変更内容」セクションを直接書き換える方式に変更する

対象ファイル:
- [`.github/workflows/pr-summary.yml`](../../.github/workflows/pr-summary.yml)
- [`.github/pull_request_template.md`](../../.github/pull_request_template.md)
- [`docs/conventions/pr-summary-automation.md`](../../docs/conventions/pr-summary-automation.md)

- [PRテンプレート](../../.github/pull_request_template.md)の「変更内容」セクションに `<!-- pr-summary:start -->` 〜 `<!-- pr-summary:end -->` マーカーを追加する
- ワークフローは `gh pr view` でPR本文を取得し、マーカー間をコミットメッセージから生成した内容で置き換えて `gh pr edit` で書き戻す（push毎に上書き）
- マーカーが見つからない場合（テンプレートを使わずに作成されたPRなど）は、ADR-0004と同様の `marocchino/sticky-pull-request-comment`（header: `pr-summary`）によるコメント投稿をフォールバックとして維持する
- ADR-0004の決定1（独立ワークフローとして管理）・決定2（トリガーはmainへのPRに限定し、ドキュメントのみのPRも対象）・決定3（AIモデルを使用しない）はそのまま維持する

## 代替案

### 代替案A: ADR-0004の方式（サマリーコメントのみ）を維持する

- 長所: 実装がシンプルで、PR本文を書き換える権限（`pull-requests: write` でのbody編集）が不要
- 短所: PR本文の「変更内容」セクションは作成者が別途手動で記載する必要があり、「変更内容記載という人間の作業を減らす」という目的を達成できない

### 代替案B: PR本文書き換えのみとし、フォールバックコメントを設けない

- 長所: 実装・運用がシンプルになる
- 短所: PRテンプレートを使わずに作成されたPRやマーカーが削除されたPRではサマリーが一切提示されなくなり、ADR-0004で得られていた可視化が失われる

## 影響

- PRの「変更内容」セクションが、push毎にコミットメッセージから自動生成・更新される
- `pr-summary:start`〜`end` の区間は手動編集してもCIにより上書きされるため、利用者に周知が必要
- ワークフローの権限は `pull-requests: write` のまま変わらないが、PR本文の取得・書き換えのために `gh pr view` / `gh pr edit` を新たに利用する
- マーカーが存在しないPRでは、ADR-0004と同じ挙動（コメント投稿）にフォールバックする

## 今後の見直しポイント

- 本方式が「実験的導入」であるため、運用後にPR本文の自動書き換えが利用者の手動編集と競合する・分かりにくいといった問題が出た場合は、ADR-0004の方式への回帰やフォールバック条件の見直しを検討する
- マーカーが見つからないケースが多発する場合、PRテンプレートの強制（lint等）を検討する
