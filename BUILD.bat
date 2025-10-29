@echo off
setlocal EnableExtensions

echo =============================================
echo   MicroLisp Windows Build
echo =============================================
echo.

set "JCODEC_CORE=lib\jcodec-0.2.3.jar"
set "JCODEC_AWT=lib\jcodec-javase-0.2.3.jar"
set "JCODEC_CP=%JCODEC_CORE%;%JCODEC_AWT%"

if not exist "%JCODEC_CORE%" (
    echo ERROR: Missing dependency %JCODEC_CORE%
    echo Download it with:
    echo   curl -L -o %JCODEC_CORE% https://repo1.maven.org/maven2/org/jcodec/jcodec/0.2.3/jcodec-0.2.3.jar
    exit /b 1
)
if not exist "%JCODEC_AWT%" (
    echo ERROR: Missing dependency %JCODEC_AWT%
    echo Download it with:
    echo   curl -L -o %JCODEC_AWT% https://repo1.maven.org/maven2/org/jcodec/jcodec-javase/0.2.3/jcodec-javase-0.2.3.jar
    exit /b 1
)

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
"%JAVABIN%\javac" -cp "%JCODEC_CP%" -d out src\*.java
if errorlevel 1 (
    echo Compile failed.
    exit /b 1
)
echo Copying resources...
copy src\banner.txt out\ >nul 2>&1

if exist src\lib (
    mkdir out\lib >nul 2>&1
    xcopy src\lib out\lib /E /I /Y >nul
)

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
if not exist "%USERPROFILE%\bin\lib" mkdir "%USERPROFILE%\bin\lib"
copy /Y "%JCODEC_CORE%" "%USERPROFILE%\bin\lib\" >nul
copy /Y "%JCODEC_AWT%" "%USERPROFILE%\bin\lib\" >nul

(
  echo @echo off
  echo java -cp "%%~dp0MicroLisp.jar;%%~dp0lib\jcodec-0.2.3.jar;%%~dp0lib\jcodec-javase-0.2.3.jar" MicroLisp %%*
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

"%JAVABIN%\java" -cp "out;%JCODEC_CP%" MicroLispTest
echo MicroLisp Installed
echo You can now run 'microlisp' in a new terminal
echo =============================================

endlocal
