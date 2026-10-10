@echo off
chcp 65001 > nul
:: Tenta detectar o Java automaticamente (JAVA_HOME, Program Files, extensoes IDE ou PATH)
set "FOUND_JDK="

:: 1. Se JAVA_HOME ja estiver definido e contiver javac.exe
if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        set "FOUND_JDK=%JAVA_HOME%"
        goto :java_detected
    )
)

:: 2. Procura em diretorios padrao de instalacao de JDK no Windows
for /d %%D in (
    "C:\Java\jdk-21*"
    "C:\Java\jdk*"
    "C:\Program Files\Eclipse Adoptium\jdk-21*"
    "C:\Program Files\Java\jdk-21*"
    "C:\Program Files\Java\jdk*"
    "C:\Program Files\BellSoft\LibericaJDK-21*"
    "C:\Program Files\Amazon Corretto\jdk21*"
    "C:\Program Files\Microsoft\jdk-21*"
) do (
    if exist "%%~D\bin\javac.exe" (
        set "FOUND_JDK=%%~D"
        goto :java_detected
    )
)

:: 3. Procura nas extensoes do Antigravity IDE / VS Code
for /f "delims=" %%I in ('dir /b /ad /o-n "%USERPROFILE%\.antigravity-ide\extensions\redhat.java*" 2^>nul') do (
    for /f "delims=" %%J in ('dir /b /ad "%USERPROFILE%\.antigravity-ide\extensions\%%I\jre" 2^>nul') do (
        if exist "%USERPROFILE%\.antigravity-ide\extensions\%%I\jre\%%J\bin\javac.exe" (
            set "FOUND_JDK=%USERPROFILE%\.antigravity-ide\extensions\%%I\jre\%%J"
            goto :java_detected
        )
    )
)

for /f "delims=" %%I in ('dir /b /ad /o-n "%USERPROFILE%\.vscode\extensions\redhat.java*" 2^>nul') do (
    for /f "delims=" %%J in ('dir /b /ad "%USERPROFILE%\.vscode\extensions\%%I\jre" 2^>nul') do (
        if exist "%USERPROFILE%\.vscode\extensions\%%I\jre\%%J\bin\javac.exe" (
            set "FOUND_JDK=%USERPROFILE%\.vscode\extensions\%%I\jre\%%J"
            goto :java_detected
        )
    )
)

:java_detected
if defined FOUND_JDK (
    set "JAVAC_EXE=%FOUND_JDK%\bin\javac.exe"
    set "JAVA_EXE=%FOUND_JDK%\bin\java.exe"
) else (
    set "JAVAC_EXE=javac"
    set "JAVA_EXE=java"
)

:: Verificar se o javac existe e funciona
"%JAVAC_EXE%" -version >nul 2>&1
if %ERRORLEVEL% neq 0 goto :jdk_missing
goto :javac_ok

:jdk_missing
echo.
echo ====================================================================
echo  [ERRO CRITICO] JDK 21 NAO ENCONTRADO NESTE COMPUTADOR!
echo ====================================================================
echo  O compilador 'javac' nao foi localizado neste computador.
echo.
echo  MOTIVO:
echo  Ter apenas o Java comum - JRE nao permite compilar o codigo.
echo  E necessario ter o JDK 21 instalado para executar via run.bat.
echo.
echo  SOLUCAO:
echo  1. Baixe o instalador do JDK 21 gratuitamente:
echo     https://adoptium.net/temurin/releases/?version=21
echo  2. Na instalacao, selecione para configurar 'JAVA_HOME' e 'PATH'.
echo  3. Apos instalar, execute o run.bat novamente.
echo.
echo  OBSERVACAO: Se voce apenas quer RODAR o sistema neste PC sem compilar,
echo  use a pasta 'gerador_executavel\aplicacao_pronta\SistemaLoja.exe'.
echo ====================================================================
echo.
pause
exit /b 1

:javac_ok

:: 4. Se o banco for local (localhost) e nao estiver rodando, inicia o XAMPP automaticamente
set "IS_REMOTE="
if exist "database.properties" (
    findstr /I /C:"db.host" "database.properties" | findstr /I /V /C:"localhost" | findstr /I /V /C:"127.0.0.1" >nul 2>&1
    if not errorlevel 1 set "IS_REMOTE=1"
)
if exist "%APPDATA%\SystemPro\database.properties" (
    findstr /I /C:"db.host" "%APPDATA%\SystemPro\database.properties" | findstr /I /V /C:"localhost" | findstr /I /V /C:"127.0.0.1" >nul 2>&1
    if not errorlevel 1 set "IS_REMOTE=1"
)

if not defined IS_REMOTE (
    netstat -ano | findstr /R /C:":3306 " >nul 2>&1
    if errorlevel 1 (
        if exist "C:\xampp\mysql\bin\mysqld.exe" (
            echo [SISTEMA] Iniciando MySQL local XAMPP...
            start "" /b "C:\xampp\mysql\bin\mysqld.exe" --defaults-file="C:\xampp\mysql\bin\my.ini" --standalone
            timeout /t 2 >nul
        ) else (
            if exist "G:\xampp\mysql\bin\mysqld.exe" (
                echo [SISTEMA] Iniciando MySQL local via G:\xampp...
                start "" /b "G:\xampp\mysql\bin\mysqld.exe" --defaults-file="G:\xampp\mysql\bin\my.ini" --standalone
                timeout /t 2 >nul
            )
        )
    )
)

set "MYSQL_JAR=mysql-connector-j-8.3.0.jar"
set "FLATLAF_JAR=flatlaf-3.5.4.jar"
set "OPENPDF_JAR=openpdf-1.3.40.jar"
set "CLASSPATH=target\classes;%MYSQL_JAR%;%FLATLAF_JAR%;%OPENPDF_JAR%"

if not exist "%MYSQL_JAR%" (
    echo [SISTEMA] Baixando driver do MySQL...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar' -OutFile 'mysql-connector-j-8.3.0.jar'"
)

if not exist "%FLATLAF_JAR%" (
    echo [SISTEMA] Baixando biblioteca FlatLaf...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/formdev/flatlaf/3.5.4/flatlaf-3.5.4.jar' -OutFile 'flatlaf-3.5.4.jar'"
)

if not exist "%OPENPDF_JAR%" (
    echo [SISTEMA] Baixando biblioteca OpenPDF...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/github/librepdf/openpdf/1.3.40/openpdf-1.3.40.jar' -OutFile 'openpdf-1.3.40.jar'"
)

echo [SISTEMA] Compilando arquivos Java...
if not exist "target\classes" mkdir "target\classes"

dir /s /b src\*.java > sources.txt
"%JAVAC_EXE%" --release 21 -d target\classes -cp "%MYSQL_JAR%;%FLATLAF_JAR%;%OPENPDF_JAR%" @sources.txt
set "COMP_STATUS=%ERRORLEVEL%"
if exist sources.txt del sources.txt

if %COMP_STATUS% neq 0 goto :erro_compilacao

if exist "src\main\resources" (
    xcopy /E /I /Y "src\main\resources\*" "target\classes\" > nul
)

echo [SISTEMA] Iniciando a aplicacao...
echo ====================================================
"%JAVA_EXE%" -cp "%CLASSPATH%" com.loja.app.Main
echo ====================================================
pause
exit /b 0

:erro_compilacao
echo.
echo [ERRO] Falha ao compilar o projeto! Codigo de erro: %COMP_STATUS%
pause
exit /b %COMP_STATUS%


