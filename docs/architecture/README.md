# Architecture（モジュール構成・パッケージ設計）

このディレクトリは、kakehashi-apiの実装構造に関する情報をまとめる場所。

- モジュール構成（Gradleサブプロジェクト等）
- パッケージ設計（DDD/Clean Architectureに基づくレイヤー・パッケージ分割方針）
- 想定しているサブプロジェクト構成

## 現状

現時点（要件定義フェーズ完了時点）では `kakehashi-api` は単一Gradleプロジェクト（[settings.gradle.kts](../../settings.gradle.kts)）であり、本ディレクトリの内容は未作成。

実装フェーズでモジュール構成・パッケージ設計が具体化した段階で、このディレクトリにドキュメントを追加する。アーキテクチャ全体の前提は[docs/requirements/README.md 4章](../requirements/README.md#4-アーキテクチャ原則adr化予定の方針)を参照。
