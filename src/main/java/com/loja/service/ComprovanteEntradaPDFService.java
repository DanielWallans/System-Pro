package com.loja.service;

import com.loja.model.Cliente;
import com.loja.model.Equipamento;
import com.loja.model.OrdemServico;
import com.loja.repository.CaixaDAO;
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

/**
 * Serviço responsável pela geração do Comprovante de Entrada do Equipamento em PDF.
 * Padrão visual corporativo, monocromático e executivo com validação jurídica de custódia.
 */
public class ComprovanteEntradaPDFService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Paleta de Cores Corporativa
    private static final Color COLOR_TEXT_PRIMARY = new Color(20, 24, 27);
    private static final Color COLOR_TEXT_SECONDARY = new Color(80, 85, 90);
    private static final Color COLOR_BORDER = new Color(180, 185, 190);
    private static final Color COLOR_BORDER_LIGHT = new Color(220, 224, 228);
    private static final Color COLOR_BG_HEADER = new Color(238, 240, 242);
    private static final Color COLOR_BG_HIGHLIGHT = new Color(248, 249, 250);
    private static final Color COLOR_TEXT_STATUS = new Color(31, 78, 121);

    // Tipografia Executiva
    private static final Font FONT_EMPRESA = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, COLOR_TEXT_PRIMARY);
    private static final Font FONT_SUBTITULO = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, COLOR_TEXT_SECONDARY);
    private static final Font FONT_TITULO_DOC = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_TEXT_PRIMARY);
    private static final Font FONT_SUBTITULO_DOC = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_TEXT_PRIMARY);
    private static final Font FONT_HEADER_SECAO = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, COLOR_TEXT_PRIMARY);
    private static final Font FONT_TEXTO = FontFactory.getFont(FontFactory.HELVETICA, 9, COLOR_TEXT_PRIMARY);
    private static final Font FONT_TEXTO_MUTED = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, COLOR_TEXT_SECONDARY);
    private static final Font FONT_TEXTO_BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_TEXT_PRIMARY);
    private static final Font FONT_TERMO_TEXTO = FontFactory.getFont(FontFactory.HELVETICA, 8.2f, COLOR_TEXT_PRIMARY);
    private static final Font FONT_RODAPE = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXT_SECONDARY);

    public static File gerarComprovanteEntradaPDF(File arquivoDestino, OrdemServico os, Cliente cliente, Equipamento equip) throws Exception {
        Document document = new Document(PageSize.A4, 32, 32, 32, 32);
        PdfWriter.getInstance(document, new FileOutputStream(arquivoDestino));
        document.open();

        // 1. Cabeçalho Principal (Empresa e Identificação da Entrada)
        adicionarCabecalho(document, os);

        // Divisor
        adicionarDivisor(document);

        // 2. Dados do Cliente e Dados do Equipamento
        adicionarDadosClienteEquipamento(document, os, cliente, equip);

        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

        // 3. Acessórios e Avarias Visíveis
        adicionarAcessoriosEAvarias(document, equip);

        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

        // 4. Defeito Relatado e Checklist de Entrada
        adicionarDefeitoEChecklist(document, os);

        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

        // 5. Termo de Declaração de Deixada ou Atendimento Externo
        adicionarTermoCustodia(document, os);

        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 8)));

        // 6. Área de Assinaturas (Cliente e Atendente)
        adicionarAssinaturas(document, cliente);

        // 7. Rodapé de Autenticidade
        adicionarRodape(document);

        document.close();
        return arquivoDestino;
    }

    private static void adicionarCabecalho(Document document, OrdemServico os) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{60, 40});
        headerTable.setSpacingAfter(6);

        CaixaDAO caixaDAO = new CaixaDAO();
        String nomeEmpresa = caixaDAO.obterConfig("nome_loja", "ASSISTÊNCIA TÉCNICA ESPECIALIZADA").toUpperCase();
        String cnpj = caixaDAO.obterConfig("cnpj_loja", "");
        String telefone = caixaDAO.obterConfig("telefone_loja", "");
        String email = caixaDAO.obterConfig("email_loja", "");
        String endereco = caixaDAO.obterConfig("endereco_loja", "");

        // Lado Esquerdo: Identificação da Empresa
        PdfPCell cellEmpresa = new PdfPCell();
        cellEmpresa.setBorder(Rectangle.NO_BORDER);
        cellEmpresa.setPadding(0);

        Paragraph pNome = new Paragraph(nomeEmpresa, FONT_EMPRESA);
        cellEmpresa.addElement(pNome);

        StringBuilder sbInfo = new StringBuilder();
        if (!cnpj.isEmpty()) sbInfo.append("CNPJ/CPF: ").append(cnpj);
        if (!telefone.isEmpty()) {
            if (sbInfo.length() > 0) sbInfo.append(" • ");
            sbInfo.append("Tel: ").append(telefone);
        }
        if (!email.isEmpty()) {
            if (sbInfo.length() > 0) sbInfo.append(" • ");
            sbInfo.append("E-mail: ").append(email);
        }
        if (sbInfo.length() > 0) {
            Paragraph pInfo = new Paragraph(sbInfo.toString(), FONT_SUBTITULO);
            cellEmpresa.addElement(pInfo);
        }

        if (!endereco.isEmpty()) {
            Paragraph pEnd = new Paragraph(endereco, FONT_SUBTITULO);
            cellEmpresa.addElement(pEnd);
        }

        String docTipo = os.isExterno() ? "Comprovante Oficial de Atendimento Técnico Externo" : "Comprovante Oficial de Entrada e Termo de Custódia";
        Paragraph pDocTipo = new Paragraph(docTipo, FONT_SUBTITULO);
        cellEmpresa.addElement(pDocTipo);
        headerTable.addCell(cellEmpresa);

        // Lado Direito: Caixa de Controle da Entrada
        PdfPCell cellOSInfo = new PdfPCell();
        cellOSInfo.setBackgroundColor(COLOR_BG_HEADER);
        cellOSInfo.setBorderColor(COLOR_BORDER);
        cellOSInfo.setBorderWidth(1f);
        cellOSInfo.setPadding(6);

        String titDoc = os.isExterno() ? "SERVIÇO EXTERNO" : "COMPROVANTE DE ENTRADA";
        Paragraph pTitulo = new Paragraph(titDoc, FONT_TITULO_DOC);
        pTitulo.setAlignment(Element.ALIGN_RIGHT);

        Paragraph pOS = new Paragraph("ORDEM DE SERVIÇO Nº #" + os.getId(), FONT_SUBTITULO_DOC);
        pOS.setAlignment(Element.ALIGN_RIGHT);

        String dataEntradaStr = os.getDataEntrada() != null ? os.getDataEntrada().format(FORMATTER) : LocalDateTime.now().format(FORMATTER);
        Paragraph pEntrada = new Paragraph("Abertura: " + dataEntradaStr, FONT_TEXTO_MUTED);
        pEntrada.setAlignment(Element.ALIGN_RIGHT);

        String statusStr = "STATUS: " + (os.getStatus() != null ? os.getStatus().toUpperCase() : "AGUARDANDO ORÇAMENTO");
        Paragraph pStatus = new Paragraph(statusStr, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, COLOR_TEXT_STATUS));
        pStatus.setAlignment(Element.ALIGN_RIGHT);

        cellOSInfo.addElement(pTitulo);
        cellOSInfo.addElement(pOS);
        cellOSInfo.addElement(pEntrada);
        cellOSInfo.addElement(pStatus);
        headerTable.addCell(cellOSInfo);

        document.add(headerTable);
    }

    private static void adicionarDivisor(Document document) throws DocumentException {
        LineSeparator separator = new LineSeparator();
        separator.setLineColor(COLOR_BORDER_LIGHT);
        separator.setLineWidth(0.8f);
        document.add(new Chunk(separator));
        document.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));
    }

    private static void adicionarDadosClienteEquipamento(Document document, OrdemServico os, Cliente cliente, Equipamento equip) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50, 50});

        PdfPCell h1 = criarCelulaHeaderSecao("DADOS DO CLIENTE / SOLICITANTE");
        String titSecao2 = (os != null && os.isExterno()) ? "DADOS DO ATENDIMENTO EXTERNO / LOCAL" : "DADOS DO EQUIPAMENTO RECEBIDO";
        PdfPCell h2 = criarCelulaHeaderSecao(titSecao2);
        table.addCell(h1);
        table.addCell(h2);

        // Cliente
        PdfPCell cCliente = new PdfPCell();
        cCliente.setBorderColor(COLOR_BORDER_LIGHT);
        cCliente.setBorderWidth(0.8f);
        cCliente.setPadding(7);

        if (cliente != null) {
            cCliente.addElement(new Paragraph("Nome: " + cliente.getNome(), FONT_TEXTO_BOLD));
            cCliente.addElement(new Paragraph("Telefone / WhatsApp: " + valorOuTraco(cliente.getTelefone()), FONT_TEXTO));
            cCliente.addElement(new Paragraph("CPF / CNPJ: " + valorOuTraco(cliente.getCpfCnpj()), FONT_TEXTO));
            cCliente.addElement(new Paragraph("Endereço: " + valorOuTraco(cliente.getEndereco()), FONT_TEXTO));
            if (cliente.getEmail() != null && !cliente.getEmail().trim().isEmpty()) {
                cCliente.addElement(new Paragraph("E-mail: " + cliente.getEmail(), FONT_TEXTO));
            }
        } else {
            cCliente.addElement(new Paragraph("Cliente não vinculado / Avulso", FONT_TEXTO));
        }
        table.addCell(cCliente);

        // Equipamento / Atendimento
        PdfPCell cEquip = new PdfPCell();
        cEquip.setBorderColor(COLOR_BORDER_LIGHT);
        cEquip.setBorderWidth(0.8f);
        cEquip.setPadding(7);

        if (os != null && os.isExterno() && !os.getEnderecoExterno().isEmpty()) {
            cEquip.addElement(new Paragraph("Local da Visita / Atendimento: " + os.getEnderecoExterno(), FONT_TEXTO_BOLD));
        }

        if (equip != null) {
            String aparelho = String.format("%s %s %s", valorOuVazio(equip.getTipo()), valorOuVazio(equip.getMarca()), valorOuVazio(equip.getModelo())).trim();
            cEquip.addElement(new Paragraph("Aparelho / Dispositivo: " + (aparelho.isEmpty() ? "Não especificado" : aparelho), FONT_TEXTO_BOLD));
            cEquip.addElement(new Paragraph("Nº de Série: " + valorOuTraco(equip.getNumeroSerie()) + " | Cor: " + valorOuTraco(equip.getCor()), FONT_TEXTO));
            if (equip.getSenhaAcesso() != null && !equip.getSenhaAcesso().trim().isEmpty()) {
                cEquip.addElement(new Paragraph("Senha de Teste Informada: " + equip.getSenhaAcesso().trim(), FONT_TEXTO_BOLD));
            }
            if (equip.getPatrimonio() != null && !equip.getPatrimonio().trim().isEmpty()) {
                cEquip.addElement(new Paragraph("Patrimônio / Etiqueta: " + equip.getPatrimonio().trim(), FONT_TEXTO));
            }
        } else {
            cEquip.addElement(new Paragraph("Equipamento não identificado", FONT_TEXTO));
        }
        table.addCell(cEquip);

        document.add(table);
    }

    private static void adicionarAcessoriosEAvarias(Document document, Equipamento equip) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50, 50});

        PdfPCell h1 = criarCelulaHeaderSecao("ACESSÓRIOS DEIXADOS PELO CLIENTE");
        PdfPCell h2 = criarCelulaHeaderSecao("ESTADO FÍSICO / AVARIAS VISÍVEIS");
        table.addCell(h1);
        table.addCell(h2);

        PdfPCell cAcess = new PdfPCell();
        cAcess.setBackgroundColor(COLOR_BG_HIGHLIGHT);
        cAcess.setBorderColor(COLOR_BORDER_LIGHT);
        cAcess.setBorderWidth(0.8f);
        cAcess.setPadding(7);

        String acess = (equip != null && equip.getAcessorios() != null && !equip.getAcessorios().trim().isEmpty())
                ? equip.getAcessorios().trim() : "Nenhum acessório informado ou deixado na recepção.";
        Paragraph pAcess = new Paragraph(acess, FONT_TEXTO_BOLD);
        cAcess.addElement(pAcess);
        table.addCell(cAcess);

        PdfPCell cAvarias = new PdfPCell();
        cAvarias.setBackgroundColor(COLOR_BG_HIGHLIGHT);
        cAvarias.setBorderColor(COLOR_BORDER_LIGHT);
        cAvarias.setBorderWidth(0.8f);
        cAvarias.setPadding(7);

        String avarias = (equip != null && equip.getAvarias() != null && !equip.getAvarias().trim().isEmpty())
                ? equip.getAvarias().trim() : "Sem avarias externas visíveis observadas no momento da entrada.";
        Paragraph pAvarias = new Paragraph(avarias, FONT_TEXTO);
        cAvarias.addElement(pAvarias);
        table.addCell(cAvarias);

        document.add(table);
    }

    private static void adicionarDefeitoEChecklist(Document document, OrdemServico os) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell hDef = criarCelulaHeaderSecao("DEFEITO INFORMADO PELO CLIENTE E CHECKLIST DE VISTORIA INICIAL");
        table.addCell(hDef);

        PdfPCell cDef = new PdfPCell();
        cDef.setBorderColor(COLOR_BORDER_LIGHT);
        cDef.setBorderWidth(0.8f);
        cDef.setPadding(7);

        Paragraph pProb = new Paragraph();
        pProb.add(new Chunk("Defeito / Problema Relatado: ", FONT_TEXTO_BOLD));
        pProb.add(new Chunk(valorOuTraco(os.getProblemaRelatado()), FONT_TEXTO));
        pProb.setSpacingAfter(4);
        cDef.addElement(pProb);

        if (os.getChecklistEntrada() != null && !os.getChecklistEntrada().trim().isEmpty()) {
            Paragraph pChk = new Paragraph();
            pChk.add(new Chunk("Checklist de Vistoria Inicial (Recepção):\n", FONT_TEXTO_BOLD));
            pChk.add(new Chunk(os.getChecklistEntrada().trim(), FONT_TEXTO));
            pChk.setSpacingAfter(4);
            cDef.addElement(pChk);
        }

        if (os.getObservacoesFinais() != null && !os.getObservacoesFinais().trim().isEmpty()) {
            Paragraph pObs = new Paragraph();
            pObs.add(new Chunk("Observações Adicionais: ", FONT_TEXTO_BOLD));
            pObs.add(new Chunk(os.getObservacoesFinais().trim(), FONT_TEXTO));
            cDef.addElement(pObs);
        }

        table.addCell(cDef);
        document.add(table);
    }

    private static void adicionarTermoCustodia(Document document, OrdemServico os) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        if (os != null && os.isExterno()) {
            PdfPCell hTermo = criarCelulaHeaderSecao("DECLARAÇÃO DE EXECUÇÃO DE SERVIÇOS NO LOCAL E ACEITE DO CLIENTE");
            table.addCell(hTermo);

            PdfPCell cTermo = new PdfPCell();
            cTermo.setBorderColor(COLOR_BORDER_LIGHT);
            cTermo.setBorderWidth(0.8f);
            cTermo.setPadding(7);

            Paragraph p1 = new Paragraph("1. DECLARAÇÃO DE ATENDIMENTO EXTERNO: O cliente acima identificado atesta que os serviços técnicos, visitas, configurações e/ou instalações descritas neste comprovante foram devidamente executados no local e endereço informados por técnico especializado.", FONT_TERMO_TEXTO);
            p1.setSpacingAfter(3);

            Paragraph p2 = new Paragraph("2. CONFERÊNCIA DOS EQUIPAMENTOS E INSTALAÇÃO: O cliente declara que acompanhou a execução das configurações e instalações, conferindo a integridade e funcionamento dos equipamentos e periféricos atendidos no local.", FONT_TERMO_TEXTO);
            p2.setSpacingAfter(3);

            Paragraph p3 = new Paragraph("3. TESTES E VALIDAÇÃO: Todos os testes operacionais foram realizados e validados na presença do cliente ou de seu representante responsável no ato da visita.", FONT_TERMO_TEXTO);

            cTermo.addElement(p1);
            cTermo.addElement(p2);
            cTermo.addElement(p3);
            table.addCell(cTermo);
        } else {
            PdfPCell hTermo = criarCelulaHeaderSecao("TERMO DE DECLARAÇÃO DE DEIXADA, CUSTÓDIA E AUTORIZAÇÃO DE ANÁLISE");
            table.addCell(hTermo);

            PdfPCell cTermo = new PdfPCell();
            cTermo.setBorderColor(COLOR_BORDER_LIGHT);
            cTermo.setBorderWidth(0.8f);
            cTermo.setPadding(7);

            Paragraph p1 = new Paragraph("1. DECLARAÇÃO DE ENTREGA E CUSTÓDIA: O cliente acima identificado declara que entregou nesta data o equipamento e os acessórios descritos neste comprovante, sob custódia desta empresa, única e exclusivamente para fins de diagnóstico técnico e montagem de proposta de orçamento.", FONT_TERMO_TEXTO);
            p1.setSpacingAfter(3);

            Paragraph p2 = new Paragraph("2. CONFERÊNCIA DE ACESSÓRIOS E AVARIAS: O cliente declara que conferiu e concorda plenamente com o checklist, com os acessórios discriminados e com o estado estético inicial do aparelho (marcas de uso/avarias relatadas neste documento).", FONT_TERMO_TEXTO);
            p2.setSpacingAfter(3);

            Paragraph p3 = new Paragraph("3. AUTORIZAÇÃO DE ANÁLISE TÉCNICA E ORÇAMENTO: Fica autorizada a desmontagem técnica e os testes necessários para elaboração do orçamento. Fica expressamente ajustado que NENHUM serviço de reparo pago será iniciado sem a prévia autorização expressa do cliente.", FONT_TERMO_TEXTO);
            p3.setSpacingAfter(3);

            Paragraph p4 = new Paragraph("4. PRAZO DE RETIRADA / GUARDA: O cliente compromete-se a responder à proposta de orçamento e a retirar o equipamento em até 90 (noventa) dias corridos após a notificação, ciente de que após este prazo o equipamento estará sujeito a cobrança de taxa de armazenagem e às medidas legais cabíveis.", FONT_TERMO_TEXTO);

            cTermo.addElement(p1);
            cTermo.addElement(p2);
            cTermo.addElement(p3);
            cTermo.addElement(p4);
            table.addCell(cTermo);
        }

        document.add(table);
    }

    private static void adicionarAssinaturas(Document document, Cliente cliente) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{48, 48});
        table.setSpacingBefore(18);

        String nomeCliente = (cliente != null && cliente.getNome() != null) ? cliente.getNome() : "Cliente";

        // Assinatura Cliente
        PdfPCell cAss1 = new PdfPCell();
        cAss1.setBorder(Rectangle.NO_BORDER);
        cAss1.setHorizontalAlignment(Element.ALIGN_CENTER);

        Paragraph pLinha1 = new Paragraph("____________________________________________", FONT_TEXTO_MUTED);
        pLinha1.setAlignment(Element.ALIGN_CENTER);
        Paragraph pNome1 = new Paragraph(nomeCliente, FONT_TEXTO_BOLD);
        pNome1.setAlignment(Element.ALIGN_CENTER);
        Paragraph pDoc1 = new Paragraph("CPF/RG: ________________________", FONT_TEXTO_MUTED);
        pDoc1.setAlignment(Element.ALIGN_CENTER);
        Paragraph pSub1 = new Paragraph("Assinatura do Cliente / Deixou o Equipamento", FONT_TEXTO_MUTED);
        pSub1.setAlignment(Element.ALIGN_CENTER);

        cAss1.addElement(pLinha1);
        cAss1.addElement(pNome1);
        cAss1.addElement(pDoc1);
        cAss1.addElement(pSub1);

        // Assinatura Atendente
        PdfPCell cAss2 = new PdfPCell();
        cAss2.setBorder(Rectangle.NO_BORDER);
        cAss2.setHorizontalAlignment(Element.ALIGN_CENTER);

        CaixaDAO caixaDAO = new CaixaDAO();
        String nomeLoja = caixaDAO.obterConfig("nome_loja", "Assistência Técnica");

        Paragraph pLinha2 = new Paragraph("____________________________________________", FONT_TEXTO_MUTED);
        pLinha2.setAlignment(Element.ALIGN_CENTER);
        Paragraph pNome2 = new Paragraph(nomeLoja, FONT_TEXTO_BOLD);
        pNome2.setAlignment(Element.ALIGN_CENTER);
        Paragraph pData2 = new Paragraph("Data de Entrada: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), FONT_TEXTO_MUTED);
        pData2.setAlignment(Element.ALIGN_CENTER);
        Paragraph pSub2 = new Paragraph("Atendente / Responsável pelo Recebimento", FONT_TEXTO_MUTED);
        pSub2.setAlignment(Element.ALIGN_CENTER);

        cAss2.addElement(pLinha2);
        cAss2.addElement(pNome2);
        cAss2.addElement(pData2);
        cAss2.addElement(pSub2);

        table.addCell(cAss1);
        table.addCell(cAss2);

        document.add(table);
    }

    private static void adicionarRodape(Document document) throws DocumentException {
        PdfPTable footer = new PdfPTable(1);
        footer.setWidthPercentage(100);
        footer.setSpacingBefore(10);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.TOP);
        cell.setBorderColor(COLOR_BORDER_LIGHT);
        cell.setBorderWidth(0.5f);
        cell.setPaddingTop(4);

        CaixaDAO caixaDAO = new CaixaDAO();
        String nomeLoja = caixaDAO.obterConfig("nome_loja", "Sistema de Gestão de Ordens de Serviço");
        String agora = LocalDateTime.now().format(FORMATTER);
        Paragraph p = new Paragraph("Comprovante de entrada emitido eletronicamente em " + agora + " • " + nomeLoja, FONT_RODAPE);
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);

        footer.addCell(cell);
        document.add(footer);
    }

    private static PdfPCell criarCelulaHeaderSecao(String titulo) {
        PdfPCell cell = new PdfPCell(new Phrase(titulo, FONT_HEADER_SECAO));
        cell.setBackgroundColor(COLOR_BG_HEADER);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBorderWidth(0.8f);
        cell.setPadding(4.5f);
        return cell;
    }

    private static String valorOuTraco(String val) {
        if (val == null || val.trim().isEmpty() || "null".equalsIgnoreCase(val.trim())) {
            return "-";
        }
        return val.trim();
    }

    private static String valorOuVazio(String val) {
        if (val == null || "null".equalsIgnoreCase(val.trim())) {
            return "";
        }
        return val.trim();
    }
}
