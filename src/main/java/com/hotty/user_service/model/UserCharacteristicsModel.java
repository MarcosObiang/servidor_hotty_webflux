package com.hotty.user_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCharacteristicsModel {

    /**
     * e.g., "SOCIALLY", "NEVER", "DONT_MIND"
     */
    private String alcohol;

    /**
     * e.g., "SERIOUS_RELATIONSHIP", "SOMETHING_CASUAL", "OPEN_TO_OPTIONS", "NOT_SURE"
     */
    private String imLookingFor;

    /**
     * e.g., "AVERAGE", "ATHLETIC", "LARGE", "SLIM"
     */
    private String bodyType;

    /**
     * e.g., "SOMEDAY", "NEVER", "HAVE_CHILDREN"
     */
    private String children;

    /**
     * e.g., "DOG", "CAT", "WOULD_LIKE_TO_HAVE"
     */
    private String pets;

    /**
     * e.g., "RIGHT_WING", "LEFT_WING", "CENTER", "APOLITICAL"
     */
    private String politics;

    /**
     * e.g., "ALONE", "FAMILY", "FRIENDS"
     */
    private String imLivingWith;

    /**
     * e.g., "NON_SMOKER", "SMOKER", "SOMETIMES", "HATE_SMOKING"
     */
    private String smoke;

    /**
     * e.g., "HETEROSEXUAL", "GAY", "LESBIAN", "BISEXUAL", "ASEXUAL", "DEMISEXUAL", "PANSEXUAL", "QUEER"
     */
    private String sexualOrientation;

    /**
     * e.g., "AQUARIUS", "PISCES", "GEMINI", "CANCER", "LEO", "VIRGO", "LIBRA", "SCORPIO", "SAGITTARIUS", "CAPRICORN", "TAURUS", "ARIES"
     */
    private String zodiacSign;

    /**
     * e.g., "INTROVERT", "EXTROVERT", "MIX"
     */
    private String personality;
}