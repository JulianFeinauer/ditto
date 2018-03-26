/*
 * Copyright (c) 2017 Bosch Software Innovations GmbH.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/index.php
 *
 * Contributors:
 *    Bosch Software Innovations GmbH - initial contribution
 */
package org.eclipse.ditto.services.connectivity.messaging.internal;

import java.time.Instant;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.eclipse.ditto.model.base.exceptions.DittoRuntimeException;

import akka.actor.ActorRef;
import akka.actor.Status;

/**
 * Immutable implementation of {@link ConnectionFailure}.
 */
@Immutable
public final class ImmutableConnectionFailure extends AbstractWithOrigin implements ConnectionFailure {

    @Nullable private final Throwable cause;
    @Nullable private final String description;
    private final Instant time;

    /**
     *
     * @param origin
     * @param cause
     * @param description
     */
    public ImmutableConnectionFailure(@Nullable final ActorRef origin, @Nullable final Throwable cause,
            @Nullable final String description) {
        super(origin);
        this.cause = cause;
        this.description = description;
        time = Instant.now();
    }

    @Override
    public Status.Failure getFailure() {
        return new Status.Failure(cause);
    }

    @Override
    public String getFailureDescription() {
        String responseStr = "";
        if (cause != null) {
            if (description != null) {
                responseStr = description + " - cause ";
            }
            responseStr += cause.getClass().getSimpleName() + ": " + cause.getMessage();
            if (cause instanceof DittoRuntimeException) {
                responseStr += " / " + ((DittoRuntimeException) cause).getDescription().orElse("");
            }
        } else if (description != null) {
            responseStr = description;
        } else {
            responseStr = "unknown failure";
        }
        responseStr += " at " + time;
        return responseStr;
    }
}
