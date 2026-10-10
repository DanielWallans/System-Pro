package com.loja.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utilitários para validações fiscais (CPF, CNPJ, NCM, Chaves de Acesso)
 * e listas de apoio tributário para o ecossistema brasileiro (SEFAZ).
 */
public class FiscalUtils {

    public static final Map<Integer, String> ORIGENS_MERCADORIA = new LinkedHashMap<>();
    public static final Map<String, String> CFOPS_SAIDA = new LinkedHashMap<>();
    public static final Map<String, String> CSOSN_SIMPLES = new LinkedHashMap<>();
    public static final String[] UNIDADES_PADRAO = { "UN", "PC", "CX", "PAR", "KG", "MT", "L" };

    static {
        // Origens da Mercadoria (Tabela A do Convênio S/N de 1970)
        ORIGENS_MERCADORIA.put(0, "0 - Nacional, exceto as indicadas nos códigos 3, 4, 5 e 8");
        ORIGENS_MERCADORIA.put(1, "1 - Estrangeira - Importação direta");
        ORIGENS_MERCADORIA.put(2, "2 - Estrangeira - Adquirida no mercado interno");
        ORIGENS_MERCADORIA.put(3, "3 - Nacional, mercadoria/bem com Conteúdo de Importação > 40% e <= 70%");
        ORIGENS_MERCADORIA.put(4, "4 - Nacional, cuja produção tenha sido feita em conformidade com o PPB");
        ORIGENS_MERCADORIA.put(5, "5 - Nacional, mercadoria/bem com Conteúdo de Importação <= 40%");
        ORIGENS_MERCADORIA.put(6, "6 - Estrangeira - Importação direta, sem similar nacional (CAMEX)");
        ORIGENS_MERCADORIA.put(7, "7 - Estrangeira - Adquirida mercado interno, sem similar nacional (CAMEX)");
        ORIGENS_MERCADORIA.put(8, "8 - Nacional, mercadoria/bem com Conteúdo de Importação > 70%");

        // CFOPs mais comuns de vendas de comércio e assistência
        CFOPS_SAIDA.put("5102", "5102 - Venda de mercadoria adquirida de terceiros (Dentro do Estado)");
        CFOPS_SAIDA.put("5405", "5405 - Venda de mercadoria com ICMS retido por Substituição Tributária (ST)");
        CFOPS_SAIDA.put("5101", "5101 - Venda de produção do estabelecimento");
        CFOPS_SAIDA.put("6102", "6102 - Venda de mercadoria adquirida de terceiros (Fora do Estado)");
        CFOPS_SAIDA.put("5915", "5915 - Remessa de mercadoria ou bem para conserto ou reparo");
        CFOPS_SAIDA.put("5916", "5916 - Retorno de mercadoria ou bem recebido para conserto ou reparo");

        // CSOSN (Código de Situação da Operação no Simples Nacional)
        CSOSN_SIMPLES.put("102", "102 - Tributada pelo Simples Nacional sem permissão de crédito (Padrão Balcão)");
        CSOSN_SIMPLES.put("500", "500 - ICMS cobrado anteriormente por Substituição Tributária (ST) ou antecipação");
        CSOSN_SIMPLES.put("101", "101 - Tributada pelo Simples Nacional com permissão de crédito");
        CSOSN_SIMPLES.put("103", "103 - Isenção do ICMS no Simples Nacional para faixa de receita bruta");
        CSOSN_SIMPLES.put("300", "300 - Imune de ICMS");
        CSOSN_SIMPLES.put("400", "400 - Não tributada pelo Simples Nacional");
        CSOSN_SIMPLES.put("900", "900 - Outros (Operações especiais)");
    }

    public static String apenasNumeros(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[^0-9]", "");
    }

    public static boolean validarCPF(String cpf) {
        String n = apenasNumeros(cpf);
        if (n.length() != 11) return false;

        // Rejeita sequências de números repetidos (ex: 111.111.111-11)
        if (n.matches("(\\d)\\1{10}")) return false;

        try {
            int soma = 0;
            for (int i = 0; i < 9; i++) {
                soma += (n.charAt(i) - '0') * (10 - i);
            }
            int r1 = 11 - (soma % 11);
            int dig1 = (r1 == 10 || r1 == 11) ? 0 : r1;

            if (dig1 != (n.charAt(9) - '0')) return false;

            soma = 0;
            for (int i = 0; i < 10; i++) {
                soma += (n.charAt(i) - '0') * (11 - i);
            }
            int r2 = 11 - (soma % 11);
            int dig2 = (r2 == 10 || r2 == 11) ? 0 : r2;

            return dig2 == (n.charAt(10) - '0');
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean validarCNPJ(String cnpj) {
        String n = apenasNumeros(cnpj);
        if (n.length() != 14) return false;

        if (n.matches("(\\d)\\1{13}")) return false;

        try {
            int[] peso1 = { 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };
            int soma = 0;
            for (int i = 0; i < 12; i++) {
                soma += (n.charAt(i) - '0') * peso1[i];
            }
            int r1 = soma % 11;
            int dig1 = r1 < 2 ? 0 : 11 - r1;

            if (dig1 != (n.charAt(12) - '0')) return false;

            int[] peso2 = { 6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };
            soma = 0;
            for (int i = 0; i < 13; i++) {
                soma += (n.charAt(i) - '0') * peso2[i];
            }
            int r2 = soma % 11;
            int dig2 = r2 < 2 ? 0 : 11 - r2;

            return dig2 == (n.charAt(13) - '0');
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean validarCpfOuCnpj(String doc) {
        String n = apenasNumeros(doc);
        if (n.length() == 11) return validarCPF(n);
        if (n.length() == 14) return validarCNPJ(n);
        return false;
    }

    public static String formatarCpfCnpj(String doc) {
        String n = apenasNumeros(doc);
        if (n.length() == 11) {
            return String.format("%s.%s.%s-%s", n.substring(0, 3), n.substring(3, 6), n.substring(6, 9), n.substring(9, 11));
        } else if (n.length() == 14) {
            return String.format("%s.%s.%s/%s-%s", n.substring(0, 2), n.substring(2, 5), n.substring(5, 8), n.substring(8, 12), n.substring(12, 14));
        }
        return doc != null ? doc : "";
    }

    public static String formatarChaveAcesso(String chave) {
        String n = apenasNumeros(chave);
        if (n.length() != 44) return chave != null ? chave : "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 44; i += 4) {
            if (i > 0) sb.append(" ");
            sb.append(n.substring(i, Math.min(i + 4, 44)));
        }
        return sb.toString();
    }

    public static boolean validarNCM(String ncm) {
        String n = apenasNumeros(ncm);
        return n.length() == 8;
    }

    public static String formatarNCM(String ncm) {
        String n = apenasNumeros(ncm);
        if (n.length() == 8) {
            return n.substring(0, 4) + "." + n.substring(4, 6) + "." + n.substring(6, 8);
        }
        return ncm != null ? ncm : "";
    }
}
