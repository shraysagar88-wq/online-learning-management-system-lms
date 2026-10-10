$ErrorActionPreference = 'Stop'

$projectRoot = $PSScriptRoot
$backendPath = Join-Path $projectRoot 'backend'
$frontendPath = Join-Path $projectRoot 'frontend'

function Test-LocalPort {
    param([int]$Port)

    return [bool](Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -First 1)
}

function Start-ProjectTerminal {
    param(
        [string]$WorkingDirectory,
        [string]$Command
    )

    $encodedCommand = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($Command))
    Start-Process -FilePath (Join-Path $PSHOME 'powershell.exe') `
        -ArgumentList @('-NoExit', '-EncodedCommand', $encodedCommand) `
        -WorkingDirectory $WorkingDirectory
}

$postgresService = Get-Service -Name 'postgresql-x64-*' -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $postgresService) {
    throw 'PostgreSQL service was not found. Install PostgreSQL, then run this script again.'
}

if ($postgresService.Status -ne 'Running') {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($identity)
    $adminRole = [Security.Principal.WindowsBuiltInRole]::Administrator
    if (-not $principal.IsInRole($adminRole)) {
        throw 'PostgreSQL is stopped. Run this script from an Administrator PowerShell so it can start the PostgreSQL service.'
    }

    Write-Host "Starting $($postgresService.Name)..."
    Start-Service -Name $postgresService.Name
}

$postgresInfo = Get-CimInstance Win32_Service -Filter "Name='$($postgresService.Name)'"
$pgCtlPath = [regex]::Match($postgresInfo.PathName, '"(?<path>[^"]*pg_ctl\.exe)"', 'IgnoreCase').Groups['path'].Value
if (-not $pgCtlPath) {
    throw 'Could not locate PostgreSQL tools from the registered Windows service.'
}

$psqlPath = Join-Path (Split-Path $pgCtlPath -Parent) 'psql.exe'
if (-not (Test-Path $psqlPath)) {
    throw "PostgreSQL client was not found at $psqlPath."
}

$databaseUser = if ($env:DATABASE_USERNAME) { $env:DATABASE_USERNAME } else { 'postgres' }
$env:PGPASSWORD = if ($env:DATABASE_PASSWORD) { $env:DATABASE_PASSWORD } else { 'postgres' }
$databaseCheck = & $psqlPath --host localhost --username $databaseUser --dbname postgres --tuples-only --no-align --command "SELECT 1 FROM pg_database WHERE datname = 'gu_lms'" 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "Could not connect to PostgreSQL. Check DATABASE_USERNAME and DATABASE_PASSWORD (default: postgres). Details: $($databaseCheck -join ' ')"
}

if ($databaseCheck -notcontains '1') {
    Write-Host 'Creating the gu_lms database...'
    $createResult = & $psqlPath --host localhost --username $databaseUser --dbname postgres --command 'CREATE DATABASE gu_lms' 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Could not create the gu_lms database: $($createResult -join ' ')"
    }
}

$mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
$mavenPath = if ($mavenCommand) { $mavenCommand.Source } else { $null }
if (-not $mavenPath) {
    $mavenPath = Get-ChildItem -Path (Join-Path $HOME '.maven') -Filter mvn.cmd -Recurse -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
}
if (-not $mavenPath) {
    throw 'Maven was not found. Install Maven or add mvn.cmd to PATH, then run this script again.'
}
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw 'Java was not found. Install JDK 25 and add java.exe to PATH.'
}
if (-not (Get-Command npm.cmd -ErrorAction SilentlyContinue)) {
    throw 'npm was not found. Install Node.js, then run this script again.'
}

if (-not (Test-Path (Join-Path $frontendPath 'node_modules'))) {
    Write-Host 'Installing frontend dependencies...'
    Push-Location $frontendPath
    try {
        & npm.cmd install
        if ($LASTEXITCODE -ne 0) {
            throw 'npm install failed.'
        }
    }
    finally {
        Pop-Location
    }
}

if (Test-LocalPort 8080) {
    Write-Host 'Backend port 8080 is already in use; leaving the existing service running.'
}
else {
    $mavenLiteral = $mavenPath.Replace("'", "''")
    Start-ProjectTerminal -WorkingDirectory $backendPath -Command "& '$mavenLiteral' spring-boot:run"
    Write-Host 'Starting backend in a new PowerShell window...'
}

if (Test-LocalPort 4200) {
    Write-Host 'Frontend port 4200 is already in use; leaving the existing service running.'
}
else {
    Start-ProjectTerminal -WorkingDirectory $frontendPath -Command 'npm.cmd start'
    Write-Host 'Starting frontend in a new PowerShell window...'
}

Write-Host ''
Write-Host 'LMS startup requested.'
Write-Host 'Frontend: http://localhost:4200/'
Write-Host 'Backend:  http://localhost:8080/'
Write-Host 'Database: http://localhost:8080/api/health/database'