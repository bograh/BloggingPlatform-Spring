package org.amalitech.bloggingplatformspring.graphql.types;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignInUserInput {
    private String email;
    private String password;
}