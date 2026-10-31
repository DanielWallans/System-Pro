@echo off
chcp 65001 > nul
title System Pro - Inicializador

:: 1. Detectar instalacao do Java JDK
set "FOUND_JDK="

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\javac.exe" (
        set "FOUND_JDK=%JAVA_HOME%"
        goto :java_detected
    )
)

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

"%JAVAC_EXE%" -version >nul 2>&1
if %ERRORLEVEL% neq 0 goto :jdk_missing
goto :javac_ok

:jdk_missing
echo.
echo ====================================================================
echo  JDK 21 nao foi localizado neste computador.
echo ====================================================================
echo  Para compilar e executar o sistema, instale o Java JDK 21:
echo  https://adoptium.net/temurin/releases/?version=21
echo ====================================================================
echo.
pause
exit /b 1

:javac_ok

:: 2. Inicializar o MySQL local se estiver configurado para localhost e nao estiver ativo
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
            echo [SISTEMA] Iniciando MySQL do XAMPP...
            start "" /b "C:\xampp\mysql\bin\mysqld.exe" --defaults-file="C:\xampp\mysql\bin\my.ini" --standalone
            timeout /t 2 >nul
        ) else if exist "G:\xampp\mysql\bin\mysqld.exe" (
            echo [SISTEMA] Iniciando MySQL via G:\xampp...
            start "" /b "G:\xampp\mysql\bin\mysqld.exe" --defaults-file="G:\xampp\mysql\bin\my.ini" --standalone
            timeout /t 2 >nul
        )
    )
)

:: 3. Verificar bibliotecas na pasta lib
if not exist "lib" mkdir "lib"

set "MYSQL_JAR=lib\mysql-connector-j-8.3.0.jar"
set "FLATLAF_JAR=lib\flatlaf-3.5.4.jar"
set "OPENPDF_JAR=lib\openpdf-1.3.40.jar"
set "CLASSPATH=target\classes;%MYSQL_JAR%;%FLATLAF_JAR%;%OPENPDF_JAR%"

if not exist "%MYSQL_JAR%" (
    echo [SISTEMA] Baixando driver do MySQL...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.3.0/mysql-connector-j-8.3.0.jar' -OutFile '%MYSQL_JAR%'"
)

if not exist "%FLATLAF_JAR%" (
    echo [SISTEMA] Baixando biblioteca de interface FlatLaf...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/formdev/flatlaf/3.5.4/flatlaf-3.5.4.jar' -OutFile '%FLATLAF_JAR%'"
)

if not exist "%OPENPDF_JAR%" (
    echo [SISTEMA] Baixando biblioteca OpenPDF...
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/github/librepdf/openpdf/1.3.40/openpdf-1.3.40.jar' -OutFile '%OPENPDF_JAR%'"
)

:: 4. Compilacao do codigo fonte
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

:: 5. Execucao da aplicacao
echo [SISTEMA] Abrindo o sistema...
"%JAVA_EXE%" -cp "%CLASSPATH%" com.loja.app.Main
exit /b 0

:erro_compilacao
echo.
echo [ERRO] Ocorreu uma falha na compilacao (codigo: %COMP_STATUS%).
pause
exit /b %COMP_STATUS%
