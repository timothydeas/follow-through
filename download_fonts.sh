#!/usr/bin/env bash
# Downloads Lora and DM Sans font files into the app's res/font directory.
# Run once before building: bash download_fonts.sh
set -euo pipefail

FONT_DIR="app/src/main/res/font"
mkdir -p "$FONT_DIR"

BASE="https://github.com/google/fonts/raw/main/ofl"

echo "Downloading Lora..."
curl -fL "$BASE/lora/Lora%5Bwght%5D.ttf"         -o "$FONT_DIR/lora_variable.ttf"
curl -fL "$BASE/lora/Lora-Italic%5Bwght%5D.ttf"  -o "$FONT_DIR/lora_variable_italic.ttf"

echo "Downloading DM Sans..."
curl -fL "$BASE/dmsans/DMSans%5Bopsz,wght%5D.ttf" -o "$FONT_DIR/dmsans_variable.ttf"

echo "Done. Font files are in $FONT_DIR"
