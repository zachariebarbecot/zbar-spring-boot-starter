#!/usr/bin/env bash
# Create or update the repository rulesets from the JSON files next to this script.
# Idempotent by name. Reads each ruleset back and fails if it differs from its file.
set -euo pipefail

dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(gh repo view --json nameWithOwner -q .nameWithOwner)"

# Fields compared after read-back (the API adds ids, links, timestamps).
normalize='{name, target, enforcement, conditions, bypass_actors, rules}
  | .bypass_actors |= sort_by(.actor_id)
  | .rules |= (map(.parameters |= (. // null)) | sort_by(.type))'

status=0
for file in "$dir"/*.json; do
  name="$(jq -r .name "$file")"
  id="$(gh api "repos/$repo/rulesets" --paginate -q ".[] | select(.name == \"$name\") | .id" | head -n1)"
  if [ -n "$id" ]; then
    gh api -X PUT "repos/$repo/rulesets/$id" --input "$file" >/dev/null
  else
    id="$(gh api -X POST "repos/$repo/rulesets" --input "$file" -q .id)"
  fi
  actual="$(gh api "repos/$repo/rulesets/$id" | jq -S "$normalize")"
  expected="$(jq -S "$normalize" "$file")"
  if [ "$actual" = "$expected" ]; then
    echo "OK   $name"
  else
    echo "DIFF $name" >&2
    diff <(echo "$expected") <(echo "$actual") >&2 || true
    status=1
  fi
done
exit "$status"
