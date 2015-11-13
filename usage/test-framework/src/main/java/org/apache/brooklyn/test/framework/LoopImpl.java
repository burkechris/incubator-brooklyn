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

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.location.Location;
import org.apache.brooklyn.core.entity.AbstractEntity;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.lifecycle.Lifecycle;
import org.apache.brooklyn.core.entity.trait.Startable;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation will loop over its children the specified number of times.
 * On the first loop through, the children will be <b>started</b>.
 * On subsequent loops, the children will be <b>restarted</b>.
 * Exceptions may be ignored so that looping continues if children fail.
 * 
 * @author Chris Burke
 */
public class LoopImpl extends AbstractEntity implements Loop {

    private static final Logger logger = LoggerFactory.getLogger(LoopImpl.class);

    /**
     * {@inheritDoc}
     */
    public void start(final Collection<? extends Location> locations) {
        logger.trace("{}, Starting.", this);
        // Let everyone know we're starting up (so that the GUI shows the correct icon).
        sensors().set(Attributes.SERVICE_STATE_ACTUAL, Lifecycle.STARTING);

        Startable startableChild = null;
        try {
            final int loopCount = getConfig(LOOP_COUNT);
            final boolean ignoreExceptions = getConfig(IGNORE_EXCEPTIONS);

            for (int i = 0; i < loopCount; i++) {
                for (final Entity childEntity : getChildren()) {
                    logger.trace("{}, loop {} - child entity {}", new Object[] { this, i, childEntity });
                    if (childEntity instanceof Startable) {
                        logger.trace("{}, loop {} - child is startable", this, i);
                        startableChild = (Startable) childEntity;
                        // The first time we just start the child,
                        // after that we need to restart it.
                        try {
                            if (i < 1) {
                                logger.debug("{}, loop {} - starting child", this, i);
                                startableChild.start(locations);
                            } else {
                                logger.debug("{}, loop {} - restarting child", this, i);
                                startableChild.restart();
                            }
                        } catch (Exception e) {
                            if (!ignoreExceptions) {
                                throw e;
                            }
                        }
                    }
                }
            }

            setServiceState(true, Lifecycle.RUNNING);
        } catch (Throwable t) {
            logger.error("{}, failure when looping over {}. Aborting loop.", this, startableChild);
            setServiceState(false, Lifecycle.ON_FIRE);
            throw Exceptions.propagate(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        logger.trace("{}, Stopping.", this);
        // Let everyone know we're stopping (so that the GUI shows the correct icon).
        sensors().set(Attributes.SERVICE_STATE_ACTUAL, Lifecycle.STOPPING);

        Startable startableChild = null;
        try {
            for (Entity child : getChildren()) {
                if (child instanceof Startable) {
                    startableChild = (Startable) child;
                    startableChild.stop();
                }
            }
            setServiceState(false, Lifecycle.STOPPED);
        } catch (Throwable t) {
            logger.error("{}, Problem stopping {}.", this, startableChild);
            setServiceState(false, Lifecycle.ON_FIRE);
            throw Exceptions.propagate(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void restart() {
        logger.trace("{}, Restarting.", this);
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
