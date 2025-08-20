package com.hotty.common.enums;

/**
 * Enum común para tipos de eventos de publicación en todo el monolito.
 * Usado por todos los servicios para eventos de creación, actualización y eliminación.
 */
public enum PublishEventType {
    CREATE,
    UPDATE, 
    DELETE,
    DELETED
}
