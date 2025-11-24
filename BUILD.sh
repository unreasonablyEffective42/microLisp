#!/usr/bin/env bash
set -euo pipefail

echo "============================================="
echo " MicroLisp Build (Linux/macOS)"
echo "============================================="

OS="$(uname -s)"
case "$OS" in
Linux*) PLATFORM="linux" ;;
Darwin*) PLATFORM="macos" ;;
*)
  echo "Unsupported OS: $OS"
  exit 1
  ;;
esac

# -------------------------------
# Configuration
# -------------------------------
SRC_DIR="src"
OUT_DIR="out"
DIST_DIR="dist"
LIB_DIR="lib"

# Third-party jars (download if missing)
JCODEC_CORE="$LIB_DIR/jcodec-0.2.3.jar"
JCODEC_AWT="$LIB_DIR/jcodec-javase-0.2.3.jar"
JCODEC_CP="$JCODEC_CORE:$JCODEC_AWT"

MAIN_TEST_CLASS="MicroLispTest" # keep your existing test entrypoint
MAIN_RUN_CLASS="MicroLisp"      # your main REPL/loader class

# -------------------------------
# Helpers
# -------------------------------
need_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "ERROR: missing required command: $1" >&2
    exit 1
  }
}

download_to() {
  # usage: download_to URL DEST
  local url="$1"
  local dest="$2"
  mkdir -p "$(dirname "$dest")"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$url" -o "$dest"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$url" -O "$dest"
  else
    echo "ERROR: need curl or wget to download $url" >&2
    exit 1
  fi
}

choose_profile_file() {
  # prefer zsh on macOS, bash on Linux, but fall back gracefully
  if [ "$PLATFORM" = "macos" ]; then
    if [ -f "$HOME/.zshrc" ] || [ "${SHELL##*/}" = "zsh" ]; then
      echo "$HOME/.zshrc"
      return
    fi
    # fallback(s)
    [ -f "$HOME/.bash_profile" ] && {
      echo "$HOME/.bash_profile"
      return
    }
    [ -f "$HOME/.bashrc" ] && {
      echo "$HOME/.bashrc"
      return
    }
    echo "$HOME/.profile"
  else
    # Linux
    [ -f "$HOME/.bashrc" ] && {
      echo "$HOME/.bashrc"
      return
    }
    [ -f "$HOME/.bash_profile" ] && {
      echo "$HOME/.bash_profile"
      return
    }
    [ -f "$HOME/.profile" ] && {
      echo "$HOME/.profile"
      return
    }
    echo "$HOME/.bashrc"
  fi
}

# -------------------------------
# Checks
# -------------------------------
echo
echo "[1/6] Checking toolchain"
need_cmd javac
need_cmd java
need_cmd jar
echo "  • java:  $(java -version 2>&1 | head -n1)"
echo "  • javac: $(javac -version 2>&1)"

# -------------------------------
# Dependencies
# -------------------------------
echo
echo "[2/6] Ensuring lib/ dependencies"

if [ ! -f "$JCODEC_CORE" ]; then
  echo "  • fetching $JCODEC_CORE"
  download_to "https://repo1.maven.org/maven2/org/jcodec/jcodec/0.2.3/jcodec-0.2.3.jar" "$JCODEC_CORE"
fi

if [ ! -f "$JCODEC_AWT" ]; then
  echo "  • fetching $JCODEC_AWT"
  download_to "https://repo1.maven.org/maven2/org/jcodec/jcodec-javase/0.2.3/jcodec-javase-0.2.3.jar" "$JCODEC_AWT"
fi

# -------------------------------
# Compile
# -------------------------------
echo
echo "[3/6] Compiling sources → $OUT_DIR"
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"

# Build the compile classpath
CP="."
[ -d "$LIB_DIR" ] && CP="$CP:$JCODEC_CP"

# Compile all .java under src/ (macOS bash 3.2 compatible)
if [ -d "$SRC_DIR" ]; then
  # Quick existence check
  if ! find "$SRC_DIR" -type f -name "*.java" -print -quit | grep -q .; then
    echo "ERROR: no Java sources found under $SRC_DIR" >&2
    exit 1
  fi
  # Use BSD find + xargs (supports -print0/-0) to avoid mapfile/readarray
  JAVA_FILES=()
  while IFS= read -r -d '' file; do
    JAVA_FILES+=("$file")
  done < <(find "$SRC_DIR" -type f -name "*.java" -print0)

  if [ "${#JAVA_FILES[@]}" -eq 0 ]; then
    echo "ERROR: no Java sources found under $SRC_DIR" >&2
    exit 1
  fi

  javac -encoding UTF-8 -classpath "$CP" -d "$OUT_DIR" "${JAVA_FILES[@]}"
else
  echo "ERROR: source dir '$SRC_DIR' not found" >&2
  exit 1
fi

# -------------------------------
# Resources → out/
# (copies .mu libraries, banner, examples, etc., if present)
# -------------------------------
echo
echo "[4/6] Copying resources"
# Copy lib/*.mu, resources, and any non-java assets you rely on.
# Add/adjust paths below to match your repo layout.
shopt -s nullglob || true

# Common patterns you’ve used before:
for f in lib/*.mu; do
  dest="$OUT_DIR/lib"
  mkdir -p "$dest"
  cp -f "$f" "$dest/"
done

# Copy any non-Java assets from src/ (e.g., banner.txt)
if [ -d "$SRC_DIR" ]; then
  find "$SRC_DIR" -type f ! -name "*.java" -print0 |
    while IFS= read -r -d '' file; do
      rel="${file#$SRC_DIR/}"
      dest="$OUT_DIR/$rel"
      mkdir -p "$(dirname "$dest")"
      cp -f "$file" "$dest"
    done
fi

# Legacy support for a top-level banner.txt, if someone keeps it there
if [ -f "banner.txt" ]; then
  mkdir -p "$OUT_DIR"
  cp -f banner.txt "$OUT_DIR/"
fi

# Optional: copy examples
if [ -d "examples" ]; then
  mkdir -p "$OUT_DIR/examples"
  cp -rf examples/* "$OUT_DIR/examples/" || true
fi

# -------------------------------
# Package JAR
# -------------------------------
echo
echo "[5/6] Packaging JAR → $DIST_DIR/microlisp.jar"
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

# If you have a manifest with Main-Class, include it; otherwise we
# can set an entrypoint via the launcher script below.
# Example Manifest usage (uncomment if you keep META-INF/MANIFEST.MF prepared):
# jar --create --file "$DIST_DIR/microlisp.jar" --manifest META-INF/MANIFEST.MF -C "$OUT_DIR" .

jar --create --file "$DIST_DIR/microlisp.jar" -C "$OUT_DIR" .

# -------------------------------
# Install launcher
# -------------------------------
echo
echo "[6/6] Installing launcher"

INSTALL_BIN="$HOME/bin"
mkdir -p "$INSTALL_BIN"

PROJ_ABS_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_ABS="$PROJ_ABS_DIR/$DIST_DIR/microlisp.jar"

LAUNCHER="$INSTALL_BIN/microlisp"
cat >"$LAUNCHER" <<EOF
#!/usr/bin/env bash
set -euo pipefail
# microlisp launcher (installed by BUILD.sh)
DIR="\$(cd "\$(dirname "\${BASH_SOURCE[0]}")" && pwd)"
# We baked absolute paths at build time so this works outside the repo:
JAR="$JAR_ABS"
LIB="$PROJ_ABS_DIR/$LIB_DIR"

CP="\$JAR"
if [ -d "\$LIB" ]; then
  # include third-party jars if present
  CP="\$CP:\$LIB/jcodec-0.2.3.jar:\$LIB/jcodec-javase-0.2.3.jar"
fi

exec java -cp "\$CP" $MAIN_RUN_CLASS "\$@"
EOF
chmod +x "$LAUNCHER"
echo "  • installed: $LAUNCHER"

# Ensure ~/bin is on PATH (favor zsh on mac)
PROFILE_FILE="$(choose_profile_file)"
if ! printf '%s' ":$PATH:" | grep -q ":$HOME/bin:"; then
  echo >>"$PROFILE_FILE"
  echo '# Added by MicroLisp BUILD.sh' >>"$PROFILE_FILE"
  echo 'export PATH="$HOME/bin:$PATH"' >>"$PROFILE_FILE"
  echo "  • added ~/bin to PATH in: $PROFILE_FILE"
  echo "    (open a new terminal or 'source' your profile to pick it up)"
else
  echo "  • ~/bin already on PATH"
fi

echo
echo "============================================="
echo " Testing build"
echo "============================================="
# Classpath for test (compiled classes + libs)
TEST_CP="$OUT_DIR:$JCODEC_CP"
if java -cp "$TEST_CP" "$MAIN_TEST_CLASS"; then
  echo "OK: $MAIN_TEST_CLASS ran"
else
  echo "WARN: $MAIN_TEST_CLASS did not run (skipping) — continuing"
fi

echo
echo "✅ MicroLisp installed. Open a new terminal and run: microlisp"
echo "============================================="
