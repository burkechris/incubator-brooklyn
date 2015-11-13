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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.location.Location;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.lifecycle.Lifecycle;
import org.apache.brooklyn.core.entity.trait.Startable;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>This implementation will run its children only when a particular condition is met.
 * This condition will be in the form of an expression in one of a number of
 * JSR-223 compliant scripting languages.
 * E.g. AppleScript, groovy, javascript, python.</p>
 * 
 * <p>Other entities may be bound and used in the expression.</p>
 * 
 * <p>Optionally, a script may be supplied and a method invoked on it.
 * Note that the specification states that script engines are not required to support
 * the Invocable interface.
 * The Nashorn (javascript) engine included in Java does but, before writing code
 * that invokes functions, you should verify that the engine for the language in which
 * your script is written supports the Invocable interface.</p>
 * 
 * @author Chris Burke
 */
public class IfConditionImpl extends RunScriptImpl implements IfCondition {

    protected static final Logger logger = LoggerFactory.getLogger(IfConditionImpl.class);

    /**
     * {@inheritDoc}
     */
    public void start(final Collection<? extends Location> locations) {
        logger.trace("{}, Starting.", this);
        // Let everyone know we're starting up (so that the GUI shows the correct icon).
        sensors().set(Attributes.SERVICE_STATE_ACTUAL, Lifecycle.STARTING);

        // Run the expression to get the boolean result.
        final Object result = processScript();
        
        // Make sure that the script returned a Boolean value.
        boolean resultBool = false;
        if (result instanceof Boolean) {
            resultBool = (Boolean) result;
        } else {
            setServiceState(false, Lifecycle.ON_FIRE);
            throw Exceptions.propagate(new RuntimeException(
                    "Incorrect return type for script evaluation result [" + result + "]. Expected Boolean."));
        }

        // Perform the starting of the children if necessary.
        startChildren(locations, resultBool);
        setServiceState(true, Lifecycle.RUNNING);
    }

    /**
     * Start the children depending on the supplied enabled flag. 
     * 
     * @param locations Collection of Locations to pass to each child.
     * @param enabled Whether or not to actually start the children.
     */
    protected void startChildren(final Collection<? extends Location> locations, final boolean enabled) {
        if (enabled) {
            Startable startableChild = null;
            final Collection<Throwable> throwableList = new ArrayList<>();
            for (final Entity childEntity : getChildren()) {
                try {
                    if (childEntity instanceof Startable) {
                        startableChild = (Startable) childEntity;
                        startableChild.start(locations);
                    }
                } catch (Exception e) {
                    logger.error("{}, Failure while trying to start {}.", this, startableChild);
                    throwableList.add(e);
                }
            }
            if (!throwableList.isEmpty()) {
                setServiceState(false, Lifecycle.ON_FIRE);
                throw Exceptions.propagate(throwableList);
            }
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
        final Collection<Throwable> throwableList = new ArrayList<>();
        for (Entity child : getChildren()) {
            try {
                if (child instanceof Startable) {
                    startableChild = (Startable) child;
                    startableChild.stop();
                }
            } catch (Throwable t) {
                logger.error("{}, Problem stopping {}.", this, startableChild);
                throwableList.add(t);
            }
        }
        if (!throwableList.isEmpty()) {
            setServiceState(false, Lifecycle.ON_FIRE);
            throw Exceptions.propagate(throwableList);
        }
        setServiceState(false, Lifecycle.STOPPED);
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
    protected void setServiceState(final boolean serviceUpState, final Lifecycle serviceStateActual) {
        sensors().set(SERVICE_UP, serviceUpState);
        sensors().set(Attributes.SERVICE_STATE_ACTUAL, serviceStateActual);
    }

}
