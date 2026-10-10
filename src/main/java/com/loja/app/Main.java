package com.loja.app;

import com.formdev.flatlaf.FlatDarkLaf;
import com.loja.repository.*;
import com.loja.service.CaixaService;
import com.loja.view.MainFrame;
import com.loja.view.dialogs.ConfigBancoDialog;
import com.loja.view.dialogs.LoginDialog;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        // 1. Configurar Tema Moderno FlatLaf (Dark Grey / Graphite SaaS)
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

            UIManager.put("Table.background", t.getBgCard()); // #1A1B1D
            UIManager.put("Table.foreground", t.getTextPrimary()); // #F1F1F1
            UIManager.put("Table.alternateRowColor", new java.awt.Color(32, 33, 36)); // #202124
            UIManager.put("Table.gridColor", t.getBorderSubtle()); // #2A2C2F
            UIManager.put("Table.selectionBackground", new java.awt.Color(41, 42, 45)); // #292A2D
            UIManager.put("Table.selectionForeground", t.getTextPrimary());
            UIManager.put("TableHeader.background", t.getBgSidebar()); // #141516
            UIManager.put("TableHeader.foreground", t.getTextSecondary()); // #A8A8A8

            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            System.err.println("[AVISO] Não foi possível carregar o tema FlatLaf: " + ex.getMessage());
        }

        // 2. Exibir Splash Screen com logo transparente e barra de progresso animada
        com.loja.view.SplashScreen splash = com.loja.view.SplashScreen.mostrar();
        splash.setStatus("Carregando módulos visuais...", 15);

        // Executar inicialização em thread separada para manter a animação fluida a 60 FPS
        new Thread(() -> {
            try {
                Thread.sleep(150);

                // Conectar e inicializar banco de dados MySQL
                splash.setStatus("Conectando ao banco de dados MySQL...", 35);
                boolean dbOk = ConnectionFactory.testarConexao();

                if (dbOk) {
                    splash.setStatus("Verificando estrutura e tabelas...", 60);
                    ConnectionFactory.criarTabela();
                    com.loja.service.AutoBackupService.getInstance().iniciar();
                } else {
                    splash.setStatus("Aguardando configuração do servidor...", 60);
                }

                splash.setStatus("Inicializando serviços do sistema...", 85);
                CaixaDAO caixaDAO = new CaixaDAO();
                CaixaService caixaService = new CaixaService();
                ProdutoDAO produtoDAO = new ProdutoDAO();
                ClienteDAO clienteDAO = new ClienteDAO();
                EquipamentoDAO equipamentoDAO = new EquipamentoDAO();
                OrdemServicoDAO osDAO = new OrdemServicoDAO();
                UsuarioDAO usuarioDAO = new UsuarioDAO();

                if (dbOk) {
                    caixaService.abrirCaixa(100.00);
                }

                splash.setStatus("Sistema pronto!", 100);
                Thread.sleep(300); // Breve momento para o usuário ver a barra completa em 100%

                SwingUtilities.invokeLater(() -> {
                    splash.fechar();

                    boolean statusDb = dbOk;
                    if (!statusDb) {
                        // Se a conexão falhar ou for o primeiro acesso, exibe painel de configuração
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
                            System.err.println("[ERRO] Ao abrir painel de configuração: " + ex.getMessage());
                        }
                    }

                    final boolean isDbConectado = statusDb;

                    // 3. Exibir Tela de Autenticação / Login
                    LoginDialog login = new LoginDialog(null, usuarioDAO);
                    login.setVisible(true);

                    if (login.isAutenticado()) {
                        MainFrame frame = new MainFrame(clienteDAO, equipamentoDAO, osDAO, produtoDAO, caixaDAO, caixaService,
                                usuarioDAO, isDbConectado);
                        frame.setVisible(true);
                    } else {
                        System.exit(0);
                    }
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> splash.fechar());
            }
        }).start();
    }
}
