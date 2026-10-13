package com.rescuesync.nacional.domain;

import com.rescuesync.nacional.util.Texto;

public enum NivelRiesgo {
    BAJO, MEDIO, ALTO, CRITICO;

    /** Interpreta la gravedad enviada por el sistema local (texto libre: "Alta", "CRITICA", "Grave"...). */
    public static NivelRiesgo parse(String valor) {
        String k = Texto.clave(valor);
        if (k.startsWith("baj") || k.startsWith("lev")) return BAJO;
        if (k.startsWith("med") || k.startsWith("mod")) return MEDIO;
        if (k.startsWith("alt") || k.startsWith("grav")) return ALTO;
        if (k.startsWith("crit") || k.startsWith("muy") || k.startsWith("extrem")) return CRITICO;
        throw new IllegalArgumentException("Gravedad no reconocida: '" + valor
                + "'. Valores admitidos: BAJO, MEDIO, ALTO, CRITICO.");
    }
}
