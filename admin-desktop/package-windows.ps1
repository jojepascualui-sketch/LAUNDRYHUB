<#
.SYNOPSIS
  Packages the Laundry HUB admin console as a Windows installer with jpackage (JDK 17).

.DESCRIPTION
  1. Builds the console jar and copies its runtime dependencies (JavaFX for Windows, Jackson).
  2. jpackage links a small Java runtime with only the needed modules and wraps it as an installer.
  The backend address is baked in as -Dlaundryhub.apiUrl (LAUNDRYHUB_API_URL still overrides it per PC).
  No secrets are packaged: the A2 map page and its key are served by the backend.

  .exe / .msi need the WiX Toolset 3.x on PATH (candle.exe, light.exe). Without WiX use -Type app-image,
  which makes a portable folder with "Laundry HUB Admin.exe" that you can zip.

.EXAMPLE
  .\package-windows.ps1 -ApiUrl https://laundryhub-api.onrender.com/api/
.EXAMPLE
  .\package-windows.ps1 -ApiUrl https://laundryhub-api.onrender.com/api/ -Type app-image
#>
param(
    [Parameter(Mandatory = $true)][string]$ApiUrl,
    [ValidateSet('exe', 'msi', 'app-image')][string]$Type = 'exe',
    [string]$Version = '0.1.0'
)
$ErrorActionPreference = 'Stop'

$AppName = 'Laundry HUB Admin'
$MainModule = 'com.laundryhub.admin/com.laundryhub.admin.AdminApp'
# Fixed per product so a newer installer upgrades the old one instead of installing twice.
$UpgradeUuid = '6f1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d'
$JavaFxVersion = '21.0.12'

if (-not $ApiUrl.EndsWith('/')) { $ApiUrl = "$ApiUrl/" }
if (-not ($ApiUrl.StartsWith('https://') -or $ApiUrl.StartsWith('http://localhost'))) {
    throw "Use the HTTPS address of the deployed API, e.g. https://laundryhub-api.onrender.com/api/"
}

# JDK 17 with jpackage (JAVA_HOME first, else the Adoptium install from CLAUDE.md §6).
$Jdk = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot' }
$JPackage = Join-Path $Jdk 'bin\jpackage.exe'
if (-not (Test-Path $JPackage)) { throw "jpackage not found in $Jdk. Set JAVA_HOME to a JDK 17." }

if ($Type -ne 'app-image' -and -not (Get-Command candle.exe -ErrorAction SilentlyContinue)) {
    throw "WiX Toolset 3 is needed for -Type $Type (winget install WiXToolset.WiXToolset, then reopen the terminal), or use -Type app-image."
}

# Build output lives outside the repo (non-ASCII path, CLAUDE.md §6).
$Build = Join-Path $env:USERPROFILE '.laundryhub-build\laundryhub-admin'
$Lib = Join-Path $Build 'jpackage\lib'
$Dest = Join-Path $Build 'installer'
Remove-Item $Lib, $Dest -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $Lib, $Dest | Out-Null

Push-Location $PSScriptRoot
try {
    & .\mvnw.cmd -q -DskipTests package dependency:copy-dependencies -DincludeScope=runtime "-DoutputDirectory=$Lib"
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed' }
} finally {
    Pop-Location
}

Copy-Item (Join-Path $Build "laundryhub-admin-$Version.jar") $Lib
# The JavaFX jars without a platform classifier are empty placeholders with the same module names as the
# -win jars; keep only the Windows ones.
Get-ChildItem $Lib -Filter "javafx-*-$JavaFxVersion.jar" | Remove-Item

$Args = @(
    '--type', $Type,
    '--name', $AppName,
    '--app-version', $Version,
    '--vendor', 'Laundry HUB',
    '--description', 'Laundry HUB admin console',
    '--dest', $Dest,
    '--module-path', $Lib,
    '--module', $MainModule,
    '--java-options', "-Dlaundryhub.apiUrl=$ApiUrl",
    '--java-options', '-Dfile.encoding=UTF-8',
    '--jlink-options', '--strip-debug --no-man-pages --no-header-files --compress=2'
)
if ($Type -ne 'app-image') {
    $Args += @('--win-menu', '--win-menu-group', 'Laundry HUB', '--win-shortcut', '--win-dir-chooser',
               '--win-upgrade-uuid', $UpgradeUuid)
}
& $JPackage @Args
if ($LASTEXITCODE -ne 0) { throw 'jpackage failed' }

Write-Host "Done: $Dest"
Get-ChildItem $Dest | Select-Object Name, @{ n = 'MB'; e = { [math]::Round(((Get-ChildItem $_.FullName -Recurse -File | Measure-Object Length -Sum).Sum) / 1MB, 1) } }
