#!/usr/bin/env bash
set -e
echo =============================================
echo MicroLisp Linux Build
echo =============================================

JCODEC_JARS=(
  "lib/jcodec-0.2.3.jar"
  "lib/jcodec-javase-0.2.3.jar"
)
for jar in "${JCODEC_JARS[@]}"; do
  if [[ ! -f "$jar" ]]; then
    case "$jar" in
      lib/jcodec-0.2.3.jar)
        url="https://repo1.maven.org/maven2/org/jcodec/jcodec/0.2.3/jcodec-0.2.3.jar"
        ;;
      lib/jcodec-javase-0.2.3.jar)
        url="https://repo1.maven.org/maven2/org/jcodec/jcodec-javase/0.2.3/jcodec-javase-0.2.3.jar"
        ;;
      *)
        url=""
        ;;
    esac
    echo "Missing dependency: $jar"
    if [[ -n "$url" ]]; then
      echo "Download it with:"
      echo "  curl -L -o $jar $url"
    fi
    exit 1
  fi
done
JCODEC_CP=$(IFS=:; echo "${JCODEC_JARS[*]}")

# 1. Clean + compile
rm -rf out
mkdir out
echo "Compiling MicroLisp"
javac -cp "$JCODEC_CP" -d out $(find src -name "*.java")

# 2. Copy resources
echo "Copying resources"
cp src/banner.txt out/
mkdir -p out/lib
cp -r src/lib/* out/lib/ 2>/dev/null || true

# 3. Package jar
echo "Packaging MicroLisp.jar"
jar cfm MicroLisp.jar manifest.mf -C out .

# 4. Ensure ~/bin exists
mkdir -p "$HOME/bin"

# 5. Create launcher in ~/bin
echo "Creating Launcher"
if cat <<'EOF' >"$HOME/bin/microlisp"; then
#!/usr/bin/env bash
exec java -cp "$HOME/MicroLisp/MicroLisp.jar:$HOME/MicroLisp/lib/jcodec-0.2.3.jar:$HOME/MicroLisp/lib/jcodec-javase-0.2.3.jar" MicroLisp "$@"
EOF
  chmod +x "$HOME/bin/microlisp"
else
  echo "Warning: could not write launcher at $HOME/bin/microlisp"
fi
echo "Checking USR/bin/ added to PATH"
# 6. Check PATH
if ! echo "$PATH" | grep -q "$HOME/bin"; then
    echo 'export PATH="$HOME/bin:$PATH"' >>"$HOME/.bashrc"
    echo
    echo ">>> Added ~/bin to your PATH in ~/.bashrc"
    echo ">>> Run: source ~/.bashrc   (or open a new terminal) to update."
fi

echo =============================================
echo Testing Build
echo =============================================

java -cp "./out:$JCODEC_CP" MicroLispTest
echo MicroLisp Installed
echo You can now run 'microlisp' in a new terminal
echo =============================================
