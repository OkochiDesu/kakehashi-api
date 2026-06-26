# TODO

## 目次

- [Platform / DevEx](#platform--devex)
  - [コーディング規約](#コーディング規約) / [CI 設計課題](#ci-設計課題) / [CI パフォーマンス](#ci-パフォーマンス) / [Copilot 活用](#copilot-活用) / [AI駆動ドキュメンテーション](#ai駆動ドキュメンテーションナレッジ管理) / [サンドボックス環境](#サンドボックス環境)
- [Frontend](#frontend)
  - [フロントエンド連携](#フロントエンド連携) / [フロントエンド実装・UI開発](#フロントエンド実装ui開発)
- [Backend](#backend)
  - [クラウド選定](#クラウド事業者ホスティング選定) / [Account ロジック整理](#account-ロジック整理)
- [Cross-cutting](#cross-cutting)
  - [冪等性キーチェック基盤](#冪等性キーチェック基盤)
- [Done（履歴）](#done履歴)

## Platform / DevEx

### コーディング規約

#### .editorconfig の追加

- Spotless（ktlint）は `.editorconfig` を参照してインデントや改行のルールを適用する
- 現在はktlintのデフォルト設定で動いているが、チーム内で設定を明示するためにリポジトリに追加する
  - 設定候補: インデント幅、文字コード（UTF-8）、改行コード（LF）、ファイル末尾改行


### CI 設計課題

#### workflow-lint.yml の Shellcheck ステップに if: 条件がない（先送り）

> Copilot 指摘 PR #10 comment 3450700854（先送り理由を記録）

- **指摘内容**: `.github/workflows/workflow-lint.yml` の "Shellcheck for git hooks" ステップに `if:` が無いため、`only_meta != 'true'`（ソースコード変更混在）時でも実行される
- **提案された修正**: `if: steps.changed-files.outputs.only_meta == 'true'` を追加してスキップ可能にする
- **先送り理由**: このワークフロー以外に `.githooks/` の Shellcheck を実行する CI が存在しないため、`if:` を追加すると「`.githooks/` 変更 + ソースコード変更」の組み合わせで Shellcheck がどの CI でも走らないカバレッジ欠如が生まれる。安全側を優先して現状維持とする
- **対応タイミング**: `.githooks/` の Shellcheck を main CI（`ci.yml`）に移設できた時点で本 TODO を再検討する

### CI パフォーマンス

#### プロジェクト拡大時のCIビルド高速化

- プロジェクトが大きくなりCIの実行時間が問題になった場合に対応する
- 候補:
  - Gradle ビルドキャッシュの活用（`--build-cache`）
  - Gradle 並列実行（`--parallel`）の有効化
  - テストの並列分割（GitHub Actions の `matrix` 戦略でモジュール分割）
  - マルチモジュール化による差分ビルド

#### 複雑度しきい値の定義（fail条件導入済み・仮値）

> 関連: [CICD-ADR-0001 今後の見直しポイント](adr/CICD-ADR-0001-CI品質ゲートとDependabot運用方針.md#今後の見直しポイント)

- CCN上限を仮値10として `reports` ジョブのfail条件に導入済み（`.github/workflows/ci.yml`）
- 運用しながら閾値が適切か見直し、必要なら調整する

#### 変更したKotlinファイルに関連するテストのみを実行

- push時は変更関連テストを優先し、PR時（main向け）は全テストを実行する二段構えを検討する
- 実装アプローチ:
  - 命名規約ベース（例: `Foo.kt` 変更時に `FooTest` を実行）
  - 依存関係解析ベース（Test Impact Analysis）
- 運用上の注意:
  - 必須チェックは全テストを残し、絞り込み実行のみでマージ可否を決めない
  - 共通基盤や設定変更時はフルテストにフォールバックする
  - 対象テストが0件の場合はフルテストを実行する

#### 変更関連テスト実行の導入コスト・利用料の整理

- GitHub Actions 実行分の費用:
  - 並列ジョブ増加、実行回数増加、実行時間増加で Actions 利用分が増える
  - private リポジトリでは無料枠超過時に課金対象
- 外部サービス利用料:
  - Test Impact Analysis をSaaSで行う場合、ユーザー数・実行回数・組織プランに応じて課金される
  - 例: CI最適化SaaS、テスト選択支援サービスなど
- セルフホスト運用コスト:
  - 依存解析基盤を自前で持つ場合、ランナー増強やストレージ/保守の運用コストが発生
- 導入・保守の人的コスト:
  - 差分判定スクリプト、命名規約整備、誤判定時のフォールバック設計、定期見直しに工数が必要

#### Dependabot で Gradle 依存更新を自動提案

- 今は見送り、将来導入時に .github/dependabot.yml へ Gradle エコシステムを追加する
- 互換性破壊を避けるため、まずは patch と minor を中心に運用する
- Kotlin / Spring Boot / Gradle など基盤依存の major 更新は手動レビュー前提にする
- Verify チェック通過をマージ条件にし、自動マージは有効化しない

### Copilot 活用

#### CopilotへのRVレビュー依頼観点をファイルで統一

- レビュー依頼時の観点（セキュリティ・パフォーマンス・可読性など）を `.github/copilot-instructions.md` に記載する
- これにより「毎回チャットで観点を説明する」手間をなくし、チーム内で統一したレビュー観点を維持できる
- 後述の実装ルール設定ファイルと同一ファイルで管理可能

#### VS Code上でCopilotが実装ルールを守れるよう設定ファイルを追加

- アーキテクチャ選定・命名規則・レイヤー責務などのルールを Copilot に読み込ませる
- 設定方法:
  - `.github/copilot-instructions.md`（リポジトリ全体に適用）
  - `.vscode/*.instructions.md`（VS Code エージェントモード向け）
- 内容候補: パッケージ構成ルール、レイヤー間依存の方向、命名規則、禁止パターンなど
- アーキテクチャが固まってから作成する

#### Copilot PR Summarize の活用

- PR作成時に GitHub.com 上の Copilot「Summarize」ボタンでPR概要を自動生成できる
- プレミアムリクエストを消費するため、自動実行はせず**手動で必要なときだけ使う**運用とする
  - 対象: レビュアーが多いPR、複雑な変更、大きめのPRなど
- チームのCopilotプランの月間上限と照らし合わせて利用頻度を調整する

### AI駆動ドキュメンテーション・ナレッジ管理

#### 実装差分からの設計書（Markdown）自動更新フローの構築

- 実装（コード）を正とし、AIがドキュメントの陳腐化を防ぐワークフローをGitHub Actionsで実装する。
- **ワークフロー詳細**:
  1. Kotlinコードの変更を含むPRが作成/更新された際、`git diff` を取得。
  2. LLM（GPT-4o / Claude 3.5 Sonnet 等）を用い、差分を反映した設計書（`docs/design.md` 等）の修正案を生成。
  3. 実装ブランチをベースとした「設計書更新PR」をGitHub Actionsから自動作成する。
- **ハルシネーション対策**: AIが直接 `main` を更新するのではなく、人間がPRをレビュー・マージするプロセスを挟む。
- **スクリプト実装**: GitHub Actionsから呼び出す差分解析・Markdown更新用スクリプト（Python等）を作成する。プロンプトには「ドメインモデルの構造変更」「状態遷移の追加」「認可ポリシーの変更」など、Kakehashiの設計エッセンスをAIが正しく理解してMarkdownに変換するための指示定義を含める。

#### NotebookLMへの設計ナレッジ自動同期の自動化

- GitHub上の最新設計ドキュメントをNotebookLMへ自動インプットし、チーム全体のドメイン知識を常に最新化する。
- **連携方式の選定**:
  - **パターンA（API連携）**: NotebookLM Enterprise API を使用し、GitHub Actionsから直接ソースを上書き更新する。
  - **パターンB（Google Drive経由）**: GitHub Actionsから Google Drive API を叩いてドキュメント（GDoc等）を更新し、NotebookLM側の「同期（Sync）」機能と連動させる。
- **期待効果**: 新規参画メンバーのオンボーディングや、複雑な仕様の検索をNotebookLM上でのチャットで完結させる。

### サンドボックス環境

- ローカル・CI 以外に「壊してよい」独立した実行環境がほしい
- 用途: 新機能の動作確認・外部サービス連携テスト・チームメンバーのデモ共有
- 構成・ホスティング方法は クラウド事業者選定（Backend セクション参照）と同時に検討する

## Frontend

### フロントエンド連携

#### Nuxt（TypeScript）+ OpenAPIでAPIクライアントを自動生成

- Spring Boot 側で OpenAPI 定義を安定的に出力できるようにする（エンドポイント・スキーマ・認証方式を明示）
- Nuxt 側で OpenAPI から TypeScript クライアント生成を行う（生成タイミングは `npm run generate:api` などで統一）
- 生成コードを直接各画面で使わず、認証ヘッダー付与やエラーハンドリングを担うラッパ層を1枚挟む
- CI で OpenAPI 定義と生成クライアントの差分検知を行い、契約不整合の混入を防ぐ

#### APIクライアント生成の運用方針（ビルド時毎回生成はしない）

- 方針: アプリの通常ビルド時には毎回生成せず、API仕様変更時に明示コマンドで生成する
- ローカル運用: `generate:api` 実行で生成し、生成物を所定ディレクトリに自動配置する
- CI運用: 生成処理を実行したうえで差分チェックを行い、生成漏れがあれば fail させる
- 自動配置ルール: 生成先ディレクトリを固定し、生成前クリーン + 生成後上書き配置を標準化する
- 生成物の扱い: 手編集禁止（ラッパ層で吸収）をチームルールとして明文化する

### フロントエンド実装・UI開発

#### AIを活用した画面コンポーネントの直接生成

- 中間生成物（HTMLコーディング）のステップは廃止し、ワイヤーフレームから直接Nuxt 3（Vue 3 SFC）コンポーネントを生成するフローを採用する
- 手順:
  1. Miro または Figma でざっくりとしたワイヤーフレームを作成する
  2. 画像認識AI（Claude 3.5 Sonnet, GPT-4o, Cursorなど）にスクリーンショットを読み込ませる
  3. 「Nuxt 3（`<script setup>`）+ TypeScript + Tailwind CSS で実装して」とプロンプトで指示する
- AI生成後は型チェック・Lint・UIライブラリ準拠チェックを通し、品質ゲートを満たしたものだけ採用する

#### UIライブラリの選定とAIへのコンテキスト付与

- 生成されるコードの品質とデザインの統一感を高めるため、UIライブラリ（`Nuxt UI` または `shadcn-vue`）を導入する
- AI（Copilot等）にコードを生成・修正させる際は、「指定したUIライブラリのコンポーネント（例: `<UButton>`, `<UTable>`）を使用すること」を指示に含める
- 実運用ではライブラリを先に1つへ固定し、色・余白・タイポグラフィのデザイントークンを共通化する

#### OpenAPI（自動生成された型）との統合

- AIに出力させたモックコンポーネント（ダミーデータ）をベースに、GitHub Copilotを活用して `openapi-typescript` 等で自動生成されたAPIの型（レスポンスDTO）と紐付ける
- バックエンドのAPI実装が未完了でも、OpenAPIの定義（`openapi.yaml`）を先行して作成し、型安全にフロントエンド開発を進める
- コンポーネントは表示ロジック中心とし、API呼び出しは composables / services 層へ分離する

#### チャット型エージェントによる画面実装フローの整備

- 目的: モックアップ作成と画面設計書作成を高速化しつつ、規約準拠を自動レビューで担保する。
- 想定フロー:
  1. チャットで要件を対話し、既存コード構造を参照して画面構成を提案・生成する。
  2. サブエージェントで規約レビューを実施し、指摘がなくなるまで修正を反復する（自動反復は最大2回まで）。
  3. 生成した画面実装（HTML/SFC）を元に画面設計書を作成する。
  4. 設計書をサブエージェントでレビューし、指摘がなくなるまで修正を反復する（自動反復は最大2回まで）。
  5. 実装と設計書の確定後に、状態遷移図・画面遷移図をMermaid形式で生成する。
  6. 遷移図をレビューし、指摘がなくなるまで修正を反復する（自動反復は最大2回まで）。
- 判断が難しい事項（要件衝突、業務ルール未定義など）はエージェントが人間へ質問して確定する。
- コスト最適化:
  - 差分のみをレビュー対象とする。
  - 合格済み成果物は再レビューしない。
  - 実行前に想定リクエスト消費（小/中/大）を表示し、実行可否を人間が選択する。

## Backend

### クラウド事業者・ホスティング選定

> 関連: [docs/requirements/quality-standards.md 8章](requirements/quality-standards.md#8-移植性portability)

- クラウド事業者・コンテナ実行環境・マネージドPostgreSQL等のホスティングサービスは未確定
- デプロイ先・構成が決まった段階でADRに記録する

### Account ロジック整理

> **対応タイミング: exec-plan 0007（認可・アクセス制御）完了後に再評価**

- 現状の UseCase-per-operation 構成は SRP を満たしており、今は問題なし
- Step1 機能（exec-plan 0005〜0011）が揃い、ユーザー種別・権限ごとの条件分岐が複数 UseCase に重複・散在してきたタイミングで Template Pattern / Strategy Pattern 導入を再検討する
- 検討観点: SOLID 原則への準拠度、パターン適用で解消できる具体的な重複・結合の特定

## Cross-cutting

### 冪等性キーチェック基盤

#### Spring Interceptor + AOP + Redis による冪等性キーチェック

POST など副作用を持つエンドポイントで、クライアントが `Idempotency-Key` ヘッダーを付与した場合に同一リクエストの二重実行を防ぐ仕組みを用意する。

**採用方針（検討結果）**

- `@Idempotent` カスタムアノテーションで対象エンドポイントを宣言的にマーキング（AOP）
- `HandlerInterceptor.preHandle` でRedisをチェックし、キー済みなら Controller に到達させずキャッシュ済みレスポンスを返す
- `SET NX` + TTL（24h 目安）で in-flight の二重実行も防止
- エラーレスポンス（4xx系）はキャッシュしない方針

**UseCase層でチェックする案との比較・却下理由**

UseCase層でのチェックも検討したが、以下の理由で Interceptor 方式を有力案とした：

- 冪等性キーは「HTTPプロトコルレベルの関心事」（Stripe API等のREST慣例に由来）であり、Interceptorに置く方が自然
- UseCase層に持ち込むと Redis依存をポートインターフェースで抽象化する必要があり、複雑度が上がる
- クリーンアーキテクチャの依存方向（UseCase層をインフラ非依存に保つ）と相性が悪い

なお「業務的な二重実行防止」（例：同じ申請を2回送らない）はDB制約（PostgreSQL ユニーク制約）で保証する方が堅牢であり、責務として分離する。

**実装タイミングでADR化すること。**

## Done（履歴）

- CI/CD（GitHub Actions）でPRにカバレッジ率を自動コメント: [72badb3](https://github.com/OkochiDesu/kakehashi-api/commit/72badb3)
- push時のGitHub Actionsで複雑度レポートを作成: [72badb3](https://github.com/OkochiDesu/kakehashi-api/commit/72badb3)
- データベース・マイグレーション基盤の導入（Flyway / MyBatis / PostgreSQL）: [APP-ADR-0004](adr/APP-ADR-0004-永続化技術スタックの導入-Flyway-MyBatis-PostgreSQL.md)
- DB テストに Testcontainers（PostgreSQL）を採用し統合テストを整備: [APP-ADR-0012](adr/APP-ADR-0012-Testcontainersを2.0.5へ移行しTestConfiguration直接起動方式を採用.md)
- ArchUnit によるアーキテクチャ依存方向チェック（domain / usecase / infrastructure / presentation）: [PR #13](https://github.com/OkochiDesu/kakehashi-api/pull/13)
