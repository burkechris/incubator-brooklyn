/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.brooklyn.test.framework;

import java.util.Collection;

import org.apache.brooklyn.api.location.Location;
import org.apache.brooklyn.core.entity.AbstractEntity;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.lifecycle.Lifecycle;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation will allow a pause to take place for a specified duration.
 * 
 * @author Chris Burke
 */
public class PauseImpl extends AbstractEntity implements Pause {

    private static final Logger logger = LoggerFactory.getLogger(PauseImpl.class);

    /**
     * {@inheritDoc}
     */
    public void start(final Collection<? extends Location> locations) {
        sensors().set(Attributes.SERVICE_STATE_ACTUAL, Lifecycle.STARTING);
        try {
            final Duration duration = getConfig(DURATION);

            logger.debug("{} sleeping for {}.", this, duration);
            Thread.sleep(duration.toMilliseconds());

            setServiceState(true, Lifecycle.RUNNING);
        } catch (InterruptedException ie) {
            setServiceState(false, Lifecycle.ON_FIRE);
            throw Exceptions.propagate(ie);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        setServiceState(false, Lifecycle.STOPPED);
    }

    /**
     * {@inheritDoc}
     */
    public void restart() {
        stop();
        start(getLocations());
    }

    /**
     * Sets the state of the Entity. Useful so that the GUI shows the correct icon.
     * 
     * @param serviceUpState Whether or not the entity is up.
     * @param serviceStateActual The actual state of the entity.
     */
    private void setServiceState(final boolean serviceUpState, final Lifecycle serviceStateActual) {
        sensors().set(SERVICE_UP, serviceUpState);
        sensors().set(Attributes.SERVICE_STATE_ACTUAL, serviceStateActual);
    }
}
