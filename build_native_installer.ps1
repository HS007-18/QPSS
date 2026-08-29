Write-Host "1. Building Spring Boot Application..."
.\mvnw clean package -DskipTests

Write-Host "2. Preparing Electron Resources..."
if (Test-Path "electron-app\qpss.war") { Remove-Item "electron-app\qpss.war" -Force }
Copy-Item "target2\qpss-1.0.0.war" -Destination "electron-app\qpss.war"

Write-Host "3. Building Custom Java Runtime..."
if (Test-Path "electron-app\jre") { Remove-Item -Path "electron-app\jre" -Recurse -Force }
# Using jlink to create a standalone JRE
# java.se includes the vast majority of standard Java classes. jdk.unsupported includes Unsafe (used by some libraries).
jlink --add-modules java.se,jdk.unsupported,java.sql,java.naming,java.desktop,java.management,java.security.jgss,java.instrument --output electron-app\jre --compress=2 --no-header-files --no-man-pages

Write-Host "4. Building Native Windows Installer (Electron)..."
Push-Location electron-app
Write-Host "Installing NPM dependencies..."
npm install
Write-Host "Running electron-builder..."
npm run build
Pop-Location

Write-Host "Done! The installer is located at: electron-app\dist\KIT QGen Setup 1.0.0.exe"
