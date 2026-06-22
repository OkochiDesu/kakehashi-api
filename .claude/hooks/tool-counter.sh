#!/bin/bash
# PostToolUse hook: ツール呼び出し回数をカウント

TOOL_COUNT_FILE="/tmp/claude_kakehashi_tool_count"
[ -f "$TOOL_COUNT_FILE" ] || exit 0

count=$(cat "$TOOL_COUNT_FILE")
echo $((count + 1)) > "$TOOL_COUNT_FILE"
