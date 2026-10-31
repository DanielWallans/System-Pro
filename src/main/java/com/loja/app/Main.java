package com.loja.app;

import com.formdev.flatlaf.FlatDarkLaf;
import com.loja.context.AppContext;
import com.loja.repository.*;
import com.loja.service.CaixaService;
import com.loja.util.AppLogger;
import com.loja.view.MainFrame;
import com.loja.view.dialogs.ConfigBancoDialog;
import com.loja.view.dialogs.LoginDialog;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        // 1. Configurar Tema Moderno FlatLaf (Dark Graphite SaaS)
        try {
            com.loja.view.theme.UITheme.setDark(true);
            com.loja.view.theme.ThemeTokens t = com.loja.view.theme.UITheme.tokens();

            UIManager.put("Button.arc", 6);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 6);
            UIManager.put("TabbedPane.tabArc", 6);
            UIManager.put("Table.arc", 8);

            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
            UIManager.put("Table.rowHeight", 34);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 1));
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Component.innerFocusWidth", 0);
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("TabbedPane.tabHeight", 34);

            UIManager.put("Component.accentColor", t.getPrimaryAccent());
            UIManager.put("Component.focusColor", com.loja.view.theme.UITheme.withAlpha(t.getFocusRing(), 0.35f));
            UIManager.put("Component.focusedBorderColor", t.getFocusRing());

            UIManager.put("Table.background", t.getBgCard());
            UIManager.put("Table.foreground", t.getTextPrimary());
            UIManager.put("Table.alternateRowColor", new java.awt.Color(32, 33, 36));
            UIManager.put("Table.gridColor", t.getBorderSubtle());
            UIManager.put("Table.selectionBackground", new java.awt.Color(41, 42, 45));
            UIManager.put("Table.selectionForeground", t.getTextPrimary());
            UIManager.put("TableHeader.background", t.getBgSidebar());
            UIManager.put("TableHeader.foreground", t.getTextSecondary());

            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            AppLogger.warn(Main.class, "Não foi possível carregar o tema FlatLaf: " + ex.getMessage());
        }

        // 2. Exibir Splash Screen com animação fluida
        com.loja.view.SplashScreen splash = com.loja.view.SplashScreen.mostrar();
        splash.setStatus("Carregando módulos visuais...", 15);

        new Thread(() -> {
            try {
                Thread.sleep(150);

                // Conectar e inicializar banco de dados MySQL
                splash.setStatus("Conectando ao banco de dados MySQL...", 35);
                boolean dbOk = ConnectionFactory.testarConexao();

                if (dbOk) {
                    splash.setStatus("Verificando estrutura e tabelas...", 55);
                    ConnectionFactory.criarTabela();
                    com.loja.service.AutoBackupService.getInstance().iniciar();
                } else {
                    splash.setStatus("Aguardando configuração do servidor...", 55);
                }

                // Inicializar Container de Dependências (IoC)
                splash.setStatus("Inicializando arquitetura e serviços...", 80);
                AppContext.getInstance().inicializar();

                CaixaService caixaService = AppContext.getInstance().getCaixaService();
                ProdutoDAO produtoDAO = AppContext.getInstance().getBean(ProdutoDAO.class);
                ClienteDAO clienteDAO = AppContext.getInstance().getBean(ClienteDAO.class);
                EquipamentoDAO equipamentoDAO = AppContext.getInstance().getBean(EquipamentoDAO.class);
                OrdemServicoDAO osDAO = AppContext.getInstance().getBean(OrdemServicoDAO.class);
                CaixaDAO caixaDAO = AppContext.getInstance().getBean(CaixaDAO.class);
                UsuarioDAO usuarioDAO = AppContext.getInstance().getBean(UsuarioDAO.class);

                if (dbOk) {
                    caixaService.abrirCaixa(100.00);
                }

                splash.setStatus("Sistema pronto!", 100);
                Thread.sleep(250);

                SwingUtilities.invokeLater(() -> {
                    splash.fechar();

                    boolean statusDb = dbOk;
                    if (!statusDb) {
                        try {
                            ConfigBancoDialog dlg = new ConfigBancoDialog(null);
                            dlg.setVisible(true);
                            statusDb = dlg.isConfiguradoComSucesso();
                            if (statusDb) {
                                ConnectionFactory.criarTabela();
                                com.loja.service.AutoBackupService.getInstance().iniciar();
                                caixaService.abrirCaixa(100.00);
                            }
                        } catch (Exception ex) {
                            AppLogger.error(Main.class, "Erro ao abrir painel de configuração de banco", ex);
                        }
                    }

                    final boolean isDbConectado = statusDb;

                    // 3. Autenticação de Usuário
                    LoginDialog login = new LoginDialog(null, usuarioDAO);
                    login.setVisible(true);

                    if (login.isAutenticado()) {
                        MainFrame frame = new MainFrame(clienteDAO, equipamentoDAO, osDAO, produtoDAO, caixaDAO,
                                caixaService,
                                usuarioDAO, isDbConectado);
                        frame.setVisible(true);
                    } else {
                        System.exit(0);
                    }
                });

            } catch (Exception ex) {
                AppLogger.error(Main.class, "Erro crítico durante o ciclo de inicialização", ex);
                SwingUtilities.invokeLater(splash::fechar);
            }
        }, "App-Bootstrap-Thread").start();
    }
}
