package com.rescuesync.nacional.util;

import java.text.Normalizer;
import java.util.Locale;

public final class Texto {
    private Texto() {
    }

    /** Clave de comparacion: sin tildes, minusculas y espacios colapsados. */
    public static String clave(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /** CUIT solo con digitos. */
    public static String soloDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }
}
