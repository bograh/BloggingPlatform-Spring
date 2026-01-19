package org.amalitech.bloggingplatformspring.graphql.config;

import graphql.language.StringValue;
import graphql.schema.*;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Configuration
public class GraphQLScalarConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(uuidScalar())
                .scalar(dateTimeScalar());
    }

    private GraphQLScalarType uuidScalar() {
        return GraphQLScalarType.newScalar()
                .name("UUID")
                .description("UUID scalar type")
                .coercing(new Coercing<UUID, String>() {
                    @Override
                    public String serialize(Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof UUID) {
                            return dataFetcherResult.toString();
                        }
                        throw new CoercingSerializeException("Expected a UUID object.");
                    }

                    @Override
                    public UUID parseValue(Object input) throws CoercingParseValueException {
                        try {
                            if (input instanceof String) {
                                return UUID.fromString((String) input);
                            }
                            throw new CoercingParseValueException("Expected a String");
                        } catch (IllegalArgumentException e) {
                            throw new CoercingParseValueException("Invalid UUID format: " + input, e);
                        }
                    }

                    @Override
                    public UUID parseLiteral(Object input) throws CoercingParseLiteralException {
                        if (input instanceof StringValue) {
                            try {
                                return UUID.fromString(((StringValue) input).getValue());
                            } catch (IllegalArgumentException e) {
                                throw new CoercingParseLiteralException("Invalid UUID format", e);
                            }
                        }
                        throw new CoercingParseLiteralException("Expected StringValue");
                    }
                })
                .build();
    }

    private GraphQLScalarType dateTimeScalar() {
        return GraphQLScalarType.newScalar()
                .name("DateTime")
                .description("DateTime scalar type")
                .coercing(new Coercing<LocalDateTime, String>() {
                    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

                    @Override
                    public String serialize(@NonNull Object dataFetcherResult) throws CoercingSerializeException {
                        if (dataFetcherResult instanceof LocalDateTime) {
                            return ((LocalDateTime) dataFetcherResult).format(formatter);
                        }
                        throw new CoercingSerializeException("Expected a LocalDateTime object.");
                    }

                    @Override
                    public LocalDateTime parseValue(@NonNull Object input) throws CoercingParseValueException {
                        try {
                            if (input instanceof String) {
                                return LocalDateTime.parse((String) input, formatter);
                            }
                            throw new CoercingParseValueException("Expected a String");
                        } catch (Exception e) {
                            throw new CoercingParseValueException("Invalid DateTime format: " + input, e);
                        }
                    }

                    @Override
                    public LocalDateTime parseLiteral(@NonNull Object input) throws CoercingParseLiteralException {
                        if (input instanceof StringValue) {
                            try {
                                return LocalDateTime.parse(((StringValue) input).getValue(), formatter);
                            } catch (Exception e) {
                                throw new CoercingParseLiteralException("Invalid DateTime format", e);
                            }
                        }
                        throw new CoercingParseLiteralException("Expected StringValue");
                    }
                })
                .build();
    }
}