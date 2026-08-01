# Genera el instalador .msi de POS_Colectivo para Windows.
#
# Requisitos en la máquina Windows donde se corre este script:
#   - JDK 17 o superior (jpackage viene incluido desde el JDK 14, no se instala aparte)
#   - Maven
#   - WiX Toolset v3 instalado y en el PATH (lo exige jpackage para generar .msi)
#   - db_engine-windows\ ya descomprimido en la raíz del repo (ver
#     referencias/EMPAQUETADO_WINDOWS.md para el enlace de descarga)
#
# Uso: desde la raíz del repo, en PowerShell:
#   .\packaging\windows\empaquetar.ps1

$ErrorActionPreference = "Stop"

$raiz = Resolve-Path "$PSScriptRoot\..\.."
Set-Location $raiz

$motorWindows = Join-Path $raiz "db_engine-windows"
if (-not (Test-Path $motorWindows)) {
    Write-Error "No se encontro db_engine-windows\. Descarga el motor MariaDB portable para Windows primero (ver referencias\EMPAQUETADO_WINDOWS.md)."
    exit 1
}

Write-Host "1/4 Compilando el .jar con todas las dependencias..."
mvn -q clean package "-DskipTests"
if ($LASTEXITCODE -ne 0) { exit 1 }

$jarNombre = "POS_Colectivo-1.0-SNAPSHOT.jar"
$jarRuta = Join-Path $raiz "target\$jarNombre"
if (-not (Test-Path $jarRuta)) {
    Write-Error "No se genero $jarRuta - revisa el build de Maven."
    exit 1
}

Write-Host "2/4 Aislando el jar ejecutable (jpackage no debe ver el jar original sin dependencias)..."
$carpetaInput = Join-Path $raiz "target\jpackage-input"
if (Test-Path $carpetaInput) { Remove-Item $carpetaInput -Recurse -Force }
New-Item -ItemType Directory -Path $carpetaInput | Out-Null
Copy-Item $jarRuta $carpetaInput

Write-Host "3/4 Preparando el motor de base de datos para incluir en el instalador..."
$carpetaStaging = Join-Path $raiz "target\app-content"
$motorStaging = Join-Path $carpetaStaging "db_engine"
if (Test-Path $carpetaStaging) { Remove-Item $carpetaStaging -Recurse -Force }
New-Item -ItemType Directory -Path $carpetaStaging | Out-Null
Copy-Item $motorWindows $motorStaging -Recurse

Write-Host "4/4 Generando el instalador .msi con jpackage..."
$salida = Join-Path $raiz "target\installer"
if (Test-Path $salida) { Remove-Item $salida -Recurse -Force }
New-Item -ItemType Directory -Path $salida | Out-Null

jpackage `
    --type msi `
    --input "$carpetaInput" `
    --main-jar $jarNombre `
    --main-class com.tuerca.pos.POS_Colectivo `
    --name "POS Colectivo" `
    --app-version "1.0.0" `
    --vendor "Aura Tienda Colectiva" `
    --app-content "$motorStaging" `
    --win-shortcut `
    --win-menu `
    --win-dir-chooser `
    --dest "$salida"

if ($LASTEXITCODE -ne 0) {
    Write-Error "jpackage fallo. Revisa que WiX Toolset este instalado y en el PATH."
    exit 1
}

Write-Host ""
Write-Host "Listo. Instalador generado en $salida"
