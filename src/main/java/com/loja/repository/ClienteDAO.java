package com.loja.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import com.loja.model.Cliente;

public class ClienteDAO {

    public boolean salvar(Cliente cliente) {
        if (cliente.getId() > 0) {
            String sql = "UPDATE cliente SET nome = ?, cpf_cnpj = ?, telefone = ?, email = ?, endereco = ? WHERE id = ?";
            try (Connection conn = ConnectionFactory.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, cliente.getNome());
                stmt.setString(2, cliente.getCpfCnpj());
                stmt.setString(3, cliente.getTelefone());
                stmt.setString(4, cliente.getEmail());
                stmt.setString(5, cliente.getEndereco());
                stmt.setInt(6, cliente.getId());
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("[ERRO DB] Falha ao atualizar cliente: " + e.getMessage());
                return false;
            }
        } else {
            String sql = "INSERT INTO cliente (nome, cpf_cnpj, telefone, email, endereco) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = ConnectionFactory.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, cliente.getNome());
                stmt.setString(2, cliente.getCpfCnpj());
                stmt.setString(3, cliente.getTelefone());
                stmt.setString(4, cliente.getEmail());
                stmt.setString(5, cliente.getEndereco());
                int rows = stmt.executeUpdate();
                if (rows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            cliente.setId(generatedKeys.getInt(1));
                        }
                    }
                    return true;
                }
                return false;
            } catch (SQLException e) {
                System.err.println("[ERRO DB] Falha ao cadastrar cliente: " + e.getMessage());
                return false;
            }
        }
    }

    public List<Cliente> buscarTodos() {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT * FROM cliente ORDER BY nome ASC";
        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                clientes.add(new Cliente(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getString("cpf_cnpj"),
                        rs.getString("telefone"),
                        rs.getString("email"),
                        rs.getString("endereco")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao buscar clientes: " + e.getMessage());
        }
        return clientes;
    }

    public Cliente buscarPorId(int id) {
        String sql = "SELECT * FROM cliente WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Cliente(
                            rs.getInt("id"),
                            rs.getString("nome"),
                            rs.getString("cpf_cnpj"),
                            rs.getString("telefone"),
                            rs.getString("email"),
                            rs.getString("endereco")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao buscar cliente: " + e.getMessage());
        }
        return null;
    }

    public Cliente buscarPorCpf(String cpf) {
        if (cpf == null || cpf.trim().isEmpty()) return null;
        String sql = "SELECT * FROM cliente WHERE cpf_cnpj = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cpf.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Cliente(
                            rs.getInt("id"),
                            rs.getString("nome"),
                            rs.getString("cpf_cnpj"),
                            rs.getString("telefone"),
                            rs.getString("email"),
                            rs.getString("endereco")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao buscar cliente por CPF/CNPJ: " + e.getMessage());
        }
        return null;
    }

    public List<Cliente> buscarPorNome(String termo) {
        List<Cliente> lista = new ArrayList<>();
        if (termo == null || termo.trim().isEmpty()) return lista;
        String sql = "SELECT * FROM cliente WHERE LOWER(nome) LIKE ? ORDER BY nome ASC";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + termo.trim().toLowerCase() + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Cliente(
                            rs.getInt("id"),
                            rs.getString("nome"),
                            rs.getString("cpf_cnpj"),
                            rs.getString("telefone"),
                            rs.getString("email"),
                            rs.getString("endereco")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao buscar clientes por nome: " + e.getMessage());
        }
        return lista;
    }

    public int contarEquipamentosVinculados(int clienteId) {
        String sql = "SELECT COUNT(*) FROM equipamento WHERE cliente_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clienteId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao contar equipamentos do cliente #" + clienteId + ": " + e.getMessage());
        }
        return 0;
    }

    public int contarOSVinculadas(int clienteId) {
        String sql = "SELECT COUNT(*) FROM ordem_servico WHERE cliente_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clienteId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao contar OS do cliente #" + clienteId + ": " + e.getMessage());
        }
        return 0;
    }

    public boolean excluir(int clienteId) {
        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Obter e excluir todas as OS vinculadas a este cliente
                List<Integer> osIds = new ArrayList<>();
                try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM ordem_servico WHERE cliente_id = ?")) {
                    stmt.setInt(1, clienteId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            osIds.add(rs.getInt("id"));
                        }
                    }
                }
                for (int osId : osIds) {
                    OrdemServicoDAO.excluirOSComDependencias(conn, osId);
                }

                // 2. Excluir todos os equipamentos vinculados a este cliente
                List<Integer> equipIds = new ArrayList<>();
                try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM equipamento WHERE cliente_id = ?")) {
                    stmt.setInt(1, clienteId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            equipIds.add(rs.getInt("id"));
                        }
                    }
                }
                for (int equipId : equipIds) {
                    EquipamentoDAO.excluirEquipamentoComDependencias(conn, equipId);
                }

                // 3. Desvincular notas fiscais do cliente se houver
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE nota_fiscal SET cliente_id = NULL WHERE cliente_id = ?")) {
                    stmt.setInt(1, clienteId);
                    stmt.executeUpdate();
                }

                // 4. Excluir o cliente
                int deleted = 0;
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM cliente WHERE id = ?")) {
                    stmt.setInt(1, clienteId);
                    deleted = stmt.executeUpdate();
                }

                conn.commit();
                if (deleted > 0) {
                    System.out.println("[DB] Cliente #" + clienteId + " excluído com sucesso.");
                    try {
                        com.loja.service.AutoBackupService.getInstance().notificarAcaoCritica("Exclusão Cliente #" + clienteId);
                    } catch (Exception ignored) {}
                    return true;
                }
                return false;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao excluir cliente #" + clienteId + ": " + e.getMessage());
            return false;
        }
    }
}

