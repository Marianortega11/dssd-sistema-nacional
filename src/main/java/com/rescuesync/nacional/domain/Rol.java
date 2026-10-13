package com.rescuesync.nacional.domain;

public enum Rol {
    /** Administrador del Sistema Nacional: habilita ONGs y asigna certificaciones. */
    ADMIN,
    /** Representante de una ONG: administra su propio inventario. */
    ONG,
    /** Cuenta de servicio usada por Bonita para validar, bloquear y liberar. */
    SISTEMA
}
