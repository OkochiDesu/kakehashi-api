plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "6.25.0"
    jacoco
}

group = "com.kakehashi"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    // JDBC / Flyway / PostgreSQL
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    // MyBatis
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.4")
    // Bean Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")
    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    // CI環境でのDB統合テストが必要になった時点でTestcontainersを追加予定
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

spotless {
    kotlin {
        ktlint("1.5.0")
        target("src/**/*.kt")
    }
    kotlinGradle {
        ktlint("1.5.0")
        target("*.gradle.kts")
    }
    // MyBatis mapper XML のフォーマットは最初のマッパー作成時に追加予定
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy("jacocoTestReport")
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.withType<Test>())
    // トップレベル関数からKotlinコンパイラが自動生成するクラス（例: *Kt）を除外
    classDirectories.setFrom(
        files(
            classDirectories.files.map {
                fileTree(it) { exclude("**/*Kt.class") }
            },
        ),
    )
    reports {
        xml.required = true
        html.required = true
        csv.required = false
    }
}

tasks.named("build") { dependsOn("spotlessApply") }
tasks.named("bootRun") { dependsOn("spotlessApply") }
tasks.withType<Test> { dependsOn("spotlessApply") }
