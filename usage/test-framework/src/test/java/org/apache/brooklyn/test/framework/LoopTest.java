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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import org.apache.brooklyn.test.framework.entity.TestEntity;

/**
 * Tests for the Loop entity.
 * 
 * @author Chris Burke
 */
public class LoopTest {

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
     * We'll have test entities like:
     * testCase
     *     -> loop
     *         -> testEntity
     * Test that the loop entity loops the required number of times when loopCount is 5 (i.e. > 0)
     * and there is a single child entity.
     */
    @Test
    public void testLoopForPositiveLoopCountSingleChild() {
        final int loopCount = 5;
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final Loop loopEntity = testCase.addChild(EntitySpec.create(Loop.class)
                .configure(Loop.LOOP_COUNT, loopCount));
        final TestEntity testEntity = loopEntity.addChild(EntitySpec.create(TestEntity.class));
        testEntity.sensors().set(TestEntity.START_COUNT, 0);
        testEntity.sensors().set(TestEntity.STOP_COUNT, 0);
        testEntity.sensors().set(TestEntity.RESTART_COUNT, 0);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once, the first time through the loop.
        assertThat(testEntity.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called, since on subsequent iterations it uses restart.
        assertThat(testEntity.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // On subsequent iterations it uses restart.
        assertThat(testEntity.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(loopCount - 1);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> loop
     *         -> testEntity
     *         -> testEntity
     * Test that the loop entity loops the required number of times when loopCount is 5 (i.e. > 0)
     * and there is more than one child entity.
     */
    @Test
    public void testLoopForPositiveLoopCountMultipleChildren() {
        final int loopCount = 5;
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final Loop loopEntity = testCase.addChild(EntitySpec.create(Loop.class)
                .configure(Loop.LOOP_COUNT, loopCount));
        final TestEntity testEntity1 = loopEntity.addChild(EntitySpec.create(TestEntity.class));
        testEntity1.sensors().set(TestEntity.START_COUNT, 0);
        testEntity1.sensors().set(TestEntity.STOP_COUNT, 0);
        testEntity1.sensors().set(TestEntity.RESTART_COUNT, 0);
        final TestEntity testEntity2 = loopEntity.addChild(EntitySpec.create(TestEntity.class));
        testEntity2.sensors().set(TestEntity.START_COUNT, 0);
        testEntity2.sensors().set(TestEntity.STOP_COUNT, 0);
        testEntity2.sensors().set(TestEntity.RESTART_COUNT, 0);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once, the first time through the loop.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called, since on subsequent iterations it uses restart.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // On subsequent iterations it uses restart.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(loopCount - 1);
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(loopCount - 1);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> loop
     *         -> testEntity
     * Test that the loop entity loops the required number of times when loopCount is 0 (i.e. no loops)
     * and there is a single child.
     */
    @Test
    public void testLoopForZeroLoopCountSingleChild() {
        final int loopCount = 0;
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final Loop loopEntity = testCase.addChild(EntitySpec.create(Loop.class)
                .configure(Loop.LOOP_COUNT, loopCount));
        final TestEntity testEntity = loopEntity.addChild(EntitySpec.create(TestEntity.class));
        testEntity.sensors().set(TestEntity.START_COUNT, 0);
        testEntity.sensors().set(TestEntity.STOP_COUNT, 0);
        testEntity.sensors().set(TestEntity.RESTART_COUNT, 0);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once, the first time through the loop.
        assertThat(testEntity.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called, since on subsequent iterations it uses restart.
        assertThat(testEntity.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // On subsequent iterations it uses restart.
        assertThat(testEntity.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> loop
     *         -> testEntity
     *         -> testEntity
     * Test that the loop entity loops the required number of times when loopCount is 0 (i.e. no loops)
     * and there are multiple children.
     */
    @Test
    public void testLoopForZeroLoopCountMultipleChildren() {
        final int loopCount = 0;
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final Loop loopEntity = testCase.addChild(EntitySpec.create(Loop.class)
                .configure(Loop.LOOP_COUNT, loopCount));
        final TestEntity testEntity1 = loopEntity.addChild(EntitySpec.create(TestEntity.class));
        testEntity1.sensors().set(TestEntity.START_COUNT, 0);
        testEntity1.sensors().set(TestEntity.STOP_COUNT, 0);
        testEntity1.sensors().set(TestEntity.RESTART_COUNT, 0);
        final TestEntity testEntity2 = loopEntity.addChild(EntitySpec.create(TestEntity.class));
        testEntity2.sensors().set(TestEntity.START_COUNT, 0);
        testEntity2.sensors().set(TestEntity.STOP_COUNT, 0);
        testEntity2.sensors().set(TestEntity.RESTART_COUNT, 0);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once, the first time through the loop.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called, since on subsequent iterations it uses restart.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // On subsequent iterations it uses restart.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }

    /**
     * We'll have test entities like:
     * testCase
     *     -> loop
     *         -> testEntity
     * Test that the loop entity loops the required number of times when loopCount is negative (-5) (i.e. no loops)
     * and there is a single child.
     */
    @Test
    public void testLoopForNegativeLoopCountSingleChild() {
        final int loopCount = -5;
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final Loop loopEntity = testCase.addChild(EntitySpec.create(Loop.class)
                .configure(Loop.LOOP_COUNT, loopCount));
        final TestEntity testEntity = loopEntity.addChild(EntitySpec.create(TestEntity.class));
        testEntity.sensors().set(TestEntity.START_COUNT, 0);
        testEntity.sensors().set(TestEntity.STOP_COUNT, 0);
        testEntity.sensors().set(TestEntity.RESTART_COUNT, 0);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once, the first time through the loop.
        assertThat(testEntity.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called, since on subsequent iterations it uses restart.
        assertThat(testEntity.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // On subsequent iterations it uses restart.
        assertThat(testEntity.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> loop
     *         -> testEntity
     *         -> testEntity
     * Test that the loop entity loops the required number of times when loopCount is negative (-5) (i.e. no loops)
     * and there are multiple children.
     */
    @Test
    public void testLoopForNegativeLoopCountMultipleChildren() {
        final int loopCount = -5;
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final Loop loopEntity = testCase.addChild(EntitySpec.create(Loop.class)
                .configure(Loop.LOOP_COUNT, loopCount));
        final TestEntity testEntity1 = loopEntity.addChild(EntitySpec.create(TestEntity.class));
        testEntity1.sensors().set(TestEntity.START_COUNT, 0);
        testEntity1.sensors().set(TestEntity.STOP_COUNT, 0);
        testEntity1.sensors().set(TestEntity.RESTART_COUNT, 0);
        final TestEntity testEntity2 = loopEntity.addChild(EntitySpec.create(TestEntity.class));
        testEntity2.sensors().set(TestEntity.START_COUNT, 0);
        testEntity2.sensors().set(TestEntity.STOP_COUNT, 0);
        testEntity2.sensors().set(TestEntity.RESTART_COUNT, 0);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once, the first time through the loop.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called, since on subsequent iterations it uses restart.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // On subsequent iterations it uses restart.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> loop
     *         -> testEntity1
     *         -> testEntity2 (throws an Exception)
     * Test that the loop entity loops the required number of times
     * when there is more than one child entity. It will not complete as the 2nd entity will
     * throw an exception on the second restart aborting the loop.
     */
    @Test
    public void testLoopForPositiveLoopCountMultipleChildrenAndThrowableEntity() {
        final int loopCount = 5;
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final Loop loopEntity = testCase.addChild(EntitySpec.create(Loop.class)
                .configure(Loop.LOOP_COUNT, loopCount));
        final TestEntity testEntity1 = loopEntity.addChild(EntitySpec.create(TestEntity.class));
        testEntity1.sensors().set(TestEntity.START_COUNT, 0);
        testEntity1.sensors().set(TestEntity.STOP_COUNT, 0);
        testEntity1.sensors().set(TestEntity.RESTART_COUNT, 0);
        final TestEntity testEntity2 = loopEntity.addChild(EntitySpec.create(TestEntity.class));
        testEntity2.setThrowOnRestart(true); //Throws exception on 2nd restart.
        testEntity2.sensors().set(TestEntity.START_COUNT, 0);
        testEntity2.sensors().set(TestEntity.STOP_COUNT, 0);
        testEntity2.sensors().set(TestEntity.RESTART_COUNT, 0);
        
        try {
            app.start(ImmutableList.of(app.newSimulatedLocation()));
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class);
        }

        // Start is called once, the first time through the loop.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called, since on subsequent iterations it uses restart.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // On subsequent iterations it uses restart.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(2); //Successfully got restarted twice.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(2); //An exception happened on the 2nd restart.
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> loop
     *         -> testEntity1
     *         -> testEntity2 (throws an Exception)
     * Test that the loop entity loops the required number of times when loopCount is 5 (i.e. > 0)
     * and there is more than one child entity. It will complete even though the 2nd entity will
     * throw an exception on the second restart but the loop is ignoring exceptions.
     */
    @Test
    public void testLoopForPositiveLoopCountMultipleChildrenAndThrowableEntityIgnoringExceptions() {
        final int loopCount = 5;
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final Loop loopEntity = testCase.addChild(EntitySpec.create(Loop.class)
                .configure(Loop.LOOP_COUNT, loopCount)
                .configure(Loop.IGNORE_EXCEPTIONS, true));
        final TestEntity testEntity1 = loopEntity.addChild(EntitySpec.create(TestEntity.class));
        testEntity1.sensors().set(TestEntity.START_COUNT, 0);
        testEntity1.sensors().set(TestEntity.STOP_COUNT, 0);
        testEntity1.sensors().set(TestEntity.RESTART_COUNT, 0);
        final TestEntity testEntity2 = loopEntity.addChild(EntitySpec.create(TestEntity.class));
        testEntity2.setThrowOnRestart(true); //Throws exception on 2nd restart.
        testEntity2.sensors().set(TestEntity.START_COUNT, 0);
        testEntity2.sensors().set(TestEntity.STOP_COUNT, 0);
        testEntity2.sensors().set(TestEntity.RESTART_COUNT, 0);
        
        try {
            app.start(ImmutableList.of(app.newSimulatedLocation()));
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class);
        }

        // Start is called once, the first time through the loop.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called, since on subsequent iterations it uses restart.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // On subsequent iterations it uses restart.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(loopCount - 1);
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(loopCount - 1);
    }

}
