#!/usr/bin/env bash
set -e
echo =============================================
echo MicroLisp Linux Build
echo =============================================

# 1. Clean + compile
rm -rf out
mkdir out
echo "Compiling MicroLisp"
javac -d out $(find src -name "*.java")

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
cat >"$HOME/bin/microlisp" <<'EOF'
#!/usr/bin/env bash
exec java -jar "$HOME/MicroLisp/MicroLisp.jar" "$@"
EOF
chmod +x "$HOME/bin/microlisp"
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

java -cp ./out/ MicroLispTest
echo MicroLisp Installed
echo You can now run 'microlisp' in a new terminal
echo =============================================
