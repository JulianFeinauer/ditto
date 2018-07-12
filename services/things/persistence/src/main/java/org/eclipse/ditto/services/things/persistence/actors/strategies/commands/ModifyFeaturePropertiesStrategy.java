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
import org.eclipse.ditto.model.things.Feature;
import org.eclipse.ditto.model.things.FeatureProperties;
import org.eclipse.ditto.model.things.Thing;
import org.eclipse.ditto.signals.commands.things.modify.ModifyFeatureProperties;
import org.eclipse.ditto.signals.commands.things.modify.ModifyFeaturePropertiesResponse;
import org.eclipse.ditto.signals.events.things.FeaturePropertiesCreated;
import org.eclipse.ditto.signals.events.things.FeaturePropertiesModified;

/**
 * This strategy handles the {@link org.eclipse.ditto.signals.commands.things.modify.ModifyFeatureProperties} command.
 */
@Immutable
final class ModifyFeaturePropertiesStrategy extends AbstractCommandStrategy<ModifyFeatureProperties> {

    /**
     * Constructs a new {@code ModifyFeaturePropertiesStrategy} object.
     */
    ModifyFeaturePropertiesStrategy() {
        super(ModifyFeatureProperties.class);
    }

    @Override
    protected Result doApply(final CommandStrategy.Context context, final ModifyFeatureProperties command) {
        final Thing thing = context.getThingOrThrow();
        final String featureId = command.getFeatureId();

        return thing.getFeatures()
                .flatMap(features -> features.getFeature(featureId))
                .map(feature -> getModifyOrCreateResult(feature, context, command))
                .orElseGet(() -> ResultFactory.newResult(
                        ExceptionFactory.featureNotFound(context.getThingId(), featureId, command.getDittoHeaders())));
    }

    private static Result getModifyOrCreateResult(final Feature feature, final Context context,
            final ModifyFeatureProperties command) {

        return feature.getProperties()
                .map(properties -> getModifyResult(context, command))
                .orElseGet(() -> getCreateResult(context, command));
    }

    private static Result getModifyResult(final Context context, final ModifyFeatureProperties command) {
        final String featureId = command.getFeatureId();
        final DittoHeaders dittoHeaders = command.getDittoHeaders();

        return ResultFactory.newResult(FeaturePropertiesModified.of(command.getId(), featureId, command.getProperties(),
                context.getNextRevision(), getEventTimestamp(), dittoHeaders),
                ModifyFeaturePropertiesResponse.modified(context.getThingId(), featureId, dittoHeaders));
    }

    private static Result getCreateResult(final Context context, final ModifyFeatureProperties command) {
        final String featureId = command.getFeatureId();
        final FeatureProperties featureProperties = command.getProperties();
        final DittoHeaders dittoHeaders = command.getDittoHeaders();

        return ResultFactory.newResult(
                FeaturePropertiesCreated.of(command.getId(), featureId, featureProperties, context.getNextRevision(),
                        getEventTimestamp(), dittoHeaders),
                ModifyFeaturePropertiesResponse.created(context.getThingId(), featureId, featureProperties,
                        dittoHeaders));
    }

}
