package com.loja.service;

import com.loja.exception.BusinessRuleException;
import com.loja.exception.EntityNotFoundException;
import com.loja.exception.ValidationException;
import com.loja.model.Equipamento;
import com.loja.repository.contract.EquipamentoRepository;
import com.loja.util.AppLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Camada de Serviço responsável pela gestão de Equipamentos e seus vínculos com clientes e OS.
 */
public class EquipamentoService {

    private final EquipamentoRepository equipamentoRepository;

    public EquipamentoService(EquipamentoRepository equipamentoRepository) {
        this.equipamentoRepository = equipamentoRepository;
    }

    public void salvar(Equipamento equipamento) {
        validarEquipamento(equipamento);

        boolean sucesso = equipamentoRepository.salvar(equipamento);
        if (!sucesso) {
            throw new BusinessRuleException("Não foi possível salvar o equipamento.");
        }
        AppLogger.info(EquipamentoService.class, "Equipamento cadastrado/atualizado com sucesso: " + equipamento.getModelo());
    }

    public List<Equipamento> buscarPorCliente(int clienteId) {
        if (clienteId <= 0) {
            throw new ValidationException("Identificador de cliente inválido.");
        }
        return equipamentoRepository.buscarPorCliente(clienteId);
    }

    public Equipamento buscarPorId(int id) {
        Equipamento e = equipamentoRepository.buscarPorId(id);
        if (e == null) {
            throw new EntityNotFoundException("Equipamento", id);
        }
        return e;
    }

    public List<Equipamento> buscarTodos() {
        return equipamentoRepository.buscarTodos();
    }

    public void excluir(int equipId) {
        Equipamento eq = buscarPorId(equipId);

        int osVinculadas = equipamentoRepository.contarOSVinculadas(equipId);
        if (osVinculadas > 0) {
            throw new BusinessRuleException(String.format(
                    "O equipamento '%s' possui %d histórico(s) de Ordem de Serviço vinculada(s).",
                    eq.getModelo(), osVinculadas));
        }

        boolean sucesso = equipamentoRepository.excluir(equipId);
        if (!sucesso) {
            throw new BusinessRuleException("Falha ao excluir o equipamento.");
        }
        AppLogger.info(EquipamentoService.class, "Equipamento excluído com sucesso: ID " + equipId);
    }

    private void validarEquipamento(Equipamento eq) {
        if (eq == null) {
            throw new ValidationException("Os dados do equipamento não podem ser nulos.");
        }

        List<String> erros = new ArrayList<>();

        if (eq.getClienteId() <= 0) {
            erros.add("É obrigatório selecionar o cliente proprietário do equipamento.");
        }
        if (eq.getTipo() == null || eq.getTipo().trim().isEmpty()) {
            erros.add("Informe o tipo do aparelho (Notebook, Celular, Desktop, etc.).");
        }
        if (eq.getModelo() == null || eq.getModelo().trim().isEmpty()) {
            erros.add("Informe a marca e modelo do equipamento.");
        }

        if (!erros.isEmpty()) {
            throw new ValidationException(erros);
        }
    }
}

