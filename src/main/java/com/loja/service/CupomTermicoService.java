package com.loja.service;

import com.loja.model.CaixaSessao;
import com.loja.model.ItemVenda;
import com.loja.model.PagamentoItem;
import com.loja.model.VendaPDV;

import javax.swing.*;
import java.awt.*;
import java.awt.print.*;
import java.time.format.DateTimeFormatter;

public class CupomTermicoService {

    private static final DateTimeFormatter FORMATTER_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Gera o texto formatado para bobina térmica (80mm ou 58mm) do Cupom de Venda Balcão / Não Fiscal.
     */
    public static String gerarTextoCupomVenda(VendaPDV venda, String nomeLoja, String cnpj, String endereco, String telefone, String operador, boolean is80mm) {
        int colunas = is80mm ? 48 : 32;
        String div = "-".repeat(colunas);
        String divDupla = "=".repeat(colunas);

        StringBuilder sb = new StringBuilder();
        sb.append(divDupla).append("\n");

        String titulo = (nomeLoja != null && !nomeLoja.trim().isEmpty()) ? nomeLoja.trim().toUpperCase() : "SYSTEM PRO - GESTÃO";
        sb.append(centralizar(titulo, colunas)).append("\n");

        if (cnpj != null && !cnpj.trim().isEmpty()) {
            sb.append(centralizar("CNPJ: " + cnpj.trim(), colunas)).append("\n");
        }
        if (endereco != null && !endereco.trim().isEmpty()) {
            sb.append(centralizar(endereco.trim(), colunas)).append("\n");
        }
        if (telefone != null && !telefone.trim().isEmpty()) {
            sb.append(centralizar("Tel: " + telefone.trim(), colunas)).append("\n");
        }
        sb.append(divDupla).append("\n");

        sb.append(centralizar("CUPOM NÃO FISCAL - VENDA BALCÃO", colunas)).append("\n");
        sb.append(String.format("VENDA Nº:          #%d\n", venda.getId()));
        sb.append(String.format("DATA / HORA:       %s\n", venda.getDataHora() != null ? venda.getDataHora().format(FORMATTER_HORA) : "-"));
        if (operador != null && !operador.trim().isEmpty()) {
            sb.append(String.format("OPERADOR:          %s\n", truncar(operador.trim(), colunas - 19)));
        }
        sb.append(div).append("\n");

        if (is80mm) {
            // Layout 80mm (48 colunas): ITEM CÓD DESCRIÇÃO QTD VL.UN TOTAL
            sb.append(String.format("%-4s %-20s %3s %8s %9s\n", "ITEM", "DESCRIÇÃO", "QTD", "VL.UN", "TOTAL"));
            sb.append(div).append("\n");

            int idx = 1;
            for (ItemVenda item : venda.getItens()) {
                String nome = truncar(item.getNomeProduto(), 20);
                sb.append(String.format("%03d  %-20s %3d %8.2f %9.2f\n",
                        idx++, nome, item.getQuantidade(), item.getValorUnitario(), item.getSubtotal()));
            }
        } else {
            // Layout 58mm (32 colunas): Duas linhas por item
            sb.append(String.format("%-20s %11s\n", "ITEM / DESCRIÇÃO", "TOTAL"));
            sb.append(div).append("\n");

            int idx = 1;
            for (ItemVenda item : venda.getItens()) {
                sb.append(String.format("%02d. %s\n", idx++, truncar(item.getNomeProduto(), 28)));
                String qtdPreco = String.format("  %dx R$ %.2f", item.getQuantidade(), item.getValorUnitario());
                String totalItem = String.format("R$ %.2f", item.getSubtotal());
                sb.append(formatarLinha(qtdPreco, totalItem, colunas)).append("\n");
            }
        }

        sb.append(div).append("\n");
        sb.append(formatarLinha("SUBTOTAL:", String.format("R$ %.2f", venda.getSubtotal()), colunas)).append("\n");
        if (venda.getDesconto() > 0) {
            sb.append(formatarLinha("(-) DESCONTO:", String.format("-R$ %.2f", venda.getDesconto()), colunas)).append("\n");
        }
        sb.append(formatarLinha("TOTAL A PAGAR:", String.format("R$ %.2f", venda.getValorTotal()), colunas)).append("\n");
        sb.append(div).append("\n");

        sb.append("FORMA(S) DE PAGAMENTO:\n");
        if (venda.getPagamentos() != null && !venda.getPagamentos().isEmpty()) {
            for (PagamentoItem p : venda.getPagamentos()) {
                String mod = p.getModalidadeFormatada();
                if (p.getParcelas() > 1) {
                    mod += " (" + p.getParcelas() + "x)";
                }
                sb.append(formatarLinha(" • " + mod, String.format("R$ %.2f", p.getValorBruto()), colunas)).append("\n");
                if (p.getTroco() > 0) {
                    sb.append(formatarLinha("   Recebido: R$ " + String.format("%.2f", p.getValorRecebidoCliente()),
                            "Troco: R$ " + String.format("%.2f", p.getTroco()), colunas)).append("\n");
                }
            }
        } else {
            sb.append(formatarLinha(" • PAGAMENTO:", String.format("R$ %.2f", venda.getValorTotal()), colunas)).append("\n");
        }

        sb.append(divDupla).append("\n");
        sb.append(centralizar("Garantia legal de 90 dias com este cupom.", colunas)).append("\n");
        sb.append(centralizar("Trocas apenas com embalagem intacta.", colunas)).append("\n");
        sb.append(centralizar("Obrigado pela preferência! Volte sempre!", colunas)).append("\n");
        sb.append(divDupla).append("\n");

        return sb.toString();
    }

    public static String gerarTextoCupomFechamento(CaixaSessao sessao, String nomeLoja, boolean is80mm) {
        int colunas = is80mm ? 48 : 32;
        String div = "-".repeat(colunas);
        String divDupla = "=".repeat(colunas);

        StringBuilder sb = new StringBuilder();
        sb.append(divDupla).append("\n");
        sb.append(centralizar(nomeLoja != null && !nomeLoja.isEmpty() ? nomeLoja.toUpperCase() : "SYSTEM PRO", colunas)).append("\n");
        sb.append(centralizar("FECHAMENTO DE CAIXA / TURNO", colunas)).append("\n");
        sb.append(divDupla).append("\n");

        sb.append(String.format("TURNO / SESSÃO:    #%d\n", sessao.getId()));
        sb.append(String.format("STATUS:            %s\n", sessao.getStatus()));
        sb.append(String.format("ABERTURA:          %s\n", sessao.getDataAberturaFormatada()));
        sb.append(String.format("FECHAMENTO:        %s\n", sessao.getDataFechamentoFormatada()));
        sb.append(String.format("OP. ABERTURA:      %s\n", truncar(sessao.getOperadorAbertura(), colunas - 19)));
        sb.append(String.format("OP. FECHAMENTO:    %s\n", truncar(sessao.getOperadorFechamento() != null ? sessao.getOperadorFechamento() : sessao.getOperadorAbertura(), colunas - 19)));
        sb.append(div).append("\n");

        sb.append("FATURAMENTO / ENTRADAS DO TURNO\n");
        sb.append(formatarLinha("Dinheiro em Vendas/OS:", String.format("R$ %.2f", sessao.getTotalDinheiro()), colunas)).append("\n");
        sb.append(formatarLinha("PIX Recebido:", String.format("R$ %.2f", sessao.getTotalPix()), colunas)).append("\n");
        sb.append(formatarLinha("Débito (Bruto):", String.format("R$ %.2f", sessao.getTotalDebitoBruto()), colunas)).append("\n");
        sb.append(formatarLinha("Débito (Líquido):", String.format("R$ %.2f", sessao.getTotalDebitoLiquido()), colunas)).append("\n");
        sb.append(formatarLinha("Crédito (Bruto):", String.format("R$ %.2f", sessao.getTotalCreditoBruto()), colunas)).append("\n");
        sb.append(formatarLinha("Crédito (Líquido):", String.format("R$ %.2f", sessao.getTotalCreditoLiquido()), colunas)).append("\n");
        sb.append(div).append("\n");

        sb.append(formatarLinha("TOTAL VENDAS BRUTO:", String.format("R$ %.2f", sessao.getTotalVendasBruto()), colunas)).append("\n");
        sb.append(formatarLinha("TOTAL VENDAS LÍQUIDO:", String.format("R$ %.2f", sessao.getTotalVendasLiquido()), colunas)).append("\n");
        sb.append(formatarLinha("QTD DE OPERAÇÕES:", String.valueOf(sessao.getQtdVendas()), colunas)).append("\n");
        sb.append(div).append("\n");

        sb.append("CONFERÊNCIA DA GAVETA (DINHEIRO)\n");
        sb.append(formatarLinha("Fundo Troco Inicial:", String.format("R$ %.2f", sessao.getSaldoInicial()), colunas)).append("\n");
        sb.append(formatarLinha("(+) Suprimentos:", String.format("R$ %.2f", sessao.getTotalSuprimentos()), colunas)).append("\n");
        sb.append(formatarLinha("(-) Sangrias Anteriores:", String.format("R$ %.2f", sessao.getTotalSangrias()), colunas)).append("\n");
        sb.append(formatarLinha("(=) Saldo Esperado Sistema:", String.format("R$ %.2f", sessao.getSaldoFinalSistema()), colunas)).append("\n");
        sb.append(formatarLinha("(=) Contagem Física Real:", String.format("R$ %.2f", sessao.getSaldoFinalInformado()), colunas)).append("\n");
        sb.append(formatarLinha("DIFERENÇA APURADA:", sessao.getStatusDiferencaFormatado(), colunas)).append("\n");

        if (sessao.getJustificativaDiferenca() != null && !sessao.getJustificativaDiferenca().trim().isEmpty()) {
            sb.append("\nJUSTIFICATIVA DA DIVERGÊNCIA:\n");
            sb.append("  ").append(sessao.getJustificativaDiferenca().trim()).append("\n");
        }

        sb.append(div).append("\n");
        sb.append("DESTINAÇÃO DO DINHEIRO FÍSICO\n");
        if (sessao.getSangriaMalote() > 0) {
            sb.append(formatarLinha("Sangria / Malote Cofre:", String.format("R$ %.2f", sessao.getSangriaMalote()), colunas)).append("\n");
            sb.append(formatarLinha("Troco Deixado na Gaveta:", String.format("R$ %.2f", sessao.getFundoTrocoDeixado()), colunas)).append("\n");
        } else {
            sb.append(formatarLinha("Saldo em Gaveta:", String.format("R$ %.2f", sessao.getFundoTrocoDeixado()), colunas)).append("\n");
        }

        if (sessao.getContagemDetalhadaTexto() != null && !sessao.getContagemDetalhadaTexto().trim().isEmpty()) {
            sb.append(div).append("\n");
            sb.append("DETALHAMENTO DA CONTAGEM:\n");
            sb.append(sessao.getContagemDetalhadaTexto());
        }

        sb.append(divDupla).append("\n\n");
        sb.append("________________________________________\n");
        sb.append(centralizar("Assinatura do Operador de Caixa", colunas)).append("\n\n");
        sb.append("________________________________________\n");
        sb.append(centralizar("Assinatura do Gerente / Supervisor", colunas)).append("\n");
        sb.append(divDupla).append("\n");

        return sb.toString();
    }

    public static boolean imprimirCupom(String textoCupom, Component parent) {
        return imprimirCupomTermico(textoCupom, "Fechamento de Caixa", parent, true, true);
    }

    /**
     * Imprime diretamente em impressoras térmicas (80mm ou 58mm) com ajuste milimétrico
     * para evitar que a bobina avance folhas em branco desnecessárias.
     */
    public static boolean imprimirCupomTermico(String textoCupom, String tituloTrabalho, Component parent, boolean is80mm, boolean mostrarDialogo) {
        if (textoCupom == null || textoCupom.trim().isEmpty()) {
            if (parent != null) {
                JOptionPane.showMessageDialog(parent, "Texto do cupom está vazio.", "Aviso", JOptionPane.WARNING_MESSAGE);
            }
            return false;
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(tituloTrabalho != null ? tituloTrabalho : "Cupom Térmico");

        PageFormat pf = job.defaultPage();
        Paper paper = new Paper();

        // 80mm = ~226.8 pt (3.15 pol * 72 pt/pol)
        // 58mm = ~164.4 pt (2.28 pol * 72 pt/pol)
        double widthPt = is80mm ? 226.0 : 164.0;

        String[] linhas = textoCupom.split("\n");
        int fontSize = is80mm ? 8 : 7;
        int lineHeight = fontSize + 4; // espaçamento de linha confortável
        double heightPt = Math.max(160.0, (linhas.length * lineHeight) + 30.0);

        // Define área imprimível ocupando toda a largura da bobina
        paper.setSize(widthPt, heightPt);
        paper.setImageableArea(4, 4, widthPt - 8, heightPt - 8);
        pf.setPaper(paper);
        pf.setOrientation(PageFormat.PORTRAIT);

        job.setPrintable(new Printable() {
            @Override
            public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {
                if (pageIndex > 0) {
                    return NO_SUCH_PAGE;
                }

                Graphics2D g2d = (Graphics2D) g;
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                g2d.setFont(new Font("Monospaced", Font.PLAIN, fontSize));
                g2d.setColor(Color.BLACK);

                FontMetrics fm = g2d.getFontMetrics();
                int h = fm.getHeight();
                int y = fm.getAscent();

                for (String linha : linhas) {
                    g2d.drawString(linha, 0, y);
                    y += h;
                }

                return PAGE_EXISTS;
            }
        }, pf);

        boolean podeImprimir = true;
        if (mostrarDialogo) {
            podeImprimir = job.printDialog();
        }

        if (podeImprimir) {
            try {
                job.print();
                if (parent != null) {
                    JOptionPane.showMessageDialog(parent, "Cupom enviado para a impressora térmica com sucesso!", "Impressão", JOptionPane.INFORMATION_MESSAGE);
                }
                return true;
            } catch (PrinterException e) {
                if (parent != null) {
                    JOptionPane.showMessageDialog(parent, "Erro ao enviar para a impressora: " + e.getMessage(), "Erro de Impressão", JOptionPane.ERROR_MESSAGE);
                }
                return false;
            }
        }
        return false;
    }

    private static String centralizar(String texto, int largura) {
        if (texto == null) return "";
        if (texto.length() >= largura) return texto.substring(0, largura);
        int espacos = (largura - texto.length()) / 2;
        return " ".repeat(espacos) + texto;
    }

    private static String formatarLinha(String esquerda, String direita, int largura) {
        if (esquerda == null) esquerda = "";
        if (direita == null) direita = "";
        int espacoDisponivel = largura - esquerda.length() - direita.length();
        if (espacoDisponivel < 1) {
            int corte = largura - direita.length() - 2;
            if (corte > 0) esquerda = esquerda.substring(0, corte) + ".";
            espacoDisponivel = 1;
        }
        return esquerda + " ".repeat(espacoDisponivel) + direita;
    }

    private static String truncar(String texto, int max) {
        if (texto == null) return "-";
        if (max <= 3) return texto;
        return texto.length() > max ? texto.substring(0, max - 3) + "..." : texto;
    }
}
