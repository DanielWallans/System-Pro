package com.loja.util;

import java.util.regex.Pattern;

/**
 * Utilitário profissional de validações cadastrais do domínio do sistema.
 * Implementa validação algorítmica de dígitos verificadores de CPF e CNPJ da Receita Federal.
 */
public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern NUMERICO_PATTERN = Pattern.compile("^\\d+$");

    private ValidationUtils() {
    }

    /**
     * Valida CPF ou CNPJ conforme a quantidade de dígitos informados.
     */
    public static boolean isCpfOuCnpjValido(String documento) {
        if (documento == null) return false;
        String limpo = apenasNumeros(documento);
        if (limpo.length() == 11) {
            return isCpfValido(limpo);
        } else if (limpo.length() == 14) {
            return isCnpjValido(limpo);
        }
        return false;
    }

    /**
     * Validação oficial de CPF com cálculo dos dois dígitos verificadores (Módulo 11).
     */
    public static boolean isCpfValido(String cpf) {
        if (cpf == null) return false;
        String limpo = apenasNumeros(cpf);
        if (limpo.length() != 11) return false;

        // Rejeita sequências de dígitos repetidos conhecidas
        if (limpo.matches("(\\d)\\1{10}")) return false;

        try {
            int soma1 = 0;
            for (int i = 0; i < 9; i++) {
                soma1 += (limpo.charAt(i) - '0') * (10 - i);
            }
            int digito1 = 11 - (soma1 % 11);
            if (digito1 > 9) digito1 = 0;
            if (digito1 != (limpo.charAt(9) - '0')) return false;

            int soma2 = 0;
            for (int i = 0; i < 10; i++) {
                soma2 += (limpo.charAt(i) - '0') * (11 - i);
            }
            int digito2 = 11 - (soma2 % 11);
            if (digito2 > 9) digito2 = 0;

            return digito2 == (limpo.charAt(10) - '0');
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validação oficial de CNPJ com cálculo dos dois dígitos verificadores.
     */
    public static boolean isCnpjValido(String cnpj) {
        if (cnpj == null) return false;
        String limpo = apenasNumeros(cnpj);
        if (limpo.length() != 14) return false;
        if (limpo.matches("(\\d)\\1{13}")) return false;

        try {
            int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            int soma1 = 0;
            for (int i = 0; i < 12; i++) {
                soma1 += (limpo.charAt(i) - '0') * pesos1[i];
            }
            int digito1 = soma1 % 11;
            digito1 = (digito1 < 2) ? 0 : 11 - digito1;
            if (digito1 != (limpo.charAt(12) - '0')) return false;

            int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            int soma2 = 0;
            for (int i = 0; i < 13; i++) {
                soma2 += (limpo.charAt(i) - '0') * pesos2[i];
            }
            int digito2 = soma2 % 11;
            digito2 = (digito2 < 2) ? 0 : 11 - digito2;

            return digito2 == (limpo.charAt(13) - '0');
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isEmailValido(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isTelefoneValido(String telefone) {
        if (telefone == null) return false;
        String limpo = apenasNumeros(telefone);
        return limpo.length() >= 10 && limpo.length() <= 11;
    }

    public static String apenasNumeros(String str) {
        if (str == null) return "";
        return str.replaceAll("\\D+", "");
    }
}

