# Spring Boot 4.x 移行に伴う CI 連鎖障害の調査記録

## 概要

Spring Boot 3.x → 4.x 移行後、CI（GitHub Actions）が 5 種類のエラーを段階的に発生させた。
DevContainer に Docker ソケットマウントを追加してローカルで Testcontainers を実行できる環境を整え、
**プラン → 実行 → 失敗 → 改善** のサイクルをローカルで回すことで全テストを通した記録。

## 目次

- [障害の連鎖（5段階）](#障害の連鎖5段階)
- [調査アプローチ](#調査アプローチ)
- [修正後の構成](#修正後の構成)
- [再発防止策](#再発防止策)
- [関連](#関連)

## 障害の連鎖（5段階）

### 障害 1: MyBatis が Spring Boot 4.x で起動しない

**エラー:**
```
java.lang.NoSuchMethodError:
  org.springframework.boot.context.properties.PropertyMapper.alwaysApplyingWhenNonNull()
```

**原因:** `mybatis-spring-boot-starter:3.0.4` は Spring Boot 3.x 向け。
Spring Boot 4.x では `PropertyMapper.alwaysApplyingWhenNonNull()` が削除された。

**対処:** `mybatis-spring-boot-starter:4.0.1` へ更新。

---

### 障害 2: Testcontainers 1.20.4 が Docker を検出できない

**エラー:**
```
com.github.dockerjava.api.exception.BadRequestException (Status 400)
Caused by: Could not find a valid Docker environment
```

**原因:** Docker Desktop WSL2 リレーソケットは空または不完全な Docker info を返す場合がある。
Testcontainers 1.20.4 が内部で使う `docker-java 3.4.0` はこれを無効な環境と判定する。

**対処:** Testcontainers を `2.0.5`（Spring Boot 4.x の BOM 管理）に移行。
`docker-java 3.7.1` は Docker Desktop リレーを正しく扱う。

あわせて 2.x でのアーティファクト名変更に対応：

| 変更前 | 変更後 |
|---|---|
| `org.testcontainers:junit-jupiter` | `org.testcontainers:testcontainers-junit-jupiter` |
| `org.testcontainers:postgresql` | `org.testcontainers:testcontainers-postgresql` |

`dependencyManagement` で `1.20.4` に固定していたブロックも削除。

---

### 障害 3: Ryuk（リソースリーパー）が接続できない

**エラー:**
```
Could not connect to Ryuk at 172.17.0.1:61082
```

**原因:** DevContainer は Docker-outside-of-Docker（DoD）構成。
Ryuk コンテナは Docker ホスト側で起動するが、テスト JVM（DevContainer 内）から
`172.17.0.1`（Docker ホストの bridge IF）に戻れない。

**対処:** 環境変数 `TESTCONTAINERS_RYUK_DISABLED=true` を設定。

> **注意:** `~/.testcontainers.properties` の `ryuk.disabled=true` は Testcontainers 2.x では機能しない。
> 環境変数が正式手順。`devcontainer.json` の `remoteEnv` に追加した。

---

### 障害 4: PostgreSQL コンテナのポートにアクセスできない

**エラー:**
```
Connection to 172.17.0.1:50881 refused
```

**原因:** DoD 構成では PostgreSQL コンテナのポートが Docker ホストの NIC にマッピングされる。
DevContainer 内から `172.17.0.1` は到達不能。

**対処:** 環境変数 `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` を設定。
`host.docker.internal` は Docker Desktop が提供する特殊ホスト名（→ `192.168.65.254`）で、
DevContainer 内から Docker ホストへ到達できる。`devcontainer.json` の `remoteEnv` に追加した。

> **CI との違い:** GitHub Actions（ubuntu-latest）は native Docker のため `172.17.x.x` に直接到達できる。
> Ryuk・host override の設定はローカル DevContainer 専用。

---

### 障害 5: Flyway マイグレーションが実行されない

**エラー:**
```
org.postgresql.util.PSQLException: ERROR: relation "accounts_account_id_seq" does not exist
```

PostgreSQL コンテナへの接続は成功するが、V2 マイグレーションで作成されるシーケンスが存在しない。

**原因:** Spring Boot 4.x でオートコンフィグがモジュール化された。
`flyway-core` を依存に追加するだけでは `FlywayAutoConfiguration` が登録されない。

調査方法: `spring-boot-autoconfigure-4.0.6.jar` を直接確認

```bash
jar tf spring-boot-autoconfigure-4.0.6.jar | grep -i flyway
# → 出力なし。Flyway 関連クラスが一切含まれていない
```

Spring Boot 4.x では各機能のオートコンフィグは専用スターターが担う
（例: `spring-boot-jdbc` が `DataSourceAutoConfiguration` を提供）。
Flyway のオートコンフィグは `spring-boot-flyway`（`spring-boot-starter-flyway` 経由）が必要。

**対処:** `implementation("org.springframework.boot:spring-boot-starter-flyway")` を追加。

---

## 調査アプローチ

### ローカル実行環境の整備

DevContainer の `docker-compose.yml` にホストの Docker ソケットをマウントし、
`ghcr.io/devcontainers/features/docker-outside-of-docker:1` feature を追加してリビルド。
これにより `./gradlew test` でローカルでも Testcontainers が動作するようになった。

```bash
# 各試行で使用したコマンド
TESTCONTAINERS_RYUK_DISABLED=true \
TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal \
./gradlew test --no-daemon
```

### サイクル

エラーが 1 種類解消されると次のエラーが現れる多段階の連鎖だった。
各段階でエラーメッセージ → JAR 内容確認 → ログ解析 → 依存解決の順で調査した。

## 修正後の構成

| 項目 | 修正前 | 修正後 |
|---|---|---|
| MyBatis | `mybatis-spring-boot-starter:3.0.4` | `4.0.1` |
| Testcontainers | `1.20.4`（`dependencyManagement` で固定） | `2.0.5`（Spring Boot 4.x BOM 管理） |
| Flyway | `flyway-core` のみ | `+spring-boot-starter-flyway` |
| DevContainer 環境変数 | なし | `TESTCONTAINERS_RYUK_DISABLED=true`<br>`TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal` |

## 再発防止策

Spring Boot のメジャーバージョンアップ時には以下を必ず確認する：

1. **サードパーティスターターのメジャーバージョン対応状況**（MyBatis 等）
2. **モジュール化によるオートコンフィグの分離**（`spring-boot-autoconfigure.jar` の内容が旧来より大幅に減る）
3. **Testcontainers のアーティファクト名・API 変更**（2.x で多数変更あり）
4. **DevContainer（DoD）固有の接続制限**（Ryuk・ポートアクセス）は CI とは別に検証が必要

## 関連

- [testcontainers-jvmstatic-kotlin.md](testcontainers-jvmstatic-kotlin.md) — `@TestConfiguration` 直接起動方式の詳細
- [APP-ADR-0012](../adr/APP-ADR-0012-Testcontainersを2.0.5へ移行しTestConfiguration直接起動方式を採用.md) — Testcontainers 2.0.5 移行の意思決定
- PR #13
