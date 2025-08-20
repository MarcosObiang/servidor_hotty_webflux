package com.hotty.user_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.hotty.user_service.DTOs.UserDTO;
import com.hotty.user_service.DTOs.UserDTOwithDistance;
import com.hotty.user_service.controller.Controller;
import com.hotty.user_service.exception.GlobalExceptionHandler;
import com.hotty.user_service.model.*;
import com.hotty.ApiResponse.ApiResponse;
import com.hotty.user_service.usecases.*;

import org.springframework.context.annotation.Import;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant; // Importar Instant
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebFluxTest(Controller.class)
@Import(GlobalExceptionHandler.class) // Remover UpdateBioUseCase de aquí por ahora
class ControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CreateUserUseCase createUserUseCase;

    @MockBean
    private GetUserByUIDUseCase getUserByUIDUseCase;

    @MockBean
    private GetUserByPositionUseCase getUserByPositionUseCase;

    @MockBean
    private UpdateUserImagesUseCase updateUserImagesUseCase;

    @MockBean
    private UpdateUserCharacteristicsUseCase updateUserCharacteristicsUseCase;

    @MockBean
    private UpdateUserSettingsUseCase updateUserSettingsUseCase;

    @MockBean
    private UpdateUserLocationUseCase updateUserLocationUseCase;

    @MockBean
    private DeleteUserUseCase deleteUserUseCase;

    @MockBean
    private UpdateBioUseCase updateBioUseCase; // Asegúrate de tener este mock
    
    private UserDataModel testUser;
    private UserDTO testUserDTO;
    private UserDTOwithDistance testUserDTOwithDistance;

    @BeforeEach
    void setUp() {
        testUser = new UserDataModel();
        testUser.setUserUID("test-uid");
        testUser.setName("Test User");
        testUser.setUserImage1("img1");
        testUser.setSex("Male");
        testUser.setLocation(new GeoJsonPoint(-3.7038, 40.4168));
        testUser.setUserBio("Bio");
        testUser.setBirthDate(LocalDate.now().minusYears(20)); // Ejemplo: 20 años en el pasado
        testUser.setSettings(new UserSettingsModel());
        testUser.setCharacteristics(new UserCharacteristicsModel());
        testUser.setRewards(new UserRewardsDataModel());

        // Crear el DTO de manera más simple
        testUserDTO = new UserDTO();
        testUserDTO.setUserUID("test-uid");
        testUserDTO.setName("Test User");
        testUserDTO.setUserImage1("img1");
        testUserDTO.setSex("Male");
        testUserDTO.setUserBio("Bio");
        testUserDTO.setBirthDate(LocalDate.now().minusYears(20)); // Ejemplo: 20 años en el pasado
        
        testUserDTOwithDistance = new UserDTOwithDistance();
        testUserDTOwithDistance.setUserUID("test-uid");
        testUserDTOwithDistance.setName("Test User");
        testUserDTOwithDistance.setUserImage1("img1");
        testUserDTOwithDistance.setSex("Male");
        testUserDTOwithDistance.setUserBio("Bio");
        testUserDTOwithDistance.setBirthDate(LocalDate.now().minusYears(20));
        testUserDTOwithDistance.setCharacteristics(new UserCharacteristicsModel());
        testUserDTOwithDistance.setDistance(5.5);

    }

    @Test
    void testCreateUser_Success() {
        when(createUserUseCase.execute(any(UserDataModel.class)))
                .thenReturn(Mono.just(testUser));

        webTestClient.post()
                .uri("/users/create")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS") // Cambiar de $.success a $.status
                .jsonPath("$.message").isEqualTo("User created successfully.")
                .jsonPath("$.data.userUID").isEqualTo("test-uid");
    }

    // Test más simple para debuggear
    @Test
    void testCreateUser_Debug() {
        when(createUserUseCase.execute(any(UserDataModel.class)))
                .thenReturn(Mono.just(testUser));

        webTestClient.post()
                .uri("/users/create")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(testUser)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    System.out.println("Status: " + response.getStatus());
                    System.out.println("Response: " + response.getResponseBody());
                });
    }

    @Test
    void testGetUserData_Success() {
        when(getUserByUIDUseCase.execute("test-uid"))
                .thenReturn(Mono.just(testUser));

        webTestClient.get()
                .uri("/users/get")
                .header("userUID", "test-uid")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS") // Cambiar aquí también
                .jsonPath("$.data.userUID").isEqualTo("test-uid");
    }

    @Test
    void testGetUser_Success() {
        when(getUserByUIDUseCase.executeWithDTO("test-uid"))
                .thenReturn(Mono.just(testUserDTO));

        webTestClient.get()
                .uri("/users/get/test-uid")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.data.userUID").isEqualTo("test-uid");
    }

    @Test
    void testGetUsersByPosition_Success_WithFilters() {
        double latitude = 40.4168;
        double longitude = -3.7038;
        double radiusInKm = 5.0;
        HashMap<String, Object> characteristics = new HashMap<>();
        characteristics.put("smoke", "NON_SMOKER");

        when(getUserByPositionUseCase.execute(eq(latitude), eq(longitude), eq(radiusInKm), eq(characteristics)))
                .thenReturn(Flux.just(testUserDTOwithDistance));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/get-users-by-position")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("radiusInKm", radiusInKm)
                        .queryParam("smoke", "NON_SMOKER")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.data").isArray()
                .jsonPath("$.data[0].userUID").isEqualTo("test-uid");
    }

    @Test
    void testGetUsersByPosition_Success_WithoutFilters() {
        double latitude = 40.4168;
        double longitude = -3.7038;
        double radiusInKm = 5.0;
        HashMap<String, Object> emptyCharacteristics = new HashMap<>();

        when(getUserByPositionUseCase.execute(eq(latitude), eq(longitude), eq(radiusInKm), eq(emptyCharacteristics)))
                .thenReturn(Flux.just(testUserDTOwithDistance));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/get-users-by-position")
                        .queryParam("latitude", latitude)
                        .queryParam("longitude", longitude)
                        .queryParam("radiusInKm", radiusInKm)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.data").isArray()
                .jsonPath("$.data[0].userUID").isEqualTo("test-uid");
    }

    @Test
    void testGetUsersByPosition_BadRequest_WithInvalidFilter() {
        double latitude = 40.4168;
        double longitude = -3.7038;
        double radiusInKm = 5.0;

        when(getUserByPositionUseCase.execute(anyDouble(), anyDouble(), anyDouble(), any(HashMap.class)))
                .thenReturn(Flux.error(new IllegalArgumentException("Invalid characteristics provided")));

        webTestClient.get()
                .uri("/users/get-users-by-position?latitude={lat}&longitude={lon}&radiusInKm={rad}&smoke=INVALID_VALUE",
                        latitude, longitude, radiusInKm)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo("BAD_REQUEST")
                .jsonPath("$.message").isEqualTo("Invalid characteristics provided");
    }

    @Test
    void testUpdateCharacteristics_Success() {
        UserCharacteristicsModel characteristics = new UserCharacteristicsModel();
        characteristics.setBodyType("ATHLETIC");

        when(updateUserCharacteristicsUseCase.execute(anyString(), any(UserCharacteristicsModel.class)))
                .thenReturn(Mono.just(testUser));

        webTestClient.put()
                .uri("/users/update-characteristics")
                .header("userUID", "test-uid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(characteristics)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.message").isEqualTo("User characteristics updated successfully.");
    }

    @Test
    void testUpdateSettings_Success() {
        UserSettingsModel settings = new UserSettingsModel();
        settings.setIsDarkModeEnabled(true);

        when(updateUserSettingsUseCase.execute(anyString(), any(UserSettingsModel.class)))
                .thenReturn(Mono.just(testUser));

        webTestClient.put()
                .uri("/users/update-settings")
                .header("userUID", "test-uid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(settings)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.message").isEqualTo("User settings updated successfully.");
    }

    @Test
    void testUpdateImages_Success() {
        Map<String, String> images = Map.of(
                "userImage1", "img1",
                "userImage2", "img2",
                "userImage3", "img3",
                "userImage4", "img4",
                "userImage5", "img5",
                "userImage6", "img6"
        );

        when(updateUserImagesUseCase.execute(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Mono.just(testUser));

        webTestClient.put()
                .uri("/users/update-images")
                .header("userUID", "test-uid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(images)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.message").isEqualTo("User images updated successfully.");
    }

    @Test
    void testUpdateLocation_Success() {
        Map<String, Double> locationData = Map.of(
                "longitude", -3.7038,
                "latitude", 40.4168
        );

        when(updateUserLocationUseCase.execute(anyString(), any(GeoJsonPoint.class)))
                .thenReturn(Mono.empty());

        webTestClient.put()
                .uri("/users/update-location")
                .header("userUID", "test-uid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(locationData)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.message").isEqualTo("User location updated successfully.");
    }

    @Test
    void testDeleteUser_Success() {
        when(deleteUserUseCase.execute("test-uid"))
                .thenReturn(Mono.empty());

        webTestClient.delete()
                .uri("/users/delete?userUID=test-uid")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS")
                .jsonPath("$.message").isEqualTo("User deleted successfully.");
    }

    // Para los tests de error, también necesitarás ajustar si usas el mismo patrón
    @Test
    void testUpdateLocation_BadRequest() {
        Map<String, Double> locationData = Map.of("longitude", -3.7038); // Missing latitude

        webTestClient.put()
                .uri("/users/update-location")
                .header("userUID", "test-uid")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(locationData)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ERROR") // O el valor que uses para errores
                .jsonPath("$.message").isEqualTo("Longitude and latitude are required.");
    }
}