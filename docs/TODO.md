# TODO

## コーディング規約

### .editorconfig の追加

- Spotless（ktlint）は `.editorconfig` を参照してインデントや改行のルールを適用する
- 現在はktlintのデフォルト設定で動いているが、チーム内で設定を明示するためにリポジトリに追加する
  - 設定候補: インデント幅、文字コード（UTF-8）、改行コード（LF）、ファイル末尾改行

## カバレッジ

### カバレッジルール（DDD / クリーンアーキテクチャ向け）

- レイヤーごとに異なるカバレッジ閾値を設定する
  - 例: ドメイン層（エンティティ・ユースケース）は80%以上を必須
  - 例: インフラ層（MyBatis Mapper・設定クラス）はカバレッジ計測から除外
- `jacocoCoverageVerification` タスクに除外パターンと閾値を設定する
  - MyBatis導入後にパッケージ構成が固まってから対応する

## CI パフォーマンス

### プロジェクト拡大時のCIビルド高速化

- プロジェクトが大きくなりCIの実行時間が問題になった場合に対応する
- 候補:
  - Gradle ビルドキャッシュの活用（`--build-cache`）
  - Gradle 並列実行（`--parallel`）の有効化
  - テストの並列分割（GitHub Actions の `matrix` 戦略でモジュール分割）
  - マルチモジュール化による差分ビルド

### 複雑度しきい値の定義（fail条件は未適用）

- PRコメントで複雑度を可視化する運用は開始済み
- しきい値（例: CCN上限）を決めるまでは CI fail 判定を入れない
- しきい値を決めたタイミングで、超過時に `reports` ジョブを fail させる条件を追加する

### 変更したKotlinファイルに関連するテストのみを実行

- push時は変更関連テストを優先し、PR時（main向け）は全テストを実行する二段構えを検討する
- 実装アプローチ:
  - 命名規約ベース（例: `Foo.kt` 変更時に `FooTest` を実行）
  - 依存関係解析ベース（Test Impact Analysis）
- 運用上の注意:
  - 必須チェックは全テストを残し、絞り込み実行のみでマージ可否を決めない
  - 共通基盤や設定変更時はフルテストにフォールバックする
  - 対象テストが0件の場合はフルテストを実行する

### 変更関連テスト実行の導入コスト・利用料の整理

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

### Dependabot で Gradle 依存更新を自動提案

- 今は見送り、将来導入時に .github/dependabot.yml へ Gradle エコシステムを追加する
- 互換性破壊を避けるため、まずは patch と minor を中心に運用する
- Kotlin / Spring Boot / Gradle など基盤依存の major 更新は手動レビュー前提にする
- Verify チェック通過をマージ条件にし、自動マージは有効化しない

## Copilot 活用

### CopilotへのRVレビュー依頼観点をファイルで統一

- レビュー依頼時の観点（セキュリティ・パフォーマンス・可読性など）を `.github/copilot-instructions.md` に記載する
- これにより「毎回チャットで観点を説明する」手間をなくし、チーム内で統一したレビュー観点を維持できる
- 後述の実装ルール設定ファイルと同一ファイルで管理可能

### VS Code上でCopilotが実装ルールを守れるよう設定ファイルを追加

- アーキテクチャ選定・命名規則・レイヤー責務などのルールを Copilot に読み込ませる
- 設定方法:
  - `.github/copilot-instructions.md`（リポジトリ全体に適用）
  - `.vscode/*.instructions.md`（VS Code エージェントモード向け）
- 内容候補: パッケージ構成ルール、レイヤー間依存の方向、命名規則、禁止パターンなど
- アーキテクチャが固まってから作成する

### Copilot PR Summarize の活用

- PR作成時に GitHub.com 上の Copilot「Summarize」ボタンでPR概要を自動生成できる
- プレミアムリクエストを消費するため、自動実行はせず**手動で必要なときだけ使う**運用とする
  - 対象: レビュアーが多いPR、複雑な変更、大きめのPRなど
- チームのCopilotプランの月間上限と照らし合わせて利用頻度を調整する

## 認証・セキュリティ

### Google SSO（IDトークン）を用いた認証基盤の構築
- Nuxt（フロントエンド）側でGoogle SSOを行い、取得したIDトークン（JWT）をAPIリクエストの `Authorization: Bearer` ヘッダーに付与する。
- Spring Boot側でGoogleの公開鍵を用いて毎リクエストごとにトークンを検証（署名チェック、有効期限確認など）する。
  - Spring Securityの `oauth2ResourceServer` などの利用を想定。

### アカウントのDB管理と照合処理
- 認証のキーにはメールアドレスではなく、変更されるリスクのない Googleの内部ID（`sub` クレーム）を使用する。
- 社員（Engineer集約）テーブルには `engineer_id` (システム主キー), `google_sub_id` (認証キー), `email` (表示用) を持たせる。
- 毎リクエスト時に `google_sub_id` でDBを検索し、無効化されたアカウント（退職者など）を即座に弾けるようにする。

### 認証処理の共通化とクリーンアーキテクチャの維持
- Controllerの各メソッドに認証やDB検索のロジックを書かず、`HandlerMethodArgumentResolver` 等を利用して処理を共通化する。
- Controller層で認証・変換を完結させ、引数としてドメインモデル（例: `@LoginEngineerId engineerId: EngineerId`）だけを受け取るようにする。
- ユースケース層やドメイン層には、JWTやHTTPヘッダーなどWeb特有の概念を一切持ち込ませない。

## Done（履歴）

- CI/CD（GitHub Actions）でPRにカバレッジ率を自動コメント: [72badb3](https://github.com/OkochiDesu/kakehashi-api/commit/72badb3)
- push時のGitHub Actionsで複雑度レポートを作成: [72badb3](https://github.com/OkochiDesu/kakehashi-api/commit/72badb3)
