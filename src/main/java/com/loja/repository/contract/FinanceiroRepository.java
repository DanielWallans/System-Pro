package com.loja.repository.contract;

import com.loja.model.ContaPagar;
import com.loja.model.Funcionario;
import com.loja.model.FuncionarioDespesa;
import com.loja.model.ResumoDRE;

import java.time.LocalDate;
import java.util.List;

public interface FinanceiroRepository {
    boolean inserirConta(ContaPagar conta);
    boolean atualizarConta(ContaPagar conta);
    boolean excluirConta(int id);
    boolean marcarContaComoPaga(int id, LocalDate dataPagamento, String formaPagamento);
    List<ContaPagar> listarContasPorMes(int ano, int mes);
    List<ContaPagar> listarTodasContas();

    boolean inserirFuncionario(Funcionario f);
    boolean atualizarFuncionario(Funcionario f);
    boolean excluirFuncionario(int id);
    List<Funcionario> listarFuncionarios();

    boolean inserirLancamentoPessoal(FuncionarioDespesa fd);
    boolean atualizarLancamentoPessoal(FuncionarioDespesa fd);
    boolean excluirLancamentoPessoal(int id);
    boolean marcarLancamentoPessoalComoPago(int id, LocalDate dataPagamento, String formaPagamento);
    List<FuncionarioDespesa> listarLancamentosPessoalPorMes(int ano, int mes);

    ResumoDRE calcularDRE(int ano, int mes);
}

