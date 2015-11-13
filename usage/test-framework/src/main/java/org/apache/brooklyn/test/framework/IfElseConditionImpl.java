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
import org.apache.brooklyn.core.entity.lifecycle.Lifecycle;
import org.apache.brooklyn.core.entity.trait.Startable;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation will run its first child only when a particular condition is met,
 * otherwise it will run its second child.
 * Any other children it might have are ignored.
 * 
 * This condition will be in the form of an expression in one of a number of
 * JSR-223 compliant scripting languages.
 * E.g. AppleScript, groovy, javascript, python.
 * 
 * Other entities may be bound and used in the expression.
 * 
 * Optionally, a script may be supplied and a method invoked on it.
 * Note that the specification states that script engines are not required to support
 * the Invocable interface.
 * The Nashorn (javascript) engine included in Java does but, before writing code
 * that invokes functions, you should verify that the engine for the language in which
 * your script is written supports the Invocable interface.
 * 
 * @author Chris Burke
 */
public class IfElseConditionImpl extends IfConditionImpl implements IfElseCondition {

    protected static final Logger logger = LoggerFactory.getLogger(IfElseConditionImpl.class);

    /**
     * Start the first or second child depending on the supplied enabled flag. 
     * 
     * @param locations Collection of Locations to pass to each child.
     * @param startFirstChild Whether to start the first or second child.
     */
    protected void startChildren(final Collection<? extends Location> locations, final boolean startFirstChild) {
        Startable startableChild = null;
        try {
            final Entity[] childEntities = new Entity[getChildren().size()];
            getChildren().toArray(childEntities);
            if (startFirstChild) {
                logger.trace("{}, Inside IF - starting first child.", this);
                if (childEntities.length > 0 && childEntities[0] instanceof Startable) {
                    startableChild = (Startable) childEntities[0];
                    startableChild.start(locations);
                }
            } else if (childEntities.length > 1 && childEntities[1] instanceof Startable) {
                logger.trace("{}, Inside ELSE - starting second child.", this);
                startableChild = (Startable) childEntities[1];
                startableChild.start(locations);
            }
        } catch (Throwable t) {
            logger.error("{}, Failure while trying to start {} child {}.",
                    new Object[] { this, (startFirstChild ? "first [if]" : "second [else]"), startableChild });
            setServiceState(false, Lifecycle.ON_FIRE);
            throw Exceptions.propagate(t);
        }
    }
}
