package com.loja.util;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class LogoUtil {
    private static Image logoIcone = null;
    private static Image logoCompleta = null;
    private static List<Image> iconesJanela = null;

    static {
        carregarImagens();
    }

    private static synchronized void carregarImagens() {
        try {
            // 1. Tentar carregar do classpath (JAR / staging)
            URL urlIcone = LogoUtil.class.getResource("/icons/logo_icone.png");
            if (urlIcone != null) {
                logoIcone = new ImageIcon(urlIcone).getImage();
            } else {
                // Fallbacks do sistema de arquivos
                String[] caminhos = {
                    "src/main/resources/icons/logo_icone.png",
                    "gerador_executavel/aplicacao_pronta/logo_icone.png",
                    "gerador_executavel/logo_icone.png",
                    "logo_icone.png"
                };
                for (String c : caminhos) {
                    File f = new File(c);
                    if (f.exists()) {
                        logoIcone = new ImageIcon(f.getAbsolutePath()).getImage();
                        break;
                    }
                }
            }

            URL urlCompleta = LogoUtil.class.getResource("/icons/logo_completa.png");
            if (urlCompleta != null) {
                logoCompleta = new ImageIcon(urlCompleta).getImage();
            } else {
                String[] caminhos = {
                    "src/main/resources/icons/logo_completa.png",
                    "gerador_executavel/aplicacao_pronta/logo_completa.png",
                    "gerador_executavel/logo_completa.png",
                    "logo_completa.png"
                };
                for (String c : caminhos) {
                    File f = new File(c);
                    if (f.exists()) {
                        logoCompleta = new ImageIcon(f.getAbsolutePath()).getImage();
                        break;
                    }
                }
            }

            if (logoIcone != null) {
                iconesJanela = new ArrayList<>();
                int[] tamanhos = { 16, 24, 32, 48, 64, 128, 256 };
                for (int s : tamanhos) {
                    iconesJanela.add(logoIcone.getScaledInstance(s, s, Image.SCALE_SMOOTH));
                }
            }
        } catch (Exception e) {
            System.err.println("[AVISO] Não foi possível carregar as imagens do logo: " + e.getMessage());
        }
    }

    /**
     * Aplica os ícones multi-resolução à barra de título e barra de tarefas da janela (JFrame, JDialog, etc.)
     */
    public static void aplicarIconeJanela(Window window) {
        if (window == null) return;
        try {
            if (iconesJanela != null && !iconesJanela.isEmpty()) {
                window.setIconImages(iconesJanela);
            } else if (logoIcone != null) {
                window.setIconImage(logoIcone);
            }
        } catch (Exception e) {
            System.err.println("[AVISO] Erro ao aplicar ícone à janela: " + e.getMessage());
        }
    }

    public static Image getLogoIcone() {
        return logoIcone;
    }

    public static Image getLogoCompleta() {
        return logoCompleta;
    }

    public static ImageIcon getIconeRedimensionado(int largura, int altura) {
        if (logoIcone == null) return null;
        return new ImageIcon(logoIcone.getScaledInstance(largura, altura, Image.SCALE_SMOOTH));
    }

    public static ImageIcon getLogoCompletaRedimensionada(int largura, int altura) {
        if (logoCompleta == null) return null;
        return new ImageIcon(logoCompleta.getScaledInstance(largura, altura, Image.SCALE_SMOOTH));
    }
}
