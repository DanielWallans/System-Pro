package com.loja.service.update;

import com.loja.repository.ConnectionFactory;
import com.loja.util.NetworkUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateService {

    public static final String VERSAO_ATUAL = "1.2.7";
    public static final String DEFAULT_UPDATE_URL = "https://raw.githubusercontent.com/DanielWallans/System-Pro/master/versao.json";

    public static String getUpdateUrl() {
        File configFile = ConnectionFactory.getArquivoConfig();
        if (configFile != null && configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(fis);
                String customUrl = props.getProperty("update.url");
                if (customUrl != null && !customUrl.trim().isEmpty()) {
                    return customUrl.trim();
                }
            } catch (Exception ignored) {
            }
        }
        return DEFAULT_UPDATE_URL;
    }

    /**
     * Verifica atualizações disponíveis para este terminal.
     * Prioridade 1: Banco de Dados da Loja (LAN, sem dependência de internet ou GitHub público).
     * Prioridade 2: Nuvem / GitHub (caso configurado ou para máquinas remotas).
     */
    public static UpdateInfo verificarAtualizacao() {
        // 1. Checagem prioritária no Banco de Dados MySQL local
        try {
            UpdateInfo infoBanco = verificarAtualizacaoNoBanco();
            if (infoBanco != null) {
                return infoBanco;
            }
        } catch (Exception ignored) {
        }

        // 2. Checagem em Nuvem / GitHub
        try {
            String urlStr = getUpdateUrl();
            UpdateInfo info = checarUrl(urlStr);
            if (info != null)
                return info;

            // Fallback caso as branches alternem entre master e main
            if (urlStr.contains("/master/")) {
                String fallbackUrl = urlStr.replace("/master/", "/main/");
                return checarUrl(fallbackUrl);
            } else if (urlStr.contains("/main/")) {
                String fallbackUrl = urlStr.replace("/main/", "/master/");
                return checarUrl(fallbackUrl);
            }
        } catch (Exception e) {
            System.err.println("[INFO] Checagem de atualização em nuvem ignorada ou sem internet: " + e.getMessage());
        }
        return null;
    }

    /**
     * Consulta a tabela 'sistema_atualizacao' no MySQL.
     */
    public static UpdateInfo verificarAtualizacaoNoBanco() {
        if (!ConnectionFactory.isConfigurado()) {
            return null;
        }

        String sql = "SELECT versao, data_lancamento, novidades, tamanho_bytes FROM sistema_atualizacao " +
                "WHERE ativo = 1 ORDER BY id DESC LIMIT 1";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                String versaoRemota = rs.getString("versao");
                String data = rs.getString("data_lancamento");
                String novidades = rs.getString("novidades");

                if (versaoRemota != null && isNovaVersao(versaoRemota, VERSAO_ATUAL)) {
                    if (novidades != null) {
                        novidades = novidades.replace("\\n", "\n");
                    }
                    return new UpdateInfo(versaoRemota, data, novidades, "db://" + versaoRemota);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static UpdateInfo checarUrl(String urlStr) {
        try {
            String urlComCache = urlStr.contains("?")
                    ? (urlStr + "&_t=" + System.currentTimeMillis())
                    : (urlStr + "?_t=" + System.currentTimeMillis());
            URL url = URI.create(urlComCache).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3500);
            conn.setReadTimeout(3500);
            conn.setRequestProperty("User-Agent", "SystemProUpdater/1.1");
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            conn.setRequestProperty("Pragma", "no-cache");

            if (conn.getResponseCode() != 200) {
                return null;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            String json = sb.toString();
            String versaoRemota = extrairCampoJson(json, "versao");
            String data = extrairCampoJson(json, "data");
            String novidades = extrairCampoJson(json, "novidades");
            String downloadUrl = extrairCampoJson(json, "download_url");

            if (versaoRemota != null && downloadUrl != null) {
                if (isNovaVersao(versaoRemota, VERSAO_ATUAL)) {
                    if (novidades != null) {
                        novidades = novidades.replace("\\n", "\n");
                    }
                    return new UpdateInfo(versaoRemota, data, novidades, downloadUrl);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static boolean isNovaVersao(String remota, String local) {
        if (remota == null || local == null)
            return false;
        String rClean = remota.replaceAll("[^0-9.]", "").trim();
        String lClean = local.replaceAll("[^0-9.]", "").trim();
        if (rClean.isEmpty() || lClean.isEmpty() || rClean.equalsIgnoreCase(lClean)) {
            return false;
        }

        try {
            String[] vRemota = rClean.split("\\.");
            String[] vLocal = lClean.split("\\.");

            int maxLen = Math.max(vRemota.length, vLocal.length);
            for (int i = 0; i < maxLen; i++) {
                int r = i < vRemota.length ? Integer.parseInt(vRemota[i]) : 0;
                int l = i < vLocal.length ? Integer.parseInt(vLocal[i]) : 0;
                if (r > l)
                    return true;
                if (r < l)
                    return false;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private static String extrairCampoJson(String json, String campo) {
        Pattern pattern = Pattern.compile("\"" + campo + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static void baixarEAplicarAtualizacao(String downloadUrl, Consumer<Integer> progressoCallback)
            throws Exception {
        File baseDir = getAppDirectory();
        File novoJar = new File(baseDir, "SistemaLoja.jar.update");

        if (downloadUrl.startsWith("db://")) {
            String versao = downloadUrl.substring(5);
            baixarJarDoBanco(versao, novoJar, progressoCallback);
        } else {
            baixarJarHttp(downloadUrl, novoJar, progressoCallback);
        }

        long pid = ProcessHandle.current().pid();

        // 1. Script PowerShell em segundo plano (invisível, sem janela preta do prompt)
        File psScript = new File(baseDir, "atualizar_sistema.ps1");
        String psContent = "$ErrorActionPreference = 'SilentlyContinue'\r\n" +
                "Start-Sleep -Milliseconds 800\r\n" +
                "try {\r\n" +
                "    $p = Get-Process -Id " + pid + " -ErrorAction SilentlyContinue\r\n" +
                "    if ($p) { $p.WaitForExit(6000) }\r\n" +
                "} catch {}\r\n" +
                "Set-Location -LiteralPath '" + baseDir.getAbsolutePath().replace("'", "''") + "'\r\n" +
                "$source = 'SistemaLoja.jar.update'\r\n" +
                "if (Test-Path $source) {\r\n" +
                "    for ($i = 0; $i -lt 25; $i++) {\r\n" +
                "        try {\r\n" +
                "            Copy-Item $source 'SystemPro.jar' -Force -ErrorAction Stop\r\n" +
                "            Copy-Item $source 'SistemaLoja.jar' -Force -ErrorAction Stop\r\n" +
                "            Remove-Item $source -Force -ErrorAction Stop\r\n" +
                "            break\r\n" +
                "        } catch {\r\n" +
                "            Start-Sleep -Milliseconds 400\r\n" +
                "        }\r\n" +
                "    }\r\n" +
                "}\r\n" +
                "if (Test-Path 'SystemPro.exe') {\r\n" +
                "    Start-Process 'SystemPro.exe'\r\n" +
                "} elseif (Test-Path 'SistemaLoja.exe') {\r\n" +
                "    Start-Process 'SistemaLoja.exe'\r\n" +
                "} elseif (Test-Path 'SystemPro.jar') {\r\n" +
                "    Start-Process 'javaw.exe' -ArgumentList '-jar', 'SystemPro.jar'\r\n" +
                "} elseif (Test-Path 'SistemaLoja.jar') {\r\n" +
                "    Start-Process 'javaw.exe' -ArgumentList '-jar', 'SistemaLoja.jar'\r\n" +
                "}\r\n" +
                "Start-Sleep -Seconds 1\r\n" +
                "Remove-Item -LiteralPath $MyInvocation.MyCommand.Path -Force -ErrorAction SilentlyContinue\r\n";

        try (FileWriter fw = new FileWriter(psScript, StandardCharsets.UTF_8)) {
            fw.write(psContent);
        }

        // 2. Script BAT de contingência 100% seguro (sem blocos com parênteses)
        File batScript = new File(baseDir, "atualizar_sistema.bat");
        String batContent = "@echo off\r\n" +
                "chcp 65001 > nul\r\n" +
                "cd /d \"%~dp0\"\r\n" +
                "ping 127.0.0.1 -n 3 > nul\r\n" +
                "set /a tries=0\r\n" +
                ":loop_copy\r\n" +
                "set /a tries+=1\r\n" +
                "if exist \"SistemaLoja.jar.update\" (\r\n" +
                "    copy /y \"SistemaLoja.jar.update\" \"SystemPro.jar\" > nul 2>&1\r\n" +
                "    copy /y \"SistemaLoja.jar.update\" \"SistemaLoja.jar\" > nul 2>&1\r\n" +
                "    del /f /q \"SistemaLoja.jar.update\" > nul 2>&1\r\n" +
                ")\r\n" +
                "if not exist \"SistemaLoja.jar.update\" goto :start_app\r\n" +
                "if %tries% lss 20 (\r\n" +
                "    ping 127.0.0.1 -n 2 > nul\r\n" +
                "    goto :loop_copy\r\n" +
                ")\r\n" +
                ":start_app\r\n" +
                "if exist \"SystemPro.exe\" (\r\n" +
                "    start \"\" \"SystemPro.exe\"\r\n" +
                "} else if exist \"SistemaLoja.exe\" (\r\n" +
                "    start \"\" \"SistemaLoja.exe\"\r\n" +
                "} else (\r\n" +
                "    start \"\" javaw -jar \"SystemPro.jar\"\r\n" +
                ")\r\n" +
                "ping 127.0.0.1 -n 2 > nul\r\n" +
                "del /f /q \".teste_perm\" >nul 2>&1\r\n" +
                "(goto) 2>nul & del \"%~f0\"\r\n";

        try (FileWriter fw = new FileWriter(batScript, StandardCharsets.UTF_8)) {
            fw.write(batContent);
        }

        // 3. Executar o PowerShell de forma 100% invisível (sem janela preta)
        try {
            new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-WindowStyle", "Hidden",
                    "-File", psScript.getAbsolutePath()).directory(baseDir).start();
        } catch (Exception e) {
            new ProcessBuilder("cmd.exe", "/c", "start", "/min", "\"System Pro Updater\"", batScript.getAbsolutePath())
                    .directory(baseDir)
                    .start();
        }

        System.exit(0);
    }

    private static void baixarJarDoBanco(String versao, File destino, Consumer<Integer> progressoCallback) throws Exception {
        String sql = "SELECT tamanho_bytes, arquivo_jar FROM sistema_atualizacao WHERE versao = ? AND ativo = 1";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, versao);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IOException("Atualização v" + versao + " não encontrada no banco de dados.");
                }

                long totalBytes = rs.getLong("tamanho_bytes");
                try (InputStream in = rs.getBinaryStream("arquivo_jar");
                     BufferedInputStream bis = new BufferedInputStream(in);
                     FileOutputStream fos = new FileOutputStream(destino)) {

                    if (in == null) {
                        throw new IOException("O arquivo de atualização não está disponível no banco.");
                    }

                    byte[] buffer = new byte[16384];
                    int lidos;
                    long baixados = 0;
                    while ((lidos = bis.read(buffer)) != -1) {
                        fos.write(buffer, 0, lidos);
                        baixados += lidos;
                        if (totalBytes > 0 && progressoCallback != null) {
                            int progresso = (int) (((double) baixados / totalBytes) * 100.0);
                            progressoCallback.accept(Math.min(progresso, 100));
                        }
                    }
                }
            }
        }
    }

    private static void baixarJarHttp(String downloadUrl, File destino, Consumer<Integer> progressoCallback) throws Exception {
        URL url = URI.create(downloadUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", "SystemProUpdater/1.1");
        conn.setInstanceFollowRedirects(true);

        int responseCode = conn.getResponseCode();
        if (responseCode == 301 || responseCode == 302 || responseCode == 307 || responseCode == 308) {
            String location = conn.getHeaderField("Location");
            if (location != null) {
                conn.disconnect();
                url = URI.create(location).toURL();
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "SystemProUpdater/1.1");
                responseCode = conn.getResponseCode();
            }
        }

        if (responseCode != 200) {
            throw new IOException("Falha no download (HTTP " + responseCode + ")");
        }

        int totalBytes = conn.getContentLength();
        int baixados = 0;

        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             FileOutputStream out = new FileOutputStream(destino)) {

            byte[] buffer = new byte[8192];
            int lidos;
            while ((lidos = in.read(buffer)) != -1) {
                out.write(buffer, 0, lidos);
                baixados += lidos;
                if (totalBytes > 0 && progressoCallback != null) {
                    int progresso = (int) (((double) baixados / totalBytes) * 100.0);
                    progressoCallback.accept(Math.min(progresso, 100));
                }
            }
        }
    }

    /**
     * Publica a versão atual e o arquivo JAR na tabela 'sistema_atualizacao'.
     */
    public static boolean publicarVersaoNoBanco(String versao, String dataLancamento, String novidades, File jarFile) {
        if (jarFile == null || !jarFile.exists() || jarFile.length() == 0) {
            System.err.println("[UPDATE SERVICE] Arquivo JAR não encontrado para publicação: " + jarFile);
            return false;
        }

        String sql = "INSERT INTO sistema_atualizacao (versao, data_lancamento, novidades, tamanho_bytes, arquivo_jar, ativo) " +
                "VALUES (?, ?, ?, ?, ?, 1) " +
                "ON DUPLICATE KEY UPDATE " +
                "data_lancamento = VALUES(data_lancamento), " +
                "novidades = VALUES(novidades), " +
                "tamanho_bytes = VALUES(tamanho_bytes), " +
                "arquivo_jar = VALUES(arquivo_jar), " +
                "ativo = 1, " +
                "atualizado_em = CURRENT_TIMESTAMP";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             FileInputStream fis = new FileInputStream(jarFile)) {

            ps.setString(1, versao);
            ps.setString(2, dataLancamento);
            ps.setString(3, novidades);
            ps.setLong(4, jarFile.length());
            ps.setBinaryStream(5, fis, jarFile.length());

            ps.executeUpdate();
            System.out.println("[UPDATE SERVICE] Versão v" + versao + " publicada no banco de dados com sucesso (" + (jarFile.length() / 1024 / 1024) + " MB)!");
            return true;
        } catch (Exception e) {
            System.err.println("[UPDATE SERVICE] Erro ao publicar versão no banco: " + e.getMessage());
            return false;
        }
    }

    /**
     * Sincroniza a versão atual com o banco caso seja a máquina Matriz e ainda não esteja gravada.
     */
    public static void sincronizarVersaoComBancoSeNecessario() {
        new Thread(() -> {
            try {
                if (!ConnectionFactory.isConfigurado()) {
                    return;
                }

                // Só publica se esta máquina for a Matriz
                if (!NetworkUtils.isEstaMaquinaMatriz()) {
                    return;
                }

                // Verifica se já está publicado
                String checkSql = "SELECT tamanho_bytes FROM sistema_atualizacao WHERE versao = ? AND arquivo_jar IS NOT NULL";
                try (Connection conn = ConnectionFactory.getConnection();
                     PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setString(1, VERSAO_ATUAL);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getLong("tamanho_bytes") > 0) {
                            return; // Já publicado!
                        }
                    }
                }

                // Localiza o JAR da versão atual
                File jar = localizarArquivoJarAtual();
                if (jar != null && jar.exists() && jar.length() > 0) {
                    String novidades = carregarNovidadesVersaoAtual();
                    String dataLancamento = carregarDataVersaoAtual();
                    publicarVersaoNoBanco(VERSAO_ATUAL, dataLancamento, novidades, jar);
                }
            } catch (Exception e) {
                System.err.println("[UPDATE SERVICE] Aviso na sincronização de versão com banco: " + e.getMessage());
            }
        }, "Update-Sync-Thread").start();
    }

    public static File localizarArquivoJarAtual() {
        File baseDir = getAppDirectory();
        File[] candidatos = new File[] {
                new File(baseDir, "SystemPro.jar"),
                new File(baseDir, "SistemaLoja.jar"),
                new File("target/SystemPro-1.0-SNAPSHOT.jar")
        };
        for (File f : candidatos) {
            if (f.exists() && f.length() > 500_000) {
                return f;
            }
        }
        return null;
    }

    public static String carregarNovidadesVersaoAtual() {
        File[] candidatos = new File[] {
                new File("versao.json"),
                new File(getAppDirectory(), "versao.json")
        };
        for (File f : candidatos) {
            if (f.exists()) {
                try {
                    String content = Files.readString(f.toPath(), StandardCharsets.UTF_8);
                    String novidades = extrairCampoJson(content, "novidades");
                    if (novidades != null && !novidades.trim().isEmpty()) {
                        return novidades.replace("\\n", "\n");
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return "- Melhorias gerais de estabilidade e novas funcionalidades da versão " + VERSAO_ATUAL;
    }

    public static String carregarDataVersaoAtual() {
        File[] candidatos = new File[] {
                new File("versao.json"),
                new File(getAppDirectory(), "versao.json")
        };
        for (File f : candidatos) {
            if (f.exists()) {
                try {
                    String content = Files.readString(f.toPath(), StandardCharsets.UTF_8);
                    String data = extrairCampoJson(content, "data");
                    if (data != null && !data.trim().isEmpty()) {
                        return data.trim();
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public static File getAppDirectory() {
        try {
            File jarDir = new File(UpdateService.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .getParentFile();
            if (jarDir != null && jarDir.isDirectory()) {
                return jarDir;
            }
        } catch (Exception ignored) {
        }
        return new File(".");
    }
}
