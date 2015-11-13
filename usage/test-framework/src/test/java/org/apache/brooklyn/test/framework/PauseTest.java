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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.api.location.LocationSpec;
import org.apache.brooklyn.api.mgmt.ManagementContext;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.test.entity.TestApplication;
import org.apache.brooklyn.location.localhost.LocalhostMachineProvisioningLocation;
import org.apache.brooklyn.util.time.Duration;
import org.assertj.core.data.Offset;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for the Pause entity.
 * 
 * @author Chris Burke
 */
public class PauseTest {

    private TestApplication app;
    private ManagementContext managementContext;
    private String testId;

    /**
     * Run before each test.
     */
    @BeforeMethod
    public void setup() {
        testId = UUID.randomUUID().toString();
        app = TestApplication.Factory.newManagedInstanceForTests();
        managementContext = app.getManagementContext();

        managementContext.getLocationManager().createLocation(
                LocationSpec.create(LocalhostMachineProvisioningLocation.class).configure("name", testId));
    }

    /**
     * Run after each test.
     * 
     * @throws Exception if something unexpectedly fails
     */
    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        if (app != null) {
            Entities.destroyAll(app.getManagementContext());
        }
    }

    /**
     * Test that the Pause entity pauses by at least the specified duration within a small tolerance.
     */
    @Test
    public void testPauseMillis() {
        final long pauseMillis = 2000; // Pause for 2 seconds.
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final Pause pauseEntity = testCase
                .addChild(EntitySpec.create(Pause.class).configure(Pause.DURATION, Duration.millis(pauseMillis)));
        final long startTime = System.currentTimeMillis();
        pauseEntity.start(null);
        final long endTime = System.currentTimeMillis();
        final long actualDurationMillis = endTime - startTime;
        assertThat(actualDurationMillis).isGreaterThanOrEqualTo(pauseMillis)
                .isCloseTo(new Long(pauseMillis), Offset.offset(500L));
    }
    
    /**
     * Test that the Pause entity pauses by at least the specified duration within a small tolerance.
     */
    @Test
    public void testPauseSeconds() {
        final int pauseSeconds = 2; // Pause for 2 seconds.
        final long pauseMillis = pauseSeconds * 1000;
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final Pause pauseEntity = testCase
                .addChild(EntitySpec.create(Pause.class).configure(Pause.DURATION, Duration.seconds(pauseSeconds)));
        final long startTime = System.currentTimeMillis();
        pauseEntity.start(null);
        final long endTime = System.currentTimeMillis();
        final long actualDurationMillis = endTime - startTime;
        assertThat(actualDurationMillis).isGreaterThanOrEqualTo(pauseMillis)
                .isCloseTo(new Long(pauseMillis), Offset.offset(500L));
    }
    
}
