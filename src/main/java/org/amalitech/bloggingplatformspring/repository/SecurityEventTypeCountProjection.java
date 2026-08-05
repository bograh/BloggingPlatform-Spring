package org.amalitech.bloggingplatformspring.repository;

public interface SecurityEventTypeCountProjection {

    String getEventType();

    long getCount();
}