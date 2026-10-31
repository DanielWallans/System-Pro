package com.loja.service;

import com.loja.exception.BusinessRuleException;
import com.loja.exception.EntityNotFoundException;
import com.loja.exception.ValidationException;
import com.loja.model.Cliente;
import com.loja.repository.contract.ClienteRepository;
import com.loja.util.AppLogger;
import com.loja.util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Camada de Serviço responsável pela gestão e regras de negócio da entidade Cliente.
 */
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public void salvar(Cliente cliente) {
        validarCliente(cliente);

        boolean sucesso = clienteRepository.salvar(cliente);
        if (!sucesso) {
            throw new BusinessRuleException("Não foi possível persistir os dados do cliente no banco de dados.");
        }
        AppLogger.info(ClienteService.class, "Cliente salvo/atualizado com sucesso: " + cliente.getNome());
    }

    public List<Cliente> buscarTodos() {
        return clienteRepository.buscarTodos();
    }

    public Cliente buscarPorId(int id) {
        Cliente c = clienteRepository.buscarPorId(id);
        if (c == null) {
            throw new EntityNotFoundException("Cliente", id);
        }
        return c;
    }

    public List<Cliente> buscarPorNome(String termo) {
        if (termo == null || termo.trim().isEmpty()) {
            return buscarTodos();
        }
        return clienteRepository.buscarPorNome(termo.trim());
    }

    public void excluir(int clienteId) {
        Cliente cliente = buscarPorId(clienteId);

        int osVinculadas = clienteRepository.contarOSVinculadas(clienteId);
        if (osVinculadas > 0) {
            throw new BusinessRuleException(String.format(
                    "O cliente '%s' possui %d Ordem(ns) de Serviço vinculada(s). Finalize ou transfira as OS antes de excluir.",
                    cliente.getNome(), osVinculadas));
        }

        boolean sucesso = clienteRepository.excluir(clienteId);
        if (!sucesso) {
            throw new BusinessRuleException("Falha ao excluir o cliente.");
        }
        AppLogger.info(ClienteService.class, "Cliente excluído com sucesso: ID " + clienteId);
    }

    private void validarCliente(Cliente cliente) {
        if (cliente == null) {
            throw new ValidationException("Os dados do cliente não podem ser nulos.");
        }

        List<String> erros = new ArrayList<>();

        if (cliente.getNome() == null || cliente.getNome().trim().length() < 3) {
            erros.add("O nome do cliente deve conter ao menos 3 caracteres.");
        }

        if (cliente.getCpfCnpj() != null && !cliente.getCpfCnpj().trim().isEmpty()) {
            String docLimpo = ValidationUtils.apenasNumeros(cliente.getCpfCnpj());
            if (!docLimpo.isEmpty() && !ValidationUtils.isCpfOuCnpjValido(docLimpo)) {
                erros.add("O CPF/CNPJ informado ('" + cliente.getCpfCnpj() + "') é inválido.");
            }
        }

        if (cliente.getTelefone() != null && !cliente.getTelefone().trim().isEmpty()) {
            String telLimpo = ValidationUtils.apenasNumeros(cliente.getTelefone());
            if (!telLimpo.isEmpty() && !ValidationUtils.isTelefoneValido(telLimpo)) {
                erros.add("O telefone informado ('" + cliente.getTelefone() + "') deve conter DDD e entre 10 e 11 dígitos.");
            }
        }

        if (!erros.isEmpty()) {
            throw new ValidationException(erros);
        }
    }
}
