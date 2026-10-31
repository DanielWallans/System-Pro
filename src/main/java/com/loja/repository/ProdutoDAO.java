package com.loja.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.loja.model.Produto;
import com.loja.repository.contract.ProdutoRepository;

public class ProdutoDAO implements ProdutoRepository {

    private static final String SELECT_CAMPOS = "id, codigo_barras, nome, preco_custo, preco, estoque, estoque_minimo, localizacao, ncm, cest, cfop, origem, csosn_cst, unidade";

    public boolean salvar(Produto produto) {
        String sql = "INSERT INTO produto (id, codigo_barras, nome, preco_custo, preco, estoque, estoque_minimo, localizacao, ncm, cest, cfop, origem, csosn_cst, unidade) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE codigo_barras = ?, nome = ?, preco_custo = ?, preco = ?, estoque = ?, " +
                     "estoque_minimo = ?, localizacao = ?, ncm = ?, cest = ?, cfop = ?, origem = ?, csosn_cst = ?, unidade = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // INSERT
            stmt.setInt(1, produto.getId());
            stmt.setString(2, produto.getCodigoBarras());
            stmt.setString(3, produto.getNome());
            stmt.setDouble(4, produto.getPrecoCusto());
            stmt.setDouble(5, produto.getPreco());
            stmt.setInt(6, produto.getEstoque());
            stmt.setInt(7, produto.getEstoqueMinimo());
            stmt.setString(8, produto.getLocalizacao() != null ? produto.getLocalizacao() : "");
            stmt.setString(9, produto.getNcm() != null ? produto.getNcm() : "");
            stmt.setString(10, produto.getCest() != null ? produto.getCest() : "");
            stmt.setString(11, produto.getCfop() != null ? produto.getCfop() : "5102");
            stmt.setInt(12, produto.getOrigem());
            stmt.setString(13, produto.getCsosnCst() != null ? produto.getCsosnCst() : "102");
            stmt.setString(14, produto.getUnidade() != null ? produto.getUnidade() : "UN");

            // UPDATE
            stmt.setString(15, produto.getCodigoBarras());
            stmt.setString(16, produto.getNome());
            stmt.setDouble(17, produto.getPrecoCusto());
            stmt.setDouble(18, produto.getPreco());
            stmt.setInt(19, produto.getEstoque());
            stmt.setInt(20, produto.getEstoqueMinimo());
            stmt.setString(21, produto.getLocalizacao() != null ? produto.getLocalizacao() : "");
            stmt.setString(22, produto.getNcm() != null ? produto.getNcm() : "");
            stmt.setString(23, produto.getCest() != null ? produto.getCest() : "");
            stmt.setString(24, produto.getCfop() != null ? produto.getCfop() : "5102");
            stmt.setInt(25, produto.getOrigem());
            stmt.setString(26, produto.getCsosnCst() != null ? produto.getCsosnCst() : "102");
            stmt.setString(27, produto.getUnidade() != null ? produto.getUnidade() : "UN");

            stmt.executeUpdate();
            System.out.println("[DB] Produto salvo/atualizado (fiscal/logístico): " + produto.getNome());
            return true;

        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao salvar produto: " + e.getMessage());
            return false;
        }
    }

    public List<Produto> buscarTodos() {
        List<Produto> produtos = new ArrayList<>();
        String sql = "SELECT " + SELECT_CAMPOS + " FROM produto ORDER BY nome ASC";

        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                produtos.add(mapearProduto(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao listar produtos: " + e.getMessage());
        }
        return produtos;
    }

    public List<Produto> buscarProdutosComEstoqueZerado() {
        List<Produto> produtos = new ArrayList<>();
        String sql = "SELECT " + SELECT_CAMPOS + " FROM produto WHERE estoque <= 0 ORDER BY nome ASC";
        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                produtos.add(mapearProduto(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao buscar produtos com estoque zerado: " + e.getMessage());
        }
        return produtos;
    }

    public Produto buscarPorId(int id) {
        String sql = "SELECT " + SELECT_CAMPOS + " FROM produto WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearProduto(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao buscar produto por ID: " + e.getMessage());
        }
        return null;
    }

    public Produto buscarPorCodigoOuId(String termo) {
        if (termo == null || termo.trim().isEmpty()) return null;
        termo = termo.trim();

        // 1. Busca por código de barras exato
        String sqlCod = "SELECT " + SELECT_CAMPOS + " FROM produto WHERE codigo_barras = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlCod)) {
            stmt.setString(1, termo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearProduto(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao buscar produto por código de barras: " + e.getMessage());
        }

        // 2. Busca por ID numérico se for número
        try {
            int id = Integer.parseInt(termo);
            Produto p = buscarPorId(id);
            if (p != null) return p;
        } catch (NumberFormatException ignored) {}

        // 3. Busca aproximada por nome
        String sqlNome = "SELECT " + SELECT_CAMPOS + " FROM produto WHERE LOWER(nome) LIKE ? LIMIT 1";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sqlNome)) {
            stmt.setString(1, "%" + termo.toLowerCase() + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapearProduto(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao buscar produto por nome aproximado: " + e.getMessage());
        }

        return null;
    }

    public List<Produto> buscarPorTermo(String termo) {
        List<Produto> lista = new ArrayList<>();
        if (termo == null) termo = "";
        String sql = "SELECT " + SELECT_CAMPOS + " FROM produto " +
                     "WHERE LOWER(nome) LIKE ? OR codigo_barras LIKE ? ORDER BY nome ASC LIMIT 50";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + termo.toLowerCase() + "%");
            stmt.setString(2, "%" + termo + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearProduto(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao buscar produtos por termo: " + e.getMessage());
        }
        return lista;
    }

    public int obterProximoId() {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 AS proximo_id FROM produto";
        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("proximo_id");
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao obter próximo ID: " + e.getMessage());
        }
        return 1;
    }

    private Produto mapearProduto(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String codigoBarras = rs.getString("codigo_barras");
        String nome = rs.getString("nome");
        double precoCusto = 0.0;
        try { precoCusto = rs.getDouble("preco_custo"); } catch (SQLException ignored) {}
        double preco = rs.getDouble("preco");
        int estoque = rs.getInt("estoque");
        int estoqueMinimo = 2;
        try { estoqueMinimo = rs.getInt("estoque_minimo"); } catch (SQLException ignored) {}
        String localizacao = "";
        try {
            String loc = rs.getString("localizacao");
            if (loc != null) localizacao = loc.trim();
        } catch (SQLException ignored) {}

        String ncm = "";
        try { ncm = rs.getString("ncm"); } catch (SQLException ignored) {}
        String cest = "";
        try { cest = rs.getString("cest"); } catch (SQLException ignored) {}
        String cfop = "5102";
        try { 
            String f = rs.getString("cfop");
            if (f != null && !f.trim().isEmpty()) cfop = f.trim();
        } catch (SQLException ignored) {}
        int origem = 0;
        try { origem = rs.getInt("origem"); } catch (SQLException ignored) {}
        String csosn = "102";
        try { 
            String c = rs.getString("csosn_cst");
            if (c != null && !c.trim().isEmpty()) csosn = c.trim();
        } catch (SQLException ignored) {}
        String unidade = "UN";
        try { 
            String u = rs.getString("unidade");
            if (u != null && !u.trim().isEmpty()) unidade = u.trim();
        } catch (SQLException ignored) {}

        return new Produto(id, codigoBarras, nome, precoCusto, preco, estoque, estoqueMinimo, localizacao, ncm, cest, cfop, origem, csosn, unidade);
    }

    public List<Produto> buscarProdutosCriticosOuEmFalta() {
        List<Produto> produtos = new ArrayList<>();
        String sql = "SELECT " + SELECT_CAMPOS + " FROM produto WHERE estoque <= estoque_minimo OR estoque <= 0 ORDER BY estoque ASC, nome ASC";
        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                produtos.add(mapearProduto(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao buscar produtos críticos/em falta: " + e.getMessage());
        }
        return produtos;
    }

    public int contarProdutosCriticosOuEmFalta() {
        String sql = "SELECT COUNT(*) FROM produto WHERE estoque <= estoque_minimo OR estoque <= 0";
        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao contar produtos críticos: " + e.getMessage());
        }
        return 0;
    }

    public boolean excluir(int produtoId) {
        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Desvincular de os_pecas para não quebrar integridade lógica
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE os_pecas SET produto_id = NULL WHERE produto_id = ?")) {
                    stmt.setInt(1, produtoId);
                    stmt.executeUpdate();
                }
                // 2. Desvincular de venda_pdv_itens (coluna NOT NULL -> seta 0)
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE venda_pdv_itens SET produto_id = 0 WHERE produto_id = ?")) {
                    stmt.setInt(1, produtoId);
                    stmt.executeUpdate();
                }
                // 3. Excluir produto
                int deleted = 0;
                try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM produto WHERE id = ?")) {
                    stmt.setInt(1, produtoId);
                    deleted = stmt.executeUpdate();
                }
                conn.commit();
                if (deleted > 0) {
                    System.out.println("[DB] Produto #" + produtoId + " excluído com sucesso.");
                    try {
                        com.loja.service.AutoBackupService.getInstance().notificarAcaoCritica("Exclusão Produto #" + produtoId);
                    } catch (Exception ignored) {}
                    return true;
                }
                return false;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao excluir produto #" + produtoId + ": " + e.getMessage());
            return false;
        }
    }
}