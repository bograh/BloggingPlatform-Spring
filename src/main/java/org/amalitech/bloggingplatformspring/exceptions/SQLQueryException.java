package org.amalitech.bloggingplatformspring.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SQLQueryException extends RuntimeException {
    public SQLQueryException(String message) {
        super(message);
    }
}