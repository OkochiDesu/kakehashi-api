#!/bin/bash
# Stop hook: セッション終了時にツール数・所要時間を集計してファイルに保存
# 次回SessionStartで読み込まれ、navigation-metrics.mdへの記録を促す

SESSION_START_FILE="/tmp/claude_kakehashi_session_start"
TOOL_COUNT_FILE="/tmp/claude_kakehashi_tool_count"
LAST_SESSION_FILE="/tmp/claude_kakehashi_last_session.json"

[ -f "$SESSION_START_FILE" ] || exit 0

start=$(cat "$SESSION_START_FILE")
tool_count=$(cat "$TOOL_COUNT_FILE" 2>/dev/null || echo "0")
now=$(date +%s)
duration_min=$(( (now - start) / 60 ))
today=$(date +%Y-%m-%d)

printf '{"date":"%s","tool_count":%s,"duration_min":%s}\n' \
  "$today" "$tool_count" "$duration_min" > "$LAST_SESSION_FILE"
