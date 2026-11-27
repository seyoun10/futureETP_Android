#!/usr/bin/env bash
# 계약 테스트 예시 저장: 로컬 dev 서버 응답의 data를 app/src/test/resources/contract/fixtures/<스키마>[.<변형>].json으로 쓴다 (D-142, AOS-012)
# 차트 두 개(disparityChart·contractsChart)는 마지막 60거래일만 남긴다(파일 크기). 모델 디코딩·스키마 검증에는 충분하다
# 접근 키는 futureETP apps/server/.env의 API_TOKEN을 읽는다. 서버는 `npm run dev -w apps/server`로 먼저 띄운다
# 사용: scripts/capture-fixtures.sh [이름 ...]   (이름이 없으면 전부)
set -euo pipefail
cd "$(dirname "$0")/.."

FUTUREETP_DIR="${FUTUREETP_DIR:-../futureETP_ReactNative}"
BASE_URL="${BASE_URL:-http://localhost:3000}"
OUT=app/src/test/resources/contract/fixtures
TOKEN=$(grep '^API_TOKEN=' "$FUTUREETP_DIR/apps/server/.env" | cut -d= -f2-)
# 다른 클라이언트의 예시 기기와 겹치지 않는 고정 ID(dev DB에 행이 하나 생긴다)
DEVICE_ID=00000000-0000-4000-8000-0000000000a0
mkdir -p "$OUT"

# 파일 이름(스키마[.변형]) 경로 인증 [PUT 본문(공백 없는 JSON)]
FIXTURES=(
  "health /v1/health no"
  "commodities /v1/commodities yes"
  "disparityChart /v1/commodities/GC/disparity?basis=FRONT&ma=20&window=Y3&levels=1,2.5,5,10,20 yes"
  "disparityChart.before /v1/commodities/CL/disparity?basis=KR&ma=200&window=ALL&levels=1,2,5,10,20&before=2018-01-02 yes"
  "contractsChart /v1/commodities/GC/contracts yes"
  "contractsChart.before /v1/commodities/NG/contracts?before=2018-01-02 yes"
  "curve /v1/commodities/GC/curve yes"
  "curve.baseDate /v1/commodities/CL/curve?baseDate=2020-04-20 yes"
  "etpList /v1/commodities/GC/etps yes"
  "etpList.CL /v1/commodities/CL/etps yes"
  # 2026년은 네 종류 예외가 모두 있고, 2028년은 아직 입력 전이라 missing이 찬다
  "calendar /v1/calendar?from=2026-01-01&to=2026-12-31 yes"
  "calendar.missing /v1/calendar?from=2027-06-01&to=2028-05-31 yes"
  "settings /v1/settings yes"
  'device /v1/devices yes {"pushToken":null,"platform":"ANDROID","appVersion":"aos-1.0.0"}'
)

# 응답 봉투에서 data만 꺼내고, 차트는 마지막 60거래일만 남긴다
TRIM='.data
  | if has("bars") then .bars |= .[-60:]
    elif has("dates") then .dates |= .[-60:] | .contracts |= map(.prices |= .[-60:])
    else . end'

capture() {
  local name=$1 path=$2 auth=$3 body=${4:-}
  local args=(-sS -m 30 --fail-with-body)
  if [[ -n $body ]]; then
    args+=(-X PUT -H "Content-Type: application/json" -d "$body")
  fi
  if [[ $auth == yes ]]; then
    args+=(-H "Authorization: Bearer $TOKEN" -H "X-Device-Id: $DEVICE_ID")
  fi
  curl "${args[@]}" "$BASE_URL$path" | jq "$TRIM" >"$OUT/$name.json"
  echo "$OUT/$name.json ← $path"
}

for entry in "${FIXTURES[@]}"; do
  read -r name path auth body <<<"$entry"
  if [[ $# -eq 0 ]] || [[ " $* " == *" $name "* ]]; then
    capture "$name" "$path" "$auth" "$body"
  fi
done

# ETP 상세는 목록의 첫 KRX ETN(총보수·상장일 null일 수 있음)과 첫 US ETF를 받는다. dev DB를 다시 만들어도 id를 고칠 필요가 없다
SELECTED_NAMES=" $* "
detail() {
  local name=$1 list=$2 filter=$3
  if [[ $SELECTED_NAMES == "  " ]] || [[ $SELECTED_NAMES == *" $name "* ]]; then
    local id
    id=$(jq -r "[.etps[] | select($filter)][0].id" "$OUT/$list.json")
    capture "$name" "/v1/etps/$id" yes
  fi
}
detail etpDetail etpList '.market == "KRX" and .type == "ETN"'
detail etpDetail.us etpList '.market == "US"'
