package com.aistudybuddy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDTOs {

    public static class RegisterRequest {
        @NotBlank(message = "Name cannot be blank")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Please provide a valid email address")
        private String email;

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
        private String password;

        private String profilePicture;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getProfilePicture() { return profilePicture; }
        public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
    }

    public static class LoginRequest {
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Please provide a valid email address")
        private String email;

        @NotBlank(message = "Password cannot be blank")
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class UpdateProfileRequest {
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;

        private String profilePicture;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getProfilePicture() { return profilePicture; }
        public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
    }

    public static class AuthResponse {
        private String message;
        private String token;
        private UserSummary user;

        public AuthResponse(String message, String token, UserSummary user) {
            this.message = message;
            this.token = token;
            this.user = user;
        }

        public String getMessage() { return message; }
        public String getToken() { return token; }
        public UserSummary getUser() { return user; }
    }

    public static class UserSummary {
        private Long userId;
        private String name;
        private String email;
        private String profilePicture;

        public UserSummary(Long userId, String name, String email, String profilePicture) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.profilePicture = profilePicture;
        }

        public Long getUserId() { return userId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getProfilePicture() { return profilePicture; }
    }
}
