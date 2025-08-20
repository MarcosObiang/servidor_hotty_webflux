package com.hotty.user_service.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Instant; // Mantener Instant
import java.time.LocalDate; // Necesario para LocalDate.now() si se compara solo la fecha
import java.time.ZoneOffset; // Para convertir Instant a LocalDate para la comparación de edad
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;

import com.hotty.user_service.model.UserCharacteristicsModel;
import com.hotty.user_service.model.UserDataModel;
import com.hotty.user_service.model.UserRewardsDataModel;
import com.hotty.user_service.model.UserSettingsModel;

public class UserDataModelValidator implements ConstraintValidator<ValidUserData, UserDataModel> {

    private static final List<String> ALLOWED_SEX_VALUES = Arrays.asList("Male", "Female", "Other");
    private static final List<String> ALLOWED_SEX_PREFERENCE_VALUES = Arrays.asList("Male", "Female", "Both");

    // --- Listas de valores permitidos para UserCharacteristicsModel ---
    // Se permite 'null' para indicar "sin respuesta".
    public static final List<String> ALLOWED_ALCOHOL_VALUES = Arrays.asList(null, "SOCIALLY", "NEVER", "DONT_MIND");
    public static final List<String> ALLOWED_IM_LOOKING_FOR_VALUES = Arrays.asList(null, "SERIOUS_RELATIONSHIP", "SOMETHING_CASUAL", "OPEN_TO_OPTIONS", "NOT_SURE");
    public static final List<String> ALLOWED_BODY_TYPE_VALUES = Arrays.asList(null, "AVERAGE", "ATHLETIC", "LARGE", "SLIM");
    public static final List<String> ALLOWED_CHILDREN_VALUES = Arrays.asList(null, "SOMEDAY", "NEVER", "HAVE_CHILDREN");
    public static final List<String> ALLOWED_PETS_VALUES = Arrays.asList(null, "DOG", "CAT", "WOULD_LIKE_TO_HAVE");
    public static final List<String> ALLOWED_POLITICS_VALUES = Arrays.asList(null, "RIGHT_WING", "LEFT_WING", "CENTER", "APOLITICAL");
    public static final List<String> ALLOWED_IM_LIVING_WITH_VALUES = Arrays.asList(null, "ALONE", "FAMILY", "FRIENDS");
    public static final List<String> ALLOWED_SMOKE_VALUES = Arrays.asList(null, "NON_SMOKER", "SMOKER", "SOMETIMES", "HATE_SMOKING");
    public static final List<String> ALLOWED_SEXUAL_ORIENTATION_VALUES = Arrays.asList(null, "HETEROSEXUAL", "GAY", "LESBIAN", "BISEXUAL", "ASEXUAL", "DEMISEXUAL", "PANSEXUAL", "QUEER");
    public static final List<String> ALLOWED_ZODIAC_SIGN_VALUES = Arrays.asList(null, "AQUARIUS", "PISCES", "GEMINI", "CANCER", "LEO", "VIRGO", "LIBRA", "SCORPIO", "SAGITTARIUS", "CAPRICORN", "TAURUS", "ARIES");
    public static final List<String> ALLOWED_PERSONALITY_VALUES = Arrays.asList(null, "INTROVERT", "EXTROVERT", "MIX");

    // Conjunto de todas las claves de características permitidas para una validación dinámica
    public static final Set<String> ALLOWED_CHARACTERISTIC_KEYS = new HashSet<>(Arrays.asList(
        "alcohol", "imLookingFor", "bodyType", "children", "pets", "politics",
        "imLivingWith", "smoke", "sexualOrientation", "zodiacSign", "personality"
    ));

    // Mapa para una validación de valores más dinámica y centralizada.
    public static final Map<String, List<String>> ALLOWED_VALUES_MAP;
    static {
        ALLOWED_VALUES_MAP = new HashMap<>();
        ALLOWED_VALUES_MAP.put("alcohol", ALLOWED_ALCOHOL_VALUES);
        ALLOWED_VALUES_MAP.put("imLookingFor", ALLOWED_IM_LOOKING_FOR_VALUES);
        ALLOWED_VALUES_MAP.put("bodyType", ALLOWED_BODY_TYPE_VALUES);
        ALLOWED_VALUES_MAP.put("children", ALLOWED_CHILDREN_VALUES);
        ALLOWED_VALUES_MAP.put("pets", ALLOWED_PETS_VALUES);
        ALLOWED_VALUES_MAP.put("politics", ALLOWED_POLITICS_VALUES);
        ALLOWED_VALUES_MAP.put("imLivingWith", ALLOWED_IM_LIVING_WITH_VALUES);
        ALLOWED_VALUES_MAP.put("smoke", ALLOWED_SMOKE_VALUES);
        ALLOWED_VALUES_MAP.put("sexualOrientation", ALLOWED_SEXUAL_ORIENTATION_VALUES);
        ALLOWED_VALUES_MAP.put("zodiacSign", ALLOWED_ZODIAC_SIGN_VALUES);
        ALLOWED_VALUES_MAP.put("personality", ALLOWED_PERSONALITY_VALUES);
    }

    @Override
    public boolean isValid(UserDataModel user, ConstraintValidatorContext context) {
        if (user == null) {
            return true; // Dejar que @NotNull en el controlador se encargue si es necesario.
        }

        boolean isValid = true;
        // Deshabilitamos el mensaje de violación por defecto para poder añadir los nuestros, campo por campo.
        context.disableDefaultConstraintViolation();

        // --- Validación de campos de UserDataModel ---

        if (isBlank(user.getUserUID())) {
            isValid = false;
            addViolation(context, "User UID is required", "userUID");
        }

        if (isBlank(user.getName())) {
            isValid = false;
            addViolation(context, "Name is required", "name");
        } else if (user.getName().length() > 25) {
            isValid = false;
            addViolation(context, "Name cannot exceed 25 characters", "name");
        }

        if (isBlank(user.getUserImage1())) {
            isValid = false;
            addViolation(context, "At least one image is required", "userImage1");
        }

        // Las imágenes opcionales (userImage2 a userImage6) no requieren validación de formato aquí,
        // ya que contendrán un identificador (ej: 'userUID-profileImage-2'), no una URL completa.

        if (isBlank(user.getSex())) {
            isValid = false;
            addViolation(context, "Sex is required", "sex");
        } else if (!ALLOWED_SEX_VALUES.contains(user.getSex())) {
            isValid = false;
            addViolation(context, "El valor para 'sex' no es válido. Valores permitidos son: Male, Female, Other.", "sex");
        }

        if (user.getLocation() == null) {
            isValid = false;
            addViolation(context, "Location is required", "location");
        } else {
            // Validar límites de coordenadas
            double longitude = user.getLocation().getX();
            double latitude = user.getLocation().getY();
            if (longitude < -180 || longitude > 180) {
                isValid = false;
                addViolation(context, "Longitude must be between -180 and 180", "location.longitude");
            }
            if (latitude < -90 || latitude > 90) {
                isValid = false;
                addViolation(context, "Latitude must be between -90 and 90", "location.latitude");
            }
        }

        if (user.getUserBio() != null && user.getUserBio().length() > 400) {
            isValid = false;
            addViolation(context, "User bio cannot exceed 400 characters", "userBio");
        }

        if (user.getBirthDate() == null) {
            isValid = false;
            addViolation(context, "Birth date is required", "birthDate");
        } else if (user.getBirthDate().isAfter(LocalDate.now())) { // Comparar Instant con Instant.now()
            // Si la intención es validar solo la fecha (día, mes, año) y no la hora,
            // se podría convertir a LocalDate para la comparación:
            // } else if (user.getBirthDate().atOffset(ZoneOffset.UTC).toLocalDate().isAfter(LocalDate.now())) {
            isValid = false;
            addViolation(context, "Birth date must be in the past", "birthDate");
        }

        // --- Validación de campos de UserSettingsModel ---
        UserSettingsModel settings = user.getSettings();
        if (settings == null) {
            isValid = false;
            addViolation(context, "User settings cannot be null", "settings");
            return false; // Detener la validación aquí si 'settings' es nulo.
        }

        if (settings.getMinAge() < 18) {
            isValid = false;
            addViolation(context, "La edad mínima no puede ser menor de 18 años.", "settings.minAge");
        }

        if (settings.getMinAge() > settings.getMaxAge()) {
            isValid = false;
            // Este error es sobre la relación entre dos campos, lo asignamos al objeto 'settings'
            addViolation(context, "La edad mínima no puede ser mayor que la edad máxima.", "settings");
        }

        if (isBlank(settings.getSexPreference())) {
            isValid = false;
            addViolation(context, "La preferencia de género no puede estar vacía.", "settings.sexPreference");
        } else if (!ALLOWED_SEX_PREFERENCE_VALUES.contains(settings.getSexPreference())) {
            isValid = false;
            addViolation(context, "La preferencia de género no es válida. Valores permitidos son: Male, Female, Both.", "settings.sexPreference");
        }

        // --- Validación de campos de UserCharacteristicsModel ---
        UserCharacteristicsModel characteristics = user.getCharacteristics();
        if (characteristics == null) {
            isValid = false;
            addViolation(context, "User characteristics cannot be null", "characteristics");
            return false; // Detener si es nulo
        }
        
        // Validamos que los valores de las características, si se proporcionan, estén en las listas permitidas.
        if (!ALLOWED_ALCOHOL_VALUES.contains(characteristics.getAlcohol())) {
            isValid = false;
            addViolation(context, "Valor para 'alcohol' no es válido.", "characteristics.alcohol");
        }
        if (!ALLOWED_IM_LOOKING_FOR_VALUES.contains(characteristics.getImLookingFor())) {
            isValid = false;
            addViolation(context, "Valor para 'imLookingFor' no es válido.", "characteristics.imLookingFor");
        }
        if (!ALLOWED_BODY_TYPE_VALUES.contains(characteristics.getBodyType())) {
            isValid = false;
            addViolation(context, "Valor para 'bodyType' no es válido.", "characteristics.bodyType");
        }
        if (!ALLOWED_CHILDREN_VALUES.contains(characteristics.getChildren())) {
            isValid = false;
            addViolation(context, "Valor para 'children' no es válido.", "characteristics.children");
        }
        if (!ALLOWED_PETS_VALUES.contains(characteristics.getPets())) {
            isValid = false;
            addViolation(context, "Valor para 'pets' no es válido.", "characteristics.pets");
        }
        if (!ALLOWED_POLITICS_VALUES.contains(characteristics.getPolitics())) {
            isValid = false;
            addViolation(context, "Valor para 'politics' no es válido.", "characteristics.politics");
        }
        if (!ALLOWED_IM_LIVING_WITH_VALUES.contains(characteristics.getImLivingWith())) {
            isValid = false;
            addViolation(context, "Valor para 'imLivingWith' no es válido.", "characteristics.imLivingWith");
        }
        if (!ALLOWED_SMOKE_VALUES.contains(characteristics.getSmoke())) {
            isValid = false;
            addViolation(context, "Valor para 'smoke' no es válido.", "characteristics.smoke");
        }
        if (!ALLOWED_SEXUAL_ORIENTATION_VALUES.contains(characteristics.getSexualOrientation())) {
            isValid = false;
            addViolation(context, "Valor para 'sexualOrientation' no es válido.", "characteristics.sexualOrientation");
        }
        if (!ALLOWED_ZODIAC_SIGN_VALUES.contains(characteristics.getZodiacSign())) {
            isValid = false;
            addViolation(context, "Valor para 'zodiacSign' no es válido.", "characteristics.zodiacSign");
        }
        if (!ALLOWED_PERSONALITY_VALUES.contains(characteristics.getPersonality())) {
            isValid = false;
            addViolation(context, "Valor para 'personality' no es válido.", "characteristics.personality");
        }

        // --- Validación de campos de UserRewardsDataModel ---
        UserRewardsDataModel rewards = user.getRewards();
        if (rewards == null) {
            isValid = false;
            addViolation(context, "User rewards cannot be null", "rewards");
            return false; // Detener si es nulo
        }

        if (rewards.getCoins() < 0) {
            isValid = false;
            addViolation(context, "Coins cannot be negative.", "rewards.coins");
        }

        if (rewards.getSuccessfulShares() < 0) {
            isValid = false;
            addViolation(context, "Successful shares cannot be negative.", "rewards.successfulShares");
        }

        return isValid;
    }

    private void addViolation(ConstraintValidatorContext context, String message, String field) {
        context.buildConstraintViolationWithTemplate(message).addPropertyNode(field).addConstraintViolation();
    }

    private boolean isBlank(String str) { return str == null || str.trim().isEmpty(); }
}