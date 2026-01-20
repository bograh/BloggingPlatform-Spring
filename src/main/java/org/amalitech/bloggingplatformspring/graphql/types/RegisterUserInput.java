package org.amalitech.bloggingplatformspring.graphql.types;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterUserInput {
    private String username;
    private String email;
    private String password;
}