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
# Embedded Tomcat + Jasper (only T31Test and Web.java use them): the real web.xml and the real JSPs
fetch tomcat-embed-core.jar org/apache/tomcat/embed/tomcat-embed-core/9.0.96/tomcat-embed-core-9.0.96.jar
fetch tomcat-embed-jasper.jar org/apache/tomcat/embed/tomcat-embed-jasper/9.0.96/tomcat-embed-jasper-9.0.96.jar
fetch tomcat-embed-el.jar org/apache/tomcat/embed/tomcat-embed-el/9.0.96/tomcat-embed-el-9.0.96.jar
fetch ecj.jar org/eclipse/jdt/ecj/3.33.0/ecj-3.33.0.jar
fetch annotation-api.jar javax/annotation/javax.annotation-api/1.3.2/javax.annotation-api-1.3.2.jar

CP="$(win "$LIBS/servlet-api.jar")$SEP$(win "$LIBS/h2.jar")$SEP$(win "$LIBS/javax.mail.jar")$SEP$(win "$LIBS/javax.activation.jar")"
for j in "$ROOT"/web/WEB-INF/lib/*.jar; do CP="$CP$SEP$(win "$j")"; done
# Tomcat 9 implements Servlet 4.0, so its jars must come BEFORE the Servlet 3.1 API jar on the classpath.
TCP=""
for j in tomcat-embed-core tomcat-embed-jasper tomcat-embed-el ecj annotation-api; do TCP="$TCP$(win "$LIBS/$j.jar")$SEP"; done

echo "== compiling src/java (javac --release 8)"
mkdir -p "$WORK/out" "$WORK/tests"
find "$ROOT/src/java" -name '*.java' | while read -r f; do win "$f"; done > "$WORK/srcs.txt"
javac --release 8 -encoding UTF-8 -nowarn -cp "$CP" -d "$(win "$WORK/out")" "@$(win "$WORK/srcs.txt")" 2>&1 | grep -v '^Note:' || true
[ -d "$WORK/out/Controller" ] || { echo "compile failed"; exit 1; }

echo "== compiling the checks"
find "$HERE" -maxdepth 1 -name '*.java' | while read -r f; do win "$f"; done > "$WORK/tsrcs.txt"
javac -encoding UTF-8 -nowarn -cp "$(win "$WORK/out")$SEP$TCP$CP" -d "$(win "$WORK/tests")" "@$(win "$WORK/tsrcs.txt")" 2>&1 | grep -v '^Note:' || true

TESTS="${*:-FilterHarness T12Test T13Test T14Test T15Test T17Test T18Test T2Test T31Test}"
failed=0
for t in $TESTS; do
  echo "== $t"
  out="$WORK/$t.log"
  # Web-based checks get the tomcat jars first and the paths of the web root and the compiled classes.
  EXTRA=""; JOPTS=""
  case "$t" in T3[0-9]Test) EXTRA="$TCP"; JOPTS="-DAISH_WEBROOT=$(win "$ROOT/web") -DAISH_CLASSES=$(win "$WORK/out")";; esac
  if java $JOPTS -cp "$(win "$WORK/tests")$SEP$EXTRA$(win "$WORK/out")$SEP$CP" "$t" > "$out" 2>&1; then rc=0; else rc=$?; fi
  grep -E '^(PASS|FAIL)|ALL PASS|FAILED' "$out" | sed 's/^/   /' | grep -E 'FAIL|ALL PASS|FAILED' || true
  echo "   $(grep -c '^PASS' "$out" || true) passed, $(grep -c '^FAIL' "$out" || true) failed (exit $rc)"
  # AISH_VERBOSE=1 shows everything that is not a PASS line (stack traces, DAO error output)
  if [ -n "${AISH_VERBOSE:-}" ]; then grep -v '^PASS' "$out" | head -80 | sed 's/^/   v /' || true; fi
  if [ "$rc" -ne 0 ]; then failed=1; tail -15 "$out" | sed 's/^/   | /'; fi
done
[ "$failed" -eq 0 ] && echo "== ALL CHECKS PASSED" || { echo "== SOME CHECKS FAILED"; exit 1; }
