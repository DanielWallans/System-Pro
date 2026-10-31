package com.loja.context;

import com.loja.repository.*;
import com.loja.repository.contract.*;
import com.loja.service.*;
import com.loja.util.AppLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Container de Inversão de Controle (IoC) e Registro Central de Dependências (Service Locator).
 * Centraliza a instanciação, injeção e ciclo de vida dos Repositórios e Serviços do sistema.
 */
public final class AppContext {

    private static final AppContext INSTANCIA = new AppContext();
    private final Map<Class<?>, Object> beans = new ConcurrentHashMap<>();
    private boolean inicializado = false;

    private AppContext() {
    }

    public static AppContext getInstance() {
        return INSTANCIA;
    }

    /**
     * Inicializa todos os repositórios e serviços na ordem correta de dependência.
     */
    public synchronized void inicializar() {
        if (inicializado) {
            return;
        }

        AppLogger.info(AppContext.class, "Inicializando Container de Dependências da Arquitetura...");

        // 1. Repositórios (Data Access Layer)
        ClienteRepository clienteRepo = new ClienteDAO();
        EquipamentoRepository equipRepo = new EquipamentoDAO();
        ProdutoRepository produtoRepo = new ProdutoDAO();
        OrdemServicoRepository osRepo = new OrdemServicoDAO();
        CaixaRepository caixaRepo = new CaixaDAO();
        FinanceiroRepository financeiroRepo = new FinanceiroDAO();
        UsuarioRepository usuarioRepo = new UsuarioDAO();
        EstoqueMovimentoRepository estoqueMovRepo = new EstoqueMovimentoDAO();

        registrar(ClienteRepository.class, clienteRepo);
        registrar(EquipamentoRepository.class, equipRepo);
        registrar(ProdutoRepository.class, produtoRepo);
        registrar(OrdemServicoRepository.class, osRepo);
        registrar(CaixaRepository.class, caixaRepo);
        registrar(FinanceiroRepository.class, financeiroRepo);
        registrar(UsuarioRepository.class, usuarioRepo);
        registrar(EstoqueMovimentoRepository.class, estoqueMovRepo);

        // Registro das implementações concretas para compatibilidade
        registrar(ClienteDAO.class, (ClienteDAO) clienteRepo);
        registrar(EquipamentoDAO.class, (EquipamentoDAO) equipRepo);
        registrar(ProdutoDAO.class, (ProdutoDAO) produtoRepo);
        registrar(OrdemServicoDAO.class, (OrdemServicoDAO) osRepo);
        registrar(CaixaDAO.class, (CaixaDAO) caixaRepo);
        registrar(FinanceiroDAO.class, (FinanceiroDAO) financeiroRepo);
        registrar(UsuarioDAO.class, (UsuarioDAO) usuarioRepo);
        registrar(EstoqueMovimentoDAO.class, (EstoqueMovimentoDAO) estoqueMovRepo);

        // 2. Serviços de Negócio (Domain Service Layer)
        ClienteService clienteService = new ClienteService(clienteRepo);
        EquipamentoService equipService = new EquipamentoService(equipRepo);
        ProdutoService produtoService = new ProdutoService(produtoRepo, estoqueMovRepo);
        OrdemServicoService osService = new OrdemServicoService(osRepo);
        FinanceiroService financeiroService = new FinanceiroService(financeiroRepo);
        UsuarioService usuarioService = new UsuarioService(usuarioRepo);
        CaixaService caixaService = new CaixaService();

        registrar(ClienteService.class, clienteService);
        registrar(EquipamentoService.class, equipService);
        registrar(ProdutoService.class, produtoService);
        registrar(OrdemServicoService.class, osService);
        registrar(FinanceiroService.class, financeiroService);
        registrar(UsuarioService.class, usuarioService);
        registrar(CaixaService.class, caixaService);

        inicializado = true;
        AppLogger.info(AppContext.class, "Container de Dependências pronto com " + beans.size() + " componentes registrados.");
    }

    public <T> void registrar(Class<T> type, T instance) {
        beans.put(type, instance);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        if (!inicializado) {
            inicializar();
        }
        T bean = (T) beans.get(type);
        if (bean == null) {
            throw new IllegalStateException("Nenhum componente registrado para o tipo: " + type.getName());
        }
        return bean;
    }

    // Getters tipados de conveniência
    public ClienteService getClienteService() { return getBean(ClienteService.class); }
    public EquipamentoService getEquipamentoService() { return getBean(EquipamentoService.class); }
    public ProdutoService getProdutoService() { return getBean(ProdutoService.class); }
    public OrdemServicoService getOrdemServicoService() { return getBean(OrdemServicoService.class); }
    public FinanceiroService getFinanceiroService() { return getBean(FinanceiroService.class); }
    public UsuarioService getUsuarioService() { return getBean(UsuarioService.class); }
    public CaixaService getCaixaService() { return getBean(CaixaService.class); }

    public ClienteRepository getClienteRepository() { return getBean(ClienteRepository.class); }
    public EquipamentoRepository getEquipamentoRepository() { return getBean(EquipamentoRepository.class); }
    public ProdutoRepository getProdutoRepository() { return getBean(ProdutoRepository.class); }
    public OrdemServicoRepository getOrdemServicoRepository() { return getBean(OrdemServicoRepository.class); }
    public CaixaRepository getCaixaRepository() { return getBean(CaixaRepository.class); }
    public FinanceiroRepository getFinanceiroRepository() { return getBean(FinanceiroRepository.class); }
    public UsuarioRepository getUsuarioRepository() { return getBean(UsuarioRepository.class); }
}

