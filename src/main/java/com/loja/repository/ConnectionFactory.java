package com.loja.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class ConnectionFactory {
    private static String host = "localhost";
    private static String port = "3306";
    private static String database = "banco_assistencia";
    private static String user = "root";
    private static String pass = "";
    private static boolean ssl = false;
    private static boolean carregado = false;

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[ERRO] Driver do MySQL não encontrado no projeto!");
        }
    }

    public static String[] parseHostPort(String rawHost, String rawPort) {
        String cleanHost = (rawHost != null && !rawHost.trim().isEmpty()) ? rawHost.trim() : "localhost";
        String cleanPort = (rawPort != null && !rawPort.trim().isEmpty()) ? rawPort.trim() : "";

        // Se o host foi informado como "dominio.com:3307" ou "192.168.1.10:3308"
        if (cleanHost.contains(":") && !cleanHost.startsWith("[")) {
            int idx = cleanHost.lastIndexOf(":");
            String extractedPort = cleanHost.substring(idx + 1).trim();
            cleanHost = cleanHost.substring(0, idx).trim();
            if (cleanPort.isEmpty()) {
                cleanPort = extractedPort;
            }
        }

        // Se a porta continuar em branco após análise, assume a padrão 3306
        if (cleanPort.isEmpty()) {
            cleanPort = "3306";
        }

        return new String[] { cleanHost, cleanPort };
    }

    public static File getArquivoConfig() {
        // 1. Tenta verificar se já existe no AppData do usuário (local padrão seguro no Windows)
        File appDataConfig = getArquivoConfigAppData();
        if (appDataConfig != null && appDataConfig.exists()) {
            return appDataConfig;
        }

        // 2. Tenta verificar se já existe na pasta atual (raiz do projeto / executável)
        File currentDirConfig = new File("database.properties");
        if (currentDirConfig.exists()) {
            return currentDirConfig;
        }

        // 3. Tenta verificar se existe na pasta do JAR
        try {
            File jarDir = new File(ConnectionFactory.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
            if (jarDir != null && jarDir.isDirectory()) {
                File localConfig = new File(jarDir, "database.properties");
                if (localConfig.exists()) {
                    return localConfig;
                }
                // Se a pasta do JAR tiver permissão de gravação, pode usar local
                if (jarDir.canWrite()) {
                    return localConfig;
                }
            }
        } catch (Exception ignored) {}

        // 3. Se a pasta do JAR não permitir escrita (ex: Program Files), usa o AppData do usuário
        if (appDataConfig != null) {
            return appDataConfig;
        }

        return new File("database.properties");
    }

    private static File getArquivoConfigAppData() {
        try {
            String appData = System.getenv("APPDATA");
            File baseDir;
            if (appData != null && !appData.trim().isEmpty()) {
                baseDir = new File(appData, "SystemPro");
                // Compatibilidade: se ainda nao existe em SystemPro, verifica se existia em SistemaLoja
                if (!baseDir.exists()) {
                    File legacyDir = new File(appData, "SistemaLoja");
                    if (legacyDir.exists()) {
                        File legacyConfig = new File(legacyDir, "database.properties");
                        if (legacyConfig.exists()) {
                            return legacyConfig;
                        }
                    }
                }
            } else {
                baseDir = new File(System.getProperty("user.home", "."), ".systempro");
            }
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }
            return new File(baseDir, "database.properties");
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isArquivoConfigExiste() {
        File f = getArquivoConfig();
        if (f != null && f.exists()) return true;
        File appData = getArquivoConfigAppData();
        return appData != null && appData.exists();
    }

    public static boolean isConfigurado() {
        return isArquivoConfigExiste();
    }

    public static synchronized void carregarConfiguracoes() {
        if (carregado) return;
        File configFile = getArquivoConfig();
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(fis);
                String rawH = props.getProperty("db.host", "localhost").trim();
                String rawP = props.getProperty("db.port", "3306").trim();
                String[] parsed = parseHostPort(rawH, rawP);
                host = parsed[0];
                port = parsed[1];
                database = props.getProperty("db.database", "banco_assistencia").trim();
                user = props.getProperty("db.user", "root").trim();
                pass = props.getProperty("db.password", "");
                ssl = Boolean.parseBoolean(props.getProperty("db.ssl", "false").trim());
            } catch (Exception e) {
                System.err.println("[AVISO] Não foi possível ler database.properties: " + e.getMessage());
            }
        }
        carregado = true;
    }

    public static synchronized void salvarConfiguracoes(String novoHost, String novaPorta, String novoBanco, String novoUser, String novaSenha) throws IOException {
        salvarConfiguracoes(novoHost, novaPorta, novoBanco, novoUser, novaSenha, false);
    }

    public static synchronized void salvarConfiguracoes(String novoHost, String novaPorta, String novoBanco, String novoUser, String novaSenha, boolean novoSsl) throws IOException {
        String[] parsed = parseHostPort(novoHost, novaPorta);
        host = parsed[0];
        port = parsed[1];
        database = (novoBanco != null && !novoBanco.trim().isEmpty()) ? novoBanco.trim() : "banco_assistencia";
        user = (novoUser != null && !novoUser.trim().isEmpty()) ? novoUser.trim() : "root";
        pass = (novaSenha != null) ? novaSenha : "";
        ssl = novoSsl;

        Properties props = new Properties();
        props.setProperty("db.host", host);
        props.setProperty("db.port", port);
        props.setProperty("db.database", database);
        props.setProperty("db.user", user);
        props.setProperty("db.password", pass);
        props.setProperty("db.ssl", String.valueOf(ssl));

        File configFile = getArquivoConfig();
        try {
            if (configFile.getParentFile() != null && !configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                props.store(fos, "Configuracoes de Conexao MySQL - Sistema Loja Assistencia");
            }
        } catch (IOException e) {
            // Se falhou por permissão (Acesso negado em C:\Program Files), salva automaticamente no AppData do usuário
            File fallbackFile = getArquivoConfigAppData();
            if (fallbackFile != null && !fallbackFile.equals(configFile)) {
                try (FileOutputStream fos = new FileOutputStream(fallbackFile)) {
                    props.store(fos, "Configuracoes de Conexao MySQL - Sistema Loja Assistencia");
                }
            } else {
                throw e;
            }
        }
        carregado = true;
    }

    public static String buildUrl(String h, String p, String db, boolean useSsl, boolean autoCreateDb) {
        String[] parsed = parseHostPort(h, p);
        String finalHost = parsed[0];
        String finalPort = parsed[1];
        String finalDb = (db != null && !db.trim().isEmpty()) ? db.trim() : "banco_assistencia";

        StringBuilder sb = new StringBuilder();
        sb.append("jdbc:mysql://").append(finalHost).append(":").append(finalPort).append("/").append(finalDb);
        sb.append("?allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=8000&useUnicode=true&characterEncoding=UTF-8");

        if (autoCreateDb) {
            sb.append("&createDatabaseIfNotExist=true");
        }

        if (useSsl) {
            sb.append("&useSSL=true&sslMode=REQUIRED");
        } else {
            sb.append("&useSSL=false");
        }

        return sb.toString();
    }

    public static String getUrl() {
        carregarConfiguracoes();
        return buildUrl(host, port, database, ssl, false);
    }

    public static Connection getConnection() throws SQLException {
        carregarConfiguracoes();
        String mainUrl = buildUrl(host, port, database, ssl, false);
        try {
            return DriverManager.getConnection(mainUrl, user, pass);
        } catch (SQLException e) {
            // Se falhar porque o banco ainda não foi criado (Código MySQL 1049: Unknown database)
            if (e.getErrorCode() == 1049 || (e.getMessage() != null && e.getMessage().toLowerCase().contains("unknown database"))) {
                String createDbUrl = buildUrl(host, port, database, ssl, true);
                return DriverManager.getConnection(createDbUrl, user, pass);
            }
            throw e;
        }
    }

    public static boolean testarConexao() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static String testarConexaoCom(String h, String p, String db, String u, String pwd) {
        return testarConexaoCom(h, p, db, u, pwd, false);
    }

    public static String testarConexaoCom(String h, String p, String db, String u, String pwd, boolean useSsl) {
        String[] parsed = parseHostPort(h, p);
        String finalHost = parsed[0];
        String finalPort = parsed[1];

        // 1. Tenta conectar diretamente ao banco
        String testUrl = buildUrl(finalHost, finalPort, db, useSsl, false);
        try (Connection conn = DriverManager.getConnection(testUrl, u, pwd)) {
            if (conn != null && !conn.isClosed()) {
                return null; // Sucesso!
            }
            return "Não foi possível abrir a conexão com o servidor MySQL.";
        } catch (SQLException ex) {
            // 2. Se o banco não existe (código 1049), tenta conectar solicitando a criação automática
            if (ex.getErrorCode() == 1049 || (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("unknown database"))) {
                String createDbUrl = buildUrl(finalHost, finalPort, db, useSsl, true);
                try (Connection conn2 = DriverManager.getConnection(createDbUrl, u, pwd)) {
                    if (conn2 != null && !conn2.isClosed()) {
                        return null; // Sucesso com criação de banco!
                    }
                } catch (SQLException ex2) {
                    return "O banco '" + db + "' não existe no servidor e o usuário não possui permissão para criá-lo automaticamente: " + ex2.getMessage();
                }
            }

            // Diagnósticos amigáveis
            String msg = ex.getMessage();
            int code = ex.getErrorCode();
            if (code == 1045 || (msg != null && msg.toLowerCase().contains("access denied"))) {
                return "Acesso negado: Usuário ou senha incorretos para o servidor (" + finalHost + ":" + finalPort + ").";
            }
            if (msg != null && (msg.toLowerCase().contains("communications link failure") || msg.toLowerCase().contains("connect timed out"))) {
                return "Falha de comunicação: Não foi possível alcançar o servidor em " + finalHost + ":" + finalPort + ".\nVerifique se o servidor está ligado, se a porta está liberada no firewall/roteador e se o IP/domínio está correto.";
            }
            if (msg != null && msg.toLowerCase().contains("ssl")) {
                return "Erro de SSL: O servidor pode exigir (ou não aceitar) conexão segura SSL. Tente alternar a opção de SSL.";
            }

            return msg != null ? msg : "Erro de conexão MySQL código: " + code;
        }
    }

    public static String getHost() { carregarConfiguracoes(); return host; }
    public static String getPort() { carregarConfiguracoes(); return port; }
    public static String getDatabase() { carregarConfiguracoes(); return database; }
    public static String getUser() { carregarConfiguracoes(); return user; }
    public static String getPass() { carregarConfiguracoes(); return pass; }
    public static boolean isSsl() { carregarConfiguracoes(); return ssl; }

    private static void garantirPermissaoRedeRoot(Connection conn) {
        try {
            if (com.loja.util.NetworkUtils.isEstaMaquinaMatriz()) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED VIA mysql_native_password USING '' WITH GRANT OPTION");
                    stmt.execute("FLUSH PRIVILEGES");
                } catch (Exception ignored) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION");
                        stmt.execute("FLUSH PRIVILEGES");
                    } catch (Exception ignored2) {}
                }
            }
        } catch (Exception ignored) {}
    }

    public static boolean criarTabela() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Se for máquina matriz / local, garante permissões para aceitar conexões da rede / Hyper-V
            garantirPermissaoRedeRoot(conn);

            // 1. Tabela de Peças/Produtos
            stmt.execute("CREATE TABLE IF NOT EXISTS produto (" +
                    "id INT PRIMARY KEY, " +
                    "codigo_barras VARCHAR(50), " +
                    "nome VARCHAR(100), " +
                    "preco_custo DOUBLE DEFAULT 0.0, " +
                    "preco DOUBLE, " +
                    "estoque INT, " +
                    "ncm VARCHAR(10) DEFAULT '', " +
                    "cest VARCHAR(10) DEFAULT '', " +
                    "cfop VARCHAR(10) DEFAULT '5102', " +
                    "origem INT DEFAULT 0, " +
                    "csosn_cst VARCHAR(10) DEFAULT '102', " +
                    "unidade VARCHAR(10) DEFAULT 'UN')");

            // 2. Tabela de Clientes
            stmt.execute("CREATE TABLE IF NOT EXISTS cliente (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "nome VARCHAR(100) NOT NULL, " +
                    "cpf_cnpj VARCHAR(20), " +
                    "telefone VARCHAR(20), " +
                    "email VARCHAR(100), " +
                    "endereco VARCHAR(255))");

            // 3. Tabela de Equipamentos
            stmt.execute("CREATE TABLE IF NOT EXISTS equipamento (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "cliente_id INT, " +
                    "tipo VARCHAR(50), " +
                    "marca VARCHAR(50), " +
                    "modelo VARCHAR(50), " +
                    "numero_serie VARCHAR(50), " +
                    "cor VARCHAR(50), " +
                    "avarias TEXT, " +
                    "patrimonio VARCHAR(50), " +
                    "senha_acesso VARCHAR(50), " +
                    "acessorios TEXT, " +
                    "FOREIGN KEY (cliente_id) REFERENCES cliente(id) ON DELETE CASCADE)");

            // 4. Tabela de Ordens de Serviço
            stmt.execute("CREATE TABLE IF NOT EXISTS ordem_servico (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "cliente_id INT, " +
                    "equipamento_id INT, " +
                    "problema_relatado TEXT, " +
                    "diagnostico_tecnico TEXT, " +
                    "status VARCHAR(100) DEFAULT 'Aguardando Orçamento', " +
                    "valor_servico DOUBLE DEFAULT 0.0, " +
                    "valor_total DOUBLE DEFAULT 0.0, " +
                    "checklist_entrada TEXT, " +
                    "observacoes_finais TEXT, " +
                    "data_entrada TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "data_saida TIMESTAMP NULL, " +
                    "FOREIGN KEY (cliente_id) REFERENCES cliente(id), " +
                    "FOREIGN KEY (equipamento_id) REFERENCES equipamento(id))");

            // 5. Tabela de Peças por Ordem de Serviço
            stmt.execute("CREATE TABLE IF NOT EXISTS os_pecas (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "os_id INT, " +
                    "produto_id INT, " +
                    "nome VARCHAR(200), " +
                    "quantidade INT, " +
                    "valor_unitario DOUBLE, " +
                    "FOREIGN KEY (os_id) REFERENCES ordem_servico(id) ON DELETE CASCADE)");

            // 6. Tabela de Serviços por Ordem de Serviço
            stmt.execute("CREATE TABLE IF NOT EXISTS os_servicos (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "os_id INT, " +
                    "descricao VARCHAR(200), " +
                    "valor DOUBLE, " +
                    "FOREIGN KEY (os_id) REFERENCES ordem_servico(id) ON DELETE CASCADE)");

            // 7. Catálogo Reutilizável de Serviços
            stmt.execute("CREATE TABLE IF NOT EXISTS catalogo_servicos (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "descricao VARCHAR(200) UNIQUE, " +
                    "valor_padrao DOUBLE)");

            // 8. Catálogo Reutilizável de Peças
            stmt.execute("CREATE TABLE IF NOT EXISTS catalogo_pecas (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "nome VARCHAR(200) UNIQUE, " +
                    "valor_padrao DOUBLE)");

            // 9. Tabela do Caixa
            stmt.execute("CREATE TABLE IF NOT EXISTS caixa (" +
                    "id INT PRIMARY KEY, " +
                    "saldo DOUBLE)");

            // 10. Tabela de Sangrias
            stmt.execute("CREATE TABLE IF NOT EXISTS sangria (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "valor DOUBLE, " +
                    "data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // 11. Tabela de Histórico / Linha do Tempo da OS
            stmt.execute("CREATE TABLE IF NOT EXISTS os_historico (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "os_id INT NOT NULL, " +
                    "status VARCHAR(100) NOT NULL, " +
                    "data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "observacao VARCHAR(255), " +
                    "FOREIGN KEY (os_id) REFERENCES ordem_servico(id) ON DELETE CASCADE)");

            // 12. Tabela de Vendas PDV (Frente de Caixa Balcão)
            stmt.execute("CREATE TABLE IF NOT EXISTS venda_pdv (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "subtotal DOUBLE NOT NULL DEFAULT 0.0, " +
                    "desconto DOUBLE NOT NULL DEFAULT 0.0, " +
                    "valor_total DOUBLE NOT NULL DEFAULT 0.0, " +
                    "valor_liquido DOUBLE NOT NULL DEFAULT 0.0, " +
                    "observacoes TEXT, " +
                    "data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // 13. Tabela de Itens de Venda PDV
            stmt.execute("CREATE TABLE IF NOT EXISTS venda_pdv_itens (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "venda_id INT NOT NULL, " +
                    "produto_id INT NOT NULL, " +
                    "nome_produto VARCHAR(200) NOT NULL, " +
                    "quantidade INT NOT NULL, " +
                    "valor_unitario DOUBLE NOT NULL, " +
                    "subtotal DOUBLE NOT NULL, " +
                    "FOREIGN KEY (venda_id) REFERENCES venda_pdv(id) ON DELETE CASCADE)");

            // 14. Tabela de Pagamentos Fracionados / Múltiplos
            stmt.execute("CREATE TABLE IF NOT EXISTS venda_pagamento (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "venda_id INT NULL, " +
                    "os_id INT NULL, " +
                    "modalidade VARCHAR(50) NOT NULL, " +
                    "valor_bruto DOUBLE NOT NULL, " +
                    "taxa_percentual DOUBLE NOT NULL DEFAULT 0.0, " +
                    "valor_taxa DOUBLE NOT NULL DEFAULT 0.0, " +
                    "valor_liquido DOUBLE NOT NULL, " +
                    "parcelas INT NOT NULL DEFAULT 1, " +
                    "data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // 15. Tabela de Movimentações de Caixa (Entradas, Sangrias, Suprimentos)
            stmt.execute("CREATE TABLE IF NOT EXISTS caixa_movimento (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "tipo VARCHAR(30) NOT NULL, " +
                    "modalidade VARCHAR(50) NOT NULL, " +
                    "valor DOUBLE NOT NULL, " +
                    "taxa DOUBLE NOT NULL DEFAULT 0.0, " +
                    "valor_liquido DOUBLE NOT NULL, " +
                    "justificativa VARCHAR(255), " +
                    "os_id INT NULL, " +
                    "venda_id INT NULL, " +
                    "sessao_id INT NULL, " +
                    "data_hora TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // 16. Tabela de Configuração de Taxas de Maquininha
            stmt.execute("CREATE TABLE IF NOT EXISTS config_taxas (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "modalidade VARCHAR(50) UNIQUE NOT NULL, " +
                    "taxa_percentual DOUBLE NOT NULL DEFAULT 0.0)");

            // 17. Tabela de Sessões / Turnos de Caixa (Abertura e Fechamento com Contagem Cega)
            stmt.execute("CREATE TABLE IF NOT EXISTS caixa_sessao (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "status VARCHAR(20) NOT NULL DEFAULT 'ABERTO', " +
                    "data_abertura TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "data_fechamento TIMESTAMP NULL, " +
                    "operador_abertura VARCHAR(100) NOT NULL, " +
                    "operador_fechamento VARCHAR(100) NULL, " +
                    "saldo_inicial DOUBLE NOT NULL DEFAULT 0.0, " +
                    "saldo_final_sistema DOUBLE NOT NULL DEFAULT 0.0, " +
                    "saldo_final_informado DOUBLE NOT NULL DEFAULT 0.0, " +
                    "diferenca DOUBLE NOT NULL DEFAULT 0.0, " +
                    "justificativa_diferenca TEXT NULL, " +
                    "fundo_troco_deixado DOUBLE NOT NULL DEFAULT 0.0, " +
                    "sangria_malote DOUBLE NOT NULL DEFAULT 0.0, " +
                    "total_dinheiro DOUBLE NOT NULL DEFAULT 0.0, " +
                    "total_pix DOUBLE NOT NULL DEFAULT 0.0, " +
                    "total_debito_bruto DOUBLE NOT NULL DEFAULT 0.0, " +
                    "total_debito_liquido DOUBLE NOT NULL DEFAULT 0.0, " +
                    "total_credito_bruto DOUBLE NOT NULL DEFAULT 0.0, " +
                    "total_credito_liquido DOUBLE NOT NULL DEFAULT 0.0, " +
                    "total_sangrias DOUBLE NOT NULL DEFAULT 0.0, " +
                    "total_suprimentos DOUBLE NOT NULL DEFAULT 0.0, " +
                    "total_vendas_bruto DOUBLE NOT NULL DEFAULT 0.0, " +
                    "total_vendas_liquido DOUBLE NOT NULL DEFAULT 0.0, " +
                    "qtd_vendas INT NOT NULL DEFAULT 0, " +
                    "contagem_detalhada_texto TEXT NULL, " +
                    "observacoes TEXT NULL)");

            // 18. Tabela de Configurações Gerais da Loja (WhatsApp do Dono, Troco padrão, etc)
            stmt.execute("CREATE TABLE IF NOT EXISTS config_geral (" +
                    "chave VARCHAR(100) PRIMARY KEY, " +
                    "valor TEXT)");

            // 19. Tabela de Usuários e Autenticação com Perfis
            stmt.execute("CREATE TABLE IF NOT EXISTS usuario (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "nome VARCHAR(100) NOT NULL, " +
                    "login VARCHAR(50) UNIQUE NOT NULL, " +
                    "senha_hash VARCHAR(128) NOT NULL, " +
                    "perfil VARCHAR(20) NOT NULL DEFAULT 'ATENDENTE', " +
                    "ativo BOOLEAN DEFAULT TRUE, " +
                    "data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // 20. Tabela de Fotos e Evidências da Ordem de Serviço
            stmt.execute("CREATE TABLE IF NOT EXISTS os_fotos (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "os_id INT NOT NULL, " +
                    "nome_arquivo VARCHAR(255) NOT NULL, " +
                    "descricao VARCHAR(255), " +
                    "dados MEDIUMBLOB NOT NULL, " +
                    "miniatura MEDIUMBLOB, " +
                    "tamanho_bytes BIGINT, " +
                    "operador VARCHAR(100), " +
                    "data_upload TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (os_id) REFERENCES ordem_servico(id) ON DELETE CASCADE)");

            // 21. Tabela de Sessões e Terminais Conectados na Rede
            stmt.execute("CREATE TABLE IF NOT EXISTS terminal_sessao (" +
                    "id VARCHAR(64) PRIMARY KEY, " +
                    "hostname VARCHAR(100) NOT NULL, " +
                    "ip VARCHAR(50) NOT NULL, " +
                    "sistema_operacional VARCHAR(100), " +
                    "versao_sistema VARCHAR(30), " +
                    "usuario_id INT NULL, " +
                    "usuario_nome VARCHAR(100), " +
                    "usuario_perfil VARCHAR(30), " +
                    "tela_atual VARCHAR(100), " +
                    "hora_login DATETIME, " +
                    "ultimo_ping DATETIME, " +
                    "status VARCHAR(20) DEFAULT 'ONLINE', " +
                    "bloqueado BOOLEAN DEFAULT FALSE, " +
                    "comando_pendente TEXT NULL, " +
                    "comando_data DATETIME NULL)");

            // 22. Tabela de Transmissões Broadcast de Mensagens
            stmt.execute("CREATE TABLE IF NOT EXISTS terminal_broadcast (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "remetente VARCHAR(100) NOT NULL, " +
                    "mensagem TEXT NOT NULL, " +
                    "data_envio DATETIME DEFAULT CURRENT_TIMESTAMP)");

            // 23. Tabela de Distribuição de Atualizações do Sistema (Atualização Local / Sem Reinstalação)
            stmt.execute("CREATE TABLE IF NOT EXISTS sistema_atualizacao (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "versao VARCHAR(30) NOT NULL UNIQUE, " +
                    "data_lancamento VARCHAR(30) NOT NULL, " +
                    "novidades TEXT NOT NULL, " +
                    "tamanho_bytes BIGINT NOT NULL, " +
                    "arquivo_jar LONGBLOB, " +
                    "ativo TINYINT(1) DEFAULT 1, " +
                    "atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");

            // 24. Tabela de Configurações Fiscais e Emissão SEFAZ (NFC-e / NF-e)
            stmt.execute("CREATE TABLE IF NOT EXISTS config_fiscal (" +
                    "id INT PRIMARY KEY, " +
                    "cnpj VARCHAR(20), " +
                    "inscricao_estadual VARCHAR(30), " +
                    "razao_social VARCHAR(150), " +
                    "nome_fantasia VARCHAR(150), " +
                    "regime_tributario INT DEFAULT 1, " +
                    "ambiente INT DEFAULT 2, " +
                    "csc_token VARCHAR(100), " +
                    "csc_id VARCHAR(10) DEFAULT '000001', " +
                    "serie_nfce INT DEFAULT 1, " +
                    "ultimo_numero_nfce INT DEFAULT 0, " +
                    "serie_nfe INT DEFAULT 1, " +
                    "ultimo_numero_nfe INT DEFAULT 0, " +
                    "certificado_caminho VARCHAR(255), " +
                    "certificado_senha VARCHAR(100), " +
                    "logradouro VARCHAR(150), " +
                    "numero VARCHAR(20), " +
                    "complemento VARCHAR(50), " +
                    "bairro VARCHAR(100), " +
                    "municipio VARCHAR(100), " +
                    "codigo_ibge VARCHAR(10), " +
                    "uf VARCHAR(2) DEFAULT 'SP', " +
                    "cep VARCHAR(10), " +
                    "telefone VARCHAR(20))");

            // 25. Tabela de Registro de Notas Fiscais Emitidas
            stmt.execute("CREATE TABLE IF NOT EXISTS nota_fiscal (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "tipo VARCHAR(10) NOT NULL, " +
                    "modelo VARCHAR(5) NOT NULL, " +
                    "serie INT NOT NULL, " +
                    "numero INT NOT NULL, " +
                    "chave_acesso VARCHAR(50), " +
                    "status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE', " +
                    "data_emissao TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "venda_id INT NULL, " +
                    "os_id INT NULL, " +
                    "cliente_id INT NULL, " +
                    "cliente_cpf_cnpj VARCHAR(20), " +
                    "cliente_nome VARCHAR(100), " +
                    "valor_total DOUBLE NOT NULL DEFAULT 0.0, " +
                    "protocolo_autorizacao VARCHAR(50), " +
                    "motivo_status TEXT, " +
                    "qr_code_url TEXT, " +
                    "xml_envio LONGTEXT, " +
                    "xml_retorno LONGTEXT, " +
                    "FOREIGN KEY (venda_id) REFERENCES venda_pdv(id) ON DELETE SET NULL, " +
                    "FOREIGN KEY (os_id) REFERENCES ordem_servico(id) ON DELETE SET NULL)");

            // Migrações automáticas de colunas e tamanhos
            try {
                stmt.execute("ALTER TABLE ordem_servico MODIFY COLUMN status VARCHAR(100)");
            } catch (SQLException ignored) {}

            adicionarColunaSeNaoExistir(conn, "produto", "codigo_barras", "VARCHAR(50)");
            adicionarColunaSeNaoExistir(conn, "produto", "preco_custo", "DOUBLE DEFAULT 0.0");
            adicionarColunaSeNaoExistir(conn, "produto", "ncm", "VARCHAR(10) DEFAULT ''");
            adicionarColunaSeNaoExistir(conn, "produto", "cest", "VARCHAR(10) DEFAULT ''");
            adicionarColunaSeNaoExistir(conn, "produto", "cfop", "VARCHAR(10) DEFAULT '5102'");
            adicionarColunaSeNaoExistir(conn, "produto", "origem", "INT DEFAULT 0");
            adicionarColunaSeNaoExistir(conn, "produto", "csosn_cst", "VARCHAR(10) DEFAULT '102'");
            adicionarColunaSeNaoExistir(conn, "produto", "unidade", "VARCHAR(10) DEFAULT 'UN'");

            adicionarColunaSeNaoExistir(conn, "cliente", "inscricao_estadual", "VARCHAR(30) DEFAULT ''");
            adicionarColunaSeNaoExistir(conn, "cliente", "indicador_ie", "INT DEFAULT 9");
            adicionarColunaSeNaoExistir(conn, "cliente", "cep", "VARCHAR(10) DEFAULT ''");
            adicionarColunaSeNaoExistir(conn, "cliente", "numero", "VARCHAR(20) DEFAULT ''");
            adicionarColunaSeNaoExistir(conn, "cliente", "bairro", "VARCHAR(100) DEFAULT ''");
            adicionarColunaSeNaoExistir(conn, "cliente", "municipio", "VARCHAR(100) DEFAULT ''");
            adicionarColunaSeNaoExistir(conn, "cliente", "uf", "VARCHAR(2) DEFAULT ''");
            adicionarColunaSeNaoExistir(conn, "cliente", "codigo_ibge", "VARCHAR(10) DEFAULT ''");

            adicionarColunaSeNaoExistir(conn, "sangria", "justificativa", "VARCHAR(255)");
            adicionarColunaSeNaoExistir(conn, "equipamento", "cor", "VARCHAR(50)");
            adicionarColunaSeNaoExistir(conn, "equipamento", "avarias", "TEXT");
            adicionarColunaSeNaoExistir(conn, "equipamento", "patrimonio", "VARCHAR(50)");
            adicionarColunaSeNaoExistir(conn, "equipamento", "senha_acesso", "VARCHAR(50)");
            adicionarColunaSeNaoExistir(conn, "equipamento", "acessorios", "TEXT");
            adicionarColunaSeNaoExistir(conn, "ordem_servico", "checklist_entrada", "TEXT");
            adicionarColunaSeNaoExistir(conn, "ordem_servico", "observacoes_finais", "TEXT");
            adicionarColunaSeNaoExistir(conn, "os_pecas", "nome", "VARCHAR(200)");
            adicionarColunaSeNaoExistir(conn, "caixa_movimento", "sessao_id", "INT NULL");
            adicionarColunaSeNaoExistir(conn, "venda_pdv", "sessao_id", "INT NULL");

            // Migração de os_pecas se tiver chave primária antiga
            migrarTabelaOsPecasSeNecessario(conn);

            // Inicializar configuração fiscal padrão caso tabela esteja vazia
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM config_fiscal")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("INSERT INTO config_fiscal (id, razao_social, nome_fantasia, regime_tributario, ambiente, serie_nfce, ultimo_numero_nfce, serie_nfe, ultimo_numero_nfe) " +
                            "VALUES (1, 'Sua Empresa LTDA', 'Minha Loja & Assistência', 1, 2, 1, 0, 1, 0)");
                    System.out.println("[DB] Configuração fiscal padrão inicializada (Ambiente: Homologação).");
                }
            }

            // Inicializar saldo do caixa caso esteja vazio
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM caixa")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("INSERT INTO caixa (id, saldo) VALUES (1, 100.00)");
                    System.out.println("[DB] Saldo inicial do caixa (R$ 100.00) inicializado.");
                }
            }

            // Inicializar configurações padrão caso tabela esteja vazia
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM config_geral")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("INSERT INTO config_geral (chave, valor) VALUES " +
                            "('nome_loja', 'Assistência Técnica & Gestão Pro'), " +
                            "('whatsapp_proprietario', ''), " +
                            "('fundo_troco_padrao', '100.00'), " +
                            "('largura_cupom_mm', '80')");
                    System.out.println("[DB] Configurações gerais da loja inicializadas.");
                }
            }

            // Inicializar taxas padrão da maquininha caso tabela esteja vazia
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM config_taxas")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.execute("INSERT INTO config_taxas (modalidade, taxa_percentual) VALUES " +
                            "('DINHEIRO', 0.0), " +
                            "('PIX', 0.0), " +
                            "('DEBITO', 1.50), " +
                            "('CREDITO_1X', 3.20), " +
                            "('CREDITO_PARCELADO_2X_6X', 5.50), " +
                            "('CREDITO_PARCELADO_7X_12X', 9.80)");
                    System.out.println("[DB] Taxas padrão de maquininha cadastradas com sucesso.");
                }
            }

            // Inicializar usuário administrador padrão se a tabela estiver vazia
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM usuario")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String hashAdmin = com.loja.service.SegurancaService.gerarHashSenha("admin");
                    try (PreparedStatement stmtUser = conn.prepareStatement(
                            "INSERT INTO usuario (nome, login, senha_hash, perfil, ativo) VALUES (?, ?, ?, ?, ?)")) {
                        stmtUser.setString(1, "Administrador Principal");
                        stmtUser.setString(2, "admin");
                        stmtUser.setString(3, hashAdmin);
                        stmtUser.setString(4, "ADMIN");
                        stmtUser.setBoolean(5, true);
                        stmtUser.executeUpdate();
                    }
                    System.out.println("[DB] Usuário padrão 'admin' (Perfil: ADMIN) cadastrado com sucesso.");
                }
            }

            System.out.println("[DB] Tabelas de assistência técnica criadas/verificadas com sucesso no MySQL.");
            return true;

        } catch (SQLException e) {
            System.err.println("[ERRO] Falha ao conectar ou organizar o banco de dados no MySQL: " + e.getMessage());
            return false;
        }
    }

    private static void adicionarColunaSeNaoExistir(Connection conn, String tabela, String coluna, String tipo) {
        try {
            DatabaseMetaData md = conn.getMetaData();
            try (ResultSet rs = md.getColumns(null, null, tabela, coluna)) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ALTER TABLE " + tabela + " ADD COLUMN " + coluna + " " + tipo);
                    }
                }
            }
        } catch (SQLException e) {
            // Ignora erro se coluna já existir ou falhar silenciosamente
        }
    }

    private static void migrarTabelaOsPecasSeNecessario(Connection conn) {
        try {
            DatabaseMetaData md = conn.getMetaData();
            boolean temId = false;
            try (ResultSet rs = md.getColumns(null, null, "os_pecas", "id")) {
                if (rs.next()) {
                    temId = true;
                }
            }

            if (!temId) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DROP TABLE IF EXISTS os_pecas");
                    stmt.execute("CREATE TABLE os_pecas (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "os_id INT, " +
                            "produto_id INT, " +
                            "nome VARCHAR(200), " +
                            "quantidade INT, " +
                            "valor_unitario DOUBLE, " +
                            "FOREIGN KEY (os_id) REFERENCES ordem_servico(id) ON DELETE CASCADE)");
                    System.out.println("[DB] Tabela os_pecas recriada com estrutura flexivel (id auto-increment).");
                }
            }
        } catch (SQLException e) {
            System.err.println("[AVISO DB] Migracao de os_pecas: " + e.getMessage());
        }
    }
}
