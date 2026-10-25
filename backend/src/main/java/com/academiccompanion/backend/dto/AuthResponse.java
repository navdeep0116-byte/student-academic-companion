
package com.academiccompanion.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String message;
    private String token;
    private String tokenType;
    private String id;
    private String fullName;
    private String email;
    private String role;
    private String status;

    public AuthResponse() {
    }

    public static AuthResponse of(
            String message,
            String token,
            String id,
            String fullName,
            String email,
            String role,
            String status) {

        AuthResponse response = new AuthResponse();
        response.message = message;
        response.token = token;
        response.tokenType = token == null ? null : "Bearer";
        response.id = id;
        response.fullName = fullName == null ? "" : fullName;
        response.email = email;
        response.role = role;
        response.status = status;
        return response;
    }

    public String getMessage() {
        return message;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }
}
