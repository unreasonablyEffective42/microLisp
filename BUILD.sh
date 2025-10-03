#!/usr/bin/env bash
set -e

# 1. Clean + compile
rm -rf out
mkdir out
javac -d out $(find src -name "*.java")
cp banner.txt out/
# 2. Package jar
jar cfm MicroLisp.jar manifest.mf -C out .

# 3. Ensure ~/bin exists
mkdir -p "$HOME/bin"

# 4. Create launcher in ~/bin
cat >"$HOME/bin/microlisp" <<'EOF'
#!/usr/bin/env bash
exec java -jar "$HOME/MicroLisp/MicroLisp.jar" "$@"
EOF
chmod +x "$HOME/bin/microlisp"

# 5. Check PATH
if ! echo "$PATH" | grep -q "$HOME/bin"; then
  echo 'export PATH="$HOME/bin:$PATH"' >>"$HOME/.bashrc"
  echo
  echo ">>> Added ~/bin to your PATH in ~/.bashrc"
  echo ">>> Run: source ~/.bashrc   (or open a new terminal) to update."
fi

echo
echo "MicroLisp Installed. You can now run in a new terminal with 'microlisp'"
