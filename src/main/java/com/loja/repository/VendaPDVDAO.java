package com.loja.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.loja.model.ItemVenda;
import com.loja.model.PagamentoItem;
import com.loja.model.VendaPDV;

public class VendaPDVDAO {

    public boolean finalizarVenda(VendaPDV venda) {
        CaixaDAO caixaDAO = new CaixaDAO();
        com.loja.model.CaixaSessao sessaoAberta = caixaDAO.obterSessaoAberta();
        Integer sessaoId = sessaoAberta != null ? sessaoAberta.getId() : null;

        String sqlVenda = "INSERT INTO venda_pdv (subtotal, desconto, valor_total, valor_liquido, observacoes, sessao_id, data_hora, cliente_id, cliente_nome, cliente_cpf) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlItem = "INSERT INTO venda_pdv_itens (venda_id, produto_id, nome_produto, quantidade, valor_unitario, subtotal) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlBaixaEstoque = "UPDATE produto SET estoque = estoque - ? WHERE id = ?";
        String sqlPagamento = "INSERT INTO venda_pagamento (venda_id, os_id, modalidade, valor_bruto, taxa_percentual, valor_taxa, valor_liquido, parcelas, data_hora) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlMovimentoCaixa = "INSERT INTO caixa_movimento (tipo, modalidade, valor, taxa, valor_liquido, justificativa, venda_id, sessao_id, data_hora) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlUpdateSaldoDinheiro = "UPDATE caixa SET saldo = saldo + ? WHERE id = 1";

        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Gravar Venda Cabeçalho
                int vendaId = 0;
                try (PreparedStatement stmtVenda = conn.prepareStatement(sqlVenda, Statement.RETURN_GENERATED_KEYS)) {
                    stmtVenda.setDouble(1, venda.getSubtotal());
                    stmtVenda.setDouble(2, venda.getDesconto());
                    stmtVenda.setDouble(3, venda.getValorTotal());
                    stmtVenda.setDouble(4, venda.getValorLiquido());
                    stmtVenda.setString(5, venda.getObservacoes());
                    if (sessaoId != null) stmtVenda.setInt(6, sessaoId); else stmtVenda.setNull(6, java.sql.Types.INTEGER);
                    stmtVenda.setTimestamp(7, java.sql.Timestamp.valueOf(venda.getDataHora() != null ? venda.getDataHora() : LocalDateTime.now()));
                    if (venda.getClienteId() != null && venda.getClienteId() > 0) stmtVenda.setInt(8, venda.getClienteId()); else stmtVenda.setNull(8, java.sql.Types.INTEGER);
                    stmtVenda.setString(9, venda.getClienteNome());
                    stmtVenda.setString(10, venda.getClienteCpf());
                    stmtVenda.executeUpdate();

                    try (ResultSet rs = stmtVenda.getGeneratedKeys()) {
                        if (rs.next()) {
                            vendaId = rs.getInt(1);
                            venda.setId(vendaId);
                        }
                    }
                }

                // 2. Gravar Itens e Dar Baixa no Estoque com Rastreabilidade Logística
                String sqlGetEstoque = "SELECT estoque FROM produto WHERE id = ?";
                try (PreparedStatement stmtItem = conn.prepareStatement(sqlItem);
                     PreparedStatement stmtBaixa = conn.prepareStatement(sqlBaixaEstoque);
                     PreparedStatement stmtGet = conn.prepareStatement(sqlGetEstoque)) {
                    for (ItemVenda item : venda.getItens()) {
                        stmtItem.setInt(1, vendaId);
                        stmtItem.setInt(2, item.getProdutoId());
                        stmtItem.setString(3, item.getNomeProduto());
                        stmtItem.setInt(4, item.getQuantidade());
                        stmtItem.setDouble(5, item.getValorUnitario());
                        stmtItem.setDouble(6, item.getSubtotal());
                        stmtItem.addBatch();

                        if (item.getProdutoId() > 0) {
                            int saldoAnt = 0;
                            stmtGet.setInt(1, item.getProdutoId());
                            try (ResultSet rs = stmtGet.executeQuery()) {
                                if (rs.next()) saldoAnt = rs.getInt("estoque");
                            }
                            int saldoPost = saldoAnt - item.getQuantidade();

                            stmtBaixa.setInt(1, item.getQuantidade());
                            stmtBaixa.setInt(2, item.getProdutoId());
                            stmtBaixa.addBatch();

                            EstoqueMovimentoDAO.registrarMovimentoConexao(conn,
                                    item.getProdutoId(),
                                    item.getNomeProduto(),
                                    "SAIDA",
                                    item.getQuantidade(),
                                    saldoAnt,
                                    saldoPost,
                                    "VENDA_PDV",
                                    "Venda #" + vendaId,
                                    (sessaoAberta != null ? sessaoAberta.getOperadorAbertura() : "Caixa Balcão"),
                                    "Venda de balcão concluída");
                        }
                    }
                    stmtItem.executeBatch();
                    stmtBaixa.executeBatch();
                }

                // 3. Gravar Pagamentos e Movimentações Financeiras
                double totalDinheiro = 0.0;
                try (PreparedStatement stmtPag = conn.prepareStatement(sqlPagamento);
                     PreparedStatement stmtMov = conn.prepareStatement(sqlMovimentoCaixa)) {
                    for (PagamentoItem pag : venda.getPagamentos()) {
                        stmtPag.setInt(1, vendaId);
                        stmtPag.setNull(2, java.sql.Types.INTEGER);
                        stmtPag.setString(3, pag.getModalidade());
                        stmtPag.setDouble(4, pag.getValorBruto());
                        stmtPag.setDouble(5, pag.getTaxaPercentual());
                        stmtPag.setDouble(6, pag.getValorTaxa());
                        stmtPag.setDouble(7, pag.getValorLiquido());
                        stmtPag.setInt(8, pag.getParcelas());
                        stmtPag.setTimestamp(9, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                        stmtPag.addBatch();

                        stmtMov.setString(1, "VENDA_PDV");
                        stmtMov.setString(2, pag.getModalidade());
                        stmtMov.setDouble(3, pag.getValorBruto());
                        stmtMov.setDouble(4, pag.getValorTaxa());
                        stmtMov.setDouble(5, pag.getValorLiquido());
                        stmtMov.setString(6, "Venda Balcão PDV #" + vendaId + (pag.getParcelas() > 1 ? " (" + pag.getParcelas() + "x)" : ""));
                        stmtMov.setInt(7, vendaId);
                        if (sessaoId != null) stmtMov.setInt(8, sessaoId); else stmtMov.setNull(8, java.sql.Types.INTEGER);
                        stmtMov.setTimestamp(9, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                        stmtMov.addBatch();

                        if ("DINHEIRO".equalsIgnoreCase(pag.getModalidade())) {
                            totalDinheiro += pag.getValorBruto();
                        }
                    }
                    stmtPag.executeBatch();
                    stmtMov.executeBatch();
                }

                // 4. Se houver pagamento em dinheiro, incrementa o saldo físico do caixa
                if (totalDinheiro > 0) {
                    try (PreparedStatement stmtSaldo = conn.prepareStatement(sqlUpdateSaldoDinheiro)) {
                        stmtSaldo.setDouble(1, totalDinheiro);
                        stmtSaldo.executeUpdate();
                    }
                }

                conn.commit();
                System.out.println("[DB] Venda PDV #" + vendaId + " finalizada com sucesso. Total: R$ " + venda.getValorTotal());
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao finalizar venda PDV: " + e.getMessage());
            return false;
        }
    }

    public List<VendaPDV> listarVendasHoje() {
        return listarVendasPeriodo(LocalDate.now().atStartOfDay(), LocalDate.now().atTime(23, 59, 59));
    }

    public List<VendaPDV> listarVendasPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        List<VendaPDV> lista = new ArrayList<>();
        String sql = "SELECT id, subtotal, desconto, valor_total, valor_liquido, observacoes, data_hora, cliente_id, cliente_nome, cliente_cpf FROM venda_pdv " +
                     "WHERE data_hora BETWEEN ? AND ? ORDER BY id DESC";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, java.sql.Timestamp.valueOf(inicio));
            stmt.setTimestamp(2, java.sql.Timestamp.valueOf(fim));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp("data_hora");
                    VendaPDV v = new VendaPDV(
                            rs.getInt("id"),
                            rs.getDouble("subtotal"),
                            rs.getDouble("desconto"),
                            rs.getDouble("valor_total"),
                            rs.getDouble("valor_liquido"),
                            rs.getString("observacoes"),
                            ts != null ? ts.toLocalDateTime() : LocalDateTime.now()
                    );
                    int cId = rs.getInt("cliente_id");
                    if (!rs.wasNull()) v.setClienteId(cId);
                    v.setClienteNome(rs.getString("cliente_nome"));
                    v.setClienteCpf(rs.getString("cliente_cpf"));
                    lista.add(v);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao listar vendas do período: " + e.getMessage());
        }
        return lista;
    }

    public VendaPDV buscarPorId(int id) {
        String sql = "SELECT id, subtotal, desconto, valor_total, valor_liquido, observacoes, data_hora, cliente_id, cliente_nome, cliente_cpf FROM venda_pdv WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp("data_hora");
                    VendaPDV v = new VendaPDV(
                            rs.getInt("id"),
                            rs.getDouble("subtotal"),
                            rs.getDouble("desconto"),
                            rs.getDouble("valor_total"),
                            rs.getDouble("valor_liquido"),
                            rs.getString("observacoes"),
                            ts != null ? ts.toLocalDateTime() : LocalDateTime.now()
                    );
                    int cId = rs.getInt("cliente_id");
                    if (!rs.wasNull()) v.setClienteId(cId);
                    v.setClienteNome(rs.getString("cliente_nome"));
                    v.setClienteCpf(rs.getString("cliente_cpf"));
                    v.setItens(obterItensVenda(id));
                    v.setPagamentos(obterPagamentosVenda(id));
                    return v;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao buscar venda por ID #" + id + ": " + e.getMessage());
        }
        return null;
    }

    public List<VendaPDV> listarVendasPorCpf(String cpf) {
        List<VendaPDV> lista = new ArrayList<>();
        if (cpf == null || cpf.trim().isEmpty()) return lista;
        String limpo = cpf.replaceAll("[^0-9]", "");
        String sql = "SELECT id, subtotal, desconto, valor_total, valor_liquido, observacoes, data_hora, cliente_id, cliente_nome, cliente_cpf FROM venda_pdv " +
                     "WHERE REPLACE(REPLACE(REPLACE(cliente_cpf, '.', ''), '-', ''), '/', '') = ? OR cliente_cpf = ? ORDER BY id DESC";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, limpo.isEmpty() ? cpf.trim() : limpo);
            stmt.setString(2, cpf.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp("data_hora");
                    VendaPDV v = new VendaPDV(
                            rs.getInt("id"),
                            rs.getDouble("subtotal"),
                            rs.getDouble("desconto"),
                            rs.getDouble("valor_total"),
                            rs.getDouble("valor_liquido"),
                            rs.getString("observacoes"),
                            ts != null ? ts.toLocalDateTime() : LocalDateTime.now()
                    );
                    int cId = rs.getInt("cliente_id");
                    if (!rs.wasNull()) v.setClienteId(cId);
                    v.setClienteNome(rs.getString("cliente_nome"));
                    v.setClienteCpf(rs.getString("cliente_cpf"));
                    lista.add(v);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao buscar vendas por CPF: " + e.getMessage());
        }
        return lista;
    }

    public List<ItemVenda> obterItensVenda(int vendaId) {
        List<ItemVenda> itens = new ArrayList<>();
        String sql = "SELECT id, venda_id, produto_id, nome_produto, quantidade, valor_unitario, subtotal FROM venda_pdv_itens WHERE venda_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vendaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    itens.add(new ItemVenda(
                            rs.getInt("id"),
                            rs.getInt("venda_id"),
                            rs.getInt("produto_id"),
                            rs.getString("nome_produto"),
                            rs.getInt("quantidade"),
                            rs.getDouble("valor_unitario"),
                            rs.getDouble("subtotal")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao obter itens da venda #" + vendaId + ": " + e.getMessage());
        }
        return itens;
    }

    public List<PagamentoItem> obterPagamentosVenda(int vendaId) {
        List<PagamentoItem> pags = new ArrayList<>();
        String sql = "SELECT id, venda_id, os_id, modalidade, valor_bruto, taxa_percentual, valor_taxa, valor_liquido, parcelas, data_hora FROM venda_pagamento WHERE venda_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, vendaId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp("data_hora");
                    pags.add(new PagamentoItem(
                            rs.getInt("id"),
                            rs.getInt("venda_id"),
                            null,
                            rs.getString("modalidade"),
                            rs.getDouble("valor_bruto"),
                            rs.getDouble("taxa_percentual"),
                            rs.getInt("parcelas"),
                            ts != null ? ts.toLocalDateTime() : LocalDateTime.now()
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao obter pagamentos da venda #" + vendaId + ": " + e.getMessage());
        }
        return pags;
    }
}
