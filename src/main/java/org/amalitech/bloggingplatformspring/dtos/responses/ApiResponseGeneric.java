package org.amalitech.bloggingplatformspring.dtos.responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiResponseGeneric<T> {
    private String status;
    private String message;
    private T data;

    public ApiResponseGeneric(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public ApiResponseGeneric(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public static <T> ApiResponseGeneric<T> success(String message, T data) {
        return new ApiResponseGeneric<>("success", message, data);
    }

    public static <T> ApiResponseGeneric<T> success(String message) {
        return new ApiResponseGeneric<>("success", message);
    }

    public static <T> ApiResponseGeneric<T> error(String message) {
        return new ApiResponseGeneric<>("error", message, null);
    }
}