package com.loja.service;

import com.loja.model.CaixaSessao;
import com.loja.repository.CaixaDAO;

import java.text.DecimalFormat;

/**
 * Serviço responsável pelo controle de fluxo de caixa da loja,
 * gerenciando abertura/fechamento de turnos, sangrias, suprimentos e consultas de saldo.
 */
public class CaixaService {

    private double saldoEmCaixa;
    private final DecimalFormat df = new DecimalFormat("R$ #,##0.00");
    private final CaixaDAO caixaDAO = new CaixaDAO();

    public boolean isCaixaAberto() {
        return caixaDAO.obterSessaoAberta() != null;
    }

    public CaixaSessao getSessaoAberta() {
        return caixaDAO.obterSessaoAberta();
    }

    public CaixaSessao abrirTurno(String operador, double fundoTrocoInicial) {
        CaixaSessao sessao = caixaDAO.abrirNovaSessao(operador, fundoTrocoInicial);
        if (sessao != null) {
            this.saldoEmCaixa = fundoTrocoInicial;
            System.out.println("[INFO] Turno #" + sessao.getId() + " aberto por " + operador + ". Saldo inicial: " + df.format(fundoTrocoInicial));
        }
        return sessao;
    }

    public boolean encerrarTurno(CaixaSessao sessao) {
        boolean ok = caixaDAO.encerrarSessao(sessao);
        if (ok) {
            this.saldoEmCaixa = sessao.getFundoTrocoDeixado();
            System.out.println("[INFO] Turno #" + sessao.getId() + " encerrado com sucesso.");
        }
        return ok;
    }

    public void abrirCaixa(double valorInicial) {
        this.saldoEmCaixa = caixaDAO.obterSaldo();
        System.out.println("[INFO] Caixa pronto. Saldo em gaveta: " + df.format(saldoEmCaixa));
    }

    public void realizarSangria(double valor) {
        realizarSangria(valor, "Sangria de Caixa");
    }

    public boolean realizarSangria(double valor, String justificativa) {
        this.saldoEmCaixa = caixaDAO.obterSaldo();
        if (valor <= saldoEmCaixa) {
            boolean ok = caixaDAO.registrarSangria(valor, justificativa);
            if (ok) {
                this.saldoEmCaixa = caixaDAO.obterSaldo();
                System.out.println("[SANGRIA] Retirada de " + df.format(valor) + " realizada. Motivo: " + justificativa);
                return true;
            }
        } else {
            System.out.println("[ALERTA] Saldo insuficiente para sangria.");
        }
        return false;
    }

    public boolean realizarSuprimento(double valor, String justificativa) {
        boolean ok = caixaDAO.registrarSuprimento(valor, justificativa);
        if (ok) {
            this.saldoEmCaixa = caixaDAO.obterSaldo();
            System.out.println("[SUPRIMENTO] Entrada de " + df.format(valor) + " realizada. Motivo: " + justificativa);
            return true;
        }
        return false;
    }

    public double consultarSaldoAtual() {
        this.saldoEmCaixa = caixaDAO.obterSaldo();
        return this.saldoEmCaixa;
    }
}
