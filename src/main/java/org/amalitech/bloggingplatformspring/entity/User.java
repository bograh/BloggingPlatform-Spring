package org.amalitech.bloggingplatformspring.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private UUID id;
    private String username;
    private String email;
    private String password;
    private LocalDateTime createdAt;
}