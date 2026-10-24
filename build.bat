@echo off
setlocal
cd /d "%~dp0"
set "TOOLS=%~dp0tools"
if not exist "%TOOLS%" mkdir "%TOOLS%"

if not exist "%TOOLS%\jdk\bin\java.exe" (
  echo [1/3] Downloading Java 21, please wait...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $ProgressPreference='SilentlyContinue'; Invoke-WebRequest 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse' -OutFile '%TOOLS%\jdk.zip'; Expand-Archive '%TOOLS%\jdk.zip' '%TOOLS%\jdk_tmp' -Force; $d=(Get-ChildItem '%TOOLS%\jdk_tmp' -Directory | Select-Object -First 1).FullName; Move-Item $d '%TOOLS%\jdk'; Remove-Item '%TOOLS%\jdk_tmp' -Recurse -Force; Remove-Item '%TOOLS%\jdk.zip' -Force"
)
if not exist "%TOOLS%\jdk\bin\java.exe" (
  echo Could not download Java. Check your internet and run again.
  pause
  exit /b 1
)

if not exist "%TOOLS%\gradle-8.12\bin\gradle.bat" (
  echo [2/3] Downloading Gradle, please wait...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $ProgressPreference='SilentlyContinue'; Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-8.12-bin.zip' -OutFile '%TOOLS%\gradle.zip'; Expand-Archive '%TOOLS%\gradle.zip' '%TOOLS%' -Force; Remove-Item '%TOOLS%\gradle.zip' -Force"
)
if not exist "%TOOLS%\gradle-8.12\bin\gradle.bat" (
  echo Could not download Gradle. Check your internet and run again.
  pause
  exit /b 1
)

set "JAVA_HOME=%TOOLS%\jdk"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [3/3] Building the mod (first time takes 5-10 minutes)...
call "%TOOLS%\gradle-8.12\bin\gradle.bat" build --no-daemon
if errorlevel 1 (
  echo.
  echo BUILD FAILED. Take a photo or copy the red text above and send it to Claude.
  pause
  exit /b 1
)

copy /Y "build\libs\humanvisual-2.0.0.jar" "%~dp0HumanVisual-mod.jar" >nul
echo.
echo DONE! Your mod is the file HumanVisual-mod.jar in this folder.
echo Put it into your .minecraft\mods folder.
pause
