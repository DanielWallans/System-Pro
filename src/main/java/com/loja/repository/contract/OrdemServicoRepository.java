package com.loja.repository.contract;

import com.loja.model.HistoricoOS;
import com.loja.model.OrdemServico;
import com.loja.model.PecaItem;
import com.loja.model.ServicoItem;

import java.time.LocalDateTime;
import java.util.List;

public interface OrdemServicoRepository {
    boolean abrirOS(OrdemServico os);
    void adicionarPecaOS(int osId, int produtoId, int quantidade, double valorUnitario);
    boolean salvarOrcamentoCompleto(int osId, String status, String diagnosticoTecnico,
                                    List<ServicoItem> servicos, List<PecaItem> pecas,
                                    double desconto, double totalFinal);
    List<ServicoItem> obterServicosOS(int osId);
    List<PecaItem> obterPecasItensOS(int osId);
    void atualizarStatusEServico(int osId, String status, double valorServico, String diagnosticoTecnico);
    List<OrdemServico> buscarTodos();
    OrdemServico buscarPorId(int id);
    List<String> obterPecasOS(int osId);
    boolean registrarHistorico(int osId, String status, String observacao);
    boolean registrarHistorico(int osId, String status, String observacao, LocalDateTime dataHora);
    List<HistoricoOS> obterHistoricoOS(int osId);
    boolean adicionarEventoHistoricoManual(int osId, String status, String observacao);
    boolean excluir(int osId);
}
