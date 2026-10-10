package com.loja.view;

import com.loja.service.update.UpdateService;
import com.loja.util.LogoUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Tela de inicialização (Splash Screen) moderna com animação da linha de progresso,
 * exibindo o logotipo transparente do System Pro, status das etapas e versão.
 */
public class SplashScreen extends JWindow {

    private String statusTexto = "Iniciando System Pro...";
    private int progressoAlvo = 0;
    private float progressoAtual = 0f;
    private float shimmerPos = 0f;
    private Timer animTimer;
    private Image logoImg;

    public SplashScreen() {
        super();
        setSize(460, 340);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        setBackground(new Color(0, 0, 0, 0));

        // Carregar logotipo do utilitário
        this.logoImg = LogoUtil.getLogoIcone();

        // Painel customizado de desenho 100% transparente
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                desenharSplash((Graphics2D) g);
            }
        };
        panel.setOpaque(false);
        setContentPane(panel);

        // Timer de animação suave a 60 FPS
        animTimer = new Timer(16, e -> {
            boolean precisaRepintar = false;

            // Interpolação suave em direção ao progresso alvo
            if (Math.abs(progressoAlvo - progressoAtual) > 0.3f) {
                progressoAtual += (progressoAlvo - progressoAtual) * 0.14f;
                precisaRepintar = true;
            } else if (progressoAtual != progressoAlvo) {
                progressoAtual = progressoAlvo;
                precisaRepintar = true;
            }

            // Animação contínua da luz/shimmer na barra
            shimmerPos = (shimmerPos + 0.02f) % 1.0f;
            precisaRepintar = true;

            if (precisaRepintar) {
                repaint();
            }
        });
        animTimer.start();
    }

    private void desenharSplash(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int w = getWidth();
        int h = getHeight();

        // 1. Logotipo Central Grande Flutuante (170x170)
        int logoSize = 170;
        int logoX = (w - logoSize) / 2;
        int logoY = 16;

        // Sombra suave sob a engrenagem para dar destaque e profundidade em qualquer papel de parede
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(logoX - 5, logoY - 5, logoSize + 10, logoSize + 10);

        if (logoImg != null) {
            g2.drawImage(logoImg, logoX, logoY, logoSize, logoSize, null);
        }

        // 2. Título "SYSTEM PRO" Flutuante com Sombra Suave
        int titY = logoY + logoSize + 28;
        g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
        FontMetrics fmTit = g2.getFontMetrics();
        String tit = "SYSTEM PRO";
        int titX = (w - fmTit.stringWidth(tit)) / 2;

        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(tit, titX + 2, titY + 2);
        g2.setColor(new Color(252, 253, 255));
        g2.drawString(tit, titX, titY);

        // 3. Subtítulo moderno em azul luminoso
        int subY = titY + 18;
        g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        FontMetrics fmSub = g2.getFontMetrics();
        String sub = "GESTÃO & EFICIÊNCIA OPERACIONAL";
        int subX = (w - fmSub.stringWidth(sub)) / 2;

        g2.setColor(new Color(0, 0, 0, 140));
        g2.drawString(sub, subX + 1, subY + 1);
        g2.setColor(new Color(96, 165, 250)); // Azul luminoso
        g2.drawString(sub, subX, subY);

        // 4. Linha de Carregamento Flutuante Translúcida
        int barW = 280;
        int barH = 5;
        int barX = (w - barW) / 2;
        int barY = subY + 24;

        // Trilho translúcido
        RoundRectangle2D.Float track = new RoundRectangle2D.Float(barX, barY, barW, barH, barH, barH);
        g2.setColor(new Color(255, 255, 255, 45));
        g2.fill(track);
        g2.setColor(new Color(0, 0, 0, 80));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(track);

        // Preenchimento animado em degradê azul/ciano
        float percentual = Math.max(0f, Math.min(100f, progressoAtual));
        int progressW = (int) ((barW * percentual) / 100f);

        if (progressW > 3) {
            RoundRectangle2D.Float fill = new RoundRectangle2D.Float(barX, barY, progressW, barH, barH, barH);
            GradientPaint gradFill = new GradientPaint(
                barX, barY, new Color(37, 99, 235),
                barX + progressW, barY, new Color(56, 189, 248)
            );
            g2.setPaint(gradFill);
            g2.fill(fill);

            // Shimmer / luz brilhante em movimento
            int shimW = 40;
            int shimX = barX + (int) (shimmerPos * progressW) - (shimW / 2);
            if (shimX >= barX && shimX + shimW <= barX + progressW) {
                g2.setPaint(new GradientPaint(
                    shimX, barY, new Color(255, 255, 255, 0),
                    shimX + (shimW / 2), barY, new Color(255, 255, 255, 130),
                    true
                ));
                g2.fill(new RoundRectangle2D.Float(shimX, barY, shimW, barH, barH, barH));
            }
        }

        // 5. Texto de Status com Sombra Suave
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        FontMetrics fmStatus = g2.getFontMetrics();
        int statusX = (w - fmStatus.stringWidth(statusTexto)) / 2;
        int statusY = barY + 22;

        g2.setColor(new Color(0, 0, 0, 150));
        g2.drawString(statusTexto, statusX + 1, statusY + 1);
        g2.setColor(new Color(226, 232, 240));
        g2.drawString(statusTexto, statusX, statusY);
    }

    /**
     * Atualiza o texto descritivo e o progresso percentual (0 a 100).
     */
    public void setStatus(String texto, int progressoPercentual) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> setStatus(texto, progressoPercentual));
            return;
        }
        this.statusTexto = texto != null ? texto : "";
        this.progressoAlvo = Math.max(0, Math.min(100, progressoPercentual));
        repaint();
    }

    /**
     * Exibe a Splash Screen centralizada na tela.
     */
    public static SplashScreen mostrar() {
        SplashScreen splash = new SplashScreen();
        splash.setVisible(true);
        return splash;
    }

    /**
     * Fecha e libera recursos da Splash Screen.
     */
    public void fechar() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::fechar);
            return;
        }
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
        setVisible(false);
        dispose();
    }
}
