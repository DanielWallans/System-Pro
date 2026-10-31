package com.loja.service;

import com.loja.event.EventBus;
import com.loja.event.events.EstoqueAlteradoEvent;
import com.loja.exception.BusinessRuleException;
import com.loja.exception.EntityNotFoundException;
import com.loja.exception.ValidationException;
import com.loja.model.Produto;
import com.loja.repository.contract.EstoqueMovimentoRepository;
import com.loja.repository.contract.ProdutoRepository;
import com.loja.util.AppLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Camada de Serviço responsável pela gestão de Produtos, Peças e Controle de Estoque.
 */
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final EstoqueMovimentoRepository movimentoRepository;

    public ProdutoService(ProdutoRepository produtoRepository, EstoqueMovimentoRepository movimentoRepository) {
        this.produtoRepository = produtoRepository;
        this.movimentoRepository = movimentoRepository;
    }

    public void salvar(Produto produto) {
        validarProduto(produto);

        boolean sucesso = produtoRepository.salvar(produto);
        if (!sucesso) {
            throw new BusinessRuleException("Não foi possível persistir o produto no banco de dados.");
        }

        EventBus.getInstance().publish(new EstoqueAlteradoEvent(produto.getId(), produto.getEstoque(), "CADASTRO"));
        AppLogger.info(ProdutoService.class, "Produto salvo com sucesso: " + produto.getNome());
    }

    public List<Produto> buscarTodos() {
        return produtoRepository.buscarTodos();
    }

    public Produto buscarPorId(int id) {
        Produto p = produtoRepository.buscarPorId(id);
        if (p == null) {
            throw new EntityNotFoundException("Produto", id);
        }
        return p;
    }

    public Produto buscarPorCodigoOuId(String termo) {
        return produtoRepository.buscarPorCodigoOuId(termo);
    }

    public List<Produto> buscarPorTermo(String termo) {
        return produtoRepository.buscarPorTermo(termo);
    }

    public List<Produto> buscarProdutosCriticos() {
        return produtoRepository.buscarProdutosCriticosOuEmFalta();
    }

    public int contarProdutosCriticos() {
        return produtoRepository.contarProdutosCriticosOuEmFalta();
    }

    public void registrarEntradaEstoque(int produtoId, int quantidade, Double novoCusto, String docRef, String usuario, String obs) {
        if (quantidade <= 0) {
            throw new ValidationException("A quantidade de entrada deve ser maior que zero.");
        }
        Produto produto = buscarPorId(produtoId);
        boolean ok = movimentoRepository.registrarEntrada(produtoId, quantidade, novoCusto, "Entrada de Mercadoria / Reposição", docRef, usuario, obs);
        if (!ok) {
            throw new BusinessRuleException("Falha ao registrar a entrada de estoque.");
        }
        EventBus.getInstance().publish(new EstoqueAlteradoEvent(produtoId, quantidade, "ENTRADA"));
        AppLogger.info(ProdutoService.class, String.format("Entrada de %d un do produto '%s' registrada por %s.", quantidade, produto.getNome(), usuario));
    }

    public void registrarSaidaEstoque(int produtoId, int quantidade, String motivo, String docRef, String usuario, String obs) {
        if (quantidade <= 0) {
            throw new ValidationException("A quantidade de saída deve ser maior que zero.");
        }
        Produto produto = buscarPorId(produtoId);
        if (produto.getEstoque() < quantidade) {
            throw new BusinessRuleException(String.format("Estoque insuficiente. Saldo atual: %d un, Saída solicitada: %d un.", produto.getEstoque(), quantidade));
        }
        boolean ok = movimentoRepository.registrarSaida(produtoId, quantidade, motivo, docRef, usuario, obs);
        if (!ok) {
            throw new BusinessRuleException("Falha ao registrar a saída de estoque.");
        }
        EventBus.getInstance().publish(new EstoqueAlteradoEvent(produtoId, -quantidade, "SAIDA"));
        AppLogger.info(ProdutoService.class, String.format("Saída de %d un do produto '%s' registrada. Motivo: %s", quantidade, produto.getNome(), motivo));
    }

    public void excluir(int produtoId) {
        Produto p = buscarPorId(produtoId);
        boolean sucesso = produtoRepository.excluir(produtoId);
        if (!sucesso) {
            throw new BusinessRuleException("Não foi possível excluir o produto '" + p.getNome() + "'. Verifique se há movimentações vinculadas.");
        }
        EventBus.getInstance().publish(new EstoqueAlteradoEvent(produtoId, 0, "EXCLUSAO"));
        AppLogger.info(ProdutoService.class, "Produto excluído: ID " + produtoId);
    }

    private void validarProduto(Produto p) {
        if (p == null) {
            throw new ValidationException("Os dados do produto não podem ser nulos.");
        }

        List<String> erros = new ArrayList<>();

        if (p.getNome() == null || p.getNome().trim().length() < 2) {
            erros.add("O nome/descrição do produto deve ter ao menos 2 caracteres.");
        }
        if (p.getPreco() <= 0) {
            erros.add("O preço de venda do produto deve ser maior que R$ 0,00.");
        }
        if (p.getEstoque() < 0) {
            erros.add("A quantidade de estoque não pode ser negativa.");
        }
        if (p.getEstoqueMinimo() < 0) {
            erros.add("O estoque mínimo não pode ser negativo.");
        }

        if (!erros.isEmpty()) {
            throw new ValidationException(erros);
        }
    }
}
