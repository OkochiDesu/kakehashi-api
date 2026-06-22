#!/bin/bash
# SessionStart hook: ナビゲーション指標のチェック
# 役割:
# 1. セッション計測開始: 開始時刻・ツールカウンタを初期化
# 2. 前回セッションサマリー読み込み: 前回の計測結果をコンテキスト注入
# 3. 閾値チェック: 直近5件のうち探索コスト3以上が3件以上 → 目次見直し警告
# 4. 鮮度チェック: 最新エントリが7日以上古い（または0件） → 記録リマインダー

METRICS_FILE="/kakehashi-api/docs/agents/navigation-metrics.md"
SESSION_START_FILE="/tmp/claude_kakehashi_session_start"
TOOL_COUNT_FILE="/tmp/claude_kakehashi_tool_count"
LAST_SESSION_FILE="/tmp/claude_kakehashi_last_session.json"

json_escape() { local s="${1//\\/\\\\}"; s="${s//\"/\\\"}"; printf '%s' "$s"; }

# --- 1. セッション計測開始 ---
date +%s > "$SESSION_START_FILE"
echo "0" > "$TOOL_COUNT_FILE"

# --- 2. 前回セッションサマリー読み込み ---
prev_msg=""
if [ -f "$LAST_SESSION_FILE" ]; then
  prev_date=$(grep -o '"date":"[^"]*"' "$LAST_SESSION_FILE" | cut -d'"' -f4)
  prev_tools=$(grep -o '"tool_count":[0-9]*' "$LAST_SESSION_FILE" | cut -d: -f2)
  prev_duration=$(grep -o '"duration_min":[0-9]*' "$LAST_SESSION_FILE" | cut -d: -f2)
  if [ -n "$prev_date" ]; then
    prev_msg="📊 [前回セッション計測] ${prev_date} / ツール呼び出し: ${prev_tools}回 / 所要時間: 約${prev_duration}分。navigation-metrics.md への記録がまだであれば追記してください。"
  fi
  rm -f "$LAST_SESSION_FILE"
fi

[ -f "$METRICS_FILE" ] || { [ -n "$prev_msg" ] && printf '{"additionalContext": "%s"}\n' "$(json_escape "$prev_msg")"; exit 0; }

# --- 3. 閾値チェック ---
high_cost_count=$(grep '^|' "$METRICS_FILE" \
  | grep -v -- '---|日付' \
  | tail -5 \
  | awk -F'|' '{cost=$5; gsub(/ /,"",cost); if(cost+0>=3) n++} END{print n+0}')

# --- 4. 鮮度チェック ---
last_date=$(grep '^|' "$METRICS_FILE" \
  | grep -v -- '---|日付' \
  | tail -1 \
  | awk -F'|' '{gsub(/ /,"",$2); print $2}')

stale_msg=""
if [ -z "$last_date" ]; then
  stale_msg="📝 [Navigation Reminder] navigation-metrics.md にまだ記録がありません。作業区切りやPR作成後に難易度・探索コストを1行追記してください。"
else
  today_epoch=$(date +%s)
  last_epoch=$(date -d "$last_date" +%s 2>/dev/null)
  if [ -n "$last_epoch" ]; then
    diff_days=$(( (today_epoch - last_epoch) / 86400 ))
    if [ "$diff_days" -ge 7 ]; then
      stale_msg="📝 [Navigation Reminder] navigation-metrics.md の最終記録から${diff_days}日経過しています。作業区切りやPR作成後に1行追記してください。"
    fi
  fi
fi

# --- メッセージ合成 ---
msgs="$prev_msg"
if [ "${high_cost_count:-0}" -ge 3 ]; then
  alert="⚠️ [Navigation Alert] 直近5件中${high_cost_count}件で探索コスト3以上。AGENTS.md/docs/README.md の目次構成の見直しを検討してください（「doc-maintainerサブエージェントでナビゲーション指標をチェックして」で改善案が出ます）。"
  msgs="${msgs:+$msgs / }$alert"
fi
if [ -n "$stale_msg" ]; then
  msgs="${msgs:+$msgs / }$stale_msg"
fi

[ -n "$msgs" ] || exit 0

printf '{"additionalContext": "%s"}\n' "$(json_escape "$msgs")"
