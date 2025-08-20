package com.hotty.user_service.usecases;

import org.springframework.data.geo.Distance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.geo.Metrics;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Component;

import com.hotty.user_service.DTOs.UserDTO;
import com.hotty.user_service.DTOs.UserDTOwithDistance;
import com.hotty.user_service.MappersDTO.UserDTOMapper;
import com.hotty.user_service.repository.interfaces.UserModelRepository;
import com.hotty.user_service.validators.UserDataModelValidator;

import reactor.core.publisher.Flux;

@Component
public class GetUserByPositionUseCase {

    private final UserModelRepository userModelRepository;

    public GetUserByPositionUseCase(UserModelRepository userModelRepository) {
        this.userModelRepository = userModelRepository;
    }

    public Flux<UserDTOwithDistance> execute(double latitude, double longitude, double radiusInKm,
            HashMap<String, Object> characteristics, Integer maxAge, Integer minAge, String preferredSex) {
        // Validar características antes de la consulta
        if (!areCharacteristicsValid(characteristics)) {
            return Flux.error(new IllegalArgumentException("Invalid characteristics provided"));
        }

        // Validar que maxAge y minAge sean enteros positivos
        if (maxAge == null || minAge == null || maxAge < 0 || minAge < 0) {
            return Flux.error(new IllegalArgumentException("maxAge and minAge must be non-negative integers"));
        }
        if (maxAge < minAge) {
            return Flux.error(new IllegalArgumentException("maxAge must be greater than or equal to minAge"));
        }
        // Validar que preferredSex sea uno de los valores permitidos
        if (preferredSex == null
                || (!preferredSex.equals("Male") && !preferredSex.equals("Female") && !preferredSex.equals("Both"))) {
            return Flux.error(new IllegalArgumentException("Invalid preferredSex value"));
        }
        // Validar que las coordenadas sean válidas
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            return Flux.error(new IllegalArgumentException("Invalid latitude or longitude values"));
        }

        // Validar que el radio sea positivo
        if (radiusInKm <= 0) {
            return Flux.error(new IllegalArgumentException("radiusInKm must be a positive number"));
        }

        if(minAge<18 || maxAge>100) {
            return Flux.error(new IllegalArgumentException("minAge must be >= 18 and maxAge must be <= 100"));
        }

        GeoJsonPoint point = new GeoJsonPoint(longitude, latitude); // OJO: primero long, luego lat
        Distance distance = new Distance(radiusInKm, Metrics.KILOMETERS);
        // CORRECCIÓN: Usar el parámetro 'preferredSex' en lugar de un valor fijo "Both".
        return userModelRepository.findByLocationNear(point, distance, characteristics, maxAge, minAge, preferredSex);

    }

    private boolean areCharacteristicsValid(HashMap<String, Object> characteristics) {
        if (characteristics == null || characteristics.isEmpty()) {
            return true; // No hay características para validar, se asume válido.
        }

        // Validar cada característica
        for (Map.Entry<String, Object> entry : characteristics.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 1. Validar si la clave de la característica es válida.
            if (!UserDataModelValidator.ALLOWED_CHARACTERISTIC_KEYS.contains(key)) {
                return false; // Clave de característica inválida
            }

            // 2. Validar si el valor de la característica es válido para esa clave.
            List<String> allowedValues = UserDataModelValidator.ALLOWED_VALUES_MAP.get(key);
            if (allowedValues != null && !allowedValues.contains(value)) {
                return false; // Valor de característica inválido
            }
        }
        return true;
    }
}
