@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT_DIR=%~dp0"
cd /d "%ROOT_DIR%"

set "APP_NAME=PokemonBattle"
set "APP_VERSION=1.0.0"
set "DIST_DIR=dist"
set "INPUT_DIR=%DIST_DIR%\input"
set "MAIN_JAR=PokemonBattle-1.0-SNAPSHOT.jar"
set "MAIN_CLASS=com.example.pokemonbattle.Launcher"
set "JDK_MODULES=java.base,java.desktop,java.logging,java.sql,java.prefs"
set "JAVA_OPTS=--enable-native-access=javafx.graphics,javafx.media,ALL-UNNAMED"

echo [1/5] Building application jar...
call mvnw.cmd -DskipTests clean package
if errorlevel 1 goto :fail

echo [2/5] Copying runtime dependencies...
call mvnw.cmd -DskipTests dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target\dependency
if errorlevel 1 goto :fail

echo [3/5] Preparing packaging directory...
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%INPUT_DIR%"
if errorlevel 1 goto :fail

copy /y "target\%MAIN_JAR%" "%INPUT_DIR%\" >nul
if errorlevel 1 goto :fail
xcopy /y /i "target\dependency\*.jar" "%INPUT_DIR%\" >nul
if errorlevel 1 goto :fail

set "JPACKAGE_EXE=jpackage"
where jpackage >nul 2>nul
if errorlevel 1 (
  if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\jpackage.exe" (
      set "JPACKAGE_EXE=%JAVA_HOME%\bin\jpackage.exe"
    ) else (
      echo [ERROR] jpackage not found. Install JDK 21+ and ensure jpackage is available.
      goto :fail
    )
  ) else (
    echo [ERROR] jpackage not found and JAVA_HOME is not set.
    goto :fail
  )
)

echo [4/5] Creating portable app image...
"%JPACKAGE_EXE%" ^
  --type app-image ^
  --dest "%DIST_DIR%" ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --input "%INPUT_DIR%" ^
  --main-jar "%MAIN_JAR%" ^
  --main-class "%MAIN_CLASS%" ^
  --add-modules "%JDK_MODULES%" ^
  --java-options "%JAVA_OPTS%"
if errorlevel 1 goto :fail

where candle >nul 2>nul
if errorlevel 1 (
  set "CAN_BUILD_INSTALLER=0"
) else (
  where light >nul 2>nul
  if errorlevel 1 (
    set "CAN_BUILD_INSTALLER=0"
  ) else (
    set "CAN_BUILD_INSTALLER=1"
  )
)

if "%CAN_BUILD_INSTALLER%"=="1" (
  echo [5/5] Creating Windows installer ^(.exe^)...
  "%JPACKAGE_EXE%" ^
    --type exe ^
    --dest "%DIST_DIR%" ^
    --name "%APP_NAME%" ^
    --app-version "%APP_VERSION%" ^
    --input "%INPUT_DIR%" ^
    --main-jar "%MAIN_JAR%" ^
    --main-class "%MAIN_CLASS%" ^
    --add-modules "%JDK_MODULES%" ^
    --java-options "%JAVA_OPTS%" ^
    --win-dir-chooser ^
    --win-menu ^
    --win-shortcut
  if errorlevel 1 goto :fail
) else (
  echo [5/5] WiX not found. Skipping installer build.
  echo        Install WiX Toolset and add candle.exe and light.exe to PATH to enable .exe installer output.
)

echo.
echo Package build complete.
echo Portable app: %DIST_DIR%\%APP_NAME%\%APP_NAME%.exe
if "%CAN_BUILD_INSTALLER%"=="1" (
  echo Installer:    %DIST_DIR%\%APP_NAME%-%APP_VERSION%.exe
) else (
  echo Installer:    not generated ^(WiX missing^)
)
echo.
echo Copy the portable app folder (or installer, if generated) to another Windows device.
echo No separate Java installation is required on the target machine.
exit /b 0

:fail
echo.
echo Packaging failed. Check the errors above.
exit /b 1
