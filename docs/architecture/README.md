# Architecture（モジュール構成・パッケージ設計）

このディレクトリは kakehashi-api の実装構造に関するドキュメントをまとめる。

## ドキュメント一覧

- [package-structure.md](package-structure.md) — パッケージ構成規約（レイヤー・命名・UseCase DI・Enum 活用）

## 現状

Step1（アカウント・ロールドメイン）実装完了。現時点では単一 Gradle プロジェクト（[settings.gradle.kts](../../settings.gradle.kts)）。
Gradle サブプロジェクト分割やモジュール構成の変更が生じた段階でこのディレクトリを拡充する。
アーキテクチャ全体の前提は [requirements/README.md](../requirements/README.md) を参照。
