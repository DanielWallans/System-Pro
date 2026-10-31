package com.loja.repository.contract;

import com.loja.model.EstoqueMovimento;
import java.time.LocalDateTime;
import java.util.List;

public interface EstoqueMovimentoRepository {
    boolean registrarEntrada(int produtoId, int quantidade, Double novoCusto, String motivo, String docRef, String usuario, String obs);
    boolean registrarSaida(int produtoId, int quantidade, String motivo, String documentoRef, String usuario, String observacoes);
    List<EstoqueMovimento> listarTodos(int limite);
    List<EstoqueMovimento> listarPorProduto(int produtoId);
    List<EstoqueMovimento> listarPeriodo(LocalDateTime inicio, LocalDateTime fim, String tipoFiltro);
}
