& {
    $ErrorActionPreference = 'Stop'
    [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12

    $repo = 'mythicisland/queue'
    $version = if ($env:QUEUE_VERSION) { $env:QUEUE_VERSION } else { 'latest' }
    $installDir = if ($env:QUEUE_INSTALL_DIR) { $env:QUEUE_INSTALL_DIR } else { Join-Path $env:LOCALAPPDATA 'Programs\queue' }

    $cpu = if ($env:PROCESSOR_ARCHITEW6432) { $env:PROCESSOR_ARCHITEW6432 } else { $env:PROCESSOR_ARCHITECTURE }
    $arch = switch ($cpu) {
        'AMD64' { 'amd64' }
        'ARM64' { 'arm64' }
        default { throw "Unsupported architecture: $cpu" }
    }

    $base = if ($version -eq 'latest') {
        "https://github.com/$repo/releases/latest/download"
    } else {
        "https://github.com/$repo/releases/download/$version"
    }

    $archive = "queue-cli_windows_$arch.zip"
    $tmp = Join-Path ([IO.Path]::GetTempPath()) ([Guid]::NewGuid())
    New-Item -ItemType Directory -Path $tmp | Out-Null

    try {
        Write-Host "Downloading $archive ($version)"
        Invoke-WebRequest -UseBasicParsing -Uri "$base/$archive" -OutFile (Join-Path $tmp $archive)
        Invoke-WebRequest -UseBasicParsing -Uri "$base/queue-cli_checksums.txt" -OutFile (Join-Path $tmp 'checksums.txt')

        $line = Get-Content (Join-Path $tmp 'checksums.txt') | Where-Object { $_ -match "\s$([regex]::Escape($archive))$" }
        $expected = if ($line) { ($line -split '\s+')[0] } else { '' }
        $actual = (Get-FileHash -Algorithm SHA256 (Join-Path $tmp $archive)).Hash
        if (-not $expected -or $expected -ne $actual) {
            throw "Checksum of $archive does not match"
        }

        Expand-Archive -Path (Join-Path $tmp $archive) -DestinationPath $tmp -Force
        New-Item -ItemType Directory -Path $installDir -Force | Out-Null
        Copy-Item (Join-Path $tmp 'queue.exe') (Join-Path $installDir 'queue.exe') -Force
    } finally {
        Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
    }

    Write-Host "Installed queue to $installDir\queue.exe"

    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    $entries = if ($userPath) { $userPath -split ';' } else { @() }
    if ($entries -notcontains $installDir) {
        [Environment]::SetEnvironmentVariable('Path', (($entries + $installDir) -join ';'), 'User')
        $env:Path = "$env:Path;$installDir"
        Write-Host "Added $installDir to your PATH, open a new terminal to use queue everywhere"
    }
}
