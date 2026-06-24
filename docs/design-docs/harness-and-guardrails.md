# ハーネスとガードレール

## 目次

- [ハーネス（馬具）― 事前設計層](#ハーネス馬具-事前設計層)
- [ガードレール（柵）― 事後検証層](#ガードレール柵-事後検証層)
- [2層の役割分担の根拠](#2層の役割分担の根拠)
- [ArchUnit ルールの例外設計](#archunit-ルールの例外設計)
- [新しいチェックを追加するときの判断フロー](#新しいチェックを追加するときの判断フロー)

このリポジトリの AI 協働システムを「**ハーネス（事前設計層）**」と「**ガードレール（事後検証層）**」の2層に分類する。

この分類により、あるファイルや仕組みが「AI を導くために存在するのか」「問題を防ぐために存在するのか」が一目でわかる。新しいファイルを追加するときの配置判断基準としても使う。

---

## ハーネス（馬具）― 事前設計層

> AI が道を**走るための馬具**。指針・文脈・計画を提供し、AI の行動を望ましい方向に導く。

| ファイル / ディレクトリ | 役割 |
|---|---|
| `CLAUDE.md` / `AGENTS.md` | 常時読込の行動指針・リポジトリマップ |
| `.claude/agents/` | サブエージェント定義（役割・呼び出し条件・出力形式） |
| `.claude/rules/` | glob ルール（ファイルタイプ限定で自動適用されるコンテキスト） |
| `.claude/skills/` | 救済スキル（セッション切れ・迷子時の再起動手順） |
| `.claude/settings.json` | 禁止操作のブロック設定（`gh pr merge` / `git reset --hard` / `rm` 系等）― ガードレール的な側面を持つが、ClaudeCode セッション設定として一体管理するためハーネスに分類 |
| `.claude/hooks/` | セッション計測・ナビゲーション指標収集・doc-maintainer チェックリマインダー（SessionStart / PostToolUse / Stop） |
| `docs/design-docs/core-beliefs.md` | 運用原則・設計思想（ハーネスの設計根拠） |
| `docs/adr/` | アーキテクチャ決定記録（ADR / AI-ADR） |
| `docs/exec-plans/` | 実行計画（フルプランでの進捗・意思決定ログ） |

**設計原則**: ハーネスは主に AI に文脈を与え「望ましい動作を引き出す」ことが目的。ただし `.claude/settings.json` のように禁止操作をブロックするものを含む（ClaudeCode 設定として一体管理）。

---

## ガードレール（柵）― 事後検証層

> AI が道を**外れないための柵**。問題を自動検出・拒否し、誤りがコードベースに定着するのを防ぐ。

| ファイル / ツール | 役割 |
|---|---|
| `.githooks/pre-commit` | 英語エラーメッセージ・`assert()` 誤使用・`companion object` 内の `@Container` に `@JvmStatic` 欠落（インスタンスフィールドは対象外）・シークレット等の静的パターン検出（commit をブロック） |
| `shellcheck`（pre-commit 内） | シェルスクリプトの静的解析 |
| `./gradlew build`（型チェック・コンパイル） | Kotlin の型エラー・コンパイルエラーを検出 |
| `./gradlew test`（テストスイート） | ビジネスロジックの正確性・回帰を検出。**ArchUnit**（`ArchitectureTest.kt`）でクリーンアーキテクチャの依存方向（domain / usecase / infrastructure / presentation）を自動検証 |
| `./gradlew spotlessCheck`（フォーマット） | コードスタイルの統一を強制 |

**設計原則**: ガードレールは人間の確認なしに動作する自動検証であり、判断を伴わない確実な検出に絞る。判断が必要なものはハーネス（LLM エージェント）が担う。

---

## 2層の役割分担の根拠

| 判断の性質 | 担当 | 理由 |
|---|---|---|
| 決定論的パターン（grep で検出可能） | ガードレール（pre-commit スクリプト） | 高速・確実・コンテキスト不要 |
| 意味的・設計的整合（文脈理解が必要） | ハーネス（LLM エージェント） | スクリプトでは文脈を持てない |
| 型安全性・コンパイル | ガードレール（ビルド） | コンパイラが確実に検証できる |
| ADR 準拠・アーキテクチャ整合 | ハーネス（code-reviewer 等） | 仕様・経緯の理解が必要 |

この分離は [AI-ADR-0013](../adr/AI-ADR-0013-LLMとスクリプトの役割分離とglobルール採用とtest-reviewer順次分離.md) で決定した。

---

## ArchUnit ルールの例外設計

`ArchitectureTest.kt` の usecase レイヤールールには APP-ADR-0008 に基づく例外がある：

- **非 Query クラス**（UseCase 系）: `infrastructure` / `presentation` への依存を禁止
- **Query クラス**（`*Query` 命名）: MyBatis Mapper（`infrastructure`）への依存を許容。ただし `presentation` への依存は禁止

この例外は意図的な設計であり、ルールを削除・緩和するのではなく「非 Query / Query に分割」することで設計書と整合させている。

また `ClassFileImporter().withImportOption(ImportOption.DoNotIncludeTests())` を必ず指定すること。テストクラスが本番パッケージ（例: `com.kakehashi.usecase.account`）に置かれている場合、テスト用 Mapper モックが infrastructure をインポートするため本番クラスと同一スキャン対象になると誤検知が発生する。

---

## 新しいチェックを追加するときの判断フロー

```
新しいチェックが必要になった
  ↓
grep / 正規表現で確実に検出できる？
  YES → .githooks/pre-commit に追加（ガードレール）
  NO  → 文脈・判断が必要？
          YES → 対応するエージェント定義に追加（ハーネス）
          NO  → ビルド/テストで検出できる？ → gradle に任せる（ガードレール）
```
