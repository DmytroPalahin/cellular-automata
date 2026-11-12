#!/usr/bin/env sh
# Regenerates the pictures of automata stored in docs/images, by running the application itself
# in batch mode on the example definitions. The report and the README use these pictures.
set -eu

cd "$(dirname "$0")/.."
./mvnw -q -B -ntp -DskipTests package

render() {
  definition=$1
  shift
  java -Djava.awt.headless=true -jar target/cellula.jar render "examples/$definition.xml" \
    --output "docs/images/$definition.png" "$@"
}

render growth          --steps 9   --cell-size 16
render sierpinski      --steps 127 --cell-size 3 --no-grid
render rule-30         --steps 127 --cell-size 3 --no-grid
render rule-110        --steps 255 --cell-size 3 --no-grid
render radius2-fractal --steps 255 --cell-size 1 --no-grid
render radius2-chaotic --steps 255 --cell-size 1 --no-grid
render game-of-life    --steps 80  --cell-size 8
render subject-example --steps 60  --cell-size 8
render brians-brain    --steps 60  --cell-size 8 --no-grid
render snowflake       --steps 46  --cell-size 8
render crystal-3d      --steps 30  --cell-size 10
render clouds-3d       --steps 30  --cell-size 14
render hypercube-4d    --steps 11  --cell-size 20
