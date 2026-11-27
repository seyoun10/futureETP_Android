#!/usr/bin/env bash
# 원본 계약(../futureETP_ReactNative/packages/shared/contract)을 app/src/test/resources/contract/schemas로 복사한다(D-142, AOS-012).
# 원본 저장소 없이도 계약 테스트가 돌게 복사본을 커밋한다. 원본이 있으면 ContractTest가 복사본과 같은지 보므로,
# 원본이 바뀌어 테스트가 실패하면 이걸 돌리고 모델·예시(scripts/capture-fixtures.sh)를 고친다.
set -euo pipefail
cd "$(dirname "$0")/.."
src="${FUTUREETP_DIR:-../futureETP_ReactNative}/packages/shared/contract"
out=app/src/test/resources/contract/schemas
rm -rf "$out"
mkdir -p "$out"
cp "$src/api.json" "$out/"
cp "$src"/schemas/*.json "$out/"
echo "copied $(ls "$out" | wc -l | tr -d ' ') files from $(git -C "$src" rev-parse --short HEAD)"
