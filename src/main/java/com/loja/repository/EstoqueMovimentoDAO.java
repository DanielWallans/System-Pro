package com.loja.repository;

import com.loja.model.EstoqueMovimento;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EstoqueMovimentoDAO {

    private static final String SQL_INSERT = 
            "INSERT INTO estoque_movimento (produto_id, nome_produto, tipo, quantidade, " +
            "saldo_anterior, saldo_posterior, motivo, documento_ref, custo_unitario, " +
            "usuario, observacoes, data_hora) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /**
     * Registra uma Entrada de Mercadoria no Estoque e atualiza o saldo do produto.
     */
    public boolean registrarEntrada(int produtoId, int quantidade, Double novoCusto, 
                                    String motivo, String docRef, String usuario, String obs) {
        if (produtoId <= 0 || quantidade <= 0) return false;

        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Obter dados atuais do produto
                String nomeProduto = "Produto #" + produtoId;
                int saldoAnterior = 0;
                String sqlGet = "SELECT nome, estoque FROM produto WHERE id = ? FOR UPDATE";
                try (PreparedStatement stmtGet = conn.prepareStatement(sqlGet)) {
                    stmtGet.setInt(1, produtoId);
                    try (ResultSet rs = stmtGet.executeQuery()) {
                        if (rs.next()) {
                            nomeProduto = rs.getString("nome");
                            saldoAnterior = rs.getInt("estoque");
                        } else {
                            conn.rollback();
                            return false;
                        }
                    }
                }

                int saldoPosterior = saldoAnterior + quantidade;

                // 2. Atualizar saldo do produto (e opcionalmente preço de custo)
                if (novoCusto != null && novoCusto > 0) {
                    String sqlUpd = "UPDATE produto SET estoque = ?, preco_custo = ? WHERE id = ?";
                    try (PreparedStatement stmtUpd = conn.prepareStatement(sqlUpd)) {
                        stmtUpd.setInt(1, saldoPosterior);
                        stmtUpd.setDouble(2, novoCusto);
                        stmtUpd.setInt(3, produtoId);
                        stmtUpd.executeUpdate();
                    }
                } else {
                    String sqlUpd = "UPDATE produto SET estoque = ? WHERE id = ?";
                    try (PreparedStatement stmtUpd = conn.prepareStatement(sqlUpd)) {
                        stmtUpd.setInt(1, saldoPosterior);
                        stmtUpd.setInt(2, produtoId);
                        stmtUpd.executeUpdate();
                    }
                }

                // 3. Gravar histórico logístico
                try (PreparedStatement stmtIns = conn.prepareStatement(SQL_INSERT)) {
                    stmtIns.setInt(1, produtoId);
                    stmtIns.setString(2, nomeProduto);
                    stmtIns.setString(3, "ENTRADA");
                    stmtIns.setInt(4, quantidade);
                    stmtIns.setInt(5, saldoAnterior);
                    stmtIns.setInt(6, saldoPosterior);
                    stmtIns.setString(7, motivo != null ? motivo : "COMPRA_FORNECEDOR");
                    stmtIns.setString(8, docRef);
                    if (novoCusto != null) stmtIns.setDouble(9, novoCusto); else stmtIns.setNull(9, Types.DOUBLE);
                    stmtIns.setString(10, usuario != null ? usuario : "Sistema");
                    stmtIns.setString(11, obs);
                    stmtIns.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
                    stmtIns.executeUpdate();
                }

                conn.commit();
                System.out.println("[LOGISTICA] Entrada de " + quantidade + " un do item #" + produtoId + " registrada com sucesso.");
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao registrar entrada de estoque: " + e.getMessage());
            return false;
        }
    }

    /**
     * Registra uma Saída Avulsa de Mercadoria do Estoque (Bancada, Avaria, Devolução, etc.).
     */
    public boolean registrarSaida(int produtoId, int quantidade, String motivo, 
                                  String docRef, String usuario, String obs) {
        if (produtoId <= 0 || quantidade <= 0) return false;

        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Obter dados atuais do produto
                String nomeProduto = "Produto #" + produtoId;
                int saldoAnterior = 0;
                String sqlGet = "SELECT nome, estoque FROM produto WHERE id = ? FOR UPDATE";
                try (PreparedStatement stmtGet = conn.prepareStatement(sqlGet)) {
                    stmtGet.setInt(1, produtoId);
                    try (ResultSet rs = stmtGet.executeQuery()) {
                        if (rs.next()) {
                            nomeProduto = rs.getString("nome");
                            saldoAnterior = rs.getInt("estoque");
                        } else {
                            conn.rollback();
                            return false;
                        }
                    }
                }

                int saldoPosterior = saldoAnterior - quantidade;

                // 2. Atualizar saldo do produto
                String sqlUpd = "UPDATE produto SET estoque = ? WHERE id = ?";
                try (PreparedStatement stmtUpd = conn.prepareStatement(sqlUpd)) {
                    stmtUpd.setInt(1, saldoPosterior);
                    stmtUpd.setInt(2, produtoId);
                    stmtUpd.executeUpdate();
                }

                // 3. Gravar histórico logístico
                try (PreparedStatement stmtIns = conn.prepareStatement(SQL_INSERT)) {
                    stmtIns.setInt(1, produtoId);
                    stmtIns.setString(2, nomeProduto);
                    stmtIns.setString(3, "SAIDA");
                    stmtIns.setInt(4, quantidade);
                    stmtIns.setInt(5, saldoAnterior);
                    stmtIns.setInt(6, saldoPosterior);
                    stmtIns.setString(7, motivo != null ? motivo : "USO_INTERNO_BANCADA");
                    stmtIns.setString(8, docRef);
                    stmtIns.setNull(9, Types.DOUBLE);
                    stmtIns.setString(10, usuario != null ? usuario : "Sistema");
                    stmtIns.setString(11, obs);
                    stmtIns.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
                    stmtIns.executeUpdate();
                }

                conn.commit();
                System.out.println("[LOGISTICA] Saída de " + quantidade + " un do item #" + produtoId + " registrada com sucesso.");
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao registrar saída de estoque: " + e.getMessage());
            return false;
        }
    }

    /**
     * Utilizado internamente por transações já abertas (como finalização de venda PDV ou OS).
     */
    public static void registrarMovimentoConexao(Connection conn, int produtoId, String nomeProduto, 
                                                 String tipo, int quantidade, int saldoAnterior, 
                                                 int saldoPosterior, String motivo, String docRef, 
                                                 String usuario, String obs) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
            stmt.setInt(1, produtoId);
            stmt.setString(2, nomeProduto != null ? nomeProduto : "Produto #" + produtoId);
            stmt.setString(3, tipo != null ? tipo : "SAIDA");
            stmt.setInt(4, Math.abs(quantidade));
            stmt.setInt(5, saldoAnterior);
            stmt.setInt(6, saldoPosterior);
            stmt.setString(7, motivo != null ? motivo : "OUTROS");
            stmt.setString(8, docRef);
            stmt.setNull(9, Types.DOUBLE);
            stmt.setString(10, usuario != null ? usuario : "Sistema");
            stmt.setString(11, obs);
            stmt.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
        }
    }

    public List<EstoqueMovimento> listarTodos(int limite) {
        List<EstoqueMovimento> lista = new ArrayList<>();
        String sql = "SELECT * FROM estoque_movimento ORDER BY id DESC LIMIT ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limite > 0 ? limite : 200);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearMovimento(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao listar movimentações de estoque: " + e.getMessage());
        }
        return lista;
    }

    public List<EstoqueMovimento> listarPorProduto(int produtoId) {
        List<EstoqueMovimento> lista = new ArrayList<>();
        String sql = "SELECT * FROM estoque_movimento WHERE produto_id = ? ORDER BY id DESC";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, produtoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearMovimento(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao listar extrato do produto #" + produtoId + ": " + e.getMessage());
        }
        return lista;
    }

    public List<EstoqueMovimento> listarPeriodo(LocalDateTime inicio, LocalDateTime fim, String tipoFiltro) {
        List<EstoqueMovimento> lista = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM estoque_movimento WHERE data_hora BETWEEN ? AND ? ");
        if (tipoFiltro != null && !tipoFiltro.equalsIgnoreCase("TODOS")) {
            sql.append("AND tipo = ? ");
        }
        sql.append("ORDER BY id DESC");

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setTimestamp(1, Timestamp.valueOf(inicio));
            stmt.setTimestamp(2, Timestamp.valueOf(fim));
            if (tipoFiltro != null && !tipoFiltro.equalsIgnoreCase("TODOS")) {
                stmt.setString(3, tipoFiltro.toUpperCase().trim());
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearMovimento(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao listar movimentações do período: " + e.getMessage());
        }
        return lista;
    }

    private EstoqueMovimento mapearMovimento(ResultSet rs) throws SQLException {
        EstoqueMovimento m = new EstoqueMovimento();
        m.setId(rs.getInt("id"));
        m.setProdutoId(rs.getInt("produto_id"));
        m.setNomeProduto(rs.getString("nome_produto"));
        m.setTipo(rs.getString("tipo"));
        m.setQuantidade(rs.getInt("quantidade"));
        m.setSaldoAnterior(rs.getInt("saldo_anterior"));
        m.setSaldoPosterior(rs.getInt("saldo_posterior"));
        m.setMotivo(rs.getString("motivo"));
        m.setDocumentoRef(rs.getString("documento_ref"));

        double c = rs.getDouble("custo_unitario");
        if (!rs.wasNull()) m.setCustoUnitario(c);

        m.setUsuario(rs.getString("usuario"));
        m.setObservacoes(rs.getString("observacoes"));

        Timestamp ts = rs.getTimestamp("data_hora");
        if (ts != null) m.setDataHora(ts.toLocalDateTime());

        return m;
    }
}
