package com.loja.view;

import com.loja.model.*;
import com.loja.repository.CaixaDAO;
import com.loja.repository.ClienteDAO;
import com.loja.repository.EquipamentoDAO;
import com.loja.repository.OrdemServicoDAO;
import com.loja.repository.ProdutoDAO;
import com.loja.service.CaixaService;
import com.loja.service.ComprovanteEntregaPDFService;
import com.loja.repository.OSFotoDAO;
import com.loja.view.dialogs.AberturaCaixaDialog;
import com.loja.view.dialogs.DetalhesOSDialog;
import com.loja.view.dialogs.GerenciadorFotosOSDialog;
import com.loja.view.dialogs.MontarOrcamentoDialog;
import com.loja.view.dialogs.NovaOSDialog;
import com.loja.view.dialogs.PagamentoPDVDialog;

import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OrdemServicoPanel extends JPanel {
    private JButton btnNovaOS;
    private JButton btnMontarOrcamento;
    private JButton btnFinalizarOS;
    private JButton btnExcluirOS;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OrdemServicoDAO osDAO;
    private final ClienteDAO clienteDAO;
    private final EquipamentoDAO equipDAO;
    private final ProdutoDAO produtoDAO;
    private final CaixaService caixaService;
    private final Frame owner;

    private JTable tabela;
    private DefaultTableModel tableModel;
    private JComboBox<String> cbFiltroStatus;
    private JLabel lblContador;

    public OrdemServicoPanel(Frame owner, OrdemServicoDAO osDAO, ClienteDAO clienteDAO,
            EquipamentoDAO equipDAO, ProdutoDAO produtoDAO, CaixaService caixaService) {
        this.owner = owner;
        this.osDAO = osDAO;
        this.clienteDAO = clienteDAO;
        this.equipDAO = equipDAO;
        this.produtoDAO = produtoDAO;
        this.caixaService = caixaService;

        setLayout(new BorderLayout(0, UITheme.SPACE_16));
        setBorder(BorderFactory.createEmptyBorder(UITheme.SPACE_20, UITheme.SPACE_24, UITheme.SPACE_20,
                UITheme.SPACE_24));

        initComponents();
        recarregarTabela();
    }

    private void initComponents() {
        ThemeTokens t = UITheme.tokens();

        // 1. Top Panel
        JPanel topPanel = new JPanel(new BorderLayout(UITheme.SPACE_16, 0));
        topPanel.setOpaque(false);

        JPanel pnlTitulo = new JPanel(new BorderLayout(0, UITheme.SPACE_4));
        pnlTitulo.setOpaque(false);
        JLabel lblTitulo = new JLabel("Ordens de Serviço");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitulo.setForeground(t.getTextPrimary());
        JLabel lblSub = new JLabel("Gerencie o fluxo de manutenção, laudos técnicos e orçamentos.");
        lblSub.setFont(UITheme.FONT_SUBTITLE);
        lblSub.setForeground(t.getTextSecondary());
        pnlTitulo.add(lblTitulo, BorderLayout.NORTH);
        pnlTitulo.add(lblSub, BorderLayout.SOUTH);

        JPanel pnlAcoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, UITheme.SPACE_8, 0));
        pnlAcoes.setOpaque(false);

        cbFiltroStatus = new JComboBox<>(new String[] {
                "Todos os Status",
                "Aguardando Orçamento",
                "Aguardando Aprovação do Cliente",
                "Aprovado - Em Manutenção",
                "Aguardando Peça",
                "Pronto (Aguardando Retirada)",
                "Entregue (Finalizado)",
                "Orçamento Recusado / Cancelada"
        });
        cbFiltroStatus.setFont(UITheme.FONT_BODY);
        cbFiltroStatus.addActionListener(e -> filtrarPorStatus());

        this.btnNovaOS = UIComponents.criarBotaoPrimario("+ Abrir Nova OS [F2]", this::abrirNovaOS);
        this.btnMontarOrcamento = UIComponents.criarBotaoSecundario("Montar Orçamento", this::abrirMontarOrcamentoSelecionado);
        this.btnFinalizarOS = UIComponents.criarBotaoPrimario("Finalizar OS [F4]", this::abrirFinalizarOSSelecionada);
        JButton btnDetalhes = UIComponents.criarBotaoSecundario("Ver Detalhes", this::abrirDetalhesOSSelecionada);
        this.btnExcluirOS = UIComponents.criarBotaoSecundario("Excluir OS", this::excluirOSSelecionada);
        this.btnExcluirOS.setToolTipText("Excluir permanentemente a OS selecionada (Apenas Administrador)");
        JButton btnAtualizar = UIComponents.criarBotaoSecundario("Atualizar [F5]", this::recarregarTabela);

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0),
                "finalizarOS");
        getActionMap().put("finalizarOS", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abrirFinalizarOSSelecionada();
            }
        });

        JLabel lblFiltro = new JLabel("Status:");
        lblFiltro.setFont(UITheme.FONT_CAPTION);
        lblFiltro.setForeground(t.getTextSecondary());

        pnlAcoes.add(lblFiltro);
        pnlAcoes.add(cbFiltroStatus);
        pnlAcoes.add(btnNovaOS);
        pnlAcoes.add(this.btnMontarOrcamento);
        pnlAcoes.add(this.btnFinalizarOS);
        JButton btnFotos = UIComponents.criarBotaoSecundario("Fotos da OS", this::abrirFotosOSSelecionada);
        pnlAcoes.add(btnFotos);
        pnlAcoes.add(btnDetalhes);
        pnlAcoes.add(this.btnExcluirOS);
        pnlAcoes.add(btnAtualizar);

        topPanel.add(pnlTitulo, BorderLayout.WEST);
        topPanel.add(pnlAcoes, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 2. Tabela de OS
        String[] colunas = { "OS #", "Entrada", "Cliente", "Aparelho / Equipamento", "Status", "Mão de Obra",
                "Total (R$)", "Saída" };
        tableModel = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabela = new JTable(tableModel);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(60);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(120);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(160);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(180);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(170);
        tabela.getColumnModel().getColumn(5).setPreferredWidth(100);
        tabela.getColumnModel().getColumn(6).setPreferredWidth(110);
        tabela.getColumnModel().getColumn(7).setPreferredWidth(120);

        // Aplica o padrão SaaS de tabela moderna com Pills translúcidos na coluna 4
        // (Status)
        UIComponents.formatarTabelaModerna(tabela, 4);

        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    abrirDetalhesOSSelecionada();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tabela);
        scrollPane.setBorder(BorderFactory.createLineBorder(t.getBorderSubtle(), 1));
        add(scrollPane, BorderLayout.CENTER);

        // 3. Rodapé
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottomPanel.setOpaque(false);
        lblContador = new JLabel("Total de Ordens de Serviço: 0");
        lblContador.setFont(UITheme.FONT_SMALL);
        lblContador.setForeground(t.getTextSecondary());
        bottomPanel.add(lblContador);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void recarregarTabela() {
        aplicarPermissoesPerfil();
        UIComponents.formatarTabelaModerna(tabela, 4);
        tableModel.setRowCount(0);
        List<OrdemServico> lista = osDAO.buscarTodos();
        String filtro = (String) cbFiltroStatus.getSelectedItem();

        int contagem = 0;
        for (OrdemServico os : lista) {
            if (filtro != null && !filtro.equals("Todos os Status") && !filtro.equalsIgnoreCase(os.getStatus())) {
                continue;
            }

            Cliente c = clienteDAO.buscarPorId(os.getClienteId());
            Equipamento eq = equipDAO.buscarPorId(os.getEquipamentoId());

            String nomeCliente = c != null ? c.getNome() : "Cliente #" + os.getClienteId();
            String descEquip = eq != null ? eq.getTipo() + " " + eq.getMarca() + " " + eq.getModelo()
                    : "Equipamento #" + os.getEquipamentoId();

            tableModel.addRow(new Object[] {
                    os.getId(),
                    os.getDataEntrada() != null ? os.getDataEntrada().format(formatter) : "-",
                    nomeCliente,
                    descEquip,
                    os.getStatus(),
                    String.format("R$ %.2f", os.getValorServico()),
                    String.format("R$ %.2f", os.getValorTotal()),
                    os.getDataSaida() != null ? os.getDataSaida().format(formatter) : "Em aberto"
            });
            contagem++;
        }
        lblContador.setText("Ordens de Serviço listadas: " + contagem);
    }

    private void filtrarPorStatus() {
        recarregarTabela();
    }

    public void abrirNovaOS() {
        Usuario user = SessaoUsuario.getInstancia().getUsuarioLogado();
        if (user != null && user.isTecnico()) {
            JOptionPane.showMessageDialog(this,
                    "O perfil Técnico possui acesso apenas para visualização e atualização de Ordens de Serviço.\n" +
                            "A abertura de novas Ordens de Serviço deve ser realizada pelo Atendente ou Administrador.",
                    "Acesso Restrito", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        NovaOSDialog dialog = new NovaOSDialog(owner, clienteDAO, equipDAO, osDAO);
        dialog.setVisible(true);
        if (dialog.isSalvo()) {
            recarregarTabela();
        }
    }

    private void abrirMontarOrcamentoSelecionado() {
        int row = tabela.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma Ordem de Serviço na tabela para montar o orçamento!",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int osId = (int) tableModel.getValueAt(row, 0);
        OrdemServico os = osDAO.buscarPorId(osId);
        if (os != null) {
            MontarOrcamentoDialog dialog = new MontarOrcamentoDialog(owner, os, osDAO, clienteDAO, equipDAO,
                    produtoDAO);
            dialog.setVisible(true);
            if (dialog.isSalvo()) {
                recarregarTabela();
            }
        }
    }

    private void abrirDetalhesOSSelecionada() {
        int row = tabela.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma Ordem de Serviço na tabela para visualizar!", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int osId = (int) tableModel.getValueAt(row, 0);
        OrdemServico os = osDAO.buscarPorId(osId);
        if (os != null) {
            DetalhesOSDialog dialog = new DetalhesOSDialog(owner, os, osDAO, clienteDAO, equipDAO, produtoDAO,
                    caixaService);
            dialog.setVisible(true);
            if (dialog.isAlterado()) {
                recarregarTabela();
            }
        }
    }

    private void abrirFotosOSSelecionada() {
        int row = tabela.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma Ordem de Serviço na tabela para visualizar ou anexar fotos!",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int osId = (int) tableModel.getValueAt(row, 0);
        GerenciadorFotosOSDialog dialog = new GerenciadorFotosOSDialog(owner, osId, new OSFotoDAO());
        dialog.setVisible(true);
    }

    public void abrirFinalizarOSSelecionada() {
        int row = tabela.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Selecione uma Ordem de Serviço na tabela para finalizar e receber o pagamento!", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int osId = (int) tableModel.getValueAt(row, 0);
        OrdemServico os = osDAO.buscarPorId(osId);
        if (os == null)
            return;

        if (os.getStatus() != null && os.getStatus().contains("Entregue")) {
            JOptionPane.showMessageDialog(this,
                    "A Ordem de Serviço #" + osId + " já está com status 'Entregue (Finalizado)'!", "OS Já Finalizada",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Cliente cliente = clienteDAO.buscarPorId(os.getClienteId());
        Equipamento equip = equipDAO.buscarPorId(os.getEquipamentoId());

        CaixaDAO caixaDAO = new CaixaDAO();
        if (caixaDAO.obterSessaoAberta() == null) {
            int opt = JOptionPane.showConfirmDialog(this,
                    "O CAIXA ESTÁ FECHADO!\n\nPara receber o pagamento da OS #" + os.getId()
                            + ", é necessário abrir o turno.\nDeseja abrir o caixa agora?",
                    "Caixa Fechado", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (opt == JOptionPane.YES_OPTION) {
                AberturaCaixaDialog abDialog = new AberturaCaixaDialog(owner, caixaService, caixaDAO);
                abDialog.setVisible(true);
            }
            if (caixaDAO.obterSessaoAberta() == null) {
                JOptionPane.showMessageDialog(this, "O caixa precisa estar aberto para registrar o pagamento da OS.",
                        "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        double totalPagar = os.getValorTotal();
        if (totalPagar <= 0) {
            String input = JOptionPane.showInputDialog(this,
                    "A OS #" + os.getId() + " está sem valor cadastrado (R$ 0,00).\n\n" +
                            "Informe o valor total a cobrar para ir ao pagamento no PDV (R$):\n" +
                            "(Ou informe 0 para finalizar como Cortesia/Garantia sem custo)",
                    "100.00");
            if (input == null) {
                return; // Cancelou
            }
            try {
                totalPagar = Double.parseDouble(input.trim().replace(",", "."));
                if (totalPagar < 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Valor numérico inválido informado!", "Aviso",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (totalPagar <= 0) {
                int opt = JOptionPane.showConfirmDialog(this,
                        "Confirmar a finalização da OS #" + os.getId() + " SEM COBRANÇA (Cortesia / Garantia R$ 0,00)?",
                        "Finalizar sem Custo", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (opt == JOptionPane.YES_OPTION) {
                    osDAO.atualizarStatusEServico(os.getId(), "Entregue (Finalizado)", os.getValorServico(),
                            os.getDiagnosticoTecnico());
                    os.setStatus("Entregue (Finalizado)");
                    os.setDataSaida(LocalDateTime.now());
                    recarregarTabela();
                    perguntarComprovanteEntrega(os, cliente, equip);
                }
                return;
            } else {
                // Atualiza o valor de serviço da OS com o valor informado
                osDAO.atualizarStatusEServico(os.getId(), os.getStatus(), totalPagar, os.getDiagnosticoTecnico());
                os.setValorServico(totalPagar);
                os.setValorTotal(totalPagar);
            }
        }

        PagamentoPDVDialog pagDialog = new PagamentoPDVDialog(owner, totalPagar, caixaDAO);
        pagDialog.setTitle("Recebimento da OS #" + os.getId() + " no PDV - Total a Pagar: R$ "
                + String.format("%.2f", totalPagar));
        pagDialog.setVisible(true);

        if (pagDialog.isConfirmado()) {
            caixaDAO.registrarRecebimentoOS(os.getId(), pagDialog.getPagamentos());
            osDAO.atualizarStatusEServico(os.getId(), "Entregue (Finalizado)", os.getValorServico(),
                    os.getDiagnosticoTecnico());
            os.setStatus("Entregue (Finalizado)");
            os.setDataSaida(LocalDateTime.now());

            JOptionPane.showMessageDialog(this,
                    "OS #" + os.getId() + " finalizada com sucesso!\nPagamento de R$ "
                            + String.format("%.2f", totalPagar) + " registrado no Caixa.",
                    "OS Finalizada", JOptionPane.INFORMATION_MESSAGE);

            recarregarTabela();
            perguntarComprovanteEntrega(os, cliente, equip);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Pagamento cancelado. A OS #" + os.getId() + " permanece com status atual.", "Cancelado",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void perguntarComprovanteEntrega(OrdemServico os, Cliente cliente, Equipamento equip) {
        int optPDF = JOptionPane.showConfirmDialog(owner,
                "Deseja gerar o Comprovante Oficial de Entrega em PDF agora?",
                "Comprovante de Entrega", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (optPDF == JOptionPane.YES_OPTION) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Salvar Comprovante de Entrega em PDF");
            fileChooser.setSelectedFile(new File("Comprovante_Entrega_OS_" + os.getId() + ".pdf"));
            if (fileChooser.showSaveDialog(owner) == JFileChooser.APPROVE_OPTION) {
                File arquivoDestino = fileChooser.getSelectedFile();
                if (!arquivoDestino.getName().toLowerCase().endsWith(".pdf")) {
                    arquivoDestino = new File(arquivoDestino.getAbsolutePath() + ".pdf");
                }
                try {
                    List<ServicoItem> servicos = osDAO.obterServicosOS(os.getId());
                    if (servicos.isEmpty() && os.getValorServico() > 0) {
                        servicos.add(new ServicoItem("Mão de Obra Geral / Manutenção", os.getValorServico()));
                    }
                    List<PecaItem> pecas = osDAO.obterPecasItensOS(os.getId());
                    List<HistoricoOS> historico = osDAO.obterHistoricoOS(os.getId());
                    ComprovanteEntregaPDFService.gerarComprovanteEntregaPDF(arquivoDestino, os, cliente, equip,
                            servicos, pecas, historico);
                    int opt = JOptionPane.showConfirmDialog(owner,
                            "Comprovante de Entrega gerado com sucesso em:\n" + arquivoDestino.getAbsolutePath()
                                    + "\n\nDeseja abrir o arquivo agora?",
                            "PDF Gerado", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (opt == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(arquivoDestino);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(owner, "Erro ao gerar PDF: " + ex.getMessage(), "Erro",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    // Apply UI permissions based on user profile
    public void aplicarPermissoesPerfil() {
        Usuario user = SessaoUsuario.getInstancia().getUsuarioLogado();
        boolean canMontarOrcamento = user == null || user.isAdmin() || user.isTecnico();
        boolean canAbrirOS = user == null || user.isAdmin() || user.isAtendente();
        boolean isAdmin = user != null && user.isAdmin();

        if (btnMontarOrcamento != null) {
            btnMontarOrcamento.setVisible(canMontarOrcamento);
        }
        if (btnNovaOS != null) {
            btnNovaOS.setVisible(canAbrirOS);
        }
        if (btnFinalizarOS != null) {
            btnFinalizarOS.setVisible(true);
        }
        if (btnExcluirOS != null) {
            btnExcluirOS.setVisible(isAdmin);
        }
    }

    private void excluirOSSelecionada() {
        Usuario user = SessaoUsuario.getInstancia().getUsuarioLogado();
        if (user == null || !user.isAdmin()) {
            JOptionPane.showMessageDialog(this,
                    "Acesso Restrito!\nApenas usuários com perfil Administrador possuem permissão para excluir Ordens de Serviço.",
                    "Acesso Negado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int row = tabela.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma Ordem de Serviço na tabela para excluir!", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int osId = (int) tableModel.getValueAt(row, 0);

        int opt = JOptionPane.showConfirmDialog(this,
                "ATENÇÃO: Deseja realmente excluir permanentemente a Ordem de Serviço #" + osId + "?\n\n" +
                "Esta ação apagará todas as peças, serviços, fotos e histórico vinculados a esta OS.\n" +
                "Esta operação não poderá ser desfeita!",
                "Confirmar Exclusão de OS", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            boolean sucesso = osDAO.excluir(osId);
            if (sucesso) {
                JOptionPane.showMessageDialog(this, "Ordem de Serviço #" + osId + " excluída com sucesso!", "Sucesso",
                        JOptionPane.INFORMATION_MESSAGE);
                recarregarTabela();
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao excluir a Ordem de Serviço #" + osId + " do banco de dados.",
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
