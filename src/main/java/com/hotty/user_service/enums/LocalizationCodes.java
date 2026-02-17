package com.hotty.user_service.enums;

/**
 * Códigos de localización con soporte para variaciones regionales
 * Formato: IDIOMA_PAIS (estándar RFC-5646 adaptado)
 */
public enum LocalizationCodes {
    // Inglés
    EN_US,  // English (United States)
    EN_GB,  // English (Great Britain)
    
    // Español
    ES_ES,  // Español (España)
    ES_MX,  // Español (México)
    ES_AR,  // Español (Argentina)
    ES_CO,  // Español (Colombia)
    
    // Francés
    FR_FR,  // Français (France)
    FR_CA,  // Français (Canada)
    
    // Alemán
    DE_DE,  // Deutsch (Deutschland)
    DE_AT,  // Deutsch (Austria)
    
    // Italiano
    IT_IT,  // Italiano (Italia)
    
    // Fallbacks genéricos (compatibilidad)
    EN,     // English genérico
    ES,     // Español genérico
    FR,     // Français genérico
    DE,     // Deutsch genérico
    IT      // Italiano genérico
}
