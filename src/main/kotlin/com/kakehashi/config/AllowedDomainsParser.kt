package com.kakehashi.config

/**
 * `app.auth.google.allowed-domains`（カンマ区切り、環境変数 `GOOGLE_ALLOWED_DOMAINS`）のパース処理
 *
 * 根拠: code-reviewer 指摘（PR #21）。[AccountUseCaseConfig.googleSsoCallbackUseCase] と
 * [AuthStartupValidator.validate] の双方で同じパース仕様（トリム・小文字化・空要素除外）を
 * 適用する必要がある。パースロジックが重複していると、一方だけ修正されて仕様がずれる
 * （例: `","` のようなカンマのみの値が [String.isNotBlank] では非空判定されるが、
 * パース後は空集合になり「ドメイン制限なし」で起動してしまう）おそれがあるため1箇所に集約する。
 */
internal object AllowedDomainsParser {
    /**
     * カンマ区切りの許可ドメイン文字列を、トリム・小文字化・空要素除外した上で集合に変換する。
     *
     * @param raw `app.auth.google.allowed-domains` プロパティの未加工値
     * @return 正規化済みの許可ドメイン集合（有効なドメインが1件もない場合は空集合）
     */
    fun parse(raw: String): Set<String> =
        raw
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
}
