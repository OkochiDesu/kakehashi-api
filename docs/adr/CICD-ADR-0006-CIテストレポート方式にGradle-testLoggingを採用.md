# CICD-ADR-0006: CIテストレポート方式にGradle testLoggingを採用

## ステータス

- [ ] Proposed
- [x] Accepted
- [ ] Superseded
- [ ] Rejected

## 日付

2026-06-28

## 関連

- Supersedes: なし
- Superseded by: なし

## 背景

`./gradlew test` タスクの `testLogging` は既に `FAILED` イベントのみ設定済みだった。
そのため CI ログでは失敗したテストしか確認できず、テストケース単位の pass/fail（成功・スキップ）を直接把握できなかった。
CI 実行結果からどのテストが実行・成功・スキップされたかを確認できるよう、テストレポートの提示方式を検討した。

## 決定

Gradle の `testLogging` に `PASSED` / `SKIPPED` イベントを追加し、`./gradlew test` の標準出力（CI ログ）に各テストケースの結果を表示する方式を採用する（`build.gradle.kts` を変更）。

```kotlin
tasks.withType<Test> {
    testLogging {
        events(
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED,
        )
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}
```

- CI ログをスクロールすれば各テストケースの結果を確認できる
- 外部 GitHub Actions・追加の依存を必要としない

## 代替案

**`mikepenz/action-junit-report`（GitHub Actions ワークフロー変更）**

- GitHub Actions の「Checks」タブに HTML 形式のテスト一覧を表示し、PR 画面から直接確認できる・見やすいという利点がある。
- 一方で外部アクションへの依存が増える。現時点の規模では Gradle testLogging で十分と判断したため採用しなかった。

## 影響

- `build.gradle.kts` の `Test` タスク設定に `PASSED` / `SKIPPED` イベントが追加され、CI ログの出力量が増える。
- テスト結果は CI ログ上にテキストで出力されるため、件数が多い場合はスクロールして確認する必要がある。
- 外部アクション・追加依存は導入しないため、ワークフロー（`.github/workflows/`）の変更は不要。

## 今後の見直しポイント

- テスト件数が増えて CI ログでの確認が見づらくなった場合。
- PR レビューで HTML 形式の詳細サマリー（Checks タブ表示）が必要になった場合。

これらの状況になった際は、代替案の `mikepenz/action-junit-report` 等への移行を検討する。
