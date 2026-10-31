package com.loja.repository;

import com.loja.model.PerfilUsuario;
import com.loja.model.Usuario;
import com.loja.repository.contract.UsuarioRepository;
import com.loja.service.SegurancaService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO implements UsuarioRepository {

    public Usuario autenticar(String login, String senhaPlana) {
        String sql = "SELECT id, nome, login, senha_hash, perfil, ativo, data_cadastro FROM usuario WHERE login = ? AND ativo = TRUE";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, login != null ? login.trim() : "");

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String hashBanco = rs.getString("senha_hash");
                    if (SegurancaService.verificarSenha(senhaPlana, hashBanco)) {
                        return extrairUsuario(rs);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao autenticar usuário: " + e.getMessage());
        }
        return null;
    }

    public List<Usuario> listarTodos() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT id, nome, login, senha_hash, perfil, ativo, data_cadastro FROM usuario ORDER BY id ASC";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(extrairUsuario(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao listar usuários: " + e.getMessage());
        }
        return lista;
    }

    public Usuario buscarPorId(int id) {
        String sql = "SELECT id, nome, login, senha_hash, perfil, ativo, data_cadastro FROM usuario WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extrairUsuario(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao buscar usuário por ID: " + e.getMessage());
        }
        return null;
    }

    public boolean salvar(Usuario u, String senhaPlana) {
        String sql = "INSERT INTO usuario (nome, login, senha_hash, perfil, ativo) VALUES (?, ?, ?, ?, ?)";
        String hash = SegurancaService.gerarHashSenha(senhaPlana);

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, u.getNome());
            stmt.setString(2, u.getLogin().trim().toLowerCase());
            stmt.setString(3, hash);
            stmt.setString(4, u.getPerfil() != null ? u.getPerfil().name() : "ATENDENTE");
            stmt.setBoolean(5, u.isAtivo());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    u.setId(rs.getInt(1));
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao salvar usuário: " + e.getMessage());
            return false;
        }
    }

    public boolean atualizar(Usuario u) {
        String sql = "UPDATE usuario SET nome = ?, login = ?, perfil = ?, ativo = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, u.getNome());
            stmt.setString(2, u.getLogin().trim().toLowerCase());
            stmt.setString(3, u.getPerfil() != null ? u.getPerfil().name() : "ATENDENTE");
            stmt.setBoolean(4, u.isAtivo());
            stmt.setInt(5, u.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao atualizar usuário: " + e.getMessage());
            return false;
        }
    }

    public boolean alterarSenha(int id, String novaSenhaPlana) {
        String sql = "UPDATE usuario SET senha_hash = ? WHERE id = ?";
        String novoHash = SegurancaService.gerarHashSenha(novaSenhaPlana);

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoHash);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao alterar senha do usuário #" + id + ": " + e.getMessage());
            return false;
        }
    }

    public boolean alternarStatus(int id, boolean ativo) {
        String sql = "UPDATE usuario SET ativo = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, ativo);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao alterar status do usuário #" + id + ": " + e.getMessage());
            return false;
        }
    }

    public boolean excluir(int id) {
        String sql = "DELETE FROM usuario WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao excluir usuário #" + id + ": " + e.getMessage());
            return false;
        }
    }

    private Usuario extrairUsuario(ResultSet rs) throws SQLException {
        java.sql.Timestamp ts = rs.getTimestamp("data_cadastro");
        return new Usuario(
                rs.getInt("id"),
                rs.getString("nome"),
                rs.getString("login"),
                rs.getString("senha_hash"),
                PerfilUsuario.fromString(rs.getString("perfil")),
                rs.getBoolean("ativo"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
