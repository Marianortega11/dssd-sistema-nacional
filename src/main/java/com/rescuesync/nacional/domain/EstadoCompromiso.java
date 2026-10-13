package com.rescuesync.nacional.domain;

public enum EstadoCompromiso {
    /** Recursos bloqueados: la ONG no puede ofrecerlos en otra emergencia. */
    ACTIVO,
    /** Recursos liberados (finalizacion de la ONG o cierre del proyecto). */
    LIBERADO
}
