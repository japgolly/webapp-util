#!/bin/bash

set -euo pipefail
cd "$(dirname "$0")"

output="*.html helium examples/*.html"

# Remove old output
rm -rf $output

sbt clean test ghpages/laikaSite

echo
cp -rv ghpages/target/docs/site/. .
echo

git add $output
git status
echo
echo "git commit -m 'Refresh ghpages'"
echo "git push && git checkout master"
echo
