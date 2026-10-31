package com.loja.repository.contract;

import com.loja.model.Cliente;
import java.util.List;

/**
 * Contrato de persistência para a entidade Cliente.
 * Segue o padrão Repository para abstração e inversão de controle (SOLID - DIP).
 */
public interface ClienteRepository {
    boolean salvar(Cliente cliente);
    List<Cliente> buscarTodos();
    Cliente buscarPorId(int id);
    Cliente buscarPorCpf(String cpf);
    List<Cliente> buscarPorNome(String termo);
    int contarEquipamentosVinculados(int clienteId);
    int contarOSVinculadas(int clienteId);
    boolean excluir(int clienteId);
}

