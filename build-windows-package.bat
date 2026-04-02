@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT_DIR=%~dp0"
cd /d "%ROOT_DIR%"

set "APP_NAME=PokemonBattle"
set "APP_VERSION=1.0.0"
set "DIST_DIR=dist"
set "INPUT_DIR=%DIST_DIR%\input"
set "HOST_LAUNCHER_FILE=%DIST_DIR%\host-launcher.properties"
set "MAIN_JAR=PokemonBattle-1.0-SNAPSHOT.jar"
set "MAIN_CLASS_CLIENT=com.example.pokemonbattle.Launcher"
set "MAIN_CLASS_HOST=com.example.pokemonbattle.HostLauncher"
set "JDK_MODULES=java.base,java.desktop,java.logging,java.sql,java.prefs,java.xml,java.scripting,jdk.unsupported"
set "JAVA_OPTS=--enable-native-access=ALL-UNNAMED"

echo [1/6] Building application jar...
call mvnw.cmd -DskipTests clean package
if errorlevel 1 goto :fail

echo [2/6] Copying runtime dependencies...
call mvnw.cmd -DskipTests dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target\dependency
if errorlevel 1 goto :fail

echo [3/6] Preparing packaging directory...
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%INPUT_DIR%"
if errorlevel 1 goto :fail

copy /y "target\%MAIN_JAR%" "%INPUT_DIR%\" >nul
if errorlevel 1 goto :fail
xcopy /y /i "target\dependency\*.jar" "%INPUT_DIR%\" >nul
if errorlevel 1 goto :fail

(
  echo main-jar=%MAIN_JAR%
  echo main-class=%MAIN_CLASS_HOST%
  echo java-options=%JAVA_OPTS%
) > "%HOST_LAUNCHER_FILE%"
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

echo [4/6] Creating portable app image...
"%JPACKAGE_EXE%" ^
  --type app-image ^
  --dest "%DIST_DIR%" ^
  --name "%APP_NAME%" ^
  --app-version "%APP_VERSION%" ^
  --input "%INPUT_DIR%" ^
  --main-jar "%MAIN_JAR%" ^
  --main-class "%MAIN_CLASS_CLIENT%" ^
  --add-modules "%JDK_MODULES%" ^
  --java-options "%JAVA_OPTS%" ^
  --add-launcher "PokemonBattleHost=%HOST_LAUNCHER_FILE%"
if errorlevel 1 goto :fail

findstr /c:"java.scripting" "%DIST_DIR%\%APP_NAME%\runtime\release" >nul
if errorlevel 1 (
  echo [ERROR] Packaged runtime is missing java.scripting module.
  goto :fail
)

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
  echo [5/6] Creating Windows installer ^(.exe^)...
  "%JPACKAGE_EXE%" ^
    --type exe ^
    --dest "%DIST_DIR%" ^
    --name "%APP_NAME%" ^
    --app-version "%APP_VERSION%" ^
    --input "%INPUT_DIR%" ^
    --main-jar "%MAIN_JAR%" ^
    --main-class "%MAIN_CLASS_CLIENT%" ^
    --add-modules "%JDK_MODULES%" ^
    --java-options "%JAVA_OPTS%" ^
    --add-launcher "PokemonBattleHost=%HOST_LAUNCHER_FILE%" ^
    --win-dir-chooser ^
    --win-menu ^
    --win-shortcut
  if errorlevel 1 goto :fail
) else (
  echo [5/6] WiX not found. Skipping installer build.
  echo        Install WiX Toolset and add candle.exe and light.exe to PATH to enable .exe installer output.
)

echo [6/6] Finalizing package layout...

echo.
echo Package build complete.
echo Client app:   %DIST_DIR%\%APP_NAME%\%APP_NAME%.exe
echo Host app:     %DIST_DIR%\%APP_NAME%\PokemonBattleHost.exe
if "%CAN_BUILD_INSTALLER%"=="1" (
  echo Installer:    %DIST_DIR%\%APP_NAME%-%APP_VERSION%.exe
) else (
  echo Installer:    not generated ^(WiX missing^)
)
echo.
echo Copy the portable app folder (or installer, if generated) to another Windows device.
echo No separate Java installation is required on the target machine.
echo.
echo Usage:
echo   - %APP_NAME%.exe        ^(client only: connect to existing server^)
echo   - PokemonBattleHost.exe ^(starts server + app together^)
exit /b 0

:fail
echo.
echo Packaging failed. Check the errors above.
exit /b 1
