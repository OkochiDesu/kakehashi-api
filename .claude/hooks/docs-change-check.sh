#!/bin/bash
# PostToolUse hook: docs/ 配下のファイル変更時に doc-maintainer チェックを促す
# セッション内で一度だけリマインドし、スパムを防ぐ

REMINDED_FILE="/tmp/claude_kakehashi_docs_reminded"

json_escape() { local s="${1//\\/\\\\}"; s="${s//\"/\\\"}"; printf '%s' "$s"; }

# stdin から tool 情報を取得
input=$(cat)

if command -v jq &>/dev/null; then
  tool_name=$(echo "$input" | jq -r '.tool_name // empty')
  file_path=$(echo "$input" | jq -r '.tool_input.file_path // empty')
else
  tool_name=$(echo "$input" | grep -o '"tool_name":"[^"]*"' | cut -d'"' -f4)
  file_path=$(echo "$input" | grep -o '"file_path":"[^"]*"' | cut -d'"' -f4)
fi

# Edit または Write ツールで docs/ 配下のファイルを変更した場合
if [[ "$tool_name" == "Edit" || "$tool_name" == "Write" ]]; then
  if [[ "$file_path" == *"/docs/"* ]]; then
    if [ ! -f "$REMINDED_FILE" ]; then
      touch "$REMINDED_FILE"
      msg="📋 [Doc-Maintainer Reminder] docs/ 配下のファイルを変更しました。コミット前に doc-maintainer-structure サブエージェントで索引・リンク整合性をチェックしてください（新規ファイル追加を含む場合は doc-maintainer-content も並列で実行）。"
      printf '{"additionalContext": "%s"}\n' "$(json_escape "$msg")"
    fi
  fi
fi
