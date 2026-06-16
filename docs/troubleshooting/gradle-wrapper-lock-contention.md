# Gradle Wrapper ロック競合による初期化失敗（備忘録）

## 目次

- [概要](#概要)
- [原因](#原因)
- [発生条件](#発生条件通常時と今回の違い)
- [恒久対策](#恒久対策今回実施)
- [一時復旧手順（再発時）](#一時復旧手順再発時)
- [他の解決策](#他の解決策選択肢)
- [予防ポイント](#予防ポイント)

## 概要

Dev Container の Rebuild 直後に、Gradle 配布ZIPの取得・展開でロック競合が発生し、
Gradle タスク取得が失敗することがある。

代表的な症状:

- `Found 0 tasks`
- `Could not install Gradle distribution`
- `Timeout waiting for exclusive access to file ... gradle-9.4.1-bin.zip`
- `zip END header not found`

## 原因

今回の根本原因は以下の複合要因。

1. Rebuild 直後に Java/Gradle 拡張と手動 `./gradlew` が同時に動き、同じ Wrapper キャッシュへアクセスした。
2. 初回ダウンロードが途中失敗し、`gradle-9.4.1-bin.zip.part` が残存した。
3. `.lck` ロックが残ったままになり、後続プロセスが排他待ちでタイムアウトした。

結果として、Gradle の初期化が完了せず、VS Code 側ではタスクが見えない状態になった。

## 発生条件（通常時と今回の違い）

- 通常の Rebuild でも、Java/Gradle 拡張の初期化タイミングと Wrapper 初回取得が重なると、低頻度で同種の競合は起こり得る。
- 今回は Rebuild 直後に手動 `./gradlew` 実行が重なり、競合発生確率が上がった。
- さらに中断や再実行が入ったことで、`.part` と `.lck` が残り、以後の実行が連鎖的に失敗しやすくなった。

要点:

- 今回は「通常でも理論上起こる事象」が、並行実行により顕在化・悪化したケース。

## 恒久対策（今回実施）

### 1. Gradle キャッシュの固定

Dev Container で `GRADLE_USER_HOME` をワークスペース内に固定する。

- 設定先: `.devcontainer/devcontainer.json`
- 値: `<workspace>/.gradle-user-home`

効果:

- キャッシュ場所が明示される
- コンテナ再作成時の挙動が安定しやすい
- 拡張機能との競合影響を限定しやすい

### 2. コンテナ作成直後に Gradle を先行初期化

`postCreateCommand` で `./gradlew --version` を実行し、
配布ZIPの取得・展開を先に完了させる。

効果:

- 拡張機能より先に Wrapper 初期化を完了できる
- 初回競合を避けやすい

## 一時復旧手順（再発時）

1. Gradle 関連プロセスを停止

```bash
pkill -f "gradle-server|gradle-wrapper.jar|GradleDaemon" || true
```

2. 破損キャッシュを削除

```bash
rm -rf "${GRADLE_USER_HOME}/wrapper/dists/gradle-9.4.1-bin"
```

3. 競合しない一時ホームで配布取得

```bash
TMP_GRADLE_HOME="${TMPDIR:-/tmp}/gradle-home"
GRADLE_USER_HOME="$TMP_GRADLE_HOME" ./gradlew --version
```

4. 正常取得できたら既定キャッシュへ反映（必要時）

```bash
TMP_GRADLE_HOME="${TMPDIR:-/tmp}/gradle-home"
DEST_GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
rm -rf "$DEST_GRADLE_HOME/wrapper/dists/gradle-9.4.1-bin"
cp -a "$TMP_GRADLE_HOME/wrapper/dists/gradle-9.4.1-bin" "$DEST_GRADLE_HOME/wrapper/dists/"
```

5. 動作確認

```bash
./gradlew --version
./gradlew tasks --quiet | head -20
```

## 他の解決策（選択肢）

### A. VS Code 側の初期化順序を徹底

- Rebuild 直後は手動で `./gradlew` を叩かず、postCreate の完了を待つ
- Java 拡張の再読み込みは `Reload Window` 後に実施

### B. Docker/Dev Container の再初期化

- Docker Desktop 再起動
- `Dev Containers: Rebuild Container`

ローカル環境不整合（ネットワーク、I/O、ロック残骸）が強い場合に有効。

### C. Gradle キャッシュを完全分離

- プロジェクトごとに `GRADLE_USER_HOME` を分離（今回方式）
- 他プロジェクトのキャッシュ影響を受けにくくする

## 予防ポイント

- 初回起動時は Gradle を並列実行しない
- `zip.part` と `.lck` が同時に残っていたら破損疑い
- `./gradlew --version` が通るまでタスク実行を急がない
- キャッシュ掃除は「週次固定」より「イベント駆動」を推奨する

イベント駆動の実施タイミング:

- Rebuild 前後で不調が出たとき
- `Could not install Gradle distribution` や `Found 0 tasks` が出たとき
- 依存関係を大きく更新したとき
