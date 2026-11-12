#!/usr/bin/env sh
# Regenerates the pictures of the user interface stored in docs/images.
#
# The windows are built for real but never shown, so the script needs a display (it cannot run
# on a headless build server) yet does not disturb the desktop.
set -eu

cd "$(dirname "$0")/.."
./mvnw -q -B -ntp test-compile
java -cp target/classes:target/test-classes fr.uspn.cellula.ui.swing.Screenshots examples docs/images
