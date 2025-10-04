@echo off
setlocal EnableExtensions

echo =============================================
echo   MicroLisp Windows Build
echo =============================================
echo.

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
echo.

:: 1. Clean + compile
if exist out rmdir /s /q out
mkdir out
echo Compiling MicroLisp...
"%JAVABIN%\javac" -d out src\*.java
if errorlevel 1 (
    echo Compile failed.
    exit /b 1
)
copy /Y banner.txt out\ >nul
echo Compilation complete.
echo.

:: 2. Package jar
echo Packaging MicroLisp.jar...
"%JAVABIN%\jar" cfm MicroLisp.jar manifest.mf -C out .
if errorlevel 1 (
    echo JAR creation failed.
    exit /b 1
)
echo Packaging complete.
echo.

:: 3. Ensure user bin folder exists
echo Creating launcher...
if not exist "%USERPROFILE%\bin" mkdir "%USERPROFILE%\bin"

:: 4. Copy jar and launcher
copy /Y "MicroLisp.jar" "%USERPROFILE%\bin\" >nul

(
  echo @echo off
  echo java -jar "%%~dp0MicroLisp.jar" %%*
) > "%USERPROFILE%\bin\microlisp.bat"

echo Launcher created at %USERPROFILE%\bin\microlisp.bat
echo.

:: 5. Add to PATH if needed
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
    echo Added %%USERPROFILE%%\bin to PATH. Open a new terminal for it to take effect.
)
echo PATH check complete.
echo.

:: 6. Run unit tests
echo =============================================
echo Testing Build
echo =============================================

"%JAVABIN%\java" -cp out MicroLispTest
echo MicroLisp Installed
echo You can now run 'microlisp' in a new terminal
echo =============================================

endlocal
