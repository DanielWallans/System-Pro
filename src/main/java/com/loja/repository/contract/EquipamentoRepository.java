package com.loja.repository.contract;

import com.loja.model.Equipamento;
import java.util.List;

public interface EquipamentoRepository {
    boolean salvar(Equipamento equipamento);
    List<Equipamento> buscarPorCliente(int clienteId);
    Equipamento buscarPorId(int id);
    List<Equipamento> buscarTodos();
    int contarOSVinculadas(int equipId);
    boolean excluir(int equipId);
}

