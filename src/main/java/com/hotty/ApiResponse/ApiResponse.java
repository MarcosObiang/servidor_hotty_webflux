package com.hotty.ApiResponse;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Una clase de respuesta genérica para estandarizar las respuestas de la API.
 *
 * @param <T> El tipo de datos que contendrá la respuesta.
 */
@JsonInclude(JsonInclude.Include.NON_NULL) // No incluye campos nulos en la serialización JSON
public class ApiResponse<T> {

    private final String status; // Ejemplo: "SUCCESS", "ERROR", "NOT_FOUND"
    private final String message;
    private final T data;

    // Constructor privado para forzar el uso de los métodos factory
    private ApiResponse(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // --- Métodos Factory para éxito ---

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(String.valueOf(HttpStatus.OK.value()), "Operación completada exitosamente.", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(String.valueOf(HttpStatus.OK.value()), message, data);
    }

    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(String.valueOf(HttpStatus.OK.value()), message, null);
    }

    // --- Métodos Factory para error ---

    public static <T> ApiResponse<T> error(String status, String message) {
        return new ApiResponse<>(status, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()), message, null);
    }

    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(String.valueOf(HttpStatus.BAD_REQUEST.value()), message, null);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return new ApiResponse<>(String.valueOf(HttpStatus.NOT_FOUND.value()), message, null);
    }

    // Getters para que Jackson pueda serializar los campos
    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}