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

import javax.annotation.concurrent.Immutable;

import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.things.Thing;
import org.eclipse.ditto.signals.commands.things.modify.DeleteFeature;
import org.eclipse.ditto.signals.commands.things.modify.DeleteFeatureResponse;
import org.eclipse.ditto.signals.commands.things.modify.ThingModifyCommandResponse;
import org.eclipse.ditto.signals.events.things.FeatureDeleted;
import org.eclipse.ditto.signals.events.things.ThingModifiedEvent;

/**
 * This strategy handles the {@link org.eclipse.ditto.signals.commands.things.modify.DeleteFeature} command.
 */
@Immutable
final class DeleteFeatureStrategy extends AbstractCommandStrategy<DeleteFeature> {

    /**
     * Constructs a new {@code DeleteFeatureStrategy} object.
     */
    DeleteFeatureStrategy() {
        super(DeleteFeature.class);
    }

    @Override
    protected CommandStrategy.Result doApply(final CommandStrategy.Context context, final DeleteFeature command) {
        final Thing thing = context.getThingOrThrow();
        final String featureId = command.getFeatureId();
        final DittoHeaders dittoHeaders = command.getDittoHeaders();
        return thing.getFeatures()
                .flatMap(features -> features.getFeature(featureId))
                .map(feature -> ResultFactory.newResult(getEventToPersist(context, featureId, dittoHeaders),
                        getResponse(context, featureId, dittoHeaders)))
                .orElseGet(() -> ResultFactory.newResult(
                        ExceptionFactory.featureNotFound(context.getThingId(), featureId, dittoHeaders)));
    }

    private static ThingModifiedEvent getEventToPersist(final Context context, final String featureId,
            final DittoHeaders dittoHeaders) {

        return FeatureDeleted.of(context.getThingId(), featureId, context.getNextRevision(), getEventTimestamp(),
                dittoHeaders);
    }

    private static ThingModifyCommandResponse getResponse(final Context context, final String featureId,
            final DittoHeaders dittoHeaders) {

        return DeleteFeatureResponse.of(context.getThingId(), featureId, dittoHeaders);
    }

}
