package org.amalitech.bloggingplatformspring.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;

@Getter
@Data
public class SignInUserDTO {

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email cannot be blank")
    private String email;

    @Size(min = 6, max = 50, message = "Password cannot be less than 6 or greater than 50")
    @NotBlank(message = "Password cannot be blank")
    private String password;
}