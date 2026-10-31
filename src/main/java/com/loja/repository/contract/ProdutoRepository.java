package com.loja.repository.contract;

import com.loja.model.Produto;
import java.util.List;

public interface ProdutoRepository {
    boolean salvar(Produto produto);
    List<Produto> buscarTodos();
    List<Produto> buscarProdutosComEstoqueZerado();
    Produto buscarPorId(int id);
    Produto buscarPorCodigoOuId(String termo);
    List<Produto> buscarPorTermo(String termo);
    int obterProximoId();
    List<Produto> buscarProdutosCriticosOuEmFalta();
    int contarProdutosCriticosOuEmFalta();
    boolean excluir(int produtoId);
}

