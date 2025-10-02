@echo off
setlocal EnableExtensions

:: Clean + compile
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
copy /Y "MicroLisp.jar" "%USERPROFILE%\bin\" >nul

(
  echo @echo off
) > "%USERPROFILE%\bin\microlisp.bat"

(
  echo java -jar "%%~dp0MicroLisp.jar" %%*
) >> "%USERPROFILE%\bin\microlisp.bat"

@echo off
setx PATH "%PATH%;%USERPROFILE%\bin">nul
echo Path updated permanently. Please restart command prompt for changes to take effect.

endlocal
