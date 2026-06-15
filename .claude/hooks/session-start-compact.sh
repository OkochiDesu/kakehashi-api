#!/bin/bash
# コンテキスト圧縮（compaction）後のSessionStartで発火し、
# CLAUDE.md / AGENTS.md を additionalContext として再注入する。
set -euo pipefail

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"

CONTENT=$(cat <<EOF
=== コンテキスト圧縮後の再注入: CLAUDE.md ===
$(cat "$PROJECT_DIR/CLAUDE.md")

=== コンテキスト圧縮後の再注入: AGENTS.md ===
$(cat "$PROJECT_DIR/AGENTS.md")
EOF
)

jq -n --arg ctx "$CONTENT" '{
  hookSpecificOutput: {
    hookEventName: "SessionStart",
    additionalContext: $ctx
  }
}'
