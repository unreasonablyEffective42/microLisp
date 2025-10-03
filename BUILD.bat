@echo off
setlocal EnableExtensions

:: --- Locate Java bin folder ---
echo Checking Java installation...

if defined JAVA_HOME (
  set "JAVABIN=%JAVA_HOME%\bin"
) else (
  for /f "usebackq tokens=*" %%i in (`where java`) do (
    set "JAVABIN=%%~dpi"
    goto :foundjava
  )
)

:foundjava
if not defined JAVABIN (
  echo Could not determine Java bin path.
  exit /b 1
)

:: Remove trailing backslash if exists
if "%JAVABIN:~-1%"=="\" set "JAVABIN=%JAVABIN:~0,-1%"

echo Using Java bin: %JAVABIN%

:: Prepend Java bin to PATH for this script only
set "PATH=%JAVABIN%;%PATH%"

:: --- Build steps ---
if exist out rmdir /s /q out
mkdir out

echo Compiling...
javac -d out src\*.java
if errorlevel 1 (
  echo Compile failed.
  exit /b 1
)

echo Creating JAR...
jar cfm MicroLisp.jar manifest.mf -C out .
if errorlevel 1 (
  echo JAR creation failed.
  exit /b 1
)

:: Ensure user bin folder exists
if not exist "%USERPROFILE%\bin" mkdir "%USERPROFILE%\bin"

:: Copy jar into bin folder so launcher can always find it
copy /Y "MicroLisp.jar" "%USERPROFILE%\bin\" 

(
  echo @echo off
) > "%USERPROFILE%\bin\microlisp.bat"

(
  echo java -jar "%%~dp0MicroLisp.jar" %%*
) >> "%USERPROFILE%\bin\microlisp.bat"

endlocal
