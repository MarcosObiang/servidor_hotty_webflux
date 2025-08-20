package com.hotty.user_service.repository;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.index.GeospatialIndex;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import java.time.temporal.ChronoUnit;

import com.hotty.user_service.model.*;
import com.hotty.user_service.repository.UserModelRepoImpl;
import com.hotty.user_service.UserServiceApplication;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

@Testcontainers
@SpringBootTest(classes = UserServiceApplication.class)
class UserModelRepoImplIT {

        @Container
        static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0.13");

        @DynamicPropertySource
        static void setProperties(DynamicPropertyRegistry registry) {
                registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        }

        @Autowired
        private ReactiveMongoTemplate reactiveMongoTemplate;

        @Autowired
        private UserModelRepoImpl userModelRepo;

        @BeforeEach
        void setUp() {
                // Limpiar completamente la colección
                reactiveMongoTemplate.dropCollection(UserDataModel.class).block();

                // Insertar datos de prueba
                insertTestData();

                // Asegurar que el índice existe usando indexOps
                reactiveMongoTemplate.indexOps(UserDataModel.class)
                                .dropAllIndexes()
                                .then(reactiveMongoTemplate.indexOps(UserDataModel.class)
                                                .ensureIndex(new GeospatialIndex("location")
                                                                .typed(GeoSpatialIndexType.GEO_2DSPHERE)))
                                .block();
        }

        @AfterEach
        void tearDown() {
                reactiveMongoTemplate.dropCollection(UserDataModel.class).block();
        }

        private void insertTestData() {
                // Usuario 1: Cerca, 30 años, Hombre, Fuma, Bebe
                UserDataModel user1 = createUserDataModel("user1", "John Doe", "Male",
                                LocalDate.now().minusYears(30), new GeoJsonPoint(-74.0060, 40.7128), // New York
                                createCharacteristics("SMOKER", "SOCIAL_DRINKER"), null, Instant.now().minus(2, ChronoUnit.DAYS));

                // Usuario 2: Cerca, 25 años, Mujer, No fuma, No bebe. Calificada hace 5 días.
                UserDataModel user2 = createUserDataModel("user2", "Jane Smith", "Female",
                                LocalDate.now().minusYears(25), new GeoJsonPoint(-74.0050, 40.7130), // New York
                                createCharacteristics("NON_SMOKER", "NON_DRINKER"), null, Instant.now().minus(5, ChronoUnit.DAYS));

                // Usuario 3: Lejos, 35 años, Hombre, No fuma, Bebe. Calificado hace 1 día.
                UserDataModel user3 = createUserDataModel("user3", "Peter Jones", "Male",
                                LocalDate.now().minusYears(35), new GeoJsonPoint(-73.9900, 40.7500), // Times Square
                                                                                                     // (más lejos)
                                createCharacteristics("NON_SMOKER", "SOCIAL_DRINKER"), null, Instant.now().minus(1, ChronoUnit.DAYS));

                // Usuario 4: Cerca, 20 años, Mujer, Fuma, No bebe. Calificada hace 10 días (la más antigua).
                UserDataModel user4 = createUserDataModel("user4", "Alice Brown", "Female",
                                LocalDate.now().minusYears(20), new GeoJsonPoint(-74.0070, 40.7110), // New York
                                createCharacteristics("SMOKER", "NON_DRINKER"), null, Instant.now().minus(10, ChronoUnit.DAYS));

                // Usuario 5: Cerca, 40 años, Hombre, No fuma, No bebe. Nunca calificado.
                UserDataModel user5 = createUserDataModel("user5", "Bob White", "Male",
                                LocalDate.now().minusYears(40), new GeoJsonPoint(-74.0040, 40.7140), // New York
                                createCharacteristics("NON_SMOKER", "NON_DRINKER"), null, null);

                // Usuario 6: Cerca, 28 años, Hombre, No fuma, No bebe. Calificado hace 3 días.
                UserDataModel user6 = createUserDataModel("user6", "Charlie Green", "Male",
                                LocalDate.now().minusYears(28), new GeoJsonPoint(-74.0065, 40.7125), // New York
                                createCharacteristics("NON_SMOKER", "NON_DRINKER"), null, Instant.now().minus(3, ChronoUnit.DAYS));

                // Guardar cada usuario individualmente
                reactiveMongoTemplate.save(user1).block();
                reactiveMongoTemplate.save(user2).block();
                reactiveMongoTemplate.save(user3).block();
                reactiveMongoTemplate.save(user4).block();
                reactiveMongoTemplate.save(user5).block();
                reactiveMongoTemplate.save(user6).block();
        }

        private UserDataModel createUserDataModel(String userUID, String name, String sex, LocalDate birthDate,
                        GeoJsonPoint location, UserCharacteristicsModel characteristics,
                        UserSettingsModel settings, Instant lastRatingDate) {
                UserDataModel user = new UserDataModel(); // settings y characteristics se inicializan aquí por defecto
                user.setUserUID(userUID);
                user.setName(name);
                user.setSex(sex);
                user.setBirthDate(birthDate);
                user.setLocation(location);
                user.setCharacteristics(characteristics);
                user.setSettings(settings);
                user.setLastRatingDate(lastRatingDate);
                // Si settings o characteristics se pasan como null, se sobrescribiría la
                // inicialización por defecto.
                // Aseguramos que siempre haya una instancia válida.
                if (settings != null) {
                        user.setSettings(settings);
                }
                if (characteristics != null) {
                        user.setCharacteristics(characteristics);
                }
                user.setUserBio("Bio for " + name);
                user.setUserImage1("image_" + userUID + "_1.jpg");
                return user;
        }

	private UserCharacteristicsModel createCharacteristics(String smoke, String alcohol) {
                UserCharacteristicsModel chars = new UserCharacteristicsModel(); // alcohol y smoke se inicializan aquí
                chars.setSmoke(smoke); // El validador espera 'smoke'
		chars.setAlcohol(alcohol); // Ahora el parámetro y el campo coinciden, es más claro.
                return chars;
        }

        @Test
        void testFindByUserUID() {
                StepVerifier.create(userModelRepo.findByUserUID("user1"))
                                .expectNextMatches(user -> user.getName().equals("John Doe"))
                                .verifyComplete();
        }

        @Test
        void testUpdateSettings() {
                UserSettingsModel newSettings = new UserSettingsModel();
                newSettings.setIsDarkModeEnabled(true); // Set to true to ensure it's different from default null/false
                newSettings.setMinAge(20);
                newSettings.setMaxAge(30);
                newSettings.setSexPreference("Female");
                // Corregir la aserción: si se establece en true, se debe esperar true.
                StepVerifier.create(userModelRepo.updateSettings("user1", newSettings))
                                .expectNextMatches(user -> user.getSettings().getIsDarkModeEnabled() == true)
                                .verifyComplete();
        }

        @Test
        void testUpdateCharacteristics() {
                UserCharacteristicsModel newChar = new UserCharacteristicsModel();
                newChar.setBodyType("ATHLETIC");
                newChar.setSmoke("NON_SMOKER");
                newChar.setAlcohol("NON_DRINKER"); // El validador espera 'alcohol', no 'drink'

                StepVerifier.create(userModelRepo.updateCharacteristics("user1", newChar))
                                .expectNextMatches(user -> "ATHLETIC".equals(user.getCharacteristics().getBodyType()))
                                .verifyComplete();
        }

        @Test
        void testUpdateBio() {
                StepVerifier.create(userModelRepo.updateBio("user1", "Nueva bio"))
                                .expectNextMatches(user -> "Nueva bio".equals(user.getUserBio()))
                                .verifyComplete();
        }

        @Test
        void testUpdateImages() {
                StepVerifier.create(userModelRepo.updateImages("user1", "img1_new", "img2_new", "img3_new", "img4_new",
                                "img5_new", "img6_new"))
                                // La aserción anterior era incorrecta (esperaba "img6" cuando se actualizaba a
                                // "img6_new")
                                .expectNextMatches(user -> "img6_new".equals(user.getUserImage6()))
                                .verifyComplete();
        }

        @Test
        void testUpdateLocationData() {
                GeoJsonPoint newLocation = new GeoJsonPoint(-58.3816, -34.6037);
                StepVerifier.create(userModelRepo.updateLocationData("user1", newLocation)
                                .then(userModelRepo.findByUserUID("user1")))
                                .expectNextMatches(user -> user.getLocation().getX() == -58.3816
                                                && user.getLocation().getY() == -34.6037)
                                .verifyComplete();
        }

        @Test
        void testDeleteByUserUID() {
                StepVerifier.create(userModelRepo.deleteByUserUID("user1"))
                                .verifyComplete();

                StepVerifier.create(userModelRepo.findByUserUID("user1"))
                                .expectError()
                                .verify();
        }

        @Test
        @DisplayName("Debe encontrar usuarios por ubicación dentro del radio especificado")
        void findByLocationNear_shouldReturnUsersWithinRadius() {
                GeoJsonPoint center = new GeoJsonPoint(-74.0060, 40.7128); // New York
                Distance radius = new Distance(1, Metrics.KILOMETERS); // 1 km

                StepVerifier.create(userModelRepo.findByLocationNear(center, radius, null, null, null, "Both"))
                                .recordWith(java.util.ArrayList::new)
                                .expectNextCount(5) // user1, user2, user4, user5, user6 están dentro de 1km
                                .consumeRecordedWith(users -> {
                                        // Verificar el nuevo orden por lastRatingDate
                                        // user5 (null) debe ser el primero.
                                        // user4 (hace 10 días) debe ser el segundo.
                                        // user2 (hace 5 días) debe ser el tercero.
                                        List<String> uids = users.stream().map(u -> u.getUserUID()).toList();
                                        Assertions.assertEquals("user5", uids.get(0), "El usuario nunca calificado (user5) debería ser el primero.");
                                        Assertions.assertEquals("user4", uids.get(1), "El usuario calificado más antiguamente (user4) debería ser el segundo.");
                                        Assertions.assertEquals("user2", uids.get(2), "El usuario user2 debería ser el tercero.");
                                })
                                .verifyComplete();
        }

        @Test
        @DisplayName("Debe encontrar usuarios filtrando por características")
        void findByLocationNear_shouldFilterByCharacteristics() {
                GeoJsonPoint center = new GeoJsonPoint(-74.0060, 40.7128);
                Distance radius = new Distance(1, Metrics.KILOMETERS);
                HashMap<String, Object> characteristics = new HashMap<>();
                characteristics.put("smoke", "NON_SMOKER");
 
                // CORRECCIÓN: No depender del orden. Recolectar los resultados y verificar el contenido.
                StepVerifier.create(
                                userModelRepo.findByLocationNear(center, radius, characteristics, null, null, "Both"))
                                .recordWith(java.util.ArrayList::new) // Recolecta los elementos en una lista
                                .expectNextCount(3) // Esperamos 3 usuarios en total
                                .consumeRecordedWith(users -> {
                                        // Extraemos los UIDs y verificamos que todos los esperados están presentes
                                        List<String> uids = users.stream().map(user -> user.getUserUID()).toList();
                                        Assertions.assertTrue(uids.containsAll(List.of("user2", "user5", "user6")));
                                })
                                .verifyComplete();
        }

        @Test
        @DisplayName("Debe encontrar usuarios filtrando por rango de edad")
        void findByLocationNear_shouldFilterByAgeRange() {
                GeoJsonPoint center = new GeoJsonPoint(-74.0060, 40.7128);
                Distance radius = new Distance(1, Metrics.KILOMETERS);
                Integer minAge = 25;
                Integer maxAge = 30;

                StepVerifier.create(userModelRepo.findByLocationNear(center, radius, null, maxAge, minAge, "Both"))
                                     .recordWith(java.util.ArrayList::new) // Recolecta los elementos en una lista
                                .expectNextCount(3) // Esperamos 3 usuarios en total
                                .consumeRecordedWith(users -> {
                                        // Extraemos los UIDs y verificamos que todos los esperados están presentes
                                        List<String> uids = users.stream().map(user -> user.getUserUID()).toList();
                                        Assertions.assertTrue(uids.containsAll(List.of("user1", "user2", "user6")));
                                })
                                .verifyComplete();
        }

        @Test
        @DisplayName("Debe encontrar usuarios filtrando por sexo preferido")
        void findByLocationNear_shouldFilterByPreferredSex() {
                GeoJsonPoint center = new GeoJsonPoint(-74.0060, 40.7128);
                Distance radius = new Distance(1, Metrics.KILOMETERS);
                String preferredSex = "Female";

                StepVerifier.create(userModelRepo.findByLocationNear(center, radius, null, null, null, preferredSex))
                                .expectNextMatches(user -> user.getUserUID().equals("user2")) // Jane Smith
                                .expectNextMatches(user -> user.getUserUID().equals("user4")) // Alice Brown
                                .expectNextCount(0)
                                .verifyComplete();
        }

        @Test
        @DisplayName("Debe encontrar usuarios combinando filtros de ubicación, características, edad y sexo")
        void findByLocationNear_shouldCombineAllFilters() {
                GeoJsonPoint center = new GeoJsonPoint(-74.0060, 40.7128);
                Distance radius = new Distance(1, Metrics.KILOMETERS);
                HashMap<String, Object> characteristics = new HashMap<>();
                // Corregir la clave de la característica de "drink" a "alcohol" para que
                // coincida con el modelo.
                characteristics.put("alcohol", "NON_DRINKER");
                Integer minAge = 20;
                Integer maxAge = 25;
                String preferredSex = "Female";

                // Esperamos a Jane Smith (user2): Female, 25 años, No bebe, Cerca
                StepVerifier.create(userModelRepo.findByLocationNear(center, radius, characteristics, maxAge, minAge,
                                preferredSex))
                                .expectNextMatches(user -> user.getUserUID().equals("user2"))
                                .expectNextCount(0)
                                .verifyComplete();
        }

        @Test
        @DisplayName("Debe encontrar solo usuarios dentro de un radio muy pequeño")
        void findByLocationNear_shouldFindOnlyUsersInTinyRadius() {
                GeoJsonPoint center = new GeoJsonPoint(-74.0060, 40.7128);
                Distance radius = new Distance(0.01, Metrics.KILOMETERS); // Radio muy pequeño (10 metros)

                // CORRECCIÓN: El test original esperaba 0 resultados, pero user1 está en el punto exacto.
                // La consulta geoespacial debe devolver a user1 ya que su distancia es 0, que es <= 10 metros.
                StepVerifier.create(userModelRepo.findByLocationNear(center, radius, null, null, null, "Both"))
                                .expectNextMatches(user -> user.getUserUID().equals("user1"))
                                .expectNextCount(0) // No debería haber más
                                .verifyComplete();
        }

        @Test
        @DisplayName("Debe manejar correctamente el caso de preferredSex 'Both'")
        void findByLocationNear_shouldHandlePreferredSexBoth() {
                GeoJsonPoint center = new GeoJsonPoint(-74.0060, 40.7128);
                Distance radius = new Distance(1, Metrics.KILOMETERS);
                String preferredSex = "Both";

                // Esperamos todos los usuarios dentro del radio, independientemente del sexo
                StepVerifier.create(userModelRepo.findByLocationNear(center, radius, null, null, null, preferredSex))
                                .expectNextCount(5) // user1, user2, user4, user5, user6
                                .verifyComplete();
        }

        @Test
        @DisplayName("Debe devolver usuarios con distancia calculada correctamente")
        void findByLocationNear_shouldReturnUsersWithCorrectDistance() {
                GeoJsonPoint center = new GeoJsonPoint(-74.0060, 40.7128); // New York
                Distance radius = new Distance(1, Metrics.KILOMETERS);
 
                // CORRECCIÓN: El test original dependía del orden, lo cual no está garantizado
                // más allá de la distancia. El usuario 6 es más cercano que el usuario 2.
                // Este test ahora recolecta todos los resultados y verifica las distancias
                // de usuarios específicos sin depender del orden.
                StepVerifier.create(userModelRepo.findByLocationNear(center, radius, null, null, null, "Both"))
                                .recordWith(java.util.ArrayList::new)
                                .expectNextCount(5)
                                .consumeRecordedWith(users -> {
                                        // Verificar que user1 (en el punto exacto) tiene una distancia casi 0.
                                        users.stream().filter(u -> u.getUserUID().equals("user1")).findFirst()
                                                        .ifPresentOrElse(
                                                                        user -> Assertions.assertTrue(user.getDistance() < 0.01, "La distancia de user1 debería ser casi 0."),
                                                                        () -> Assertions.fail("No se encontró a user1"));
                                        // Verificar que user6 (muy cercano) tiene una distancia pequeña pero > 0, aprox 0.05km.
                                        users.stream().filter(u -> u.getUserUID().equals("user6")).findFirst()
                                                        .ifPresentOrElse(
                                                                        user -> Assertions.assertTrue(user.getDistance() > 0.04 && user.getDistance() < 0.06, "La distancia de user6 debería ser ~0.05 km"),
                                                                        () -> Assertions.fail("No se encontró a user6"));
                                })
                                .verifyComplete();
        }

        @Test
        @DisplayName("Debe devolver un Flux vacío si no hay usuarios en la base de datos")
        void findByLocationNear_shouldReturnEmptyFluxIfNoUsersInDb() {
                // CORRECCIÓN: No se debe usar dropCollection, ya que elimina la colección y sus índices,
                // causando un error en $geoNear. En su lugar, se eliminan todos los documentos
                // para probar contra una colección vacía pero existente.
                reactiveMongoTemplate.remove(new Query(), UserDataModel.class).block();
                GeoJsonPoint center = new GeoJsonPoint(-74.0060, 40.7128);
                Distance radius = new Distance(1, Metrics.KILOMETERS);

                StepVerifier.create(userModelRepo.findByLocationNear(center, radius, null, null, null, "Both"))
                                .expectNextCount(0)
                                .verifyComplete();
        }

        @Test
        void testFindByUserUID_NotFound() {
                StepVerifier.create(userModelRepo.findByUserUID("not-exist"))
                                .expectErrorMatches(throwable -> throwable instanceof NoSuchElementException &&
                                                throwable.getMessage().contains(
                                                                "No se encontró un usuario con el UID: not-exist"))
                                .verify();
        }

        @Test
        void testUpdateSettings_NotFound() {
                UserSettingsModel settings = new UserSettingsModel();
                StepVerifier.create(userModelRepo.updateSettings("not-exist", settings))
                                .expectErrorMatches(throwable -> throwable instanceof NoSuchElementException &&
                                                throwable.getMessage().contains(
                                                                "No se encontró un usuario con el UID: not-exist"))
                                .verify();
        }

        @Test
        void testUpdateCharacteristics_NotFound() {
                UserCharacteristicsModel characteristics = new UserCharacteristicsModel();
                StepVerifier.create(userModelRepo.updateCharacteristics("not-exist", characteristics))
                                .expectErrorMatches(throwable -> throwable instanceof NoSuchElementException &&
                                                throwable.getMessage().contains(
                                                                "No se encontró un usuario con el UID: not-exist"))
                                .verify();
        }

        @Test
        void testUpdateBio_NotFound() {
                StepVerifier.create(userModelRepo.updateBio("not-exist", "bio"))
                                .expectErrorMatches(throwable -> throwable instanceof NoSuchElementException &&
                                                throwable.getMessage().contains(
                                                                "No se encontró un usuario con el UID: not-exist"))
                                .verify();
        }

        @Test
        void testUpdateImages_NotFound() {
                StepVerifier.create(userModelRepo.updateImages("not-exist", "i1", "i2", "i3", "i4", "i5", "i6"))
                                .expectErrorMatches(throwable -> throwable instanceof NoSuchElementException &&
                                                throwable.getMessage().contains(
                                                                "No se encontró un usuario con el UID: not-exist"))
                                .verify();
        }

        @Test
        void testUpdateLocationData_NotFound() {
                GeoJsonPoint location = new GeoJsonPoint(0, 0);
                StepVerifier.create(userModelRepo.updateLocationData("not-exist", location))
                                .expectErrorMatches(throwable -> throwable instanceof NoSuchElementException &&
                                                throwable.getMessage().contains(
                                                                "No se encontró un usuario con el UID: not-exist"))
                                .verify();
        }

        @Test
        void testDeleteByUserUID_NotFound() {
                StepVerifier.create(userModelRepo.deleteByUserUID("not-exist"))
                                .expectErrorMatches(throwable -> throwable instanceof NoSuchElementException &&
                                                throwable.getMessage().contains(
                                                                "No se encontró un usuario con el UID: not-exist"))
                                .verify();
        }
}