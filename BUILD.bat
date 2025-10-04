@echo off
setlocal EnableExtensions

:: --- Locate JDK bin folder ---
set "JAVABIN="
for /d %%D in ("C:\Program Files\Java\jdk-*") do (
    if exist "%%D\bin\jar.exe" (
        set "JAVABIN=%%D\bin"
        goto foundjdk
    )
)

:foundjdk
if not defined JAVABIN (
    echo ERROR: Could not find a JDK with jar.exe under C:\Program Files\Java\jdk-*
    exit /b 1
)

echo Using JDK bin: %JAVABIN%

:: Clean + compile
if exist out rmdir /s /q out
mkdir out

echo Compiling...
"%JAVABIN%\javac" -d out src\*.java
if errorlevel 1 (
  echo Compile failed.
  exit /b 1
)

copy /Y banner.txt out\

echo Creating JAR...
"%JAVABIN%\jar" cfm MicroLisp.jar manifest.mf -C out .
if errorlevel 1 (
  echo JAR creation failed.
  exit /b 1
)

:: Ensure user bin folder exists
if not exist "%USERPROFILE%\bin" mkdir "%USERPROFILE%\bin"

:: Copy jar into bin folder so launcher can always find it
copy /Y "MicroLisp.jar" "%USERPROFILE%\bin\" >nul
echo Adding microlisp to the path
(
  echo @echo off
) > "%USERPROFILE%\bin\microlisp.bat"

(
  echo java -jar "%%~dp0MicroLisp.jar" %%*
) >> "%USERPROFILE%\bin\microlisp.bat"

:: --- Safely add %USERPROFILE%\bin to PATH without truncating ---
reg query "HKCU\Environment" /v PATH | find /i "%USERPROFILE%\bin" >nul
if errorlevel 1 (
    for /f "tokens=2*" %%a in ('reg query "HKCU\Environment" /v PATH ^| find /i "PATH"') do (
        set "UserPath=%%b"
    )
    if not defined UserPath (
        setx PATH "%USERPROFILE%\bin"
    ) else (
        setx PATH "%UserPath%;%USERPROFILE%\bin"
    )
)

echo MicroLisp installed, open a new terminal and enter 'microlisp' to use

endlocal
