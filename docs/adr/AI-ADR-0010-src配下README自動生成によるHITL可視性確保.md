# AI-ADR-0010: src配下README自動生成によるHITL可視性確保

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-06-22

## 関連

- Supersedes: なし
- Superseded by: なし
- 関連: [AI-ADR-0001](AI-ADR-0001-Step1実装サポート用マルチエージェントパイプライン構成の採用.md)（マルチエージェントパイプライン）、[AI-ADR-0003](AI-ADR-0003-doc-maintainerの読み取り専用チェッカー設計.md)（doc-maintainer読み取り専用設計）

## 背景

HITL（Human-in-the-Loop）運用において、人間がPRレビューや意思決定を行う際に、実装の構造を素早く把握できる土台が必要。現状、設計書は `docs/` 配下にあるが、GitHubでコードをbrowseする際（PRレビュー等）に設計情報へのアクセスが困難。`src/` 内の `README.md` はGitHubがディレクトリ表示時に自動レンダリングするため、コードと設計情報の距離を最短にできる。

## 決定

`src/main/kotlin/` 配下の各ドメインパッケージ（`domain/account/`、`usecase/account/` 等）に `README.md` を配置し、Mermaid形式のクラス図・関連図・ステータス遷移図を掲載する。この `README.md` は「コードの副産物」として位置づけ、class-diagram-updaterエージェントがソースコードから自動生成・更新する。手動編集は行わない。

### エージェント構成

- **class-diagram-updater**: kotlin-implementer完了後に自動呼び出し。`src/` のKotlinを読んで `README.md` を生成・更新する。
- **src-doc-maintainer**: class-diagram-updater完了後に整合性チェック（読み取り専用）。
- **呼び出し順**: kotlin-implementer → class-diagram-updater → src-doc-maintainer → code-reviewer

## 代替案

1. **`docs/architecture/` に配置する**: doc-maintainerのチェック対象に入るが、GitHubでコードと離れ、PRレビュー時に設計情報へ到達するコストが下がらない。
2. **手動でクラス図を書く**: 陳腐化リスクが高く、HITL土台の信頼性が下がる。
3. **`src/` 内READMEは作らない**: HITL可視性要件を満たせない。

## 影響

- kotlin-implementerが実装を変更した後は必ずclass-diagram-updaterを呼び出す。
- `src/` 配下の `README.md` は手動編集しない（自動生成ファイルである旨をREADME内に明記する）。
- doc-maintainerのチェック範囲は `docs/` のみを維持し、`src/` 内READMEはsrc-doc-maintainerが担当する（[AI-ADR-0003](AI-ADR-0003-doc-maintainerの読み取り専用チェッカー設計.md) のチェック範囲分離方針と整合）。

## 今後の見直しポイント

- kotlin-implementer → class-diagram-updater → src-doc-maintainer のパイプラインが実運用で安定したことが確認できた時点で、呼び出しポリシー（自動 vs 手動トリガー）の調整を検討する。
