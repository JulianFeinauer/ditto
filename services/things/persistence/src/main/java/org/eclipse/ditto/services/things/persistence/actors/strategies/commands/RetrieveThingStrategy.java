/*
 * Copyright (c) 2017 Bosch Software Innovations GmbH.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/index.php
 * Contributors:
 *    Bosch Software Innovations GmbH - initial contribution
 *
 */
package org.eclipse.ditto.services.things.persistence.actors.strategies.commands;

import static org.eclipse.ditto.services.things.persistence.actors.strategies.commands.ResultFactory.newResult;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.annotation.concurrent.ThreadSafe;

import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.model.base.exceptions.DittoRuntimeException;
import org.eclipse.ditto.model.base.headers.WithDittoHeaders;
import org.eclipse.ditto.model.things.Thing;
import org.eclipse.ditto.services.things.persistence.snapshotting.ThingSnapshotter;
import org.eclipse.ditto.signals.commands.things.exceptions.ThingNotAccessibleException;
import org.eclipse.ditto.signals.commands.things.exceptions.ThingUnavailableException;
import org.eclipse.ditto.signals.commands.things.query.RetrieveThing;
import org.eclipse.ditto.signals.commands.things.query.RetrieveThingResponse;

/**
 * This strategy handles the {@link RetrieveThing} command.
 */
@ThreadSafe
final class RetrieveThingStrategy extends AbstractCommandStrategy<RetrieveThing> {

    /**
     * Constructs a new {@code RetrieveThingStrategy} object.
     */
    RetrieveThingStrategy() {
        super(RetrieveThing.class);
    }

    @Override
    public boolean isDefined(final Context context, final RetrieveThing command) {
        final Thing thing = context.getThing().orElse(null);

        return Objects.equals(context.getThingId(), command.getId()) && null != thing && !isThingDeleted(thing);
    }

    @Override
    protected CommandStrategy.Result doApply(final CommandStrategy.Context context, final RetrieveThing command) {
        final String thingId = context.getThingId();
        final Thing thing = context.getThingOrThrow();
        final Optional<Long> snapshotRevisionOptional = command.getSnapshotRevision();
        if (snapshotRevisionOptional.isPresent()) {
            try {
                final ThingSnapshotter<?, ?> thingSnapshotter = context.getThingSnapshotter();

                final Optional<Thing> thingOptional = thingSnapshotter.loadSnapshot(snapshotRevisionOptional.get())
                        // TODO timeout???
                        .toCompletableFuture().get();
                return thingOptional.map(thing1 -> newResult(respondWithLoadSnapshotResult(command, thing1)))
                        .orElseGet(() -> newResult(respondWithNotAccessibleException(command)));
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                context.getLog().info("Retrieving thing with ID <{}> was interrupted.", thingId);
                return newResult(respondWithUnavailableException(command));
            } catch (final ExecutionException e) {
                context.getLog().info("Failed to retrieve thing with ID <{}>: {}", thingId, e.getMessage());
                return newResult(respondWithUnavailableException(command));
            }
        } else {
            final JsonObject thingJson = command.getSelectedFields()
                    .map(sf -> thing.toJson(command.getImplementedSchemaVersion(), sf))
                    .orElseGet(() -> thing.toJson(command.getImplementedSchemaVersion()));

            return newResult(RetrieveThingResponse.of(thingId, thingJson, command.getDittoHeaders()));
        }
    }

    private static WithDittoHeaders<RetrieveThingResponse> respondWithLoadSnapshotResult(final RetrieveThing command,
            final Thing snapshotThing) {

        final JsonObject thingJson = command.getSelectedFields()
                .map(sf -> snapshotThing.toJson(command.getImplementedSchemaVersion(), sf))
                .orElseGet(() -> snapshotThing.toJson(command.getImplementedSchemaVersion()));

        return RetrieveThingResponse.of(command.getThingId(), thingJson, command.getDittoHeaders());
    }

    private static DittoRuntimeException respondWithNotAccessibleException(final RetrieveThing command) {
        // reset command headers so that correlationId etc. are preserved
        return new ThingNotAccessibleException(command.getThingId(), command.getDittoHeaders());
    }

    private static DittoRuntimeException respondWithUnavailableException(final RetrieveThing command) {
        // reset command headers so that correlationId etc. are preserved
        return ThingUnavailableException.newBuilder(command.getThingId())
                .dittoHeaders(command.getDittoHeaders())
                .build();
    }

    @Override
    protected Result unhandled(final Context context, final RetrieveThing command) {
        return newResult(new ThingNotAccessibleException(context.getThingId(), command.getDittoHeaders()));
    }

}