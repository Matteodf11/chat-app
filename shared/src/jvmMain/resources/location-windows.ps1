$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
$watcher = $null
try {
    Add-Type -AssemblyName System.Device
    $watcher = [System.Device.Location.GeoCoordinateWatcher]::new()
    $ready = $watcher.TryStart($false, [TimeSpan]::FromSeconds(20))
    if ($watcher.Permission -eq [System.Device.Location.GeoPositionPermission]::Denied) {
        [Console]::WriteLine('DENIED')
    } elseif (!$ready -or $watcher.Position.Location.IsUnknown) {
        [Console]::WriteLine('UNAVAILABLE')
    } else {
        $point = $watcher.Position.Location
        $culture = [Globalization.CultureInfo]::InvariantCulture
        [Console]::WriteLine('OK|' + $point.Latitude.ToString('R', $culture) + '|' + $point.Longitude.ToString('R', $culture))
    }
} catch {
    [Console]::WriteLine('UNAVAILABLE')
} finally {
    if ($null -ne $watcher) { $watcher.Stop(); $watcher.Dispose() }
}
