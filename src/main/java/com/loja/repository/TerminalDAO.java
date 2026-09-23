package com.loja.repository;

import com.loja.model.TerminalBroadcast;
import com.loja.model.TerminalSessao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TerminalDAO {

    public boolean registrarOuAtualizar(TerminalSessao sessao) {
        // Remove registros antigos com IP de Tailscale ou duplicatas offline do mesmo hostname
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement delStmt = conn.prepareStatement(
                     "DELETE FROM terminal_sessao WHERE (hostname = ? AND id != ?) OR ip LIKE '100.%'")) {
            delStmt.setString(1, sessao.getHostname());
            delStmt.setString(2, sessao.getId());
            delStmt.executeUpdate();
        } catch (Exception ignored) {}

        String sql = "INSERT INTO terminal_sessao " +
                "(id, hostname, ip, sistema_operacional, versao_sistema, usuario_id, usuario_nome, usuario_perfil, tela_atual, hora_login, ultimo_ping, status, bloqueado) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "hostname = VALUES(hostname), ip = VALUES(ip), sistema_operacional = VALUES(sistema_operacional), " +
                "versao_sistema = VALUES(versao_sistema), usuario_id = VALUES(usuario_id), usuario_nome = VALUES(usuario_nome), " +
                "usuario_perfil = VALUES(usuario_perfil), tela_atual = VALUES(tela_atual), ultimo_ping = VALUES(ultimo_ping), " +
                "status = VALUES(status)";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sessao.getId());
            stmt.setString(2, sessao.getHostname());
            stmt.setString(3, sessao.getIp());
            stmt.setString(4, sessao.getSistemaOperacional());
            stmt.setString(5, sessao.getVersaoSistema());
            if (sessao.getUsuarioId() != null) {
                stmt.setInt(6, sessao.getUsuarioId());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }
            stmt.setString(7, sessao.getUsuarioNome());
            stmt.setString(8, sessao.getUsuarioPerfil());
            stmt.setString(9, sessao.getTelaAtual());
            stmt.setTimestamp(10, Timestamp.valueOf(sessao.getHoraLogin() != null ? sessao.getHoraLogin() : LocalDateTime.now()));
            stmt.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(12, sessao.getStatus() != null ? sessao.getStatus() : "ONLINE");
            stmt.setBoolean(13, sessao.isBloqueado());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[TERMINAL DAO] Erro ao registrar/atualizar sessão: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizarPing(String sessaoId, String telaAtual, String status) {
        String sql = "UPDATE terminal_sessao SET ultimo_ping = NOW(), tela_atual = ?, status = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, telaAtual);
            stmt.setString(2, status != null ? status : "ONLINE");
            stmt.setString(3, sessaoId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[TERMINAL DAO] Erro ao atualizar ping: " + e.getMessage());
            return false;
        }
    }

    public boolean marcarOffline(String sessaoId) {
        String sql = "UPDATE terminal_sessao SET status = 'OFFLINE', ultimo_ping = NOW() WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessaoId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[TERMINAL DAO] Erro ao marcar offline: " + e.getMessage());
            return false;
        }
    }

    public TerminalSessao buscarPorId(String sessaoId) {
        String sql = "SELECT * FROM terminal_sessao WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessaoId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[TERMINAL DAO] Erro ao buscar por id: " + e.getMessage());
        }
        return null;
    }

    public List<TerminalSessao> listarTodos() {
        List<TerminalSessao> lista = new ArrayList<>();
        String sql = "SELECT * FROM terminal_sessao WHERE ip NOT LIKE '100.%' ORDER BY " +
                "bloqueado DESC, " +
                "CASE WHEN status = 'ONLINE' THEN 1 WHEN status = 'AUSENTE' THEN 2 ELSE 3 END ASC, " +
                "ultimo_ping DESC";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                TerminalSessao s = mapearResultSet(rs);
                if (s.getIp() != null) {
                    try {
                        java.net.InetAddress addr = java.net.InetAddress.getByName(s.getIp());
                        if (com.loja.util.NetworkUtils.isTailscale(addr)) continue;
                    } catch (Exception ignored) {}
                }
                lista.add(s);
            }
        } catch (SQLException e) {
            System.err.println("[TERMINAL DAO] Erro ao listar terminais: " + e.getMessage());
        }
        return lista;
    }

    public boolean enviarComando(String sessaoId, String comando) {
        String sql = "UPDATE terminal_sessao SET comando_pendente = ?, comando_data = NOW() WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, comando);
            stmt.setString(2, sessaoId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[TERMINAL DAO] Erro ao enviar comando: " + e.getMessage());
            return false;
        }
    }

    public boolean limparComando(String sessaoId) {
        String sql = "UPDATE terminal_sessao SET comando_pendente = NULL, comando_data = NULL WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sessaoId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[TERMINAL DAO] Erro ao limpar comando: " + e.getMessage());
            return false;
        }
    }

    public boolean alternarBloqueio(String sessaoId, boolean bloquear) {
        String sql = "UPDATE terminal_sessao SET bloqueado = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, bloquear);
            stmt.setString(2, sessaoId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[TERMINAL DAO] Erro ao alternar bloqueio: " + e.getMessage());
            return false;
        }
    }

    public boolean enviarBroadcast(String remetente, String mensagem) {
        String sql = "INSERT INTO terminal_broadcast (remetente, mensagem, data_envio) VALUES (?, ?, NOW())";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, remetente != null ? remetente : "Administrador");
            stmt.setString(2, mensagem);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[TERMINAL DAO] Erro ao enviar broadcast: " + e.getMessage());
            return false;
        }
    }

    public int obterUltimoBroadcastId() {
        String sql = "SELECT COALESCE(MAX(id), 0) FROM terminal_broadcast";
        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[TERMINAL DAO] Erro ao obter último ID de broadcast: " + e.getMessage());
        }
        return 0;
    }

    public List<TerminalBroadcast> buscarBroadcastsApos(int ultimoId) {
        List<TerminalBroadcast> lista = new ArrayList<>();
        String sql = "SELECT * FROM terminal_broadcast WHERE id > ? ORDER BY id ASC";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ultimoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TerminalBroadcast b = new TerminalBroadcast();
                    b.setId(rs.getInt("id"));
                    b.setRemetente(rs.getString("remetente"));
                    b.setMensagem(rs.getString("mensagem"));
                    Timestamp ts = rs.getTimestamp("data_envio");
                    if (ts != null) b.setDataEnvio(ts.toLocalDateTime());
                    lista.add(b);
                }
            }
        } catch (SQLException e) {
            System.err.println("[TERMINAL DAO] Erro ao buscar broadcasts: " + e.getMessage());
        }
        return lista;
    }

    public int limparTerminaisInativos(int horasLimite) {
        String sql = "DELETE FROM terminal_sessao WHERE status = 'OFFLINE' AND ultimo_ping < DATE_SUB(NOW(), INTERVAL ? HOUR)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, horasLimite);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[TERMINAL DAO] Erro ao limpar terminais inativos: " + e.getMessage());
            return 0;
        }
    }

    private TerminalSessao mapearResultSet(ResultSet rs) throws SQLException {
        TerminalSessao s = new TerminalSessao();
        s.setId(rs.getString("id"));
        s.setHostname(rs.getString("hostname"));
        s.setIp(rs.getString("ip"));
        s.setSistemaOperacional(rs.getString("sistema_operacional"));
        s.setVersaoSistema(rs.getString("versao_sistema"));
        int uid = rs.getInt("usuario_id");
        s.setUsuarioId(rs.wasNull() ? null : uid);
        s.setUsuarioNome(rs.getString("usuario_nome"));
        s.setUsuarioPerfil(rs.getString("usuario_perfil"));
        s.setTelaAtual(rs.getString("tela_atual"));

        Timestamp tLogin = rs.getTimestamp("hora_login");
        if (tLogin != null) s.setHoraLogin(tLogin.toLocalDateTime());

        Timestamp tPing = rs.getTimestamp("ultimo_ping");
        if (tPing != null) s.setUltimoPing(tPing.toLocalDateTime());

        s.setStatus(rs.getString("status"));
        s.setBloqueado(rs.getBoolean("bloqueado"));
        s.setComandoPendente(rs.getString("comando_pendente"));

        Timestamp tCmd = rs.getTimestamp("comando_data");
        if (tCmd != null) s.setComandoData(tCmd.toLocalDateTime());

        return s;
    }
}
