#!/usr/bin/env sh
# Builds the archive to hand in: the source code, the input files and the report in PDF, as
# required by section 2.6 of the subject. The result is dist/cellula-<version>.zip.
set -eu

cd "$(dirname "$0")/.."
version=$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' pom.xml | head -n 1)
name="cellula-$version"

./mvnw -q -B -ntp verify
if [ ! -f report/report.pdf ]; then
  echo "report/report.pdf is missing: build it with 'make -C report' first" >&2
  exit 1
fi

rm -rf "dist/$name" "dist/$name.zip"
mkdir -p "dist/$name"
cp -R src examples docs scripts .mvn mvnw mvnw.cmd pom.xml \
  README.md CHANGELOG.md CONTRIBUTING.md LICENSE "dist/$name/"
mkdir -p "dist/$name/report"
cp report/report.pdf "dist/$name/"
cp -R report/main.tex report/sections report/Makefile "dist/$name/report/" 2>/dev/null || true
cp target/cellula.jar "dist/$name/"

(cd dist && zip -q -r "$name.zip" "$name" -x '*.DS_Store')
rm -rf "dist/$name"
echo "Wrote dist/$name.zip"
