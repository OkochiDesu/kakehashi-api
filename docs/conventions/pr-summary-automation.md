# PR Summary 自動生成

## 目的

PRのコミットメッセージから、PR本文「変更内容」セクションの箇条書きを自動生成・更新する。
AIモデル（Copilot premium request・LLM API）を使わず、コミットメッセージのパースのみで実現することで、
レビュー前のPR概要作成コストを下げつつ、トークン・利用料を消費しない。

## 仕組み

- ワークフロー: [`.github/workflows/pr-summary.yml`](../../.github/workflows/pr-summary.yml)
- トリガー: mainへのPR（`opened` / `synchronize` / `reopened`）。ドキュメントのみのPRも対象に含む。
- PRのbase..head間のコミットメッセージを取得し、[Git Prefixes](git-prefixes.md)のプレフィックス（`feat:` / `fix:` / `docs:` / `refactor:` / `test:` / `ci:` / `chore:`）ごとに分類して箇条書きにする。
- プレフィックスに従っていないコミットは「その他（Conventional Commits形式に従っていないコミット）」にまとめる。
- [PRテンプレート](../../.github/pull_request_template.md)の「変更内容」セクションに埋め込まれた `<!-- pr-summary:start -->` 〜 `<!-- pr-summary:end -->` マーカーの間を、生成した内容で置き換えて `gh pr edit` でPR本文を更新する（push毎に上書き更新）。
- マーカーが見つからない場合（テンプレートを使わず作成されたPRなど）は、フォールバックとして `marocchino/sticky-pull-request-comment`（header: `pr-summary`）でコメント投稿する。

## 動作

| 状況 | 動作 |
|---|---|
| コミットメッセージがプレフィックス付き | プレフィックスに応じたセクションに分類される |
| プレフィックスなし・規約外の表記 | 「その他（規約外）」セクションに分類される |
| PRにコミットが追加された（再push） | PR本文の `pr-summary:start`〜`end` 区間が最新の内容で上書きされる |
| PR本文に `pr-summary:start`/`end` マーカーがない | PR本文は変更せず、サマリーをコメントとして投稿する |

## 限界・注意

- 生成されるのは「コミット一覧の整形」であり、「なぜ変更したか」という背景や意図までは要約しない。[PRテンプレート](../../.github/pull_request_template.md)の「概要」「動作確認」「気になる部分」は引き続き手動で記載する。
- コミットメッセージが[Git Prefixes](git-prefixes.md)の規約に従っていない場合、分類されず「その他」に集約される。規約に沿ったコミットメッセージを書くことで、このサマリーの精度が上がる。
- `pr-summary:start`〜`end` の区間はCIが上書きするため、**手動で編集しない**（編集してもpush毎に自動生成内容で上書きされる）。
- マージ可否には影響しない（PR本文/コメントの更新のみ）。

## 関連ファイル

- ワークフロー: [.github/workflows/pr-summary.yml](../../.github/workflows/pr-summary.yml)
- コミットメッセージ規約: [Git Prefixes](git-prefixes.md)
- PRテンプレート: [.github/pull_request_template.md](../../.github/pull_request_template.md)
- 意思決定の背景: [ADR-0004: コミットメッセージベースのPRサマリー自動コメント導入](../adr/ADR-0004-コミットメッセージベースのPRサマリー自動コメント導入.md)
