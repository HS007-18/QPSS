Write-Host "Building Spring Boot Application..."
.\mvnw clean package -DskipTests

Write-Host "Building Electron Desktop Installer..."
Push-Location electron
npm install
npm run build
Pop-Location

Write-Host ""
Write-Host "Done! Installer created at: electron\dist\KIT QGen Setup 1.0.1.exe"
Write-Host "Double-click to install the desktop application."