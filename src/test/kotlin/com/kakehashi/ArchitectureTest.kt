package com.kakehashi

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

/**
 * クリーンアーキテクチャの依存方向をビルド時に自動検証する（ガードレール）。
 *
 * 許容する依存方向:
 *   presentation → usecase → domain ← infrastructure
 *   config → すべてのレイヤー（DI 配線のため）
 *
 * Query UseCase（APP-ADR-0008）: MyBatis Mapper（infrastructure）をドメインバイパスで直接参照するため
 * infrastructure への依存を例外として許容する。ただし presentation への依存は禁止。
 *
 * ImportOption.DoNotIncludeTests(): テストクラスを除外して本番コードのみをスキャンする。
 * テストコードは同一パッケージに置かれることがあり、テスト用 Mapper モックが
 * infrastructure をインポートするため誤検知が発生する。
 *
 * 参照: harness-and-guardrails.md（ガードレール層）、APP-ADR-0010
 *
 * ★観点
 * クリーンアーキテクチャの依存方向をビルド時に自動検証するガードレール。
 * 開発者が気づく前にレイヤー越境・ドメイン汚染を検出し、設計崩壊を防ぐ。
 * （参照: APP-ADR-0010、harness-and-guardrails.md）
 *
 * ★★ルール（正常系・異常系の区別なし）★★
 * 《観　点》ドメイン層の独立性保証
 * 《テスト》domain 層は usecase・infrastructure・presentation 層に依存しない
 *
 * 《観　点》Web 層・永続化層がユースケースを汚染しないことの確認
 * 《テスト》usecase 非 Query 層は infrastructure・presentation 層に依存しない
 *
 * 《観　点》CQRS 例外（APP-ADR-0008）の範囲内に収まることの確認
 * 《テスト》usecase Query 層は presentation 層に依存しない
 *
 * 《観　点》外側から内側への逆依存（違反）がないことの確認
 * 《テスト》infrastructure 層は presentation・usecase 層に依存しない
 *
 * 《観　点》presentation が usecase をバイパスして DB に直接触れないことの確認
 * 《テスト》presentation 層は infrastructure 層に直接依存しない
 */
class ArchitectureTest {
    private val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages("com.kakehashi")

    @Test
    fun `domain層はusecase・infrastructure・presentation層に依存しない`() {
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..usecase..",
                "..infrastructure..",
                "..presentation..",
            ).check(classes)
    }

    @Test
    fun `usecase非Query層はinfrastructure・presentation層に依存しない`() {
        noClasses()
            .that()
            .resideInAPackage("..usecase..")
            .and()
            .haveSimpleNameNotEndingWith("Query")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..infrastructure..",
                "..presentation..",
            ).check(classes)
    }

    @Test
    fun `usecaseQuery層はpresentation層に依存しない（APP-ADR-0008）`() {
        noClasses()
            .that()
            .resideInAPackage("..usecase..")
            .and()
            .haveSimpleNameEndingWith("Query")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..presentation..")
            .check(classes)
    }

    @Test
    fun `infrastructure層はpresentation・usecase層に依存しない`() {
        noClasses()
            .that()
            .resideInAPackage("..infrastructure..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..presentation..",
                "..usecase..",
            ).check(classes)
    }

    @Test
    fun `presentation層はinfrastructure層に直接依存しない`() {
        noClasses()
            .that()
            .resideInAPackage("..presentation..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
            .check(classes)
    }
}
