package com.loja.service;

import com.loja.model.ContaPagar;
import com.loja.model.FuncionarioDespesa;
import com.loja.model.ResumoDRE;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class RelatorioFinanceiroPDFService {
    private static final DateTimeFormatter FORMATTER_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATTER_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Paleta Executiva Monocromática com Acentos Elegantes
    private static final Color COLOR_TEXT_PRIMARY = new Color(20, 24, 27);
    private static final Color COLOR_TEXT_SECONDARY = new Color(85, 90, 95);
    private static final Color COLOR_BORDER = new Color(200, 205, 210);
    private static final Color COLOR_BG_HEADER = new Color(240, 242, 245);
    private static final Color COLOR_BG_ZEBRA = new Color(250, 251, 252);
    private static final Color COLOR_BG_HIGHLIGHT = new Color(235, 245, 238);
    private static final Color COLOR_TEXT_SUCCESS = new Color(20, 120, 50);

    // Tipografia Corporativa
    private static final Font FONT_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, COLOR_TEXT_PRIMARY);
    private static final Font FONT_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_TEXT_SECONDARY);
    private static final Font FONT_SECAO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_TEXT_PRIMARY);
    private static final Font FONT_TH = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, COLOR_TEXT_PRIMARY);
    private static final Font FONT_TD = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, COLOR_TEXT_PRIMARY);
    private static final Font FONT_TD_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, COLOR_TEXT_PRIMARY);
    private static final Font FONT_TD_MUTED = FontFactory.getFont(FontFactory.HELVETICA, 8f, COLOR_TEXT_SECONDARY);
    private static final Font FONT_LUCRO_TITULO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_TEXT_SUCCESS);
    private static final Font FONT_LUCRO_VALOR = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COLOR_TEXT_SUCCESS);
    private static final Font FONT_RODAPE = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_TEXT_SECONDARY);

    public static File gerarRelatorioMensalPDF(File arquivoDestino, ResumoDRE dre, String nomeLoja, String operador) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, new FileOutputStream(arquivoDestino));
        document.open();

        String nomeMes = obterNomeMes(dre.getMes());
        String periodoFormatado = nomeMes.toUpperCase(Locale.ROOT) + " / " + dre.getAno();

        // 1. Cabeçalho Principal
        adicionarCabecalho(document, nomeLoja, periodoFormatado, operador);

        adicionarDivisor(document);

        // 2. Quadro Executivo DRE (Faturamento, Custos, Despesas e Lucro Líquido)
        adicionarQuadroDRE(document, dre);

        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 6)));

        // 3. Tabela Detalhada de Contas e Despesas Pagas
        adicionarTabelaContas(document, dre);

        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 6)));

        // 4. Tabela de Despesas com Pessoal / Equipe
        adicionarTabelaPessoal(document, dre);

        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 10)));

        // 5. Termo de Prestação de Contas e Assinaturas
        adicionarPrestacaoContasAssinaturas(document, periodoFormatado);

        // 6. Rodapé
        adicionarRodape(document);

        document.close();
        return arquivoDestino;
    }

    private static void adicionarCabecalho(Document document, String nomeLoja, String periodo, String operador) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{65, 35});

        PdfPCell cEsq = new PdfPCell();
        cEsq.setBorder(Rectangle.NO_BORDER);
        cEsq.setPadding(0);

        String tituloLoja = (nomeLoja != null && !nomeLoja.trim().isEmpty()) ? nomeLoja : "SYSTEM PRO - GESTÃO EMPRESARIAL";
        cEsq.addElement(new Paragraph(tituloLoja.toUpperCase(), FONT_TITULO));
        cEsq.addElement(new Paragraph("RELATÓRIO FINANCEIRO & PRESTAÇÃO DE CONTAS", FONT_SUBTITULO));
        cEsq.addElement(new Paragraph("Competência: " + periodo, FONT_TD_BOLD));

        PdfPCell cDir = new PdfPCell();
        cDir.setBorder(Rectangle.NO_BORDER);
        cDir.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cDir.setPadding(0);

        Paragraph pData = new Paragraph("Emissão: " + LocalDateTime.now().format(FORMATTER_DATA_HORA), FONT_TD_MUTED);
        pData.setAlignment(Element.ALIGN_RIGHT);
        cDir.addElement(pData);

        if (operador != null && !operador.trim().isEmpty()) {
            Paragraph pOp = new Paragraph("Emitido por: " + operador, FONT_TD_MUTED);
            pOp.setAlignment(Element.ALIGN_RIGHT);
            cDir.addElement(pOp);
        }

        headerTable.addCell(cEsq);
        headerTable.addCell(cDir);
        document.add(headerTable);
    }

    private static void adicionarDivisor(Document document) throws DocumentException {
        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 3)));
        LineSeparator line = new LineSeparator(0.8f, 100, COLOR_BORDER, Element.ALIGN_CENTER, -2);
        document.add(line);
        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));
    }

    private static void adicionarQuadroDRE(Document document, ResumoDRE dre) throws DocumentException {
        Paragraph pSecao = new Paragraph("1. BALANÇO FINANCEIRO & APURAÇÃO DE RESULTADO", FONT_SECAO);
        document.add(pSecao);
        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 3)));

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{55, 25, 20});

        adicionarHeaderTabela(table, "Discriminação Financeira", "Subtotal (R$)", "Total (R$)");

        // Faturamento
        adicionarLinhaDRE(table, "(+) Faturamento Mão de Obra de Serviços (OS)", formatarMoeda(dre.getFaturamentoServicos()), "", false);
        adicionarLinhaDRE(table, "(+) Faturamento Peças em Ordens de Serviço", formatarMoeda(dre.getFaturamentoPecasOS()), "", false);
        adicionarLinhaDRE(table, "(+) Faturamento Vendas Balcão (PDV)", formatarMoeda(dre.getFaturamentoPDV()), "", false);
        adicionarLinhaDRE(table, "(=) FATURAMENTO BRUTO TOTAL", "", formatarMoeda(dre.getFaturamentoBrutoTotal()), true);

        // Deduções
        adicionarLinhaDRE(table, "(-) Taxas Operadoras de Cartão/Maquininha", formatarMoeda(dre.getTotalTaxasCartao()), "", false);
        adicionarLinhaDRE(table, "(=) FATURAMENTO LÍQUIDO", "", formatarMoeda(dre.getFaturamentoLiquido()), true);

        // Custos (CMV)
        adicionarLinhaDRE(table, "(-) Custo das Peças aplicadas em OSs", formatarMoeda(dre.getCustoPecasOS()), "", false);
        adicionarLinhaDRE(table, "(-) Custo das Mercadorias Balcão PDV", formatarMoeda(dre.getCustoPecasPDV()), "", false);
        adicionarLinhaDRE(table, "(-) CUSTO TOTAL DE MERCADORIAS (CMV)", "", "-" + formatarMoeda(dre.getCustoTotalMercadoriasCMV()), true);

        // Lucro Bruto
        adicionarLinhaDRE(table, "(=) LUCRO BRUTO OPERACIONAL", "", formatarMoeda(dre.getLucroBruto()), true);

        // Despesas
        adicionarLinhaDRE(table, "(-) Contas e Despesas Fixas/Variáveis Pagas", formatarMoeda(dre.getTotalDespesasPagas()), "", false);
        adicionarLinhaDRE(table, "(-) Despesas com Pessoal / Funcionários Pagas", formatarMoeda(dre.getTotalPessoalPago()), "", false);
        if (dre.getTotalSangriasAvulsas() > 0) {
            adicionarLinhaDRE(table, "(-) Outras Retiradas / Sangrias Avulsas do Caixa", formatarMoeda(dre.getTotalSangriasAvulsas()), "", false);
        }
        adicionarLinhaDRE(table, "(-) TOTAL GASTOS OPERACIONAIS LIQUIDADOS", "", "-" + formatarMoeda(dre.getTotalGastosOperacionaisPagos()), true);

        // Destaque do Lucro Líquido Real
        PdfPCell cLucroTit = new PdfPCell(new Phrase("(=) LUCRO LÍQUIDO REAL DISPONÍVEL (Margem: " + String.format("%.1f%%", dre.getMargemLiquidaPercentual()) + ")", FONT_LUCRO_TITULO));
        cLucroTit.setColspan(2);
        cLucroTit.setBackgroundColor(COLOR_BG_HIGHLIGHT);
        cLucroTit.setPadding(6);
        cLucroTit.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPCell cLucroVal = new PdfPCell(new Phrase(formatarMoeda(dre.getLucroLiquidoReal()), FONT_LUCRO_VALOR));
        cLucroVal.setBackgroundColor(COLOR_BG_HIGHLIGHT);
        cLucroVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cLucroVal.setPadding(6);
        cLucroVal.setVerticalAlignment(Element.ALIGN_MIDDLE);

        table.addCell(cLucroTit);
        table.addCell(cLucroVal);

        document.add(table);
    }

    private static void adicionarLinhaDRE(PdfPTable table, String desc, String subtotal, String total, boolean isDestaque) {
        Font font = isDestaque ? FONT_TD_BOLD : FONT_TD;
        Color bg = isDestaque ? COLOR_BG_HEADER : Color.WHITE;

        PdfPCell cDesc = new PdfPCell(new Phrase(desc, font));
        cDesc.setBackgroundColor(bg);
        cDesc.setPadding(4.5f);
        cDesc.setBorderColor(COLOR_BORDER);

        PdfPCell cSub = new PdfPCell(new Phrase(subtotal, font));
        cSub.setBackgroundColor(bg);
        cSub.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cSub.setPadding(4.5f);
        cSub.setBorderColor(COLOR_BORDER);

        PdfPCell cTot = new PdfPCell(new Phrase(total, font));
        cTot.setBackgroundColor(bg);
        cTot.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cTot.setPadding(4.5f);
        cTot.setBorderColor(COLOR_BORDER);

        table.addCell(cDesc);
        table.addCell(cSub);
        table.addCell(cTot);
    }

    private static void adicionarTabelaContas(Document document, ResumoDRE dre) throws DocumentException {
        Paragraph pSecao = new Paragraph("2. DISCRIMINAÇÃO DE CONTAS E DESPESAS PAGAS NO MÊS", FONT_SECAO);
        document.add(pSecao);
        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 3)));

        if (dre.getContasPagas().isEmpty()) {
            Paragraph pVazio = new Paragraph("Nenhuma conta a pagar registrada como liquidada no período.", FONT_TD_MUTED);
            document.add(pVazio);
            return;
        }

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{14, 26, 32, 14, 14});

        adicionarHeaderTabela(table, "Pagamento", "Categoria", "Descrição", "Forma", "Valor");

        boolean zebra = false;
        for (ContaPagar c : dre.getContasPagas()) {
            Color bg = zebra ? COLOR_BG_ZEBRA : Color.WHITE;
            zebra = !zebra;

            String dt = c.getDataPagamento() != null ? c.getDataPagamento().format(FORMATTER_DATA) : "-";
            adicionarCelula(table, dt, Element.ALIGN_CENTER, bg, FONT_TD);
            adicionarCelula(table, c.getCategoria(), Element.ALIGN_LEFT, bg, FONT_TD);
            adicionarCelula(table, c.getDescricao(), Element.ALIGN_LEFT, bg, FONT_TD);
            adicionarCelula(table, formatarFormaPagamento(c.getFormaPagamento()), Element.ALIGN_CENTER, bg, FONT_TD_MUTED);
            adicionarCelula(table, formatarMoeda(c.getValor()), Element.ALIGN_RIGHT, bg, FONT_TD_BOLD);
        }

        // Linha Total de Despesas
        PdfPCell cTotTit = new PdfPCell(new Phrase("TOTAL DE DESPESAS PAGAS", FONT_TD_BOLD));
        cTotTit.setColspan(4);
        cTotTit.setBackgroundColor(COLOR_BG_HEADER);
        cTotTit.setPadding(4.5f);
        cTotTit.setBorderColor(COLOR_BORDER);

        PdfPCell cTotVal = new PdfPCell(new Phrase(formatarMoeda(dre.getTotalDespesasPagas()), FONT_TD_BOLD));
        cTotVal.setBackgroundColor(COLOR_BG_HEADER);
        cTotVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cTotVal.setPadding(4.5f);
        cTotVal.setBorderColor(COLOR_BORDER);

        table.addCell(cTotTit);
        table.addCell(cTotVal);

        document.add(table);
    }

    private static void adicionarTabelaPessoal(Document document, ResumoDRE dre) throws DocumentException {
        Paragraph pSecao = new Paragraph("3. DESPESAS COM PESSOAL / FOLHA LIQUIDADA", FONT_SECAO);
        document.add(pSecao);
        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 3)));

        if (dre.getPagamentosPessoal().isEmpty()) {
            Paragraph pVazio = new Paragraph("Nenhum lançamento de pessoal liquidado no período.", FONT_TD_MUTED);
            document.add(pVazio);
            return;
        }

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{14, 30, 22, 18, 16});

        adicionarHeaderTabela(table, "Pagamento", "Colaborador", "Cargo", "Tipo", "Valor");

        boolean zebra = false;
        for (FuncionarioDespesa fd : dre.getPagamentosPessoal()) {
            Color bg = zebra ? COLOR_BG_ZEBRA : Color.WHITE;
            zebra = !zebra;

            String dt = fd.getDataPagamento() != null ? fd.getDataPagamento().format(FORMATTER_DATA) : "-";
            adicionarCelula(table, dt, Element.ALIGN_CENTER, bg, FONT_TD);
            adicionarCelula(table, fd.getFuncionarioNome(), Element.ALIGN_LEFT, bg, FONT_TD);
            adicionarCelula(table, fd.getCargo(), Element.ALIGN_LEFT, bg, FONT_TD_MUTED);
            adicionarCelula(table, fd.getTipo().replace("_", " "), Element.ALIGN_CENTER, bg, FONT_TD);
            adicionarCelula(table, formatarMoeda(fd.getValor()), Element.ALIGN_RIGHT, bg, FONT_TD_BOLD);
        }

        PdfPCell cTotTit = new PdfPCell(new Phrase("TOTAL PAGO A FUNCIONÁRIOS / COLABORADORES", FONT_TD_BOLD));
        cTotTit.setColspan(4);
        cTotTit.setBackgroundColor(COLOR_BG_HEADER);
        cTotTit.setPadding(4.5f);
        cTotTit.setBorderColor(COLOR_BORDER);

        PdfPCell cTotVal = new PdfPCell(new Phrase(formatarMoeda(dre.getTotalPessoalPago()), FONT_TD_BOLD));
        cTotVal.setBackgroundColor(COLOR_BG_HEADER);
        cTotVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cTotVal.setPadding(4.5f);
        cTotVal.setBorderColor(COLOR_BORDER);

        table.addCell(cTotTit);
        table.addCell(cTotVal);

        document.add(table);
    }

    private static void adicionarPrestacaoContasAssinaturas(Document document, String periodo) throws DocumentException {
        Paragraph pTermo = new Paragraph(
                "PRESTAÇÃO DE CONTAS: Declaro para os devidos fins administrativos e contábeis que os valores acima " +
                "discriminados espelham com veracidade e exatidão todas as operações comerciais de vendas, serviços " +
                "e despesas ocorridas na competência de " + periodo + ".", FONT_TD_MUTED);
        pTermo.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(pTermo);

        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 16)));

        PdfPTable tableAssinaturas = new PdfPTable(2);
        tableAssinaturas.setWidthPercentage(100);
        tableAssinaturas.setWidths(new float[]{50, 50});

        PdfPCell c1 = new PdfPCell();
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph pLinha1 = new Paragraph("____________________________________________\nGestor Responsável / Caixa", FONT_TD_BOLD);
        pLinha1.setAlignment(Element.ALIGN_CENTER);
        c1.addElement(pLinha1);

        PdfPCell c2 = new PdfPCell();
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph pLinha2 = new Paragraph("____________________________________________\nDiretoria / Sócios Proprietários", FONT_TD_BOLD);
        pLinha2.setAlignment(Element.ALIGN_CENTER);
        c2.addElement(pLinha2);

        tableAssinaturas.addCell(c1);
        tableAssinaturas.addCell(c2);
        document.add(tableAssinaturas);
    }

    private static void adicionarRodape(Document document) throws DocumentException {
        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 10)));
        Paragraph pRodape = new Paragraph("Documento gerado automaticamente pelo System Pro • Sistema de Gestão Empresarial e Assistência Técnica", FONT_RODAPE);
        pRodape.setAlignment(Element.ALIGN_CENTER);
        document.add(pRodape);
    }

    private static void adicionarHeaderTabela(PdfPTable table, String... colunas) {
        for (String col : colunas) {
            PdfPCell c = new PdfPCell(new Phrase(col, FONT_TH));
            c.setBackgroundColor(COLOR_BG_HEADER);
            c.setPadding(5);
            c.setBorderColor(COLOR_BORDER);
            table.addCell(c);
        }
    }

    private static void adicionarCelula(PdfPTable table, String texto, int alinhamento, Color bg, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setHorizontalAlignment(alinhamento);
        c.setBackgroundColor(bg);
        c.setPadding(4.5f);
        c.setBorderColor(COLOR_BORDER);
        table.addCell(c);
    }

    private static String formatarMoeda(double valor) {
        return String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", valor);
    }

    private static String formatarFormaPagamento(String forma) {
        if (forma == null) return "Caixa";
        return switch (forma) {
            case "DINHEIRO_CAIXA" -> "Dinheiro (Caixa)";
            case "PIX_BANCO" -> "PIX (Banco)";
            case "BOLETO" -> "Boleto";
            case "CARTAO" -> "Cartão";
            case "TRANSFERENCIA" -> "Transferência";
            default -> forma;
        };
    }

    private static String obterNomeMes(int mes) {
        return switch (mes) {
            case 1 -> "Janeiro";
            case 2 -> "Fevereiro";
            case 3 -> "Março";
            case 4 -> "Abril";
            case 5 -> "Maio";
            case 6 -> "Junho";
            case 7 -> "Julho";
            case 8 -> "Agosto";
            case 9 -> "Setembro";
            case 10 -> "Outubro";
            case 11 -> "Novembro";
            case 12 -> "Dezembro";
            default -> "Mês " + mes;
        };
    }
}
