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

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.location.Location;
import org.apache.brooklyn.api.mgmt.Task;
import org.apache.brooklyn.camp.brooklyn.spi.dsl.methods.DslComponent;
import org.apache.brooklyn.core.entity.AbstractEntity;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.lifecycle.Lifecycle;
import org.apache.brooklyn.util.core.task.Tasks;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This implementation will run an arbitrary script or expression written in one of a number of
 * JSR-223 compliant scripting languages.
 * E.g. AppleScript, groovy, javascript, python.
 * 
 * Other entities may be bound and used in the script or expression.
 * 
 * A named function may be invoked on the script.
 * Note that the specification states that script engines are not required to support
 * the Invocable interface.
 * The Nashorn (javascript) engine included in Java does but, before writing code
 * that invokes functions, you should verify that the engine for the language in which
 * your script is written supports the Invocable interface.
 * 
 * A file may be specified in place of the script. Only one or other should be specified.
 * In the case of both being provided, the file will take precedence.
 * This should be specified as a URL.
 * 
 * @author Chris Burke
 */
public class RunScriptImpl extends AbstractEntity implements RunScript {

    protected static final Logger logger = LoggerFactory.getLogger(RunScriptImpl.class);

    /**
     * {@inheritDoc}
     */
    public void start(final Collection<? extends Location> locations) {
        logger.trace("{}, Starting.", this);
        // Let everyone know we're starting up (so that the GUI shows the correct icon).
        sensors().set(Attributes.SERVICE_STATE_ACTUAL, Lifecycle.STARTING);

        processScript();

        setServiceState(true, Lifecycle.RUNNING);
    }

    /**
     * Gets all the configuration, sets everything up and then
     * runs the script or expression and returns the result.
     * 
     * @return the result of the script or expression
     */
    @SuppressWarnings("rawtypes")
    protected Object processScript() {
        // Read in the specified configuration values for this entity.
        final String script = getConfig(SCRIPT);
        final URL file = getConfig(FILE);
        final String language = getConfig(LANGUAGE);
        final String invokedFunctionName = getConfig(INVOKE_FUNCTION);
        final List argumentsList = getConfig(ARGUMENTS);
        final Map bindingsMap = getConfig(BINDINGS);

        final ScriptEngineManager manager = new ScriptEngineManager();

        // Set up any bindings needed for the expression
        setupBindings(bindingsMap, manager.getBindings());

        // Run the script.
        final Object result = evaluateScript(manager, language, file, script, invokedFunctionName, argumentsList);

        return result;
    }

    /**
     * Runs the supplied script on the given Script Engine, invoking the supplied function if necessary.
     * The script may be a single expression requiring no invokedFunctionName.
     * Only one of file or script should be specified.
     * 
     * @param manager The ScriptEngineManager which contains any required entity bindings.
     * @param language The language in which the script has been written.
     * @param file The file containing the script.
     * @param script The script or expression to run.
     * @param invokedFunctionName Optional. The name of the function to run in the script.
     * @param argumentsList The list of arguments to pass when running a named function.
     * @return The result object.
     */
    private Object evaluateScript(final ScriptEngineManager manager, final String language,
                                  final URL file, final String script,
                                  final String invokedFunctionName, final List<?> argumentsList) {
        // Look up the Script Engine for the specified language.
        final ScriptEngine engine = manager.getEngineByName(language);
        if (engine == null) {
            throw Exceptions.propagate(new RuntimeException("Cannot find the ScriptEngine for [" + language + "]."));
        }
        
        Object result = null;
        try {
            if (file != null) {
                // Use the file variable instead of the script variable.
                URL fileLocation = file;
                try {
                    final URL url = file;
                    final BufferedInputStream bis = new BufferedInputStream(url.openStream());
                    final InputStreamReader reader = new InputStreamReader(bis);
                    logger.trace("{}, About to run script file [{}].", this, file);
                    result = engine.eval(reader);
                } catch (FileNotFoundException fnfe) {
                    logger.error("{}, Cannot find script file [{}].", this, file);
                    setServiceState(false, Lifecycle.ON_FIRE);
                    throw Exceptions.propagate(fnfe);
                } catch (IOException ioe) {
                    logger.error("{}, Error while reading script file [{}].", this, fileLocation);
                    setServiceState(false, Lifecycle.ON_FIRE);
                    throw Exceptions.propagate(ioe);
                }
            } else {
                // Otherwise use the script variable.
                logger.trace("{}, About to run script [{}].", this, script);
                result = engine.eval(script);
            }
            logger.trace("{}, Got result [{}].", this, result);
        } catch (ScriptException se) {
            logger.error("{}, Failure while trying to run script.", this);
            setServiceState(false, Lifecycle.ON_FIRE);
            throw Exceptions.propagate(se);
        }
        // If a function has been specified then invoke it.
        if (invokedFunctionName != null) {
            Invocable inv = null;
            try {
                inv = (Invocable) engine;
            } catch (ClassCastException cce) {
                logger.error("{}, Script Engine for [{}] does not support invokable functions.", this, language);
                setServiceState(false, Lifecycle.ON_FIRE);
                throw Exceptions.propagate(cce);
            }
            try {
                logger.trace("{}, Invoking function [{}] with {} arguments.", new Object[]{this, invokedFunctionName, argumentsList.size()});
                Object[] argsArray = argumentsList.toArray(new Object[argumentsList.size()]);
                // Eval the arguments if possible, otherwise use as is.
                for (int i = 0; i < argsArray.length; i++) {
                    try {
                        argsArray[i] = engine.eval("" + argsArray[i]);
                    } catch (ScriptException se) {
                        // Ignore.. use the arg as is.
                    }
                }
                result = inv.invokeFunction(invokedFunctionName, argsArray);
            } catch (NoSuchMethodException | ScriptException e) {
                logger.error("{}, Error invoking function [{}]", this, invokedFunctionName);
                setServiceState(false, Lifecycle.ON_FIRE);
                throw Exceptions.propagate(e);
            }
        }
        logger.debug("{}, Returning evaluated result [{}]", this, result);
        return result;
    }

    /**
     * Sets up any entity bindings needed for the expression using the supplied Map.
     * 
     * @param bindingsMap The Map containing the entity bindings.
     * @param bindings The Bindings object from the Script Engine Manager.
     */
    @SuppressWarnings("rawtypes")
    private void setupBindings(final Map bindingsMap, final Bindings bindings) {
        try {
            final Iterator it = bindingsMap.keySet().iterator();
            while (it.hasNext()) {
                final Object key = it.next();
                final Object value = bindingsMap.get(key);

                Entity bindingEntity = null;
                if (value instanceof Entity) {
                    bindingEntity = (Entity) value;
                } else if (value != null) {
                    bindingEntity = getEntityById(value.toString());
                }
                bindings.put((String) key, bindingEntity);
            }
        } catch (Throwable t) {
            logger.error("{}, Failure while trying to setup expression entity bindings.", this);
            setServiceState(false, Lifecycle.ON_FIRE);
            throw Exceptions.propagate(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void stop() {
        logger.trace("{}, Stopping.", this);
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
    
    /**
     * Returns an Entity based on its id.
     * 
     * @param componentId the entity id
     * @return An Entity object
     * @throws ExecutionException if the lookup task failed
     * @throws InterruptedException if the lookup was interrupted
     */
    private Entity getEntityById(final String componentId) throws ExecutionException,
                                                                  InterruptedException {
        final Task<Entity> targetLookup = new DslComponent(componentId).newTask();
        Entity entity = null;
        try {
            entity = Tasks.resolveValue(targetLookup, Entity.class, getExecutionContext(), "Finding entity " + componentId);
            logger.debug("Found entity by id {}", componentId);
        } catch (final ExecutionException | InterruptedException e) {
            logger.error("Error finding entity {}", componentId);
            throw Exceptions.propagate(e);
        }
        return entity;
    }
}
