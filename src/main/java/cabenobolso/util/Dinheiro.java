package cabenobolso.util;

import cabenobolso.model.RegraNegocioException;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Conversão entre texto digitado pelo usuário e centavos (long), e formatação
 * de centavos para exibição. Usar inteiros em centavos evita erros de
 * arredondamento do ponto flutuante.
 */
public final class Dinheiro {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Dinheiro() {
    }

    /**
     * Converte textos como "70", "70,5", "70,50", "70.50", "R$ 1.234,56" em centavos.
     * Aceita no máximo duas casas decimais. Pode devolver valor negativo; quem
     * usa o valor decide se negativos são aceitos.
     */
    public static long parse(String texto) throws RegraNegocioException {
        if (texto == null || texto.trim().isEmpty()) {
            throw new RegraNegocioException("Informe um valor em reais (campo vazio).");
        }
        String s = texto.trim().replace("R$", "").replace(" ", "");
        if (s.isEmpty()) {
            throw new RegraNegocioException("Informe um valor em reais (campo vazio).");
        }
        if (s.contains(",")) {
            s = s.replace(".", "").replace(",", ".");   // 1.234,56 -> 1234.56
        } else if (s.indexOf('.') != s.lastIndexOf('.')) {
            s = s.replace(".", "");                       // 1.234.567 -> 1234567
        }
        BigDecimal valor;
        try {
            valor = new BigDecimal(s).stripTrailingZeros();
        } catch (NumberFormatException e) {
            throw new RegraNegocioException("Valor inválido: \"" + texto.trim() + "\". Use o formato 70,00.");
        }
        if (valor.scale() > 2) {
            throw new RegraNegocioException("Use no máximo duas casas decimais (centavos).");
        }
        try {
            return valor.movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new RegraNegocioException("O valor informado é alto demais.");
        }
    }

    /** Formata centavos como "R$ 1.234,56". */
    public static String formatar(long centavos) {
        return "R$ " + formatarSemSimbolo(centavos, true);
    }

    /** Formata centavos como "1234,56" (sem milhar), adequado para preencher campos de texto. */
    public static String formatarCampo(long centavos) {
        return formatarSemSimbolo(centavos, false);
    }

    private static String formatarSemSimbolo(long centavos, boolean milhar) {
        String sinal = centavos < 0 ? "-" : "";
        long absoluto = Math.abs(centavos);
        String reais = String.format(PT_BR, milhar ? "%,d" : "%d", absoluto / 100);
        return String.format("%s%s,%02d", sinal, reais, absoluto % 100);
    }
}
