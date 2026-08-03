#!/usr/bin/env bash
#
# Runs the stateless-MCP module unit tests and/or the external conformance suite.
#
#   ./run-all-tests.sh              # unit tests for all stateless modules, then conformance
#   ./run-all-tests.sh unit         # just the module unit tests
#   ./run-all-tests.sh conformance  # just the conformance suite (starts+stops its own server)
#   MCP_SERVER=undertow ./run-all-tests.sh conformance   # probe fall-through on a different backend
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"   # repo root
GRADLE="$ROOT/gradlew"
CONFORMANCE_DIR="$ROOT/pro/ai/mcp-stateless/conformance"
SERVER_URL="http://localhost:4001/mcp"

MODULES=(
  :http4k-ai-mcp-stateless-core
  :http4k-ai-mcp-stateless-sdk
  :http4k-ai-mcp-stateless-client
  :http4k-ai-mcp-stateless-testing
  :http4k-ai-mcp-stateless-conformance
  :http4k-ai-mcp-stateless-a2a-bridge
  :http4k-ai-mcp-stateless-mpp
  :http4k-ai-mcp-stateless-x402
)

run_unit() {
  echo "==> Unit tests: ${MODULES[*]}"
  "$GRADLE" "${MODULES[@]/%/:test}"
}

run_conformance() {
  echo "==> Conformance suite (backend: ${MCP_SERVER:-jetty})"
  # Start the reference server in the background; kill it (and the forked JVM) on exit.
  "$GRADLE" -Dmcp.server="${MCP_SERVER:-jetty}" \
    :http4k-ai-mcp-stateless-conformance:runMcpConformanceServer \
    >/tmp/mcp-conformance-server.log 2>&1 &
  local gradle_pid=$!
  trap 'kill "$gradle_pid" 2>/dev/null; pkill -f McpConformanceServerKt 2>/dev/null; true' RETURN

  echo "    waiting for $SERVER_URL ..."
  for _ in $(seq 1 90); do
    curl -s -o /dev/null "$SERVER_URL" && break
    sleep 1
  done
  curl -s -o /dev/null "$SERVER_URL" || { echo "!! server never came up — see /tmp/mcp-conformance-server.log"; return 1; }

  # run-mcp-conformance-tests.sh runs the npx harness vs conformance-baseline.yml (exit 0 = only baselined fails).
  ( cd "$CONFORMANCE_DIR" && ./run-mcp-conformance-tests.sh )
}

case "${1:-all}" in
  unit)         run_unit ;;
  conformance)  run_conformance ;;
  all)          run_unit; run_conformance ;;
  *) echo "usage: $0 [unit|conformance|all]"; exit 2 ;;
esac

echo "==> done"
