package com.loja.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.loja.model.HistoricoOS;
import com.loja.model.OrdemServico;
import com.loja.model.PecaItem;
import com.loja.model.ServicoItem;

public class OrdemServicoDAO {

    public boolean abrirOS(OrdemServico os) {
        String sql = "INSERT INTO ordem_servico (cliente_id, equipamento_id, problema_relatado, status, valor_servico, valor_total, checklist_entrada, observacoes_finais, data_entrada) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, os.getClienteId());
            stmt.setInt(2, os.getEquipamentoId());
            stmt.setString(3, os.getProblemaRelatado());
            stmt.setString(4, os.getStatus());
            stmt.setDouble(5, os.getValorServico());
            stmt.setDouble(6, os.getValorTotal());
            stmt.setString(7, os.getChecklistEntrada());
            stmt.setString(8, os.getObservacoesFinais());
            stmt.setTimestamp(9, java.sql.Timestamp.valueOf(os.getDataEntrada()));
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        os.setId(generatedKeys.getInt(1));
                    }
                }
                // Registra evento de abertura no histórico da OS
                registrarHistorico(conn, os.getId(), os.getStatus(), "Abertura da Ordem de Serviço", os.getDataEntrada());
                System.out.println("[DB] Ordem de Serviço #" + os.getId() + " aberta com sucesso.");
                try {
                    com.loja.service.AutoBackupService.getInstance().notificarAcaoCritica("Abertura OS #" + os.getId());
                } catch (Exception ignored) {}
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao abrir OS: " + e.getMessage());
            return false;
        }
    }

    public void adicionarPecaOS(int osId, int produtoId, int quantidade, double valorUnitario) {
        String sqlInsert = "INSERT INTO os_pecas (os_id, produto_id, quantidade, valor_unitario) VALUES (?, ?, ?, ?) " +
                           "ON DUPLICATE KEY UPDATE quantidade = quantidade + ?, valor_unitario = ?";
        String sqlBaixaEstoque = "UPDATE produto SET estoque = estoque - ? WHERE id = ?";
        String sqlUpdateTotal = "UPDATE ordem_servico SET valor_total = valor_total + ? WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement stmtInsert = conn.prepareStatement(sqlInsert);
                 PreparedStatement stmtBaixa = conn.prepareStatement(sqlBaixaEstoque);
                 PreparedStatement stmtUpdateTotal = conn.prepareStatement(sqlUpdateTotal)) {

                // 1. Inserir/Atualizar peça na OS
                stmtInsert.setInt(1, osId);
                stmtInsert.setInt(2, produtoId);
                stmtInsert.setInt(3, quantidade);
                stmtInsert.setDouble(4, valorUnitario);
                stmtInsert.setInt(5, quantidade);
                stmtInsert.setDouble(6, valorUnitario);
                stmtInsert.executeUpdate();

                // 2. Baixar do estoque se ID for de produto real
                if (produtoId > 0) {
                    stmtBaixa.setInt(1, quantidade);
                    stmtBaixa.setInt(2, produtoId);
                    stmtBaixa.executeUpdate();
                }

                // 3. Atualizar valor total da OS
                double acrescimo = quantidade * valorUnitario;
                stmtUpdateTotal.setDouble(1, acrescimo);
                stmtUpdateTotal.setInt(2, osId);
                stmtUpdateTotal.executeUpdate();

                conn.commit();
                System.out.println("[DB] Peca ID " + produtoId + " (" + quantidade + "x) adicionada a OS #" + osId);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao adicionar peca a OS #" + osId + ": " + e.getMessage());
        }
    }

    public boolean salvarOrcamentoCompleto(int osId, String status, String diagnosticoTecnico, 
                                           List<ServicoItem> servicos, List<PecaItem> pecas, 
                                           double valorServicoTotal, double valorTotalGeral) {
        String sqlGetStatusAntigo = "SELECT status FROM ordem_servico WHERE id = ?";
        String sqlUpdateOS = "UPDATE ordem_servico SET status = ?, valor_servico = ?, valor_total = ?, diagnostico_tecnico = ?, data_saida = ? WHERE id = ?";
        String sqlLimparServicos = "DELETE FROM os_servicos WHERE os_id = ?";
        String sqlInsertServico = "INSERT INTO os_servicos (os_id, descricao, valor) VALUES (?, ?, ?)";
        String sqlLimparPecas = "DELETE FROM os_pecas WHERE os_id = ?";
        String sqlInsertPeca = "INSERT INTO os_pecas (os_id, produto_id, nome, quantidade, valor_unitario) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Verificar status anterior
                String statusAntigo = null;
                try (PreparedStatement stmtGet = conn.prepareStatement(sqlGetStatusAntigo)) {
                    stmtGet.setInt(1, osId);
                    try (ResultSet rs = stmtGet.executeQuery()) {
                        if (rs.next()) {
                            statusAntigo = rs.getString("status");
                        }
                    }
                }

                // 2. Atualizar dados principais da OS
                try (PreparedStatement stmtOS = conn.prepareStatement(sqlUpdateOS)) {
                    stmtOS.setString(1, status);
                    stmtOS.setDouble(2, valorServicoTotal);
                    stmtOS.setDouble(3, valorTotalGeral);
                    stmtOS.setString(4, diagnosticoTecnico);
                    if (status != null && (status.contains("Entregue") || status.contains("Pronto"))) {
                        stmtOS.setTimestamp(5, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                    } else {
                        stmtOS.setTimestamp(5, null);
                    }
                    stmtOS.setInt(6, osId);
                    stmtOS.executeUpdate();
                }

                // 3. Atualizar Serviços
                try (PreparedStatement stmtDelServ = conn.prepareStatement(sqlLimparServicos)) {
                    stmtDelServ.setInt(1, osId);
                    stmtDelServ.executeUpdate();
                }
                if (servicos != null && !servicos.isEmpty()) {
                    try (PreparedStatement stmtAddServ = conn.prepareStatement(sqlInsertServico)) {
                        for (ServicoItem s : servicos) {
                            stmtAddServ.setInt(1, osId);
                            stmtAddServ.setString(2, s.getDescricao());
                            stmtAddServ.setDouble(3, s.getValor());
                            stmtAddServ.addBatch();
                        }
                        stmtAddServ.executeBatch();
                    }
                }

                // 4. Atualizar Peças
                try (PreparedStatement stmtDelPeca = conn.prepareStatement(sqlLimparPecas)) {
                    stmtDelPeca.setInt(1, osId);
                    stmtDelPeca.executeUpdate();
                }
                if (pecas != null && !pecas.isEmpty()) {
                    try (PreparedStatement stmtAddPeca = conn.prepareStatement(sqlInsertPeca)) {
                        for (PecaItem p : pecas) {
                            stmtAddPeca.setInt(1, osId);
                            stmtAddPeca.setInt(2, p.getProdutoId());
                            stmtAddPeca.setString(3, p.getNome());
                            stmtAddPeca.setInt(4, p.getQuantidade());
                            stmtAddPeca.setDouble(5, p.getValorUnitario());
                            stmtAddPeca.addBatch();
                        }
                        stmtAddPeca.executeBatch();
                    }
                }

                // 5. Registrar no histórico se status foi alterado
                if (status != null && !status.equalsIgnoreCase(statusAntigo)) {
                    String obs = statusAntigo != null ? "Status alterado de [" + statusAntigo + "] para [" + status + "]" : "Definição de status";
                    registrarHistorico(conn, osId, status, obs, LocalDateTime.now());
                }

                conn.commit();
                System.out.println("[DB] Orçamento completo da OS #" + osId + " salvo com sucesso!");
                try {
                    com.loja.service.AutoBackupService.getInstance().notificarAcaoCritica("Edição OS #" + osId);
                } catch (Exception ignored) {}
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao salvar orcamento completo da OS #" + osId + ": " + e.getMessage());
            return false;
        }
    }

    public List<ServicoItem> obterServicosOS(int osId) {
        List<ServicoItem> lista = new ArrayList<>();
        String sql = "SELECT id, descricao, valor FROM os_servicos WHERE os_id = ? ORDER BY id ASC";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, osId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new ServicoItem(
                            rs.getInt("id"),
                            osId,
                            rs.getString("descricao"),
                            rs.getDouble("valor")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao obter servicos da OS: " + e.getMessage());
        }
        return lista;
    }

    public List<PecaItem> obterPecasItensOS(int osId) {
        List<PecaItem> lista = new ArrayList<>();
        String sql = "SELECT op.produto_id, COALESCE(op.nome, p.nome, 'Peça Avulsa') AS nome, op.quantidade, op.valor_unitario " +
                     "FROM os_pecas op LEFT JOIN produto p ON op.produto_id = p.id WHERE op.os_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, osId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new PecaItem(
                            rs.getInt("produto_id"),
                            rs.getString("nome"),
                            rs.getInt("quantidade"),
                            rs.getDouble("valor_unitario")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao obter pecas itens da OS: " + e.getMessage());
        }
        return lista;
    }

    public void atualizarStatusEServico(int osId, String status, double valorServico, String diagnosticoTecnico) {
        String sqlObterOS = "SELECT status, valor_servico FROM ordem_servico WHERE id = ?";
        String sqlUpdate = "UPDATE ordem_servico SET status = ?, valor_servico = ?, valor_total = valor_total - ? + ?, diagnostico_tecnico = ?, data_saida = ? WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String statusAntigo = null;
                double valorServicoAntigo = 0.0;
                try (PreparedStatement stmtGet = conn.prepareStatement(sqlObterOS)) {
                    stmtGet.setInt(1, osId);
                    try (ResultSet rs = stmtGet.executeQuery()) {
                        if (rs.next()) {
                            statusAntigo = rs.getString("status");
                            valorServicoAntigo = rs.getDouble("valor_servico");
                        }
                    }
                }

                try (PreparedStatement stmtUpdate = conn.prepareStatement(sqlUpdate)) {
                    stmtUpdate.setString(1, status);
                    stmtUpdate.setDouble(2, valorServico);
                    stmtUpdate.setDouble(3, valorServicoAntigo);
                    stmtUpdate.setDouble(4, valorServico);
                    stmtUpdate.setString(5, diagnosticoTecnico);
                    
                    if (status != null && (status.contains("Entregue") || status.contains("Pronto"))) {
                        stmtUpdate.setTimestamp(6, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                    } else {
                        stmtUpdate.setTimestamp(6, null);
                    }
                    
                    stmtUpdate.setInt(7, osId);
                    stmtUpdate.executeUpdate();
                }

                // Se o status foi modificado, registra no histórico
                if (status != null && !status.equalsIgnoreCase(statusAntigo)) {
                    String obs = statusAntigo != null ? "Status alterado de [" + statusAntigo + "] para [" + status + "]" : "Alteração de status";
                    registrarHistorico(conn, osId, status, obs, LocalDateTime.now());
                }

                conn.commit();
                System.out.println("[DB] OS #" + osId + " atualizada para o status: " + status);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao atualizar OS #" + osId + ": " + e.getMessage());
        }
    }

    public List<OrdemServico> buscarTodos() {
        List<OrdemServico> lista = new ArrayList<>();
        String sql = "SELECT * FROM ordem_servico ORDER BY id DESC";
        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                java.sql.Timestamp entrada = rs.getTimestamp("data_entrada");
                java.sql.Timestamp saida = rs.getTimestamp("data_saida");
                lista.add(new OrdemServico(
                        rs.getInt("id"),
                        rs.getInt("cliente_id"),
                        rs.getInt("equipamento_id"),
                        rs.getString("problema_relatado"),
                        rs.getString("diagnostico_tecnico"),
                        rs.getString("status"),
                        rs.getDouble("valor_servico"),
                        rs.getDouble("valor_total"),
                        rs.getString("checklist_entrada"),
                        rs.getString("observacoes_finais"),
                        entrada != null ? entrada.toLocalDateTime() : null,
                        saida != null ? saida.toLocalDateTime() : null
                ));
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao listar OSs: " + e.getMessage());
        }
        return lista;
    }

    public OrdemServico buscarPorId(int id) {
        String sql = "SELECT * FROM ordem_servico WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp entrada = rs.getTimestamp("data_entrada");
                    java.sql.Timestamp saida = rs.getTimestamp("data_saida");
                    return new OrdemServico(
                            rs.getInt("id"),
                            rs.getInt("cliente_id"),
                            rs.getInt("equipamento_id"),
                            rs.getString("problema_relatado"),
                            rs.getString("diagnostico_tecnico"),
                            rs.getString("status"),
                            rs.getDouble("valor_servico"),
                            rs.getDouble("valor_total"),
                            rs.getString("checklist_entrada"),
                            rs.getString("observacoes_finais"),
                            entrada != null ? entrada.toLocalDateTime() : null,
                            saida != null ? saida.toLocalDateTime() : null
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao obter OS por ID: " + e.getMessage());
        }
        return null;
    }

    public List<String> obterPecasOS(int osId) {
        List<String> pecas = new ArrayList<>();
        String sql = "SELECT COALESCE(op.nome, p.nome, 'Peça Avulsa') AS nome, op.quantidade, op.valor_unitario FROM os_pecas op " +
                     "LEFT JOIN produto p ON op.produto_id = p.id WHERE op.os_id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, osId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    pecas.add(String.format("%d x %s (R$ %.2f)", 
                            rs.getInt("quantidade"), 
                            rs.getString("nome"), 
                            rs.getDouble("valor_unitario")));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao obter pecas da OS: " + e.getMessage());
        }
        return pecas;
    }

    // ==========================================
    // HISTÓRICO / LINHA DO TEMPO DA OS
    // ==========================================

    public boolean registrarHistorico(int osId, String status, String observacao) {
        return registrarHistorico(osId, status, observacao, LocalDateTime.now());
    }

    public boolean registrarHistorico(int osId, String status, String observacao, LocalDateTime dataHora) {
        try (Connection conn = ConnectionFactory.getConnection()) {
            return registrarHistorico(conn, osId, status, observacao, dataHora);
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao registrar histórico para OS #" + osId + ": " + e.getMessage());
            return false;
        }
    }

    public boolean registrarHistorico(Connection conn, int osId, String status, String observacao, LocalDateTime dataHora) {
        String sql = "INSERT INTO os_historico (os_id, status, data_hora, observacao) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, osId);
            stmt.setString(2, status != null ? status : "Status Indefinido");
            stmt.setTimestamp(3, java.sql.Timestamp.valueOf(dataHora != null ? dataHora : LocalDateTime.now()));
            stmt.setString(4, observacao != null ? observacao : "");
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("[DB] Evento de Histórico registrado na OS #" + osId + " -> [" + status + "]");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Erro ao gravar evento de histórico na OS #" + osId + ": " + e.getMessage());
        }
        return false;
    }

    public List<HistoricoOS> obterHistoricoOS(int osId) {
        List<HistoricoOS> historico = new ArrayList<>();
        String sql = "SELECT id, os_id, status, data_hora, observacao FROM os_historico WHERE os_id = ? ORDER BY data_hora ASC, id ASC";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, osId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp("data_hora");
                    historico.add(new HistoricoOS(
                            rs.getInt("id"),
                            rs.getInt("os_id"),
                            rs.getString("status"),
                            ts != null ? ts.toLocalDateTime() : LocalDateTime.now(),
                            rs.getString("observacao")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao obter histórico da OS #" + osId + ": " + e.getMessage());
        }

        // Se a OS não possui registros históricos (OS antiga), gera entrada inicial retroativa
        if (historico.isEmpty()) {
            OrdemServico os = buscarPorId(osId);
            if (os != null) {
                LocalDateTime dataCriacao = os.getDataEntrada() != null ? os.getDataEntrada() : LocalDateTime.now();
                HistoricoOS inicial = new HistoricoOS(osId, "Aguardando Orçamento", dataCriacao, "Abertura da Ordem de Serviço");
                registrarHistorico(osId, inicial.getStatus(), inicial.getObservacao(), inicial.getDataHora());
                historico.add(inicial);

                if (os.getStatus() != null && !os.getStatus().equalsIgnoreCase("Aguardando Orçamento")) {
                    LocalDateTime dataAtual = os.getDataSaida() != null ? os.getDataSaida() : dataCriacao.plusHours(1);
                    HistoricoOS atual = new HistoricoOS(osId, os.getStatus(), dataAtual, "Atualização de status registrada no sistema");
                    registrarHistorico(osId, atual.getStatus(), atual.getObservacao(), atual.getDataHora());
                    historico.add(atual);
                }
            }
        }

        return historico;
    }

    public boolean adicionarEventoHistoricoManual(int osId, String status, String observacao) {
        return registrarHistorico(osId, status, observacao, LocalDateTime.now());
    }

    public static void excluirOSComDependencias(Connection conn, int osId) throws SQLException {
        try (PreparedStatement s = conn.prepareStatement("DELETE FROM os_fotos WHERE os_id = ?")) {
            s.setInt(1, osId);
            s.executeUpdate();
        }
        try (PreparedStatement s = conn.prepareStatement("DELETE FROM os_historico WHERE os_id = ?")) {
            s.setInt(1, osId);
            s.executeUpdate();
        }
        try (PreparedStatement s = conn.prepareStatement("DELETE FROM os_pecas WHERE os_id = ?")) {
            s.setInt(1, osId);
            s.executeUpdate();
        }
        try (PreparedStatement s = conn.prepareStatement("DELETE FROM os_servicos WHERE os_id = ?")) {
            s.setInt(1, osId);
            s.executeUpdate();
        }
        try (PreparedStatement s = conn.prepareStatement("UPDATE nota_fiscal SET os_id = NULL WHERE os_id = ?")) {
            s.setInt(1, osId);
            s.executeUpdate();
        }
        try (PreparedStatement s = conn.prepareStatement("UPDATE caixa_movimento SET os_id = NULL WHERE os_id = ?")) {
            s.setInt(1, osId);
            s.executeUpdate();
        }
        try (PreparedStatement s = conn.prepareStatement("DELETE FROM ordem_servico WHERE id = ?")) {
            s.setInt(1, osId);
            s.executeUpdate();
        }
    }

    public boolean excluir(int osId) {
        try (Connection conn = ConnectionFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                excluirOSComDependencias(conn, osId);
                conn.commit();
                System.out.println("[DB] Ordem de Serviço #" + osId + " excluída com sucesso.");
                try {
                    com.loja.service.AutoBackupService.getInstance().notificarAcaoCritica("Exclusão OS #" + osId);
                } catch (Exception ignored) {}
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[ERRO DB] Falha ao excluir OS #" + osId + ": " + e.getMessage());
            return false;
        }
    }
}

