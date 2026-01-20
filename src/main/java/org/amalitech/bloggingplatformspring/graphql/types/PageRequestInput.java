package org.amalitech.bloggingplatformspring.graphql.types;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageRequestInput {
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}