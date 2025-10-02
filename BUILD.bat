@echo off
if exist out rmdir /s /q out
mkdir out

echo Compiling...
for /R src %%f in (*.java) do javac -d out "%%f"

echo Creating JAR...
jar cfm MicroLisp.jar manifest.mf -C out .

:: Create user bin folder if missing
if not exist "%USERPROFILE%\bin" mkdir "%USERPROFILE%\bin"

:: Create launcher in %USERPROFILE%\bin
(
echo @echo off
echo java -jar "%%~dp0..\MicroLisp.jar" %%*
) > "%USERPROFILE%\bin\microlisp.bat"

:: Add to PATH if not present
echo %PATH% | findstr /I "%USERPROFILE%\bin" >nul
if errorlevel 1 (
    echo.
    echo NOTE: You should add %%USERPROFILE%%\bin to your PATH:
    echo   setx PATH "%%PATH%%;%%USERPROFILE%%\bin"
)

echo.
echo Installed. Run with: microlisp examples.scm
