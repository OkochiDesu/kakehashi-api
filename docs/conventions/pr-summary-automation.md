# PR Summary 自動生成

## 目的

PRのコミットメッセージから「変更内容」の箇条書きを自動生成し、PRコメントとして投稿する。
AIモデル（Copilot premium request・LLM API）を使わず、コミットメッセージのパースのみで実現することで、
レビュー前のPR概要作成コストを下げつつ、トークン・利用料を消費しない。

## 仕組み

- ワークフロー: [`.github/workflows/pr-summary.yml`](../../.github/workflows/pr-summary.yml)
- トリガー: mainへのPR（`opened` / `synchronize` / `reopened`）。ドキュメントのみのPRも対象に含む。
- PRのbase..head間のコミットメッセージを取得し、[Git Prefixes](git-prefixes.md)のプレフィックス（`feat:` / `fix:` / `docs:` / `refactor:` / `test:` / `ci:` / `chore:`）ごとに分類して箇条書きにする。
- プレフィックスに従っていないコミットは「その他（Conventional Commits形式に従っていないコミット）」にまとめる。
- 生成結果は `marocchino/sticky-pull-request-comment`（header: `pr-summary`）でPRに自動コメントされ、push毎に同じコメントが更新される。

## 動作

| 状況 | 動作 |
|---|---|
| コミットメッセージがプレフィックス付き | プレフィックスに応じたセクションに分類される |
| プレフィックスなし・規約外の表記 | 「その他（規約外）」セクションに分類される |
| PRにコミットが追加された（再push） | コメントが最新の内容で上書きされる |

## 限界・注意

- 生成されるのは「コミット一覧の整形」であり、「なぜ変更したか」という背景や意図までは要約しない。[PRテンプレート](../../.github/pull_request_template.md)の「概要」「動作確認」「気になる部分」は引き続き手動で記載する。
- コミットメッセージが[Git Prefixes](git-prefixes.md)の規約に従っていない場合、分類されず「その他」に集約される。規約に沿ったコミットメッセージを書くことで、このサマリーの精度が上がる。
- マージ可否には影響しない（コメント投稿のみ）。

## 関連ファイル

- ワークフロー: [.github/workflows/pr-summary.yml](../../.github/workflows/pr-summary.yml)
- コミットメッセージ規約: [Git Prefixes](git-prefixes.md)
- PRテンプレート: [.github/pull_request_template.md](../../.github/pull_request_template.md)
- 意思決定の背景: [ADR-0004: コミットメッセージベースのPRサマリー自動コメント導入](../adr/ADR-0004-コミットメッセージベースのPRサマリー自動コメント導入.md)
