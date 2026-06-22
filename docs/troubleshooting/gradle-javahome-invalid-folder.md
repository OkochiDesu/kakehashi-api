# Gradle javaHome invalid folder 備忘録

## 目次

- [目的](#目的)
- [事象](#事象)
- [原因](#原因)
- [確認手順](#確認手順)
- [対応](#対応)
- [再発防止](#再発防止)

## 目的

Dev Container 環境で Gradle が起動しないときの最小切り分け手順を残す。
このメモには機密情報（トークン、パスワード、個人メール、社内URL）は記載しない。

## 事象

Gradle 拡張機能ログで以下のエラーが出る。

```text
Error getting build: Supplied javaHome is not a valid folder
```

## 原因

VS Code の設定が、実在しない Java Home を参照している。

例:

```text
java.import.gradle.java.home = /usr/local/sdkman/candidates/java/current
```

上記パスが存在しない環境では、Gradle がタスク一覧を取得できない。

## 確認手順

1. Java の実体を確認する。

```bash
which java
java -version
```

2. Java Home の候補を確認する。

```bash
echo "$JAVA_HOME"
ls -la /usr/lib/jvm
```

3. VS Code 設定の上書き値を確認する。

- ワークスペース設定
- ユーザー設定
- マシン設定

## 対応

1. 存在する JDK パスに統一する。

例:

```text
/usr/lib/jvm/msopenjdk-current
```

2. VS Code の Java / Gradle 関連設定を修正する。

- java.jdt.ls.java.home
- java.home
- gradle.javaHome
- java.import.gradle.java.home

3. VS Code の Reload Window で拡張機能を再起動する。

## 再発防止

- Dev Container 初回起動時に Java パスを確認する。
- Java パスは「存在確認できる値」のみ設定する。
- 環境固有の古いパス（例: 廃止済み SDKMAN パス）を使い回さない。