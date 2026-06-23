package com.kakehashi

import com.tngtech.archunit.core.importer.ClassFileImporter
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
 * 参照: harness-and-guardrails.md（ガードレール層）、APP-ADR-0010
 */
class ArchitectureTest {
    private val classes = ClassFileImporter().importPackages("com.kakehashi")

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
