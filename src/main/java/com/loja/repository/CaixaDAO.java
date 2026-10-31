package com.loja.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.loja.model.CaixaMovimento;
import com.loja.model.CaixaSessao;
import com.loja.model.PagamentoItem;
import com.loja.model.ResumoFechamentoCaixa;
import com.loja.repository.contract.CaixaRepository;

public class CaixaDAO implements CaixaRepository {

    public double obterSaldo() {
        String sql = "SELECT saldo FROM caixa WHERE id = 1";
        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble("saldo");
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao obter saldo do caixa: " + e.getMessage());
        }
        return 100.00; // Valor padrão inicial
    }

    public void atualizarSaldo(double novoSaldo) {
        String sql = "UPDATE caixa SET saldo = ? WHERE id = 1";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, novoSaldo);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao atualizar saldo do caixa: " + e.getMessage());
        }
    }

    public CaixaSessao obterSessaoAberta() {
        String sql = "SELECT id, status, data_abertura, data_fechamento, operador_abertura, operador_fechamento, " +
                     "saldo_inicial, saldo_final_sistema, saldo_final_informado, diferenca, justificativa_diferenca, " +
                     "fundo_troco_deixado, sangria_malote, total_dinheiro, total_pix, total_debito_bruto, " +
                     "total_debito_liquido, total_credito_bruto, total_credito_liquido, total_sangrias, " +
                     "total_suprimentos, total_vendas_bruto, total_vendas_liquido, qtd_vendas, " +
                     "contagem_detalhada_texto, observacoes FROM caixa_sessao WHERE status = 'ABERTO' ORDER BY id DESC LIMIT 1";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return extrairSessaoResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao obter sessão aberta de caixa: " + e.getMessage());
        }
        return null;
    }

    public CaixaSessao abrirNovaSessao(String operador, double fundoTrocoInicial) {
        String sqlInsert = "INSERT INTO caixa_sessao (status, data_abertura, operador_abertura, saldo_inicial) VALUES ('ABERTO', ?, ?, ?)";
        String sqlUpdateSaldo = "UPDATE caixa SET saldo = ? WHERE id = 1";
        String sqlMovimento = "INSERT INTO caixa_movimento (tipo, modalidade, valor, taxa, valor_liquido, justificativa, sessao_id, data_hora) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        LocalDateTime agora = LocalDateTime.now();
        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int sessaoId = 0;
                try (PreparedStatement stmt = conn.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setTimestamp(1, java.sql.Timestamp.valueOf(agora));
                    stmt.setString(2, operador != null && !operador.trim().isEmpty() ? operador.trim() : "Operador Padrão");
                    stmt.setDouble(3, fundoTrocoInicial);
                    stmt.executeUpdate();
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            sessaoId = rs.getInt(1);
                        }
                    }
                }

                // Ajusta saldo gaveta para o fundo de troco inicial
                try (PreparedStatement stmtSaldo = conn.prepareStatement(sqlUpdateSaldo)) {
                    stmtSaldo.setDouble(1, fundoTrocoInicial);
                    stmtSaldo.executeUpdate();
                }

                // Registra suprimento inicial vinculado a sessão
                try (PreparedStatement stmtMov = conn.prepareStatement(sqlMovimento)) {
                    stmtMov.setString(1, "SUPRIMENTO");
                    stmtMov.setString(2, "DINHEIRO");
                    stmtMov.setDouble(3, fundoTrocoInicial);
                    stmtMov.setDouble(4, 0.0);
                    stmtMov.setDouble(5, fundoTrocoInicial);
                    stmtMov.setString(6, "Abertura de Caixa (Turno #" + sessaoId + " - Operador: " + operador + ")");
                    stmtMov.setInt(7, sessaoId);
                    stmtMov.setTimestamp(8, java.sql.Timestamp.valueOf(agora));
                    stmtMov.executeUpdate();
                }

                conn.commit();
                System.out.println("[DB] Nova sessão de caixa #" + sessaoId + " aberta com sucesso por " + operador);

                CaixaSessao s = new CaixaSessao();
                s.setId(sessaoId);
                s.setStatus("ABERTO");
                s.setDataAbertura(agora);
                s.setOperadorAbertura(operador);
                s.setSaldoInicial(fundoTrocoInicial);
                return s;

            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao abrir sessão de caixa: " + e.getMessage());
            return null;
        }
    }

    public boolean encerrarSessao(CaixaSessao sessao) {
        String sqlUpdateSessao = "UPDATE caixa_sessao SET status = 'FECHADO', data_fechamento = ?, operador_fechamento = ?, " +
                "saldo_final_sistema = ?, saldo_final_informado = ?, diferenca = ?, justificativa_diferenca = ?, " +
                "fundo_troco_deixado = ?, sangria_malote = ?, total_dinheiro = ?, total_pix = ?, total_debito_bruto = ?, " +
                "total_debito_liquido = ?, total_credito_bruto = ?, total_credito_liquido = ?, total_sangrias = ?, " +
                "total_suprimentos = ?, total_vendas_bruto = ?, total_vendas_liquido = ?, qtd_vendas = ?, " +
                "contagem_detalhada_texto = ?, observacoes = ? WHERE id = ?";

        String sqlSangriaMalote = "INSERT INTO caixa_movimento (tipo, modalidade, valor, taxa, valor_liquido, justificativa, sessao_id, data_hora) " +
                "VALUES ('SANGRIA', 'DINHEIRO', ?, 0.0, ?, ?, ?, ?)";
        String sqlUpdateSaldoGaveta = "UPDATE caixa SET saldo = ? WHERE id = 1";

        LocalDateTime agora = LocalDateTime.now();
        sessao.setDataFechamento(agora);
        sessao.setStatus("FECHADO");

        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Atualiza a sessão
                try (PreparedStatement stmt = conn.prepareStatement(sqlUpdateSessao)) {
                    stmt.setTimestamp(1, java.sql.Timestamp.valueOf(agora));
                    stmt.setString(2, sessao.getOperadorFechamento());
                    stmt.setDouble(3, sessao.getSaldoFinalSistema());
                    stmt.setDouble(4, sessao.getSaldoFinalInformado());
                    stmt.setDouble(5, sessao.getDiferenca());
                    stmt.setString(6, sessao.getJustificativaDiferenca());
                    stmt.setDouble(7, sessao.getFundoTrocoDeixado());
                    stmt.setDouble(8, sessao.getSangriaMalote());
                    stmt.setDouble(9, sessao.getTotalDinheiro());
                    stmt.setDouble(10, sessao.getTotalPix());
                    stmt.setDouble(11, sessao.getTotalDebitoBruto());
                    stmt.setDouble(12, sessao.getTotalDebitoLiquido());
                    stmt.setDouble(13, sessao.getTotalCreditoBruto());
                    stmt.setDouble(14, sessao.getTotalCreditoLiquido());
                    stmt.setDouble(15, sessao.getTotalSangrias());
                    stmt.setDouble(16, sessao.getTotalSuprimentos());
                    stmt.setDouble(17, sessao.getTotalVendasBruto());
                    stmt.setDouble(18, sessao.getTotalVendasLiquido());
                    stmt.setInt(19, sessao.getQtdVendas());
                    stmt.setString(20, sessao.getContagemDetalhadaTexto());
                    stmt.setString(21, sessao.getObservacoes());
                    stmt.setInt(22, sessao.getId());
                    stmt.executeUpdate();
                }

                // 2. Se gerou sangria de malote/cofre, registra movimentação
                if (sessao.getSangriaMalote() > 0) {
                    try (PreparedStatement stmtSangria = conn.prepareStatement(sqlSangriaMalote)) {
                        stmtSangria.setDouble(1, sessao.getSangriaMalote());
                        stmtSangria.setDouble(2, -sessao.getSangriaMalote());
                        stmtSangria.setString(3, "Sangria Automática de Fechamento / Malote para Cofre (Turno #" + sessao.getId() + ")");
                        stmtSangria.setInt(4, sessao.getId());
                        stmtSangria.setTimestamp(5, java.sql.Timestamp.valueOf(agora));
                        stmtSangria.executeUpdate();
                    }
                }

                // 3. Atualiza saldo restante na gaveta física
                try (PreparedStatement stmtSaldo = conn.prepareStatement(sqlUpdateSaldoGaveta)) {
                    stmtSaldo.setDouble(1, sessao.getFundoTrocoDeixado());
                    stmtSaldo.executeUpdate();
                }

                conn.commit();
                System.out.println("[DB] Sessão de caixa #" + sessao.getId() + " encerrada com sucesso!");
                try {
                    com.loja.service.AutoBackupService.getInstance().notificarAcaoCritica("Fechamento de Caixa #" + sessao.getId());
                } catch (Exception ignored) {}
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao encerrar sessão de caixa: " + e.getMessage());
            return false;
        }
    }

    public List<CaixaSessao> listarHistoricoFechamentos() {
        List<CaixaSessao> lista = new ArrayList<>();
        String sql = "SELECT id, status, data_abertura, data_fechamento, operador_abertura, operador_fechamento, " +
                     "saldo_inicial, saldo_final_sistema, saldo_final_informado, diferenca, justificativa_diferenca, " +
                     "fundo_troco_deixado, sangria_malote, total_dinheiro, total_pix, total_debito_bruto, " +
                     "total_debito_liquido, total_credito_bruto, total_credito_liquido, total_sangrias, " +
                     "total_suprimentos, total_vendas_bruto, total_vendas_liquido, qtd_vendas, " +
                     "contagem_detalhada_texto, observacoes FROM caixa_sessao ORDER BY id DESC";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(extrairSessaoResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao listar histórico de fechamentos: " + e.getMessage());
        }
        return lista;
    }

    public CaixaSessao obterSessaoPorId(int id) {
        String sql = "SELECT id, status, data_abertura, data_fechamento, operador_abertura, operador_fechamento, " +
                     "saldo_inicial, saldo_final_sistema, saldo_final_informado, diferenca, justificativa_diferenca, " +
                     "fundo_troco_deixado, sangria_malote, total_dinheiro, total_pix, total_debito_bruto, " +
                     "total_debito_liquido, total_credito_bruto, total_credito_liquido, total_sangrias, " +
                     "total_suprimentos, total_vendas_bruto, total_vendas_liquido, qtd_vendas, " +
                     "contagem_detalhada_texto, observacoes FROM caixa_sessao WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extrairSessaoResultSet(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao buscar sessão por ID: " + e.getMessage());
        }
        return null;
    }

    private CaixaSessao extrairSessaoResultSet(ResultSet rs) throws SQLException {
        CaixaSessao s = new CaixaSessao();
        s.setId(rs.getInt("id"));
        s.setStatus(rs.getString("status"));
        java.sql.Timestamp tsAbertura = rs.getTimestamp("data_abertura");
        if (tsAbertura != null) s.setDataAbertura(tsAbertura.toLocalDateTime());
        java.sql.Timestamp tsFechamento = rs.getTimestamp("data_fechamento");
        if (tsFechamento != null) s.setDataFechamento(tsFechamento.toLocalDateTime());

        s.setOperadorAbertura(rs.getString("operador_abertura"));
        s.setOperadorFechamento(rs.getString("operador_fechamento"));
        s.setSaldoInicial(rs.getDouble("saldo_inicial"));
        s.setSaldoFinalSistema(rs.getDouble("saldo_final_sistema"));
        s.setSaldoFinalInformado(rs.getDouble("saldo_final_informado"));
        s.setDiferenca(rs.getDouble("diferenca"));
        s.setJustificativaDiferenca(rs.getString("justificativa_diferenca"));
        s.setFundoTrocoDeixado(rs.getDouble("fundo_troco_deixado"));
        s.setSangriaMalote(rs.getDouble("sangria_malote"));

        s.setTotalDinheiro(rs.getDouble("total_dinheiro"));
        s.setTotalPix(rs.getDouble("total_pix"));
        s.setTotalDebitoBruto(rs.getDouble("total_debito_bruto"));
        s.setTotalDebitoLiquido(rs.getDouble("total_debito_liquido"));
        s.setTotalCreditoBruto(rs.getDouble("total_credito_bruto"));
        s.setTotalCreditoLiquido(rs.getDouble("total_credito_liquido"));
        s.setTotalSangrias(rs.getDouble("total_sangrias"));
        s.setTotalSuprimentos(rs.getDouble("total_suprimentos"));
        s.setTotalVendasBruto(rs.getDouble("total_vendas_bruto"));
        s.setTotalVendasLiquido(rs.getDouble("total_vendas_liquido"));
        s.setQtdVendas(rs.getInt("qtd_vendas"));
        s.setContagemDetalhadaTexto(rs.getString("contagem_detalhada_texto"));
        s.setObservacoes(rs.getString("observacoes"));
        return s;
    }

    public boolean registrarSangria(double valor, String justificativa) {
        CaixaSessao sessaoAberta = obterSessaoAberta();
        Integer sessaoId = sessaoAberta != null ? sessaoAberta.getId() : null;

        String sqlSangria = "INSERT INTO sangria (valor, justificativa) VALUES (?, ?)";
        String sqlMov = "INSERT INTO caixa_movimento (tipo, modalidade, valor, taxa, valor_liquido, justificativa, sessao_id, data_hora) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlUpdateSaldo = "UPDATE caixa SET saldo = saldo - ? WHERE id = 1";

        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmtS = conn.prepareStatement(sqlSangria)) {
                    stmtS.setDouble(1, valor);
                    stmtS.setString(2, justificativa != null ? justificativa : "Sangria de Caixa");
                    stmtS.executeUpdate();
                }

                try (PreparedStatement stmtM = conn.prepareStatement(sqlMov)) {
                    stmtM.setString(1, "SANGRIA");
                    stmtM.setString(2, "DINHEIRO");
                    stmtM.setDouble(3, valor);
                    stmtM.setDouble(4, 0.0);
                    stmtM.setDouble(5, -valor);
                    stmtM.setString(6, justificativa != null && !justificativa.trim().isEmpty() ? justificativa : "Sangria / Retirada");
                    if (sessaoId != null) stmtM.setInt(7, sessaoId); else stmtM.setNull(7, java.sql.Types.INTEGER);
                    stmtM.setTimestamp(8, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                    stmtM.executeUpdate();
                }

                try (PreparedStatement stmtSaldo = conn.prepareStatement(sqlUpdateSaldo)) {
                    stmtSaldo.setDouble(1, valor);
                    stmtSaldo.executeUpdate();
                }

                conn.commit();
                System.out.println("[DB] Sangria de R$ " + valor + " registrada com sucesso.");
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao registrar sangria: " + e.getMessage());
            return false;
        }
    }

    public boolean registrarSuprimento(double valor, String justificativa) {
        CaixaSessao sessaoAberta = obterSessaoAberta();
        Integer sessaoId = sessaoAberta != null ? sessaoAberta.getId() : null;

        String sqlMov = "INSERT INTO caixa_movimento (tipo, modalidade, valor, taxa, valor_liquido, justificativa, sessao_id, data_hora) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlUpdateSaldo = "UPDATE caixa SET saldo = saldo + ? WHERE id = 1";

        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmtM = conn.prepareStatement(sqlMov)) {
                    stmtM.setString(1, "SUPRIMENTO");
                    stmtM.setString(2, "DINHEIRO");
                    stmtM.setDouble(3, valor);
                    stmtM.setDouble(4, 0.0);
                    stmtM.setDouble(5, valor);
                    stmtM.setString(6, justificativa != null && !justificativa.trim().isEmpty() ? justificativa : "Suprimento / Entrada de Troco");
                    if (sessaoId != null) stmtM.setInt(7, sessaoId); else stmtM.setNull(7, java.sql.Types.INTEGER);
                    stmtM.setTimestamp(8, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                    stmtM.executeUpdate();
                }

                try (PreparedStatement stmtSaldo = conn.prepareStatement(sqlUpdateSaldo)) {
                    stmtSaldo.setDouble(1, valor);
                    stmtSaldo.executeUpdate();
                }

                conn.commit();
                System.out.println("[DB] Suprimento de R$ " + valor + " registrado com sucesso.");
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao registrar suprimento: " + e.getMessage());
            return false;
        }
    }

    public boolean registrarRecebimentoOS(int osId, List<PagamentoItem> pagamentos) {
        CaixaSessao sessaoAberta = obterSessaoAberta();
        Integer sessaoId = sessaoAberta != null ? sessaoAberta.getId() : null;

        String sqlPagamento = "INSERT INTO venda_pagamento (venda_id, os_id, modalidade, valor_bruto, taxa_percentual, valor_taxa, valor_liquido, parcelas, data_hora) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlMovimentoCaixa = "INSERT INTO caixa_movimento (tipo, modalidade, valor, taxa, valor_liquido, justificativa, os_id, sessao_id, data_hora) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlUpdateSaldoDinheiro = "UPDATE caixa SET saldo = saldo + ? WHERE id = 1";

        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                double totalDinheiro = 0.0;
                try (PreparedStatement stmtPag = conn.prepareStatement(sqlPagamento);
                     PreparedStatement stmtMov = conn.prepareStatement(sqlMovimentoCaixa)) {
                    for (PagamentoItem pag : pagamentos) {
                        stmtPag.setNull(1, java.sql.Types.INTEGER);
                        stmtPag.setInt(2, osId);
                        stmtPag.setString(3, pag.getModalidade());
                        stmtPag.setDouble(4, pag.getValorBruto());
                        stmtPag.setDouble(5, pag.getTaxaPercentual());
                        stmtPag.setDouble(6, pag.getValorTaxa());
                        stmtPag.setDouble(7, pag.getValorLiquido());
                        stmtPag.setInt(8, pag.getParcelas());
                        stmtPag.setTimestamp(9, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                        stmtPag.addBatch();

                        stmtMov.setString(1, "RECEBIMENTO_OS");
                        stmtMov.setString(2, pag.getModalidade());
                        stmtMov.setDouble(3, pag.getValorBruto());
                        stmtMov.setDouble(4, pag.getValorTaxa());
                        stmtMov.setDouble(5, pag.getValorLiquido());
                        stmtMov.setString(6, "Recebimento da OS #" + osId + (pag.getParcelas() > 1 ? " (" + pag.getParcelas() + "x)" : ""));
                        stmtMov.setInt(7, osId);
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

                if (totalDinheiro > 0) {
                    try (PreparedStatement stmtSaldo = conn.prepareStatement(sqlUpdateSaldoDinheiro)) {
                        stmtSaldo.setDouble(1, totalDinheiro);
                        stmtSaldo.executeUpdate();
                    }
                }

                conn.commit();
                System.out.println("[DB] Pagamentos da OS #" + osId + " registrados com sucesso.");
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao registrar recebimento da OS #" + osId + ": " + e.getMessage());
            return false;
        }
    }

    public List<PagamentoItem> obterPagamentosOS(int osId) {
        List<PagamentoItem> lista = new ArrayList<>();
        String sql = "SELECT modalidade, valor_bruto, taxa_percentual, parcelas FROM venda_pagamento WHERE os_id = ? ORDER BY id ASC";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, osId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String mod = rs.getString("modalidade");
                    double valor = rs.getDouble("valor_bruto");
                    double taxa = rs.getDouble("taxa_percentual");
                    int parc = rs.getInt("parcelas");
                    lista.add(new PagamentoItem(mod, valor, taxa, parc));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao obter pagamentos da OS #" + osId + ": " + e.getMessage());
        }
        return lista;
    }

    public List<CaixaMovimento> obterMovimentacoesDia(LocalDate data) {
        List<CaixaMovimento> lista = new ArrayList<>();
        LocalDateTime inicio = data.atStartOfDay();
        LocalDateTime fim = data.atTime(23, 59, 59);

        String sql = "SELECT id, tipo, modalidade, valor, taxa, valor_liquido, justificativa, os_id, venda_id, data_hora " +
                     "FROM caixa_movimento WHERE data_hora BETWEEN ? AND ? ORDER BY id DESC";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, java.sql.Timestamp.valueOf(inicio));
            stmt.setTimestamp(2, java.sql.Timestamp.valueOf(fim));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp("data_hora");
                    lista.add(new CaixaMovimento(
                            rs.getInt("id"),
                            rs.getString("tipo"),
                            rs.getString("modalidade"),
                            rs.getDouble("valor"),
                            rs.getDouble("taxa"),
                            rs.getDouble("valor_liquido"),
                            rs.getString("justificativa"),
                            (Integer) rs.getObject("os_id"),
                            (Integer) rs.getObject("venda_id"),
                            ts != null ? ts.toLocalDateTime() : LocalDateTime.now()
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao obter movimentações do caixa: " + e.getMessage());
        }
        return lista;
    }

    public List<CaixaMovimento> obterMovimentacoesSessao(int sessaoId) {
        List<CaixaMovimento> lista = new ArrayList<>();
        String sql = "SELECT id, tipo, modalidade, valor, taxa, valor_liquido, justificativa, os_id, venda_id, data_hora " +
                     "FROM caixa_movimento WHERE sessao_id = ? ORDER BY id ASC";

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sessaoId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp("data_hora");
                    lista.add(new CaixaMovimento(
                            rs.getInt("id"),
                            rs.getString("tipo"),
                            rs.getString("modalidade"),
                            rs.getDouble("valor"),
                            rs.getDouble("taxa"),
                            rs.getDouble("valor_liquido"),
                            rs.getString("justificativa"),
                            (Integer) rs.getObject("os_id"),
                            (Integer) rs.getObject("venda_id"),
                            ts != null ? ts.toLocalDateTime() : LocalDateTime.now()
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao obter movimentações da sessão #" + sessaoId + ": " + e.getMessage());
        }
        return lista;
    }

    public ResumoFechamentoCaixa obterResumoFechamentoDia(LocalDate data) {
        CaixaSessao sessaoAberta = obterSessaoAberta();
        List<CaixaMovimento> movs;
        if (sessaoAberta != null) {
            movs = obterMovimentacoesSessao(sessaoAberta.getId());
            if (movs.isEmpty()) {
                movs = obterMovimentacoesDia(data);
            }
        } else {
            movs = obterMovimentacoesDia(data);
        }

        ResumoFechamentoCaixa resumo = new ResumoFechamentoCaixa(data);
        resumo.setSaldoAtualGaveta(obterSaldo());
        resumo.setMovimentacoes(movs);

        double din = 0.0, pix = 0.0, debB = 0.0, debL = 0.0, credB = 0.0, credL = 0.0;
        double sangrias = 0.0, suprimentos = 0.0;
        int vendasCont = 0;

        for (CaixaMovimento m : movs) {
            String tipo = m.getTipo();
            String mod = m.getModalidade();

            if ("SANGRIA".equalsIgnoreCase(tipo)) {
                sangrias += m.getValor();
            } else if ("SUPRIMENTO".equalsIgnoreCase(tipo)) {
                suprimentos += m.getValor();
            } else if ("VENDA_PDV".equalsIgnoreCase(tipo) || "RECEBIMENTO_OS".equalsIgnoreCase(tipo)) {
                vendasCont++;
                if ("DINHEIRO".equalsIgnoreCase(mod)) {
                    din += m.getValor();
                } else if ("PIX".equalsIgnoreCase(mod)) {
                    pix += m.getValor();
                } else if ("DEBITO".equalsIgnoreCase(mod)) {
                    debB += m.getValor();
                    debL += m.getValorLiquido();
                } else if (mod != null && mod.contains("CREDITO")) {
                    credB += m.getValor();
                    credL += m.getValorLiquido();
                }
            }
        }

        resumo.setTotalDinheiro(din);
        resumo.setTotalPix(pix);
        resumo.setTotalDebitoBruto(debB);
        resumo.setTotalDebitoLiquido(debL);
        resumo.setTotalCreditoBruto(credB);
        resumo.setTotalCreditoLiquido(credL);
        resumo.setTotalSangrias(sangrias);
        resumo.setTotalSuprimentos(suprimentos);
        resumo.setTotalGeralVendasBruto(din + pix + debB + credB);
        resumo.setTotalGeralVendasLiquido(din + pix + debL + credL);
        resumo.setQuantidadeVendas(vendasCont);

        return resumo;
    }

    public Map<String, Double> obterTaxasMaquininha() {
        Map<String, Double> taxas = new HashMap<>();
        taxas.put("DINHEIRO", 0.0);
        taxas.put("PIX", 0.0);
        taxas.put("DEBITO", 1.50);
        taxas.put("CREDITO_1X", 3.20);
        taxas.put("CREDITO_PARCELADO_2X_6X", 5.50);
        taxas.put("CREDITO_PARCELADO_7X_12X", 9.80);

        String sql = "SELECT modalidade, taxa_percentual FROM config_taxas";
        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                taxas.put(rs.getString("modalidade"), rs.getDouble("taxa_percentual"));
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao obter taxas de maquininha: " + e.getMessage());
        }
        return taxas;
    }

    public boolean salvarTaxasMaquininha(Map<String, Double> taxas) {
        String sql = "INSERT INTO config_taxas (modalidade, taxa_percentual) VALUES (?, ?) " +
                     "ON DUPLICATE KEY UPDATE taxa_percentual = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Map.Entry<String, Double> entry : taxas.entrySet()) {
                stmt.setString(1, entry.getKey());
                stmt.setDouble(2, entry.getValue());
                stmt.setDouble(3, entry.getValue());
                stmt.addBatch();
            }
            stmt.executeBatch();
            System.out.println("[DB] Taxas de maquininha atualizadas com sucesso.");
            return true;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao salvar taxas da maquininha: " + e.getMessage());
            return false;
        }
    }

    public String obterConfig(String chave, String valorPadrao) {
        String sql = "SELECT valor FROM config_geral WHERE chave = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, chave);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String val = rs.getString("valor");
                    return val != null ? val : valorPadrao;
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao obter config " + chave + ": " + e.getMessage());
        }
        return valorPadrao;
    }

    public boolean salvarConfig(String chave, String valor) {
        String sql = "INSERT INTO config_geral (chave, valor) VALUES (?, ?) ON DUPLICATE KEY UPDATE valor = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, chave);
            stmt.setString(2, valor);
            stmt.setString(3, valor);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao salvar config " + chave + ": " + e.getMessage());
            return false;
        }
    }

    public List<String> obterHistoricoSangrias() {
        List<String> sangrias = new ArrayList<>();
        String sql = "SELECT valor, justificativa, data_hora FROM sangria ORDER BY data_hora DESC";
        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                double valor = rs.getDouble("valor");
                String just = rs.getString("justificativa");
                java.sql.Timestamp ts = rs.getTimestamp("data_hora");
                sangrias.add(String.format("Retirada: R$ %.2f (%s) em %s", 
                        valor, (just != null && !just.isEmpty() ? just : "Sangria"), ts != null ? ts.toString() : "-"));
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao obter histórico de sangrias: " + e.getMessage());
        }
        return sangrias;
    }
}
