package com.loja.repository;

import com.loja.model.ContaPagar;
import com.loja.model.Funcionario;
import com.loja.model.FuncionarioDespesa;
import com.loja.model.ResumoDRE;
import com.loja.repository.contract.FinanceiroRepository;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FinanceiroDAO implements FinanceiroRepository {

    // ==========================================
    // 1. GESTÃO DE CONTAS A PAGAR / DESPESAS
    // ==========================================

    public boolean inserirConta(ContaPagar conta) {
        String sql = "INSERT INTO conta_pagar (descricao, categoria, valor, data_vencimento, data_pagamento, status, forma_pagamento, observacoes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, conta.getDescricao());
            stmt.setString(2, conta.getCategoria());
            stmt.setDouble(3, conta.getValor());
            stmt.setDate(4, Date.valueOf(conta.getDataVencimento() != null ? conta.getDataVencimento() : LocalDate.now()));
            if (conta.getDataPagamento() != null) {
                stmt.setDate(5, Date.valueOf(conta.getDataPagamento()));
            } else {
                stmt.setNull(5, Types.DATE);
            }
            stmt.setString(6, conta.getStatus());
            stmt.setString(7, conta.getFormaPagamento());
            stmt.setString(8, conta.getObservacoes());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        conta.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao inserir conta a pagar: " + e.getMessage());
        }
        return false;
    }

    public boolean atualizarConta(ContaPagar conta) {
        String sql = "UPDATE conta_pagar SET descricao = ?, categoria = ?, valor = ?, data_vencimento = ?, " +
                     "data_pagamento = ?, status = ?, forma_pagamento = ?, observacoes = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, conta.getDescricao());
            stmt.setString(2, conta.getCategoria());
            stmt.setDouble(3, conta.getValor());
            stmt.setDate(4, Date.valueOf(conta.getDataVencimento()));
            if (conta.getDataPagamento() != null) {
                stmt.setDate(5, Date.valueOf(conta.getDataPagamento()));
            } else {
                stmt.setNull(5, Types.DATE);
            }
            stmt.setString(6, conta.getStatus());
            stmt.setString(7, conta.getFormaPagamento());
            stmt.setString(8, conta.getObservacoes());
            stmt.setInt(9, conta.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao atualizar conta a pagar #" + conta.getId() + ": " + e.getMessage());
        }
        return false;
    }

    public boolean excluirConta(int id) {
        String sql = "DELETE FROM conta_pagar WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao excluir conta a pagar #" + id + ": " + e.getMessage());
        }
        return false;
    }

    public boolean marcarContaComoPaga(int id, LocalDate dataPagamento, String formaPagamento) {
        String sql = "UPDATE conta_pagar SET status = 'PAGO', data_pagamento = ?, forma_pagamento = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(dataPagamento != null ? dataPagamento : LocalDate.now()));
            stmt.setString(2, formaPagamento != null ? formaPagamento : "DINHEIRO_CAIXA");
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao quitar conta a pagar #" + id + ": " + e.getMessage());
        }
        return false;
    }

    public List<ContaPagar> listarContasPorMes(int ano, int mes) {
        List<ContaPagar> lista = new ArrayList<>();
        LocalDate inicio = LocalDate.of(ano, mes, 1);
        LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());

        String sql = "SELECT id, descricao, categoria, valor, data_vencimento, data_pagamento, status, forma_pagamento, observacoes, data_cadastro " +
                     "FROM conta_pagar WHERE (data_vencimento BETWEEN ? AND ?) OR (data_pagamento BETWEEN ? AND ?) " +
                     "ORDER BY data_vencimento ASC, id ASC";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(inicio));
            stmt.setDate(2, Date.valueOf(fim));
            stmt.setDate(3, Date.valueOf(inicio));
            stmt.setDate(4, Date.valueOf(fim));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(extrairContaResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao listar contas do mês: " + e.getMessage());
        }
        return lista;
    }

    public List<ContaPagar> listarTodasContas() {
        List<ContaPagar> lista = new ArrayList<>();
        String sql = "SELECT id, descricao, categoria, valor, data_vencimento, data_pagamento, status, forma_pagamento, observacoes, data_cadastro " +
                     "FROM conta_pagar ORDER BY data_vencimento DESC, id DESC LIMIT 200";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(extrairContaResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao listar todas as contas: " + e.getMessage());
        }
        return lista;
    }

    private ContaPagar extrairContaResultSet(ResultSet rs) throws SQLException {
        Date dtVenc = rs.getDate("data_vencimento");
        Date dtPag = rs.getDate("data_pagamento");
        Timestamp tsCad = rs.getTimestamp("data_cadastro");

        return new ContaPagar(
                rs.getInt("id"),
                rs.getString("descricao"),
                rs.getString("categoria"),
                rs.getDouble("valor"),
                dtVenc != null ? dtVenc.toLocalDate() : null,
                dtPag != null ? dtPag.toLocalDate() : null,
                rs.getString("status"),
                rs.getString("forma_pagamento"),
                rs.getString("observacoes"),
                tsCad != null ? tsCad.toLocalDateTime() : LocalDateTime.now()
        );
    }

    // ==========================================
    // 2. GESTÃO DE FUNCIONÁRIOS
    // ==========================================

    public boolean inserirFuncionario(Funcionario f) {
        String sql = "INSERT INTO funcionario (nome, cargo, telefone, salario_base, percentual_comissao, ativo) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, f.getNome());
            stmt.setString(2, f.getCargo());
            stmt.setString(3, f.getTelefone());
            stmt.setDouble(4, f.getSalarioBase());
            stmt.setDouble(5, f.getPercentualComissao());
            stmt.setBoolean(6, f.isAtivo());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        f.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao inserir funcionário: " + e.getMessage());
        }
        return false;
    }

    public boolean atualizarFuncionario(Funcionario f) {
        String sql = "UPDATE funcionario SET nome = ?, cargo = ?, telefone = ?, salario_base = ?, " +
                     "percentual_comissao = ?, ativo = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, f.getNome());
            stmt.setString(2, f.getCargo());
            stmt.setString(3, f.getTelefone());
            stmt.setDouble(4, f.getSalarioBase());
            stmt.setDouble(5, f.getPercentualComissao());
            stmt.setBoolean(6, f.isAtivo());
            stmt.setInt(7, f.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao atualizar funcionário #" + f.getId() + ": " + e.getMessage());
        }
        return false;
    }

    public boolean excluirFuncionario(int id) {
        String sql = "DELETE FROM funcionario WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao excluir funcionário #" + id + ": " + e.getMessage());
        }
        return false;
    }

    public List<Funcionario> listarFuncionarios() {
        List<Funcionario> lista = new ArrayList<>();
        String sql = "SELECT id, nome, cargo, telefone, salario_base, percentual_comissao, ativo, data_cadastro " +
                     "FROM funcionario ORDER BY nome ASC";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("data_cadastro");
                lista.add(new Funcionario(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getString("cargo"),
                        rs.getString("telefone"),
                        rs.getDouble("salario_base"),
                        rs.getDouble("percentual_comissao"),
                        rs.getBoolean("ativo"),
                        ts != null ? ts.toLocalDateTime() : LocalDateTime.now()
                ));
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao listar funcionários: " + e.getMessage());
        }
        return lista;
    }

    // ==========================================
    // 3. DESPESAS COM PESSOAL / FOLHA DE PAGAMENTO
    // ==========================================

    public boolean inserirLancamentoPessoal(FuncionarioDespesa fd) {
        String sql = "INSERT INTO funcionario_despesa (funcionario_id, funcionario_nome, cargo, tipo, valor, data_referencia, data_pagamento, status, forma_pagamento, observacoes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (fd.getFuncionarioId() != null) {
                stmt.setInt(1, fd.getFuncionarioId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setString(2, fd.getFuncionarioNome());
            stmt.setString(3, fd.getCargo());
            stmt.setString(4, fd.getTipo());
            stmt.setDouble(5, fd.getValor());
            stmt.setDate(6, Date.valueOf(fd.getDataReferencia() != null ? fd.getDataReferencia() : LocalDate.now()));
            if (fd.getDataPagamento() != null) {
                stmt.setDate(7, Date.valueOf(fd.getDataPagamento()));
            } else {
                stmt.setNull(7, Types.DATE);
            }
            stmt.setString(8, fd.getStatus());
            stmt.setString(9, fd.getFormaPagamento());
            stmt.setString(10, fd.getObservacoes());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        fd.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao lançar despesa de funcionário: " + e.getMessage());
        }
        return false;
    }

    public boolean atualizarLancamentoPessoal(FuncionarioDespesa fd) {
        String sql = "UPDATE funcionario_despesa SET funcionario_id = ?, funcionario_nome = ?, cargo = ?, tipo = ?, " +
                     "valor = ?, data_referencia = ?, data_pagamento = ?, status = ?, forma_pagamento = ?, observacoes = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (fd.getFuncionarioId() != null) {
                stmt.setInt(1, fd.getFuncionarioId());
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setString(2, fd.getFuncionarioNome());
            stmt.setString(3, fd.getCargo());
            stmt.setString(4, fd.getTipo());
            stmt.setDouble(5, fd.getValor());
            stmt.setDate(6, Date.valueOf(fd.getDataReferencia()));
            if (fd.getDataPagamento() != null) {
                stmt.setDate(7, Date.valueOf(fd.getDataPagamento()));
            } else {
                stmt.setNull(7, Types.DATE);
            }
            stmt.setString(8, fd.getStatus());
            stmt.setString(9, fd.getFormaPagamento());
            stmt.setString(10, fd.getObservacoes());
            stmt.setInt(11, fd.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao atualizar despesa de funcionário #" + fd.getId() + ": " + e.getMessage());
        }
        return false;
    }

    public boolean excluirLancamentoPessoal(int id) {
        String sql = "DELETE FROM funcionario_despesa WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao excluir despesa de funcionário #" + id + ": " + e.getMessage());
        }
        return false;
    }

    public boolean marcarLancamentoPessoalComoPago(int id, LocalDate dataPagamento, String formaPagamento) {
        String sql = "UPDATE funcionario_despesa SET status = 'PAGO', data_pagamento = ?, forma_pagamento = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(dataPagamento != null ? dataPagamento : LocalDate.now()));
            stmt.setString(2, formaPagamento != null ? formaPagamento : "DINHEIRO_CAIXA");
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao quitar despesa de funcionário #" + id + ": " + e.getMessage());
        }
        return false;
    }

    public List<FuncionarioDespesa> listarLancamentosPessoalPorMes(int ano, int mes) {
        List<FuncionarioDespesa> lista = new ArrayList<>();
        LocalDate inicio = LocalDate.of(ano, mes, 1);
        LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());

        String sql = "SELECT id, funcionario_id, funcionario_nome, cargo, tipo, valor, data_referencia, data_pagamento, status, forma_pagamento, observacoes, data_cadastro " +
                     "FROM funcionario_despesa WHERE (data_referencia BETWEEN ? AND ?) OR (data_pagamento BETWEEN ? AND ?) " +
                     "ORDER BY data_referencia ASC, id ASC";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(inicio));
            stmt.setDate(2, Date.valueOf(fim));
            stmt.setDate(3, Date.valueOf(inicio));
            stmt.setDate(4, Date.valueOf(fim));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Date dtRef = rs.getDate("data_referencia");
                    Date dtPag = rs.getDate("data_pagamento");
                    Timestamp tsCad = rs.getTimestamp("data_cadastro");

                    Integer funcId = (Integer) rs.getObject("funcionario_id");
                    lista.add(new FuncionarioDespesa(
                            rs.getInt("id"),
                            funcId,
                            rs.getString("funcionario_nome"),
                            rs.getString("cargo"),
                            rs.getString("tipo"),
                            rs.getDouble("valor"),
                            dtRef != null ? dtRef.toLocalDate() : null,
                            dtPag != null ? dtPag.toLocalDate() : null,
                            rs.getString("status"),
                            rs.getString("forma_pagamento"),
                            rs.getString("observacoes"),
                            tsCad != null ? tsCad.toLocalDateTime() : LocalDateTime.now()
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao listar lançamentos de pessoal: " + e.getMessage());
        }
        return lista;
    }

    // ==========================================
    // 4. CONSOLIDAÇÃO DRE & PRESTAÇÃO DE CONTAS
    // ==========================================

    public ResumoDRE calcularDRE(int ano, int mes) {
        ResumoDRE dre = new ResumoDRE(ano, mes);
        LocalDate inicio = LocalDate.of(ano, mes, 1);
        LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());
        Timestamp tsInicio = Timestamp.valueOf(inicio.atStartOfDay());
        Timestamp tsFim = Timestamp.valueOf(fim.atTime(23, 59, 59));

        try (Connection conn = ConnectionFactory.getConnection()) {

            // A) VENDAS PDV DO MÊS (Faturamento e Quantidade)
            String sqlPDV = "SELECT COUNT(*) as qtd, COALESCE(SUM(valor_total), 0.0) as total_pdv FROM venda_pdv WHERE data_hora BETWEEN ? AND ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlPDV)) {
                stmt.setTimestamp(1, tsInicio);
                stmt.setTimestamp(2, tsFim);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dre.setQtdVendasPDV(rs.getInt("qtd"));
                        dre.setFaturamentoPDV(rs.getDouble("total_pdv"));
                    }
                }
            }

            // B) CUSTO DOS PRODUTOS VENDIDOS NO PDV (CMV PDV)
            String sqlCustoPDV = "SELECT COALESCE(SUM(vi.quantidade * COALESCE(p.preco_custo, 0.0)), 0.0) as custo_pdv " +
                                 "FROM venda_pdv_itens vi " +
                                 "JOIN venda_pdv v ON v.id = vi.venda_id " +
                                 "LEFT JOIN produto p ON p.id = vi.produto_id " +
                                 "WHERE v.data_hora BETWEEN ? AND ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlCustoPDV)) {
                stmt.setTimestamp(1, tsInicio);
                stmt.setTimestamp(2, tsFim);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dre.setCustoPecasPDV(rs.getDouble("custo_pdv"));
                    }
                }
            }

            // C) ORDENS DE SERVIÇO CONCLUÍDAS/ENTREGUES OU PAGAS NO MÊS
            // Considera OS com status de entrega/finalização no período ou que tiveram pagamentos registrados no mês
            String sqlOSGeral = "SELECT DISTINCT o.id, o.valor_servico, o.valor_total, o.status, o.data_entrada, o.data_saida " +
                                "FROM ordem_servico o " +
                                "LEFT JOIN venda_pagamento vp ON vp.os_id = o.id " +
                                "LEFT JOIN caixa_movimento cm ON cm.os_id = o.id " +
                                "WHERE ( " +
                                "    ( " +
                                "        (o.status LIKE '%Entregue%' OR o.status LIKE '%Finalizado%' OR o.status LIKE '%Conclu%' OR o.status LIKE '%Pronto%') " +
                                "        AND ( " +
                                "            (o.data_saida IS NOT NULL AND o.data_saida BETWEEN ? AND ?) " +
                                "            OR (o.data_saida IS NULL AND o.data_entrada BETWEEN ? AND ?) " +
                                "        ) " +
                                "    ) " +
                                "    OR (vp.id IS NOT NULL AND vp.data_hora BETWEEN ? AND ?) " +
                                "    OR (cm.id IS NOT NULL AND cm.data_hora BETWEEN ? AND ?) " +
                                ") ORDER BY o.id ASC";

            class OSInfo {
                int id;
                double valorServico;
                double valorTotal;
            }
            Map<Integer, OSInfo> osMap = new LinkedHashMap<>();

            try (PreparedStatement stmt = conn.prepareStatement(sqlOSGeral)) {
                stmt.setTimestamp(1, tsInicio);
                stmt.setTimestamp(2, tsFim);
                stmt.setTimestamp(3, tsInicio);
                stmt.setTimestamp(4, tsFim);
                stmt.setTimestamp(5, tsInicio);
                stmt.setTimestamp(6, tsFim);
                stmt.setTimestamp(7, tsInicio);
                stmt.setTimestamp(8, tsFim);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int osId = rs.getInt("id");
                        if (!osMap.containsKey(osId)) {
                            OSInfo info = new OSInfo();
                            info.id = osId;
                            info.valorServico = rs.getDouble("valor_servico");
                            info.valorTotal = rs.getDouble("valor_total");
                            osMap.put(osId, info);
                        }
                    }
                }
            }
            dre.setQtdOSConcluidas(osMap.size());

            if (!osMap.isEmpty()) {
                StringBuilder placeholders = new StringBuilder();
                List<Integer> idList = new ArrayList<>(osMap.keySet());
                for (int i = 0; i < idList.size(); i++) {
                    placeholders.append(i == 0 ? "?" : ", ?");
                }

                // Soma serviços específicos detalhados da OS
                Map<Integer, Double> servicosDetMap = new HashMap<>();
                String sqlServicosOS = "SELECT os_id, COALESCE(SUM(valor), 0.0) as total_serv FROM os_servicos WHERE os_id IN (" + placeholders + ") GROUP BY os_id";
                try (PreparedStatement stmt = conn.prepareStatement(sqlServicosOS)) {
                    for (int i = 0; i < idList.size(); i++) {
                        stmt.setInt(i + 1, idList.get(i));
                    }
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            servicosDetMap.put(rs.getInt("os_id"), rs.getDouble("total_serv"));
                        }
                    }
                }

                // Peças de OS: Faturamento de Venda e Custo (CMV de Peças de OS)
                Map<Integer, Double> pecasFaturMap = new HashMap<>();
                Map<Integer, Double> pecasCustoMap = new HashMap<>();
                String sqlPecasOS = "SELECT op.os_id, " +
                                    "COALESCE(SUM(op.quantidade * op.valor_unitario), 0.0) as fatur_pecas, " +
                                    "COALESCE(SUM(op.quantidade * COALESCE(p.preco_custo, 0.0)), 0.0) as custo_pecas " +
                                    "FROM os_pecas op " +
                                    "LEFT JOIN produto p ON p.id = op.produto_id " +
                                    "WHERE op.os_id IN (" + placeholders + ") GROUP BY op.os_id";
                try (PreparedStatement stmt = conn.prepareStatement(sqlPecasOS)) {
                    for (int i = 0; i < idList.size(); i++) {
                        stmt.setInt(i + 1, idList.get(i));
                    }
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            int id = rs.getInt("os_id");
                            pecasFaturMap.put(id, rs.getDouble("fatur_pecas"));
                            pecasCustoMap.put(id, rs.getDouble("custo_pecas"));
                        }
                    }
                }

                // Pagamentos registrados da OS em venda_pagamento
                Map<Integer, Double> pagamentosMap = new HashMap<>();
                String sqlPagamentosOS = "SELECT os_id, COALESCE(SUM(valor_bruto), 0.0) as total_pago FROM venda_pagamento WHERE os_id IN (" + placeholders + ") GROUP BY os_id";
                try (PreparedStatement stmt = conn.prepareStatement(sqlPagamentosOS)) {
                    for (int i = 0; i < idList.size(); i++) {
                        stmt.setInt(i + 1, idList.get(i));
                    }
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            pagamentosMap.put(rs.getInt("os_id"), rs.getDouble("total_pago"));
                        }
                    }
                }

                double totalFaturamentoServicos = 0.0;
                double totalFaturamentoPecas = 0.0;
                double totalCustoPecas = 0.0;

                for (OSInfo os : osMap.values()) {
                    double faturPecas = pecasFaturMap.getOrDefault(os.id, 0.0);
                    double custoPecas = pecasCustoMap.getOrDefault(os.id, 0.0);
                    double servDet = servicosDetMap.getOrDefault(os.id, 0.0);
                    double totalPago = pagamentosMap.getOrDefault(os.id, 0.0);

                    double servicoOS = 0.0;
                    if (servDet > 0) {
                        servicoOS = servDet;
                    } else if (os.valorServico > 0) {
                        servicoOS = os.valorServico;
                    } else if (os.valorTotal > faturPecas) {
                        servicoOS = os.valorTotal - faturPecas;
                    } else if (totalPago > faturPecas) {
                        servicoOS = totalPago - faturPecas;
                    } else if (faturPecas == 0.0 && os.valorTotal > 0.0) {
                        servicoOS = os.valorTotal;
                    } else if (faturPecas == 0.0 && totalPago > 0.0) {
                        servicoOS = totalPago;
                    }

                    totalFaturamentoServicos += servicoOS;
                    totalFaturamentoPecas += faturPecas;
                    totalCustoPecas += custoPecas;
                }

                dre.setFaturamentoServicos(totalFaturamentoServicos);
                dre.setFaturamentoPecasOS(totalFaturamentoPecas);
                dre.setCustoPecasOS(totalCustoPecas);
            }

            // D) TAXAS DE CARTÃO / MAQUININHA DESCONTADAS NO PERÍODO
            String sqlTaxas = "SELECT COALESCE(SUM(valor_taxa), 0.0) as total_taxas FROM venda_pagamento WHERE data_hora BETWEEN ? AND ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlTaxas)) {
                stmt.setTimestamp(1, tsInicio);
                stmt.setTimestamp(2, tsFim);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dre.setTotalTaxasCartao(rs.getDouble("total_taxas"));
                    }
                }
            }

            // E) CONTAS A PAGAR / DESPESAS DO MÊS
            List<ContaPagar> contas = listarContasPorMes(ano, mes);
            double despesasPagas = 0.0;
            double despesasPendentes = 0.0;
            List<ContaPagar> listaPagas = new ArrayList<>();

            for (ContaPagar c : contas) {
                if (c.isPaga()) {
                    despesasPagas += c.getValor();
                    listaPagas.add(c);
                } else {
                    despesasPendentes += c.getValor();
                }
            }
            dre.setTotalDespesasPagas(despesasPagas);
            dre.setTotalDespesasPendentes(despesasPendentes);
            dre.setContasPagas(listaPagas);

            // F) DESPESAS COM PESSOAL / FUNCIONÁRIOS DO MÊS
            List<FuncionarioDespesa> pessoal = listarLancamentosPessoalPorMes(ano, mes);
            double pessoalPago = 0.0;
            double pessoalPendente = 0.0;
            List<FuncionarioDespesa> listaPessoalPaga = new ArrayList<>();

            for (FuncionarioDespesa fd : pessoal) {
                if (fd.isPaga()) {
                    pessoalPago += fd.getValor();
                    listaPessoalPaga.add(fd);
                } else {
                    pessoalPendente += fd.getValor();
                }
            }
            dre.setTotalPessoalPago(pessoalPago);
            dre.setTotalPessoalPendente(pessoalPendente);
            dre.setPagamentosPessoal(listaPessoalPaga);

            // G) SANGRIAS AVULSAS DE CAIXA (Exclui as que foram identificadas como contas ou funcionários)
            String sqlSangrias = "SELECT COALESCE(SUM(valor), 0.0) as total_sangrias FROM caixa_movimento " +
                                 "WHERE tipo = 'SANGRIA' AND data_hora BETWEEN ? AND ? " +
                                 "AND justificativa NOT LIKE 'Pagamento de Conta:%' " +
                                 "AND justificativa NOT LIKE 'Pagamento Funcionário:%'";
            try (PreparedStatement stmt = conn.prepareStatement(sqlSangrias)) {
                stmt.setTimestamp(1, tsInicio);
                stmt.setTimestamp(2, tsFim);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        dre.setTotalSangriasAvulsas(rs.getDouble("total_sangrias"));
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao calcular DRE financeiro: " + e.getMessage());
        }

        return dre;
    }
}
