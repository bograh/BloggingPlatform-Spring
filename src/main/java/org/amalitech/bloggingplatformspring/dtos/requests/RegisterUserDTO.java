package org.amalitech.bloggingplatformspring.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterUserDTO {

    @Size(min = 3, max = 36, message = "Username cannot be less than 3 or greater than 36")
    @NotBlank(message = "Username cannot be blank")
    private String username;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email cannot be blank")
    private String email;

    @Size(min = 6, max = 50, message = "Username cannot be less than 6 or greater than 50")
    @NotBlank(message = "Password cannot be blank")
    private String password;
}