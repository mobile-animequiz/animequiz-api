param([switch]$Test)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
    throw 'Configure JAVA_HOME para um JDK completo, versão 17 ou superior.'
}
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
