package com.hotty.auth_service.models;

import java.time.Instant;
import java.util.Date;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.repository.Update;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Document(collection = "auth")
@Data
public class AuthDataModel {

    /**
     * Identificador único del documento en la base de datos.
     */
    @Id
    private String id;
    /**
     * Identificador único del usuario. Debe ser único en la colección.
     */
    @Indexed(unique = true)
    @NotBlank(message = "User UID is required")
    private String userUID;
    /**
     * Dirección de correo electrónico del usuario. Debe ser única y tener un
     * formato válido.
     */
    @Indexed(unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Field("email") // Si en MongoDB se llama email_address
    private String email;

    /**
     * Proveedor de autenticación utilizado (por ejemplo, "google", "facebook").
     */
    @NotBlank(message = "Auth provider is required")
    private String authProvider;

    /**
     * Indica si el usuario ya ha completado el proceso de registro en la
     * aplicación.
     * Por defecto es falso.
     */
    private Boolean isUserRegisteredAlready = false;

    @CreatedDate
    private Instant created_at;

    @LastModifiedDate
    private Instant updated_at;

    /**
     * Versión del documento para control de concurrencia optimista.
     */
    @Version
    private Long version;

}
