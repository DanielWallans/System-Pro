package com.loja.repository.contract;

import com.loja.model.CaixaMovimento;
import com.loja.model.CaixaSessao;
import com.loja.model.PagamentoItem;
import com.loja.model.ResumoFechamentoCaixa;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CaixaRepository {
    double obterSaldo();
    void atualizarSaldo(double novoSaldo);
    CaixaSessao obterSessaoAberta();
    CaixaSessao abrirNovaSessao(String operador, double fundoTrocoInicial);
    boolean encerrarSessao(CaixaSessao sessao);
    List<CaixaSessao> listarHistoricoFechamentos();
    CaixaSessao obterSessaoPorId(int id);
    boolean registrarSangria(double valor, String justificativa);
    boolean registrarSuprimento(double valor, String justificativa);
    boolean registrarRecebimentoOS(int osId, List<PagamentoItem> pagamentos);
    List<PagamentoItem> obterPagamentosOS(int osId);
    List<CaixaMovimento> obterMovimentacoesDia(LocalDate data);
    List<CaixaMovimento> obterMovimentacoesSessao(int sessaoId);
    ResumoFechamentoCaixa obterResumoFechamentoDia(LocalDate data);
    Map<String, Double> obterTaxasMaquininha();
    boolean salvarTaxasMaquininha(Map<String, Double> taxas);
    List<String> obterHistoricoSangrias();
}

