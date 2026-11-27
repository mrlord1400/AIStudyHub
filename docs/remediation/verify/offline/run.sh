#!/usr/bin/env bash
# Offline verification harness for the remediation work (see README.md in this folder).
# Compiles src/java and runs the checks against an in-memory H2 database. No Tomcat and no SQL Server.
# Downloads two jars (servlet API, H2) into $AISH_LIBS (default: $TMPDIR/aish-libs), never into the repo.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../../../.." && pwd)"
LIBS="${AISH_LIBS:-${TMPDIR:-/tmp}/aish-libs}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# Git Bash on Windows hands javac/java MSYS paths they cannot read; convert them and use ';' as separator.
if command -v cygpath >/dev/null 2>&1; then
  win() { cygpath -m "$1"; }
  SEP=";"
else
  win() { echo "$1"; }
  SEP=":"
fi

MAVEN=https://repo1.maven.org/maven2
fetch() { # fetch <file> <path under maven central>
  [ -s "$LIBS/$1" ] || curl -sSfL -o "$LIBS/$1" "$MAVEN/$2"
}
mkdir -p "$LIBS"
fetch servlet-api.jar javax/servlet/javax.servlet-api/3.1.0/javax.servlet-api-3.1.0.jar
fetch h2.jar com/h2database/h2/2.2.224/h2-2.2.224.jar
fetch javax.mail.jar com/sun/mail/javax.mail/1.6.2/javax.mail-1.6.2.jar
fetch javax.activation.jar com/sun/activation/javax.activation/1.2.0/javax.activation-1.2.0.jar

CP="$(win "$LIBS/servlet-api.jar")$SEP$(win "$LIBS/h2.jar")$SEP$(win "$LIBS/javax.mail.jar")$SEP$(win "$LIBS/javax.activation.jar")"
for j in "$ROOT"/web/WEB-INF/lib/*.jar; do CP="$CP$SEP$(win "$j")"; done

echo "== compiling src/java (javac --release 8)"
mkdir -p "$WORK/out" "$WORK/tests"
find "$ROOT/src/java" -name '*.java' | while read -r f; do win "$f"; done > "$WORK/srcs.txt"
javac --release 8 -encoding UTF-8 -nowarn -cp "$CP" -d "$(win "$WORK/out")" "@$(win "$WORK/srcs.txt")" 2>&1 | grep -v '^Note:' || true
[ -d "$WORK/out/Controller" ] || { echo "compile failed"; exit 1; }

echo "== compiling the checks"
find "$HERE" -maxdepth 1 -name '*.java' | while read -r f; do win "$f"; done > "$WORK/tsrcs.txt"
javac -encoding UTF-8 -nowarn -cp "$(win "$WORK/out")$SEP$CP" -d "$(win "$WORK/tests")" "@$(win "$WORK/tsrcs.txt")" 2>&1 | grep -v '^Note:' || true

TESTS="${*:-FilterHarness T12Test T13Test T14Test T15Test T17Test T18Test}"
failed=0
for t in $TESTS; do
  echo "== $t"
  out="$WORK/$t.log"
  if java -cp "$(win "$WORK/tests")$SEP$(win "$WORK/out")$SEP$CP" "$t" > "$out" 2>&1; then rc=0; else rc=$?; fi
  grep -E '^(PASS|FAIL)|ALL PASS|FAILED' "$out" | sed 's/^/   /' | grep -E 'FAIL|ALL PASS|FAILED' || true
  echo "   $(grep -c '^PASS' "$out" || true) passed, $(grep -c '^FAIL' "$out" || true) failed (exit $rc)"
  if [ "$rc" -ne 0 ]; then failed=1; tail -15 "$out" | sed 's/^/   | /'; fi
done
[ "$failed" -eq 0 ] && echo "== ALL CHECKS PASSED" || { echo "== SOME CHECKS FAILED"; exit 1; }
