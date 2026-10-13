$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$desktopConfig = [IO.File]::ReadAllText((Join-Path $projectRoot 'desktopApp/build.gradle.kts'))
$version = [regex]::Match($desktopConfig, 'packageVersion\s*=\s*"(\d+\.\d+\.\d+)"').Groups[1].Value
if (!$version) { throw 'No se pudo determinar la versión del cliente.' }
$apk = Join-Path $projectRoot 'androidApp/build/outputs/apk/debug/androidApp-debug.apk'
$msi = Join-Path $projectRoot "desktopApp/build/compose/binaries/main/msi/Chimera-$version.msi"
foreach ($file in @($apk, $msi)) {
    if (!(Test-Path -LiteralPath $file -PathType Leaf)) { throw 'Falta un paquete. Compila primero APK y MSI.' }
}

$sdkCandidates = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, (Join-Path $env:USERPROFILE 'AppData/Local/Android/Sdk'))
$aapt = $null
foreach ($sdk in $sdkCandidates) {
    if ($sdk -and (Test-Path -LiteralPath (Join-Path $sdk 'build-tools'))) {
        $aapt = Get-ChildItem -LiteralPath (Join-Path $sdk 'build-tools') -Filter aapt.exe -Recurse |
            Select-Object -First 1 -ExpandProperty FullName
        if ($aapt) { break }
    }
}
if (!$aapt) { throw 'No se encontró aapt.exe. Configura ANDROID_HOME con el SDK utilizado para compilar.' }
$apkInfo = & $aapt dump badging $apk
if ($LASTEXITCODE -ne 0) { throw 'No se pudo comprobar la versión del APK.' }
$apkHeader = $apkInfo | Where-Object { $_ -match '^package:' } | Select-Object -First 1
if ($apkHeader -notmatch "versionName='$([regex]::Escape($version))'") { throw 'La versión real del APK no coincide con la del MSI. Recompila ambos.' }

$installer = $null; $database = $null; $view = $null; $record = $null
try {
    $installer = New-Object -ComObject WindowsInstaller.Installer
    $database = $installer.OpenDatabase($msi, 0)
    $view = $database.OpenView('SELECT `Value` FROM `Property` WHERE `Property` = ''ProductVersion''')
    $view.Execute(); $record = $view.Fetch()
    if ($record.StringData(1) -ne $version) { throw 'La versión interna del MSI no coincide con el candidato.' }
} finally {
    if ($view) { $view.Close() }
    foreach ($instance in @($record, $view, $database, $installer)) {
        if ($null -ne $instance) { [Runtime.InteropServices.Marshal]::FinalReleaseComObject($instance) | Out-Null }
    }
}

$guide = Join-Path $projectRoot 'docs/demo-guide.md'
if (![IO.File]::ReadAllText($guide).Contains("Chimera-$version.msi")) { throw 'Actualiza la guía de demo para esta versión antes de empaquetar.' }
$releaseDirectory = Join-Path $projectRoot "build/release/$version"
[IO.Directory]::CreateDirectory($releaseDirectory) | Out-Null
$copies = @(
    @{ Source = $apk; Name = "Chimera-$version-debug.apk" },
    @{ Source = $msi; Name = "Chimera-$version.msi" },
    @{ Source = $guide; Name = 'README-DEMO.md' },
    @{ Source = (Join-Path $projectRoot 'docs/release-checklist.md'); Name = 'release-checklist.md' }
)
$packageFiles = @()
foreach ($copy in $copies) {
    $destination = Join-Path $releaseDirectory $copy.Name
    Copy-Item -LiteralPath $copy.Source -Destination $destination -Force
    $packageFiles += $destination
}
$checksums = foreach ($file in $packageFiles) {
    '{0} *{1}' -f (Get-FileHash -LiteralPath $file -Algorithm SHA256).Hash.ToLowerInvariant(), [IO.Path]::GetFileName($file)
}
$checksumFile = Join-Path $releaseDirectory 'SHA256SUMS.txt'
[IO.File]::WriteAllLines($checksumFile, $checksums, [Text.Encoding]::ASCII)
$packageFiles += $checksumFile
$archive = Join-Path $projectRoot "build/release/Chimera-$version-demo.zip"
# Explicit file list: never package the repository, .env, caches or account data.
Compress-Archive -LiteralPath $packageFiles -DestinationPath $archive -Force
$archiveChecksum = '{0} *{1}' -f (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash.ToLowerInvariant(), [IO.Path]::GetFileName($archive)
[IO.File]::WriteAllText("$archive.sha256", $archiveChecksum + [Environment]::NewLine, [Text.Encoding]::ASCII)
Write-Output "Paquete de demostración creado: $archive"
