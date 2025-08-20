package com.hotty.auth_service.response;

import org.springframework.http.HttpStatus;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor

public class SignUpResponse<T> {

    private String message;
    private int statusCode;
    private String statusDescripcion;
    private T data;

    public static <T> SignUpResponse<T> createResponse(String message, T data,
            HttpStatus httpStatus) {

        return new SignUpResponse<T>(message, httpStatus.value(),
                httpStatus.getReasonPhrase(), data);

    }

    @Override
    public String toString() {
        return "SignUpResponse{" +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", statusCode=" + statusCode +
                ", statusDescripcion='" + statusDescripcion + '\'' +
                '}';
    }

}
