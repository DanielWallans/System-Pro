; =====================================================================
; SCRIPT DO INNO SETUP - SYSTEM PRO
; =====================================================================
; Este script gera o arquivo "Instalador_SystemPro_Setup_v1.0.exe"
; que o seu cliente usará para instalar o sistema com 2 cliques.
; =====================================================================

#ifndef MyAppVersion
  #define MyAppVersion "1.2.3"
#endif
#define MyAppName "System Pro"
#define MyAppPublisher "System Pro"
#define MyAppExeName "SystemPro.exe"

[Setup]
AppId={{D1A39F74-B368-4F7D-89C4-32B8DF129F01}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={autopf}\SystemPro
DisableProgramGroupPage=yes
OutputDir=instalador
OutputBaseFilename=SystemPro_Setup_v{#MyAppVersion}
Compression=lzma
SolidCompression=yes
WizardStyle=modern
SetupIconFile=icone.ico

[Languages]
Name: "brazilianportuguese"; MessagesFile: "compiler:Languages\BrazilianPortuguese.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Dirs]
Name: "{app}"; Permissions: users-modify

[Files]
; Copia o executável nativo e o JAR com todas as dependências embutidas
Source: "aplicacao_pronta\SystemPro.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "aplicacao_pronta\SystemPro.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "icone.ico"; DestDir: "{app}"; Flags: ignoreversion
; Inclui a JRE 21 embutida para rodar 100% offline em qualquer computador sem precisar instalar Java!
Source: "aplicacao_pronta\jre\*"; DestDir: "{app}\jre"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; IconFilename: "{app}\icone.ico"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon; IconFilename: "{app}\icone.ico"

[Run]
Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Filename: "{app}\{#MyAppExeName}"; Flags: nowait postinstall skipifsilent
