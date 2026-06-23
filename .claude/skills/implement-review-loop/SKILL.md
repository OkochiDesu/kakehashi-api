---
name: implement-review-loop
description: "kotlin-implementerによる実装とcode-reviewerによるレビューをAPPROVEDになるまでループする救済スキル。通常は同一セッション内でAIが自動的にagentsを選択して実行するが、セッション切れ・コンテキスト喪失等でAIが迷子になった場合にユーザーが明示的に起動する。"
argument-hint: "実装対象のUCまたはドメイン名（例: UC-R1, 経歴書登録）"
user-invocable: true
---

# Implement-Review-Loop スキル

## このスキルの位置づけ

**通常は不要**。設計レビュー（人間）が同一セッション内で完了している場合、「実装進めて」と指示するだけでメインAIが `kotlin-implementer` → `code-reviewer` を自動的に呼び出す。

このスキルは以下の**救済措置**として使用する:
- セッションが切れてコンテキストを失った場合
- AIが次のステップを見失った場合
- 明示的にループを起動し直したい場合

## 前提条件（起動前に確認）

以下がすべて完了していること:
1. `db-designer` による Flyway SQL 設計済み
2. `api-designer` による API 設計書（`docs/design/api/`）作成済み
3. **人間が設計内容をレビュー・承認済み**

設計が未完了の場合はこのスキルを起動せず、先に設計エージェントを実行すること。

## 手順

1. 引数（UCまたはドメイン名）と `docs/design/api/` の設計書を確認する
2. `test-scenario-planner` サブエージェントを呼び出しテストシナリオ一覧を生成する
3. **シナリオ一覧をユーザーに提示し、承認を得る**（承認なしに次へ進まない）
4. `kotlin-implementer` サブエージェントを呼び出す。シナリオ一覧を入力として渡す
5. `code-reviewer` サブエージェントを呼び出し本体コードをレビューする
6. `code-reviewer` が **REQUIRES_CHANGES** を返した場合:
   - 指摘内容を `kotlin-implementer` に伝えて修正を依頼する
   - 手順5に戻る（最大3回まで自動ループ）
7. `code-reviewer` が **APPROVED** を返した場合:
   - `test-reviewer` サブエージェントを呼び出しテストコードをレビューする
8. `test-reviewer` が **REQUIRES_CHANGES** を返した場合:
   - 指摘内容を `kotlin-implementer` に伝えてテストを修正依頼する
   - 手順7に戻る（最大3回まで自動ループ）
9. `test-reviewer` が **APPROVED** を返した場合:
   - 実装内容・変更ファイル一覧・両レビュー結果をユーザーに提示する
   - commit 確認をユーザーに求める（commitはユーザー承認後のみ実行）
10. いずれかが3回ループしても APPROVED にならない場合:
    - 残存する指摘事項をまとめてユーザーに報告し、判断を委ねる

## 安全設定

- `git push` は行わない（[CLAUDE.md](../../../CLAUDE.md) 参照）
- `git commit` はユーザーの明示的承認後のみ実行する
- 設計書（`docs/design/api/`）に記載のない仕様は実装しない
