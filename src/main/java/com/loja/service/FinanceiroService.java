package com.loja.service;

import com.loja.exception.BusinessRuleException;
import com.loja.exception.ValidationException;
import com.loja.model.ContaPagar;
import com.loja.model.Funcionario;
import com.loja.model.FuncionarioDespesa;
import com.loja.model.ResumoDRE;
import com.loja.repository.contract.FinanceiroRepository;
import com.loja.util.AppLogger;

import java.time.LocalDate;
import java.util.List;

/**
 * Camada de Serviço responsável pela gestão Financeira, Contas a Pagar e DRE.
 */
public class FinanceiroService {

    private final FinanceiroRepository financeiroRepository;

    public FinanceiroService(FinanceiroRepository financeiroRepository) {
        this.financeiroRepository = financeiroRepository;
    }

    public void registrarConta(ContaPagar conta) {
        if (conta == null) {
            throw new ValidationException("Os dados da conta não podem ser nulos.");
        }
        if (conta.getDescricao() == null || conta.getDescricao().trim().isEmpty()) {
            throw new ValidationException("Informe a descrição da conta a pagar.");
        }
        if (conta.getValor() <= 0) {
            throw new ValidationException("O valor da conta deve ser superior a R$ 0,00.");
        }
        if (conta.getDataVencimento() == null) {
            throw new ValidationException("Informe a data de vencimento da conta.");
        }

        boolean ok = financeiroRepository.inserirConta(conta);
        if (!ok) {
            throw new BusinessRuleException("Não foi possível salvar a conta a pagar.");
        }
        AppLogger.info(FinanceiroService.class, "Conta a pagar registrada: " + conta.getDescricao());
    }

    public void marcarContaComoPaga(int contaId, LocalDate dataPagamento, String formaPagamento) {
        if (dataPagamento == null) {
            dataPagamento = LocalDate.now();
        }
        boolean ok = financeiroRepository.marcarContaComoPaga(contaId, dataPagamento, formaPagamento);
        if (!ok) {
            throw new BusinessRuleException("Não foi possível liquidar a conta de ID " + contaId);
        }
        AppLogger.info(FinanceiroService.class, "Conta #" + contaId + " liquidada com sucesso.");
    }

    public List<ContaPagar> listarContasDoMes(int ano, int mes) {
        return financeiroRepository.listarContasPorMes(ano, mes);
    }

    public List<ContaPagar> listarTodasContas() {
        return financeiroRepository.listarTodasContas();
    }

    public ResumoDRE calcularDRE(int ano, int mes) {
        return financeiroRepository.calcularDRE(ano, mes);
    }

    public List<Funcionario> listarFuncionarios() {
        return financeiroRepository.listarFuncionarios();
    }

    public void cadastrarFuncionario(Funcionario f) {
        if (f == null || f.getNome() == null || f.getNome().trim().length() < 3) {
            throw new ValidationException("O nome do funcionário deve conter ao menos 3 caracteres.");
        }
        boolean ok = financeiroRepository.inserirFuncionario(f);
        if (!ok) {
            throw new BusinessRuleException("Não foi possível cadastrar o funcionário.");
        }
        AppLogger.info(FinanceiroService.class, "Novo funcionário cadastrado: " + f.getNome());
    }

    public List<FuncionarioDespesa> listarLancamentosPessoalPorMes(int ano, int mes) {
        return financeiroRepository.listarLancamentosPessoalPorMes(ano, mes);
    }
}

