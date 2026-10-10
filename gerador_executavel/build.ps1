param (
    [string]$Version = ""
)

$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot

$versaoJsonPath = "..\versao.json"
$appVersion = "1.2.3"
if (Test-Path $versaoJsonPath) {
    try {
        $j = Get-Content $versaoJsonPath -Raw | ConvertFrom-Json
        if ($j.versao) { $appVersion = $j.versao }
    } catch {}
}

if ($Version -and $Version.Trim() -ne "") {
    $appVersion = $Version.Trim()
    try {
        $j = Get-Content $versaoJsonPath -Raw | ConvertFrom-Json
        $j.versao = $appVersion
        $j.data = (Get-Date -Format 'dd/MM/yyyy')
        $t = $j | ConvertTo-Json -Depth 5
        [System.IO.File]::WriteAllText((Resolve-Path $versaoJsonPath), $t, (New-Object System.Text.UTF8Encoding($false)))
        Write-Host "[INFO] versao.json atualizado para v$appVersion" -ForegroundColor Green
    } catch {}

    $updateServicePath = "..\src\main\java\com\loja\service\update\UpdateService.java"
    if (Test-Path $updateServicePath) {
        try {
            $t = (Get-Content $updateServicePath -Raw) -replace 'public static final String VERSAO_ATUAL = "[^"]+";', "public static final String VERSAO_ATUAL = `"$appVersion`";"
            [System.IO.File]::WriteAllText((Resolve-Path $updateServicePath), $t, (New-Object System.Text.UTF8Encoding($false)))
            Write-Host "[INFO] UpdateService.java atualizado para v$appVersion" -ForegroundColor Green
        } catch {}
    }
}

Write-Host "====================================================================" -ForegroundColor Cyan
Write-Host " GERANDO EXECUTAVEL E INSTALADOR - VERSAO: v$appVersion" -ForegroundColor Cyan
Write-Host "====================================================================" -ForegroundColor Cyan

$jdk = if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\javac.exe")) { $env:JAVA_HOME } elseif (Test-Path "C:\Java\jdk-21\bin\javac.exe") { "C:\Java\jdk-21" } else { "C:\Program Files\Eclipse Adoptium\jdk-21" }
$javac = "$jdk\bin\javac.exe"
$jar = "$jdk\bin\jar.exe"
$csc = "C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe"
$iscc = if (Test-Path "C:\Program Files\Inno Setup 7\ISCC.exe") { "C:\Program Files\Inno Setup 7\ISCC.exe" } elseif (Test-Path "C:\Program Files (x86)\Inno Setup 6\ISCC.exe") { "C:\Program Files (x86)\Inno Setup 6\ISCC.exe" } else { "C:\Program Files\Inno Setup 6\ISCC.exe" }

Write-Host "[1/5] Compilando codigo Java..." -ForegroundColor Cyan
if (Test-Path "staging") {
    cmd /c "rd /s /q staging"
}
New-Item -ItemType Directory -Path "staging" -Force | Out-Null

cmd /c "dir /s /b ..\src\*.java > sources.txt"
& $javac --release 21 -d staging -cp "..\mysql-connector-j-8.3.0.jar;..\flatlaf-3.5.4.jar;..\openpdf-1.3.40.jar" "@sources.txt"
if (Test-Path "sources.txt") { Remove-Item "sources.txt" -Force }

if (Test-Path "..\src\main\resources") {
    Write-Host "[INFO] Copiando arquivos de recursos..." -ForegroundColor Cyan
    Copy-Item -Path "..\src\main\resources\*" -Destination "staging" -Recurse -Force
}

Write-Host "[2/5] Extraindo bibliotecas para Fat-JAR..." -ForegroundColor Cyan
Set-Location -Path "staging"
tar -xf "..\..\mysql-connector-j-8.3.0.jar"
tar -xf "..\..\flatlaf-3.5.4.jar"
tar -xf "..\..\openpdf-1.3.40.jar"
Remove-Item -Path "META-INF\*.SF", "META-INF\*.DSA", "META-INF\*.RSA" -Force -ErrorAction SilentlyContinue
Set-Location -Path $PSScriptRoot

Write-Host "[3/5] Gerando aplicacao_pronta\SystemPro.jar (v$appVersion)..." -ForegroundColor Cyan
if (-not (Test-Path "aplicacao_pronta")) {
    New-Item -ItemType Directory -Path "aplicacao_pronta" -Force | Out-Null
}
& $jar --create --file "aplicacao_pronta\SystemPro.jar" --main-class com.loja.app.Main -C staging .
Copy-Item "aplicacao_pronta\SystemPro.jar" "aplicacao_pronta\SistemaLoja.jar" -Force
cmd /c "rd /s /q staging"

Write-Host "[4/5] Compilando executavel nativo SystemPro.exe..." -ForegroundColor Cyan
$iconArg = if (Test-Path "icone.ico") { "/win32icon:icone.ico" } else { "" }
if ($iconArg -ne "") {
    & $csc /nologo /target:winexe $iconArg /out:"aplicacao_pronta\SystemPro.exe" Launcher.cs
} else {
    & $csc /nologo /target:winexe /out:"aplicacao_pronta\SystemPro.exe" Launcher.cs
}
Copy-Item "aplicacao_pronta\SystemPro.exe" "aplicacao_pronta\SistemaLoja.exe" -Force
if (Test-Path "icone.ico") { Copy-Item "icone.ico" "aplicacao_pronta\icone.ico" -Force }

if (Test-Path $iscc) {
    Write-Host "[5/5] Compilando Setup.exe com Inno Setup (v$appVersion)..." -ForegroundColor Cyan
    if (-not (Test-Path "instalador")) { New-Item -ItemType Directory -Path "instalador" -Force | Out-Null }
    & $iscc "-dMyAppVersion=$appVersion" "-fSystemPro_Setup_v$appVersion" inno_setup.iss
    Write-Host "`n[SUCESSO] Instalador gerado em: gerador_executavel\instalador\SystemPro_Setup_v$appVersion.exe" -ForegroundColor Green
} else {
    Write-Host "[5/5] Inno Setup nao instalado (opcional). Executavel gerado em aplicacao_pronta\!" -ForegroundColor Yellow
}

Write-Host "`nBUILD FINALIZADO COM SUCESSO!" -ForegroundColor Green
