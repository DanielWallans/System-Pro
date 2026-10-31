package com.loja.service;

import com.loja.event.EventBus;
import com.loja.event.events.OSAlteradaEvent;
import com.loja.exception.BusinessRuleException;
import com.loja.exception.EntityNotFoundException;
import com.loja.exception.ValidationException;
import com.loja.model.HistoricoOS;
import com.loja.model.OrdemServico;
import com.loja.model.PecaItem;
import com.loja.model.ServicoItem;
import com.loja.repository.contract.OrdemServicoRepository;
import com.loja.util.AppLogger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Camada de Serviço responsável pelas regras de ciclo de vida das Ordens de Serviço (OS).
 */
public class OrdemServicoService {

    private final OrdemServicoRepository osRepository;

    public OrdemServicoService(OrdemServicoRepository osRepository) {
        this.osRepository = osRepository;
    }

    public void abrirOS(OrdemServico os) {
        validarAberturaOS(os);

        boolean sucesso = osRepository.abrirOS(os);
        if (!sucesso) {
            throw new BusinessRuleException("Não foi possível registrar a nova Ordem de Serviço.");
        }

        osRepository.registrarHistorico(os.getId(), "Aberta", "OS cadastrada no sistema.");
        EventBus.getInstance().publish(new OSAlteradaEvent(os.getId(), OSAlteradaEvent.TipoAcao.CRIADA, os));
        AppLogger.info(OrdemServicoService.class, "Nova OS aberta com sucesso: #" + os.getId());
    }

    public void atualizarStatus(int osId, String novoStatus, String observacao) {
        OrdemServico os = buscarPorId(osId);

        if (novoStatus == null || novoStatus.trim().isEmpty()) {
            throw new ValidationException("O novo status não pode ser vazio.");
        }

        String statusAnterior = os.getStatus();
        os.setStatus(novoStatus);

        osRepository.atualizarStatusEServico(osId, novoStatus, os.getValorServico(), os.getDiagnosticoTecnico());
        osRepository.registrarHistorico(osId, novoStatus,
                observacao != null && !observacao.trim().isEmpty() ? observacao : "Status alterado de '" + statusAnterior + "' para '" + novoStatus + "'");

        EventBus.getInstance().publish(new OSAlteradaEvent(osId, OSAlteradaEvent.TipoAcao.STATUS_ALTERADO, os));
        AppLogger.info(OrdemServicoService.class, String.format("OS #%d alterada para status: %s", osId, novoStatus));
    }

    public void salvarOrcamentoCompleto(int osId, String status, String diagnostico,
                                        List<ServicoItem> servicos, List<PecaItem> pecas,
                                        double desconto, double totalFinal) {
        OrdemServico os = buscarPorId(osId);

        boolean ok = osRepository.salvarOrcamentoCompleto(osId, status, diagnostico, servicos, pecas, desconto, totalFinal);
        if (!ok) {
            throw new BusinessRuleException("Falha ao salvar o orçamento da OS #" + osId);
        }

        os.setStatus(status);
        os.setValorTotal(totalFinal);
        osRepository.registrarHistorico(osId, status, "Orçamento atualizado. Total: R$ " + String.format("%.2f", totalFinal));

        EventBus.getInstance().publish(new OSAlteradaEvent(osId, OSAlteradaEvent.TipoAcao.ATUALIZADA, os));
        AppLogger.info(OrdemServicoService.class, "Orçamento da OS #" + osId + " atualizado com sucesso.");
    }

    public OrdemServico buscarPorId(int id) {
        OrdemServico os = osRepository.buscarPorId(id);
        if (os == null) {
            throw new EntityNotFoundException("Ordem de Serviço", id);
        }
        return os;
    }

    public List<OrdemServico> buscarTodos() {
        return osRepository.buscarTodos();
    }

    public List<HistoricoOS> obterHistorico(int osId) {
        return osRepository.obterHistoricoOS(osId);
    }

    public void excluir(int osId) {
        buscarPorId(osId);
        boolean ok = osRepository.excluir(osId);
        if (!ok) {
            throw new BusinessRuleException("Não foi possível excluir a Ordem de Serviço #" + osId);
        }
        EventBus.getInstance().publish(new OSAlteradaEvent(osId, OSAlteradaEvent.TipoAcao.EXCLUIDA, null));
        AppLogger.info(OrdemServicoService.class, "OS excluída: #" + osId);
    }

    private void validarAberturaOS(OrdemServico os) {
        if (os == null) {
            throw new ValidationException("Os dados da Ordem de Serviço não podem ser nulos.");
        }

        List<String> erros = new ArrayList<>();

        if (os.getClienteId() <= 0) {
            erros.add("Selecione o cliente responsável pela OS.");
        }
        if (os.getEquipamentoId() <= 0) {
            erros.add("Selecione o equipamento a ser reparado.");
        }
        if (os.getProblemaRelatado() == null || os.getProblemaRelatado().trim().isEmpty()) {
            erros.add("Informe a descrição do defeito relatado pelo cliente.");
        }

        if (!erros.isEmpty()) {
            throw new ValidationException(erros);
        }
    }
}
