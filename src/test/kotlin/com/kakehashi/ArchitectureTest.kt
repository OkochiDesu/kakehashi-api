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
    fun `usecase層はinfrastructure・presentation層に依存しない`() {
        noClasses()
            .that()
            .resideInAPackage("..usecase..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..infrastructure..",
                "..presentation..",
            ).check(classes)
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
