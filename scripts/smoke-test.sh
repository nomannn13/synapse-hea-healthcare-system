#!/usr/bin/env sh
set -eu
BASE_URL="${BASE_URL:-http://localhost:8080/api/v1}"
COOKIE_FILE="${TMPDIR:-/tmp}/synapse-cookies.txt"

login() {
  curl -fsS -c "$COOKIE_FILE" -X POST "$BASE_URL/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"Password123!\"}"
}

PATIENT_TOKEN="$(login patient@synapse.local | python3 -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])')"
ADMIN_TOKEN="$(login admin@synapse.local | python3 -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])')"
DOCTOR_TOKEN="$(login doctor@synapse.local | python3 -c 'import json,sys; print(json.load(sys.stdin)["accessToken"])')"

curl -fsS "$BASE_URL/dashboard" -H "Authorization: Bearer $PATIENT_TOKEN" >/dev/null
curl -fsS "$BASE_URL/doctors" >/dev/null
curl -fsS "$BASE_URL/prescriptions" -H "Authorization: Bearer $PATIENT_TOKEN" >/dev/null
curl -fsS "$BASE_URL/invoices" -H "Authorization: Bearer $PATIENT_TOKEN" >/dev/null
curl -fsS "$BASE_URL/admin/reports/summary" -H "Authorization: Bearer $ADMIN_TOKEN" >/dev/null
curl -fsS "$BASE_URL/doctors/me/availability" -H "Authorization: Bearer $DOCTOR_TOKEN" >/dev/null
curl -fsS "$BASE_URL/search?q=card" -H "Authorization: Bearer $PATIENT_TOKEN" >/dev/null

printf 'Synapse HEA Phase 2 smoke test passed.\n'
