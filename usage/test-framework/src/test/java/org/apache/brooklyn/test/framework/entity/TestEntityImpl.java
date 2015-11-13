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

package org.apache.brooklyn.test.framework.entity;

import org.apache.brooklyn.api.location.Location;
import org.apache.brooklyn.core.annotation.EffectorParam;
import org.apache.brooklyn.core.entity.AbstractEntity;

import java.util.Collection;

/**
 * @author m4rkmckenna on 27/10/2015.
 */
public class TestEntityImpl extends AbstractEntity implements TestEntity {
    private String testValue;
    private boolean doThrowOnRestart;
    
    @Override
    public void start(final Collection<? extends Location> locations) {
        Integer existingCount = sensors().get(START_COUNT);
        sensors().set(START_COUNT, existingCount == null ? 1 : existingCount + 1);
    }

    @Override
    public void stop() {
        Integer existingCount = sensors().get(STOP_COUNT);
        sensors().set(STOP_COUNT, existingCount == null ? 1 : existingCount + 1);
    }

    @Override
    public void restart() {
        Integer existingCount = sensors().get(RESTART_COUNT);
        sensors().set(RESTART_COUNT, existingCount == null ? 1 : existingCount + 1);
        if(doThrowOnRestart && existingCount == 1){ // This is the 2nd restart - throw exception.
            throw new RuntimeException("Throwing Restart Exception.");
        }
    }

    @Override
    public void simpleEffector() {
        sensors().set(SIMPLE_EFFECTOR_INVOKED, Boolean.TRUE);
    }

    @Override
    public TestPojo complexEffector(@EffectorParam(name = "stringValue") final String stringValue, @EffectorParam(name = "booleanValue") final Boolean booleanValue, @EffectorParam(name = "longValue") final Long longValue) {
        sensors().set(COMPLEX_EFFECTOR_INVOKED, Boolean.TRUE);
        sensors().set(COMPLEX_EFFECTOR_STRING, stringValue);
        sensors().set(COMPLEX_EFFECTOR_BOOLEAN, booleanValue);
        sensors().set(COMPLEX_EFFECTOR_LONG, longValue);
        return new TestPojo(stringValue, booleanValue, longValue);
    }

    /**
     * Gets any test value that has been set into the entity during the test.
     * 
     * @return the test value
     */
    public String getTestValue() {
        return testValue;
    }

    /**
     * Sets a test value into the entity for later verification.
     * 
     * @param testValue the test value to set into the entity.
     */
    public void setTestValue(final String testValue) {
        this.testValue = testValue;
    }
    
    /**
     * Tells the entity to throw an Exception on restart.
     * 
     * @param doThrow whether to throw on restart.
     */
    public void setThrowOnRestart(final boolean doThrow) {
        doThrowOnRestart = doThrow;
    }
}
