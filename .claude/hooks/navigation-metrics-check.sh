#!/bin/bash
# SessionStart hook: ナビゲーション指標のチェック
# 2つの役割を持つ:
# 1. 閾値チェック: 直近5件のうち探索コスト3以上が3件以上 → 目次見直し警告
# 2. 鮮度チェック: 最新エントリが7日以上古い（または0件） → 記録リマインダー

METRICS_FILE="/kakehashi-api/docs/agents/navigation-metrics.md"
[ -f "$METRICS_FILE" ] || exit 0

# --- 1. 閾値チェック ---
high_cost_count=$(grep '^|' "$METRICS_FILE" \
  | grep -v -- '---|日付' \
  | tail -5 \
  | awk -F'|' '{cost=$5; gsub(/ /,"",cost); if(cost+0>=3) n++} END{print n+0}')

# --- 2. 鮮度チェック ---
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
msgs=""
if [ "${high_cost_count:-0}" -ge 3 ]; then
  msgs="⚠️ [Navigation Alert] 直近5件中${high_cost_count}件で探索コスト3以上。AGENTS.md/docs/README.md の目次構成の見直しを検討してください（「doc-maintainerサブエージェントでナビゲーション指標をチェックして」で改善案が出ます）。"
fi
if [ -n "$stale_msg" ]; then
  msgs="${msgs:+$msgs / }$stale_msg"
fi

[ -n "$msgs" ] || exit 0

printf '{"additionalContext": "%s"}\n' "$msgs"
