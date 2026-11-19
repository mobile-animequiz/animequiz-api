param([switch]$Test)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

$jdkCandidates = @($env:JAVA_HOME, (Join-Path $env:ProgramFiles 'Android/Android Studio/jbr'))
$jetbrainsDirectory = Join-Path $env:ProgramFiles 'JetBrains'
if (Test-Path -LiteralPath $jetbrainsDirectory) {
    $jdkCandidates += @(Get-ChildItem -LiteralPath $jetbrainsDirectory -Directory | ForEach-Object {
        Join-Path $_.FullName 'jbr'
    })
}
$validJdk = $jdkCandidates | Where-Object {
    $_ -and (Test-Path -LiteralPath (Join-Path $_ 'bin/java.exe')) -and
    (Test-Path -LiteralPath (Join-Path $_ 'lib/tzdb.dat'))
} | Select-Object -First 1
if (-not $validJdk) {
    throw 'Configure JAVA_HOME para um JDK completo, versão 17 ou superior.'
}
$env:JAVA_HOME = $validJdk
$env:PATH = (Join-Path $env:JAVA_HOME 'bin') + [IO.Path]::PathSeparator + $env:PATH

if ($Test) {
    & (Join-Path $PSScriptRoot 'mvnw.cmd') clean test
} else {
    if (-not $env:JWT_SECRET) {
        $jwtBytes = New-Object byte[] 48
        $jwtGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $jwtGenerator.GetBytes($jwtBytes) } finally { $jwtGenerator.Dispose() }
        $env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
    }
    & (Join-Path $PSScriptRoot 'mvnw.cmd') spring-boot:run
}
exit $LASTEXITCODE
