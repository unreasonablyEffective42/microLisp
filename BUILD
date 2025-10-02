#!/usr/bin/env bash

case "$(uname -s)" in
  Linux*|Darwin*) exec bash BUILD.sh "$@";;
  CYGWIN*|MINGW*|MSYS*|Windows_NT) exec cmd /c BUILD.bat "$@";;
  *) echo "Unsupported OS"; exit 1;;
esac
