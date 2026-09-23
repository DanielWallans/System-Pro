package com.loja.view;

import com.loja.model.TerminalSessao;
import com.loja.repository.ConnectionFactory;
import com.loja.repository.TerminalDAO;
import com.loja.service.TerminalService;
import com.loja.service.update.UpdateService;
import com.loja.util.NetworkUtils;
import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TerminaisPanel extends JPanel {
    private final MainFrame mainFrame;
    private final TerminalDAO terminalDAO;

    private JLabel lblTotalTerminais;
    private JLabel lblTerminaisOnline;
    private JLabel lblTerminaisAusentes;
    private JLabel lblTerminaisAlertas;

    private JTable tabelaTerminais;
    private DefaultTableModel tableModel;
    private List<TerminalSessao> listaTerminais = new ArrayList<>();

    private JLabel lblTerminalSelecionadoInfo;
    private JButton btnMensagemPrivada;
    private JButton btnKick;
    private JButton btnBloquear;
    private JButton btnNotificarUpdate;

    private JLabel lblInfoRede;
    private String ipMatrizParaCopiar;

    private Timer autoRefreshTimer;

    public TerminaisPanel(MainFrame mainFrame, TerminalDAO terminalDAO) {
        this.mainFrame = mainFrame;
        this.terminalDAO = terminalDAO != null ? terminalDAO : new TerminalDAO();

        setLayout(new BorderLayout(0, UITheme.SPACE_16));
        setBorder(BorderFactory.createEmptyBorder(UITheme.SPACE_16, UITheme.SPACE_24, UITheme.SPACE_16, UITheme.SPACE_24));

        initComponents();
        iniciarAutoRefresh();
        recarregarDados();
    }

    private void initComponents() {
        ThemeTokens t = UITheme.tokens();

        // 1. Painel Norte: Título + Cards de Indicadores
        JPanel pnlNorte = new JPanel();
        pnlNorte.setLayout(new BoxLayout(pnlNorte, BoxLayout.Y_AXIS));
        pnlNorte.setOpaque(false);

        // Cabeçalho
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);

        JLabel lblTit = new JLabel("Central de Máquinas & Terminais da Rede");
        lblTit.setFont(UITheme.FONT_TITLE.deriveFont(18f));
        lblTit.setForeground(t.getTextPrimary());

        JLabel lblSub = new JLabel("Monitore em tempo real as máquinas conectadas ao sistema, telas ativas e execute ações remotas.");
        lblSub.setFont(UITheme.FONT_SUBTITLE);
        lblSub.setForeground(t.getTextSecondary());

        JPanel pnlTitulos = new JPanel(new GridLayout(2, 1, 0, 4));
        pnlTitulos.setOpaque(false);
        pnlTitulos.add(lblTit);
        pnlTitulos.add(lblSub);

        pnlHeader.add(pnlTitulos, BorderLayout.WEST);

        // Barra de Ações Rápidas no Topo
        JPanel pnlAcoesTopo = new JPanel(new FlowLayout(FlowLayout.RIGHT, UITheme.SPACE_8, 0));
        pnlAcoesTopo.setOpaque(false);

        JButton btnBroadcast = UIComponents.criarBotaoPrimario("📢 Transmitir Aviso Broadcast", this::abrirModalBroadcast);
        btnBroadcast.setPreferredSize(new Dimension(240, 36));

        JButton btnAtualizar = UIComponents.criarBotaoSecundario("🔄 Atualizar", this::recarregarDados);
        btnAtualizar.setPreferredSize(new Dimension(110, 36));

        JButton btnLimpar = UIComponents.criarBotaoSecundario("🧹 Limpar Offline", this::limparTerminaisOffline);
        btnLimpar.setPreferredSize(new Dimension(130, 36));

        JButton btnPublicarUpdate = UIComponents.criarBotaoPrimario("🚀 Publicar Atualização", this::publicarAtualizacaoRede);
        btnPublicarUpdate.setPreferredSize(new Dimension(180, 36));

        pnlAcoesTopo.add(btnPublicarUpdate);
        pnlAcoesTopo.add(btnBroadcast);
        pnlAcoesTopo.add(btnAtualizar);
        pnlAcoesTopo.add(btnLimpar);
        pnlHeader.add(pnlAcoesTopo, BorderLayout.EAST);

        pnlNorte.add(pnlHeader);
        pnlNorte.add(Box.createVerticalStrut(UITheme.SPACE_12));

        // Banner Informativo de Conexão de Rede Local
        JPanel pnlBannerRede = new JPanel(new BorderLayout(10, 0));
        pnlBannerRede.setBackground(t.isDark() ? new Color(26, 32, 44) : new Color(237, 242, 247));
        pnlBannerRede.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(t.getPrimaryAccent(), 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        pnlBannerRede.putClientProperty("Component.arc", 8);

        lblInfoRede = new JLabel();
        lblInfoRede.setFont(UITheme.FONT_BODY);
        lblInfoRede.setForeground(t.getTextPrimary());

        JButton btnCopiarIp = new JButton("📋 Copiar IP");
        btnCopiarIp.setFont(UITheme.FONT_SMALL);
        btnCopiarIp.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCopiarIp.putClientProperty("JButton.arc", 6);
        btnCopiarIp.addActionListener(e -> {
            try {
                if (ipMatrizParaCopiar != null && !ipMatrizParaCopiar.isEmpty()) {
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(ipMatrizParaCopiar), null);
                    JOptionPane.showMessageDialog(mainFrame, "IP da Máquina Matriz (" + ipMatrizParaCopiar + ") copiado para a área de transferência!", "Copiado", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ignored) {}
        });

        atualizarBannerRede();

        pnlBannerRede.add(lblInfoRede, BorderLayout.CENTER);
        pnlBannerRede.add(btnCopiarIp, BorderLayout.EAST);

        pnlNorte.add(pnlBannerRede);
        pnlNorte.add(Box.createVerticalStrut(UITheme.SPACE_16));

        // Cards de Métricas
        JPanel pnlCards = new JPanel(new GridLayout(1, 4, UITheme.SPACE_12, 0));
        pnlCards.setOpaque(false);

        lblTotalTerminais = new JLabel("0");
        lblTerminaisOnline = new JLabel("0");
        lblTerminaisAusentes = new JLabel("0");
        lblTerminaisAlertas = new JLabel("0");

        pnlCards.add(UIComponents.criarCardMetrica("TOTAL TERMINAIS", lblTotalTerminais, "🖥️", t.getTextSecondary()));
        pnlCards.add(UIComponents.criarCardMetrica("ONLINE AGORA", lblTerminaisOnline, "🟢", t.getSuccess()));
        pnlCards.add(UIComponents.criarCardMetrica("AUSENTES / OCIOSOS", lblTerminaisAusentes, "🟡", t.getWarning()));
        pnlCards.add(UIComponents.criarCardMetrica("ALERTAS / BLOQUEIOS", lblTerminaisAlertas, "⚠️", t.getDanger()));

        pnlNorte.add(pnlCards);
        add(pnlNorte, BorderLayout.NORTH);

        // 2. Painel Central: Tabela Estilizada de Terminais
        JPanel pnlCentro = new JPanel(new BorderLayout(0, UITheme.SPACE_8));
        pnlCentro.setOpaque(false);

        String[] colunas = {
                "Status", "Computador & IP", "Operador / Usuário",
                "O que está fazendo agora (Tela)", "Versão Sistema", "Login / Último Ping"
        };

        tableModel = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabelaTerminais = new JTable(tableModel);
        tabelaTerminais.setRowHeight(38);
        tabelaTerminais.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaTerminais.getTableHeader().setReorderingAllowed(false);

        // Larguras das colunas
        tabelaTerminais.getColumnModel().getColumn(0).setPreferredWidth(100);
        tabelaTerminais.getColumnModel().getColumn(1).setPreferredWidth(210);
        tabelaTerminais.getColumnModel().getColumn(2).setPreferredWidth(170);
        tabelaTerminais.getColumnModel().getColumn(3).setPreferredWidth(200);
        tabelaTerminais.getColumnModel().getColumn(4).setPreferredWidth(110);
        tabelaTerminais.getColumnModel().getColumn(5).setPreferredWidth(160);

        // Renderizadores personalizados
        configurarRenderizadoresTabela();

        // Listener de seleção
        tabelaTerminais.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                atualizarBotoesAcao();
            }
        });

        // Menu de Contexto (botão direito)
        JPopupMenu menuContexto = criarMenuContexto();
        tabelaTerminais.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = tabelaTerminais.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        tabelaTerminais.setRowSelectionInterval(row, row);
                        menuContexto.show(tabelaTerminais, e.getX(), e.getY());
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(tabelaTerminais);
        scroll.setBorder(BorderFactory.createLineBorder(t.getBorderSubtle(), 1));
        pnlCentro.add(scroll, BorderLayout.CENTER);

        add(pnlCentro, BorderLayout.CENTER);

        // 3. Painel Sul: Barra de Ações para o Terminal Selecionado
        JPanel pnlSul = new JPanel(new BorderLayout());
        pnlSul.setOpaque(false);
        pnlSul.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(UITheme.SPACE_12, 0, 0, 0)
        ));

        lblTerminalSelecionadoInfo = new JLabel("Selecione um terminal na tabela para realizar ações.");
        lblTerminalSelecionadoInfo.setFont(UITheme.FONT_BODY);
        lblTerminalSelecionadoInfo.setForeground(t.getTextSecondary());

        JPanel pnlBotoesLinha = new JPanel(new FlowLayout(FlowLayout.RIGHT, UITheme.SPACE_8, 0));
        pnlBotoesLinha.setOpaque(false);

        btnMensagemPrivada = UIComponents.criarBotaoSecundario("💬 Enviar Mensagem", this::enviarMensagemTerminalSelecionado);
        btnKick = UIComponents.criarBotaoSecundario("🚪 Desconectar Remoto (Kick)", this::desconectarTerminalSelecionado);
        btnBloquear = UIComponents.criarBotaoSecundario("🔒 Bloquear Terminal", this::alternarBloqueioTerminalSelecionado);
        btnNotificarUpdate = UIComponents.criarBotaoSecundario("📢 Notificar Atualização", this::notificarAtualizacaoTerminalSelecionado);

        btnMensagemPrivada.setEnabled(false);
        btnKick.setEnabled(false);
        btnBloquear.setEnabled(false);
        btnNotificarUpdate.setEnabled(false);

        pnlBotoesLinha.add(btnNotificarUpdate);
        pnlBotoesLinha.add(btnMensagemPrivada);
        pnlBotoesLinha.add(btnKick);
        pnlBotoesLinha.add(btnBloquear);

        pnlSul.add(lblTerminalSelecionadoInfo, BorderLayout.WEST);
        pnlSul.add(pnlBotoesLinha, BorderLayout.EAST);

        add(pnlSul, BorderLayout.SOUTH);
    }

    private void configurarRenderizadoresTabela() {
        ThemeTokens t = UITheme.tokens();

        // Renderizador da Coluna 0: Status com Badges coloridos
        tabelaTerminais.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(getFont().deriveFont(Font.BOLD, 12f));

                if (value != null && !isSelected) {
                    String st = value.toString();
                    if (st.contains("ONLINE")) {
                        setForeground(t.getSuccess());
                    } else if (st.contains("AUSENTE")) {
                        setForeground(t.getWarning());
                    } else if (st.contains("BLOQUEADO")) {
                        setForeground(t.getDanger());
                    } else {
                        setForeground(t.getTextSecondary());
                    }
                }
                return c;
            }
        });

        // Renderizador da Coluna 4: Versão com destaque caso diferente
        tabelaTerminais.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (value != null && !isSelected) {
                    String v = value.toString();
                    if (v.contains("⚠️")) {
                        setForeground(t.getDanger());
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setForeground(t.getTextPrimary());
                        setFont(getFont().deriveFont(Font.PLAIN));
                    }
                }
                return c;
            }
        });
    }

    private JPopupMenu criarMenuContexto() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem mnuMsg = new JMenuItem("💬 Enviar Mensagem Privada");
        mnuMsg.addActionListener(e -> enviarMensagemTerminalSelecionado());

        JMenuItem mnuKick = new JMenuItem("🚪 Desconectar Remotamente (Kick)");
        mnuKick.addActionListener(e -> desconectarTerminalSelecionado());

        JMenuItem mnuBloq = new JMenuItem("🔒 Alternar Bloqueio do Terminal");
        mnuBloq.addActionListener(e -> alternarBloqueioTerminalSelecionado());

        JMenuItem mnuUpdate = new JMenuItem("📢 Notificar Atualização deste Terminal");
        mnuUpdate.addActionListener(e -> notificarAtualizacaoTerminalSelecionado());

        menu.add(mnuMsg);
        menu.add(mnuUpdate);
        menu.addSeparator();
        menu.add(mnuKick);
        menu.add(mnuBloq);

        return menu;
    }

    public void recarregarDados() {
        atualizarBannerRede();

        int linhaSelecionada = tabelaTerminais.getSelectedRow();
        String idSelecionado = null;
        if (linhaSelecionada >= 0 && linhaSelecionada < listaTerminais.size()) {
            idSelecionado = listaTerminais.get(linhaSelecionada).getId();
        }

        listaTerminais = terminalDAO.listarTodos();
        tableModel.setRowCount(0);

        int onlineCount = 0;
        int ausenteCount = 0;
        int alertaCount = 0;
        String versaoAtualServidor = UpdateService.VERSAO_ATUAL;

        int novaLinhaParaSelecionar = -1;
        String proprioSessaoId = TerminalService.getInstance().getSessaoId();

        for (int i = 0; i < listaTerminais.size(); i++) {
            TerminalSessao s = listaTerminais.get(i);

            if (idSelecionado != null && idSelecionado.equals(s.getId())) {
                novaLinhaParaSelecionar = i;
            }

            boolean isEsteTerminal = s.getId().equals(proprioSessaoId);
            boolean isMatriz = NetworkUtils.isTerminalMatriz(s, proprioSessaoId);

            String badgeStatus;
            if (s.isBloqueado()) {
                badgeStatus = "🔴 BLOQUEADO";
                alertaCount++;
            } else if (s.isOnline()) {
                badgeStatus = "🟢 ONLINE";
                onlineCount++;
            } else if (s.isAusente()) {
                badgeStatus = "🟡 AUSENTE";
                ausenteCount++;
            } else {
                badgeStatus = "⚪ OFFLINE";
            }

            // Destaque de versão divergente
            String versaoStr = "v" + (s.getVersaoSistema() != null ? s.getVersaoSistema() : "-");
            if (s.isVersaoDivergente(versaoAtualServidor)) {
                versaoStr = "⚠️ " + versaoStr + " (Desat.)";
                alertaCount++;
            }

            String tagMatriz = isMatriz ? " 🏢 [MATRIZ]" : "";
            String tagEstePc = isEsteTerminal ? " [Este PC]" : "";
            String computadorDesc = s.getHostname() + " (" + s.getIp() + ")" + tagMatriz + tagEstePc;
            String usuarioDesc = s.getUsuarioNome() + " (" + s.getUsuarioPerfil() + ")";
            String tempoDesc = s.getHoraLoginFormatada() + " (" + s.getUltimoVistoFormatado() + ")";

            tableModel.addRow(new Object[]{
                    badgeStatus,
                    computadorDesc,
                    usuarioDesc,
                    s.getTelaAtual(),
                    versaoStr,
                    tempoDesc
            });
        }

        // Atualiza Cards de Métricas
        lblTotalTerminais.setText(String.valueOf(listaTerminais.size()));
        lblTerminaisOnline.setText(String.valueOf(onlineCount));
        lblTerminaisAusentes.setText(String.valueOf(ausenteCount));
        lblTerminaisAlertas.setText(String.valueOf(alertaCount));

        // Restaura a seleção suavemente
        if (novaLinhaParaSelecionar >= 0 && novaLinhaParaSelecionar < tabelaTerminais.getRowCount()) {
            tabelaTerminais.setRowSelectionInterval(novaLinhaParaSelecionar, novaLinhaParaSelecionar);
        } else {
            atualizarBotoesAcao();
        }
    }

    private void atualizarBannerRede() {
        String hostBanco = ConnectionFactory.getHost();
        String portaBanco = ConnectionFactory.getPort();
        String ipMatriz = NetworkUtils.obterIpMatriz(hostBanco);
        boolean souMatriz = NetworkUtils.isEstaMaquinaMatriz();
        String meuIp = TerminalService.getInstance().getIp();
        this.ipMatrizParaCopiar = ipMatriz;

        if (lblInfoRede != null) {
            if (souMatriz) {
                lblInfoRede.setText("<html>🌐 <b>Máquina Matriz (Servidor MySQL):</b> <font color='#38bdf8'><b>" +
                        ipMatriz + "</b></font> (Porta: <b>" + portaBanco + "</b>) • <b>Servidor Principal Local</b> • Conecte outros computadores/terminais usando este IP.</html>");
            } else {
                lblInfoRede.setText("<html>🌐 <b>Máquina Matriz Conectada:</b> <font color='#38bdf8'><b>" +
                        ipMatriz + "</b></font> (Porta: <b>" + portaBanco + "</b>) • <b>IP deste Terminal:</b> <font color='#a3e635'><b>" +
                        meuIp + "</b></font> • Conectado à Matriz através da rede.</html>");
            }
        }
    }

    private TerminalSessao getTerminalSelecionado() {
        int row = tabelaTerminais.getSelectedRow();
        if (row >= 0 && row < listaTerminais.size()) {
            return listaTerminais.get(row);
        }
        return null;
    }

    private void atualizarBotoesAcao() {
        TerminalSessao s = getTerminalSelecionado();
        if (s == null) {
            lblTerminalSelecionadoInfo.setText("Selecione um terminal na tabela para realizar ações.");
            btnMensagemPrivada.setEnabled(false);
            btnKick.setEnabled(false);
            btnBloquear.setEnabled(false);
            if (btnNotificarUpdate != null) btnNotificarUpdate.setEnabled(false);
            btnBloquear.setText("🔒 Bloquear Terminal");
        } else {
            String status = s.getStatusCalculado();
            boolean isMatriz = NetworkUtils.isTerminalMatriz(s, TerminalService.getInstance().getSessaoId());
            String tagMatriz = isMatriz ? " [MATRIZ]" : "";
            lblTerminalSelecionadoInfo.setText(String.format("Terminal selecionado: %s • IP: %s%s • Operador: %s [%s]",
                    s.getHostname(), s.getIp(), tagMatriz, s.getUsuarioNome(), status));

            boolean isProprioPC = s.getId().equals(TerminalService.getInstance().getSessaoId());

            btnMensagemPrivada.setEnabled(!s.isOffline());
            btnKick.setEnabled(!isProprioPC && !s.isOffline() && !isMatriz);
            btnBloquear.setEnabled(!isProprioPC && !isMatriz);
            if (btnNotificarUpdate != null) {
                btnNotificarUpdate.setEnabled(!s.isOffline() && !isProprioPC);
            }

            if (s.isBloqueado()) {
                btnBloquear.setText("🔓 Desbloquear Terminal");
            } else {
                btnBloquear.setText("🔒 Bloquear Terminal");
            }
        }
    }

    private void abrirModalBroadcast() {
        JPanel pnl = new JPanel(new BorderLayout(0, UITheme.SPACE_8));
        JLabel lblAviso = new JLabel("Digite a mensagem que aparecerá instantaneamente em TODAS as máquinas conectadas:");
        lblAviso.setFont(UITheme.FONT_BODY);

        JTextArea txtMsg = new JTextArea(4, 30);
        txtMsg.setFont(UITheme.FONT_BODY);
        txtMsg.setLineWrap(true);
        txtMsg.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(txtMsg);

        pnl.add(lblAviso, BorderLayout.NORTH);
        pnl.add(scroll, BorderLayout.CENTER);

        int opt = JOptionPane.showConfirmDialog(mainFrame, pnl, "📢 Transmissão Broadcast para Toda a Loja",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (opt == JOptionPane.OK_OPTION) {
            String mensagem = txtMsg.getText().trim();
            if (mensagem.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "A mensagem não pode estar vazia!", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean ok = terminalDAO.enviarBroadcast("Administrador", mensagem);
            if (ok) {
                JOptionPane.showMessageDialog(mainFrame, "Aviso broadcast transmitido com sucesso para a rede!",
                        "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Não foi possível transmitir o aviso. Verifique o banco.",
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void enviarMensagemTerminalSelecionado() {
        TerminalSessao s = getTerminalSelecionado();
        if (s == null) return;

        String msg = JOptionPane.showInputDialog(mainFrame,
                "Digite a mensagem privada para " + s.getHostname() + " (" + s.getUsuarioNome() + "):",
                "Enviar Mensagem Direta", JOptionPane.QUESTION_MESSAGE);

        if (msg != null && !msg.trim().isEmpty()) {
            boolean ok = terminalDAO.enviarComando(s.getId(), "MENSAGEM:" + msg.trim());
            if (ok) {
                JOptionPane.showMessageDialog(mainFrame, "Mensagem enviada com sucesso para " + s.getHostname() + "!",
                        "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Falha ao enviar mensagem.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void desconectarTerminalSelecionado() {
        TerminalSessao s = getTerminalSelecionado();
        if (s == null) return;

        if (s.getId().equals(TerminalService.getInstance().getSessaoId())) {
            JOptionPane.showMessageDialog(mainFrame, "Você não pode desconectar a sua própria máquina administrativa por aqui!\nUse o botão 'Sair' no topo.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int opt = JOptionPane.showConfirmDialog(mainFrame,
                "Deseja realmente encerrar a sessão de " + s.getUsuarioNome() + " no computador " + s.getHostname() + "?\n" +
                        "O sistema daquela máquina será finalizado imediatamente.",
                "Confirmar Desconexão Remota", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            boolean ok = terminalDAO.enviarComando(s.getId(), "KICK:Desconectado pelo Administrador.");
            if (ok) {
                JOptionPane.showMessageDialog(mainFrame, "Comando de logout enviado com sucesso para " + s.getHostname() + "!",
                        "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                recarregarDados();
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Falha ao enviar comando de desconexão.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void alternarBloqueioTerminalSelecionado() {
        TerminalSessao s = getTerminalSelecionado();
        if (s == null) return;

        if (s.getId().equals(TerminalService.getInstance().getSessaoId())) {
            JOptionPane.showMessageDialog(mainFrame, "Você não pode bloquear o terminal do Administrador atual!",
                    "Ação Não Permitida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean novoBloqueio = !s.isBloqueado();
        String acaoStr = novoBloqueio ? "bloquear o acesso ao" : "desbloquear o";

        int opt = JOptionPane.showConfirmDialog(mainFrame,
                "Deseja " + acaoStr + " computador " + s.getHostname() + " (" + s.getUsuarioNome() + ")?",
                "Confirmar Ação", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            boolean ok = terminalDAO.alternarBloqueio(s.getId(), novoBloqueio);
            if (ok) {
                recarregarDados();
                String resultado = novoBloqueio ? "Terminal BLOQUEADO com sucesso!" : "Terminal DESBLOQUEADO com sucesso!";
                JOptionPane.showMessageDialog(mainFrame, resultado, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Erro ao alterar o status de bloqueio.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void notificarAtualizacaoTerminalSelecionado() {
        TerminalSessao s = getTerminalSelecionado();
        if (s == null) return;

        int opt = JOptionPane.showConfirmDialog(mainFrame,
                "Deseja enviar comando de atualização para o computador " + s.getHostname() + " (" + s.getUsuarioNome() + ")?\n\n" +
                "• Versão no computador: " + (s.getVersaoSistema() != null ? s.getVersaoSistema() : "-") + "\n" +
                "• Versão mais recente disponível: v" + UpdateService.VERSAO_ATUAL + "\n\n" +
                "A janela de atualização abrirá imediatamente na tela do operador com o que há de novo e o botão 'Atualizar Agora'.",
                "Notificar Atualização de Terminal", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            boolean ok = terminalDAO.enviarComando(s.getId(), "NOTIFICAR_ATUALIZACAO");
            if (ok) {
                JOptionPane.showMessageDialog(mainFrame,
                        "Notificação de atualização enviada para " + s.getHostname() + "!\nA janela de atualização abrirá no terminal.",
                        "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Falha ao enviar notificação de atualização.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void publicarAtualizacaoRede() {
        int opt = JOptionPane.showConfirmDialog(mainFrame,
                "Deseja disponibilizar a versão atual (v" + UpdateService.VERSAO_ATUAL + ")\n" +
                "no Banco de Dados para que todos os computadores da loja atualizem automaticamente?\n\n" +
                "• Tamanho do arquivo: ~20 MB\n" +
                "• Tempo de download nos caixas: 2 a 3 segundos (LAN)\n" +
                "• Sem necessidade de reinstalação ou pendrive.",
                "Publicar Atualização na Rede Local", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            File jar = UpdateService.localizarArquivoJarAtual();
            if (jar == null || !jar.exists()) {
                JOptionPane.showMessageDialog(mainFrame,
                        "Arquivo JAR da aplicação não foi localizado automaticamente nos caminhos padrão.\n" +
                        "Certifique-se de que o sistema foi compilado.",
                        "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String novidades = UpdateService.carregarNovidadesVersaoAtual();
            String data = UpdateService.carregarDataVersaoAtual();

            boolean sucesso = UpdateService.publicarVersaoNoBanco(UpdateService.VERSAO_ATUAL, data, novidades, jar);
            if (sucesso) {
                terminalDAO.enviarBroadcast("Sistema Matriz", "Uma nova atualização do sistema (v" + UpdateService.VERSAO_ATUAL + ") foi publicada! A janela de atualização abrirá para aplicar as novidades.");
                JOptionPane.showMessageDialog(mainFrame,
                        "Versão v" + UpdateService.VERSAO_ATUAL + " publicada no banco de dados com sucesso!\n" +
                        "Todos os computadores da rede já podem atualizar com 1 clique.",
                        "Atualização Publicada", JOptionPane.INFORMATION_MESSAGE);
                recarregarDados();
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Falha ao publicar versão no banco de dados. Verifique a conexão MySQL.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void limparTerminaisOffline() {
        int opt = JOptionPane.showConfirmDialog(mainFrame,
                "Deseja remover da lista todos os terminais desconectados (OFFLINE) há mais de 24 horas?",
                "Limpeza de Registros Antigos", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            int removidos = terminalDAO.limparTerminaisInativos(24);
            recarregarDados();
            JOptionPane.showMessageDialog(mainFrame, "Limpeza concluída! " + removidos + " registro(s) antigo(s) removido(s).",
                    "Limpeza Concluída", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void iniciarAutoRefresh() {
        autoRefreshTimer = new Timer(10000, e -> {
            if (isShowing()) {
                recarregarDados();
            }
        });
        autoRefreshTimer.start();
    }
}
