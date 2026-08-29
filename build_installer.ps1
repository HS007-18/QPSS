Write-Host "Building application..."
.\mvnw clean package -DskipTests

Write-Host "Preparing jpackage input..."
if (Test-Path "jpackage_input") { Remove-Item -Path "jpackage_input" -Recurse -Force }
New-Item -ItemType Directory -Path "jpackage_input"
Copy-Item "target2\qpss-1.0.0.war" -Destination "jpackage_input\"

Write-Host "Running jpackage..."
if (Test-Path "KIT_QGen_DesktopApp") { Remove-Item -Path "KIT_QGen_DesktopApp" -Recurse -Force }
jpackage --type app-image --name "KIT QGen" --input jpackage_input --main-jar qpss-1.0.0.war --icon icon.ico --dest KIT_QGen_DesktopApp

Write-Host "Zipping the desktop app..."
if (Test-Path "KIT_QGen_App.zip") { Remove-Item -Path "KIT_QGen_App.zip" -Force }
Compress-Archive -Path "KIT_QGen_DesktopApp\KIT QGen" -DestinationPath "KIT_QGen_App.zip" -Force
Write-Host "Desktop application package created at KIT_QGen_App.zip!"
