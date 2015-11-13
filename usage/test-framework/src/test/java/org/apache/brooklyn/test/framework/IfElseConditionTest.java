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
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.api.location.LocationSpec;
import org.apache.brooklyn.api.mgmt.ManagementContext;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.test.entity.TestApplication;
import org.apache.brooklyn.location.localhost.LocalhostMachineProvisioningLocation;
import org.apache.brooklyn.util.exceptions.PropagatedRuntimeException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import org.apache.brooklyn.test.framework.entity.TestEntity;

/**
 * Tests for the IfElseCondition entity.
 * 
 * @author Chris Burke
 */
public class IfElseConditionTest {

    private static final String LANGUAGE_GROOVY = "groovy";
    private static final String LANGUAGE_JAVASCRIPT = "javascript";
    
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
     * Creates a test entity as a child of the specified parent entity.
     * 
     * @param parentEntity The parent entity for the child test entity.
     * @return The child test entity.
     */
    private TestEntity createChildTestEntity(final Entity parentEntity) {
        final TestEntity testEntity = parentEntity.addChild(EntitySpec.create(TestEntity.class));
        testEntity.sensors().set(TestEntity.START_COUNT, 0);
        testEntity.sensors().set(TestEntity.STOP_COUNT, 0);
        testEntity.sensors().set(TestEntity.RESTART_COUNT, 0);
        return testEntity;
    }
    
    /**
     * Creates a IfElseCondition entity as a child of the specified parent entity.
     * 
     * @param parentEntity The parent entity for the child test entity.
     * @param language The language in which the script has been written.
     * @param script The actual script to run.
     * @param file The location of the script file. Overrides script variable.
     * @param invokeFunction The function to invoke on the script. Optional.
     * @param arguments Arguments to pass to the function if required.
     * @param bindings Entity bindings used by the script.
     * @return The child IfElseCondition entity.
     */
    private IfElseCondition createChildIfElseConditionEntity(final Entity parentEntity,
                                                             final String language,
                                                             final String script,
                                                             final URL file,
                                                             final String invokeFunction,
                                                             final List<String> arguments,
                                                             final Map<String, Object> bindings) {
        final List<String> args = arguments == null ? new ArrayList<String>() : arguments;
        final Map<String, Object> bindingsMap = bindings == null ? new LinkedHashMap<String, Object>() : bindings;
        final IfElseCondition ifElseConditionEntity = parentEntity.addChild(EntitySpec.create(IfElseCondition.class)
                .configure(IfElseCondition.LANGUAGE, language)
                .configure(IfElseCondition.SCRIPT, script)
                .configure(IfElseCondition.FILE, file)
                .configure(IfElseCondition.INVOKE_FUNCTION, invokeFunction)
                .configure(IfElseCondition.ARGUMENTS, args)
                .configure(IfElseCondition.BINDINGS, bindingsMap));
        return ifElseConditionEntity;   
    }
    
    /**
     * Creates a temporary file containing the supplied String.
     * The file will be deleted upon JVM exit.
     * 
     * @param scriptString The contents of the file.
     * @return The File object for the temporary file.
     */
    private File createTempScriptFile(final String scriptString) {
        //Create the Script File.
        File file = null;
        try {
            file = File.createTempFile("my_script", ".script");
            file.deleteOnExit();
            Files.write(file.toPath(), scriptString.getBytes(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            fail("Could not create temporary script file.", e);
        }
        return file;
    }

    /**
     * We'll have test entities like:
     * testCase
     *     -> IfElseCondition
     *         -> testEntity1
     *         -> testEntity2
     * Test that the ifElseCondition entity runs only the first child entity if the condition is true.
     * Script is passed as an expression in a variable. No bindings are used.
     */
    @Test
    public void testIfElseConditionForTrueJavascriptExpression() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_JAVASCRIPT,
                "Boolean(10 > 9)", null, null, null, null);
        final TestEntity testEntity1 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);

        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is not called.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> IfElseCondition
     *         -> testEntity1
     *         -> testEntity2
     * Test that the ifElseCondition entity runs only the second child entity if the condition is false.
     * Script is passed as an expression in a variable. No bindings are used.
     */
    @Test
    public void testIfElseConditionForFalseJavascriptExpression() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_JAVASCRIPT,
                "Boolean(10 < 9)", null, null, null, null);
        final TestEntity testEntity1 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is never called.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is called once.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> IfElseCondition
     *         -> testEntity1
     *         -> testEntity2
     * Test that the ifElseCondition entity throws an exception if the condition is not a Boolean value.
     * Script is passed as an expression in a variable. No bindings are used.
     */
    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testIfElseConditionForNonBooleanJavascriptExpression() {
        final String returnVal = "10";
        final String expectedExceptionMessage = "Incorrect return type for script evaluation result [" + returnVal
                + "]. Expected Boolean.";
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_JAVASCRIPT,
                returnVal, null, null, null, null);
        createChildTestEntity(ifElseConditionEntity);
        createChildTestEntity(ifElseConditionEntity);

        try {
            app.start(ImmutableList.of(app.newSimulatedLocation()));
        } catch (Exception e) {
            assertThat(e.getCause().getMessage()).contains(expectedExceptionMessage);
            throw e;
        }
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> IfElseCondition
     *         -> testEntity1
     *         -> testEntity2
     *         -> testEntity3
     *         -> testEntity4
     * Test that the ifElseCondition entity runs only the first child entity if the condition is true
     * regardless of how many children are present.
     * Script is passed as an expression in a variable. No bindings are used.
     */
    @Test
    public void testIfElseConditionForTrueJavascriptExpressionIgnoresExtraChildren() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_JAVASCRIPT,
                "Boolean(10 > 9)", null, null, null, null);
        final TestEntity testEntity1 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity3 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity4 = createChildTestEntity(ifElseConditionEntity);

        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is not called.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is not called.
        assertThat(testEntity3.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity3.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity3.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is not called.
        assertThat(testEntity4.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity4.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity4.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> IfElseCondition
     *         -> testEntity1
     *         -> testEntity2
     *         -> testEntity3
     *         -> testEntity4
     * Test that the ifElseCondition entity runs only the second child entity if the condition is false
     * regardless of how many children are present.
     * Script is passed as an expression in a variable. No bindings are used.
     */
    @Test
    public void testIfElseConditionForFalseJavascriptExpressionIgnoresExtraChildren() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_JAVASCRIPT,
                "Boolean(10 < 9)", null, null, null, null);
        final TestEntity testEntity1 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity3 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity4 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is never called.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is called once.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is never called.
        assertThat(testEntity3.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity3.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity3.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is never called.
        assertThat(testEntity4.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity4.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity4.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> IfElseCondition
     *         -> testEntity1
     *         -> testEntity2
     * Test that the ifElseCondition entity runs only the first child entity if the condition is true.
     * Script is passed as an function in a variable. The function is invoked. 
     * No bindings are used.
     */
    @Test
    public void testIfElseConditionForTrueJavascriptInvoke() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_JAVASCRIPT,
                "function myFunction() { return Boolean(10 > 9); }", null, "myFunction", null, null);
        final TestEntity testEntity1 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is never called.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> IfElseCondition
     *         -> testEntity1
     *         -> testEntity2
     * Test that the ifElseCondition entity only runs the second child entity if the condition is false.
     * Script is passed as a function in a variable. The function is invoked.
     * No bindings are used.
     */
    @Test
    public void testIfElseConditionForFalseJavascriptInvoke() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_JAVASCRIPT,
                "function myFunction() { return Boolean(10 < 9); }", null, "myFunction", null, null);
        final TestEntity testEntity1 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is never called.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is called once.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> IfElseCondition
     *         -> testEntity1
     *         -> testEntity2
     * Test that the ifElseCondition entity only runs the first child entity if the condition is true.
     * Script is passed as an expression in a variable. No bindings are used.
     */
    @Test
    public void testIfElseConditionForTrueGroovyExpression() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY, "(10 > 9)",
                null, null, null, null);
        final TestEntity testEntity1 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is never called.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }

    /**
     * We'll have test entities like:
     * testCase
     *     -> IfElseCondition
     *         -> testEntity1
     *         -> testEntity2
     * Test that the ifElseCondition entity only runs the second child entity if the condition is false.
     * Script is passed as an expression in a variable. No bindings are used.
     */
    @Test
    public void testIfElseConditionForFalseGroovyExpression() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY, "(10 < 9)",
                null, null, null, null);
        final TestEntity testEntity1 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is never called.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is called once.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> IfElseCondition
     *         -> testEntity1
     *         -> testEntity2
     * Test that the ifElseCondition entity only runs the first child entity if the condition is true.
     * Script is passed as an function in a variable. The function is invoked. 
     * No bindings are used.
     */
    @Test
    public void testIfElseConditionForTrueGroovyInvoke() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY,
                "boolean myMethod() { return (10 > 9); }", null, "myMethod", null, null);
        final TestEntity testEntity1 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is never called.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> IfElseCondition
     *         -> testEntity1
     *         -> testEntity2
     * Test that the ifElseCondition entity only runs the second child entity if the condition is false.
     * Script is passed as a function in a variable. The function is invoked.
     * No bindings are used.
     */
    @Test
    public void testIfElseConditionForFalseGroovyInvoke() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY,
                "boolean myMethod() { return (10 < 9); }", null, "myMethod", null, null);
        final TestEntity testEntity1 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is never called.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is called once.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> IfElseCondition
     *         -> testEntity1
     *         -> testEntity2
     * Test that the ifElseCondition entity runs only the first child entity if the condition is true.
     * Script is passed in as a location to file which contains the script.
     * Script uses an invoked method. 
     * No bindings are used.
     */
    @Test
    public void testIfElseConditionForTrueGroovyInvokeUsingFile() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        
        // Create the script file and get the URL.
        final String scriptString = "boolean myMethod() { return (10 > 9); }";
        final File file = createTempScriptFile(scriptString);
        URL fileUrl = null;
        try {
            fileUrl = file.toURI().toURL();
        } catch (MalformedURLException e) {
            fail("Could not locate temporary script file.", e);
        }
        
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY, null, fileUrl,
                "myMethod", null, null);
        final TestEntity testEntity1 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is never called.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> IfElseCondition
     *         -> testEntity1
     *         -> testEntity2
     * Test that the ifElseCondition entity runs only the second child entity if the condition is false.
     * Script is passed in as a location to file which contains the script.
     * Script uses an invoked method. 
     * No bindings are used.
     */
    @Test
    public void testIfElseConditionForFalseGroovyInvokeUsingFile() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        
        // Create the script file and get the URL.
        final String scriptString = "boolean myMethod() { return (10 < 9); }";
        final File file = createTempScriptFile(scriptString);
        URL fileUrl = null;
        try {
            fileUrl = file.toURI().toURL();
        } catch (MalformedURLException e) {
            fail("Could not locate temporary script file.", e);
        }
        
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY, null, fileUrl,
                "myMethod", null, null);
        final TestEntity testEntity1 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is never called.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is called once.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> testEntity1
     *     -> IfElseCondition
     *         -> testEntity2
     *         -> testEntity3
     * Test that the ifElseCondition entity runs only the first child entity if the condition is true.
     * Script is passed as a function in a variable. The function is invoked.
     * testEntity1 is used as a binding.
     */
    @Test
    public void testIfElseConditionForGroovyScriptInvokeWithBinding() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final TestEntity testEntity1 = createChildTestEntity(testCase);
        testEntity1.setTestValue("5");
        // Create the Bindings
        final Map<String, Object> bindingsMap = new HashMap<String, Object>();
        bindingsMap.put("testEntity", testEntity1);
        
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY,
                "boolean myMethod() { return (testEntity.getTestValue().toInteger() < 10); }", null, "myMethod", null, bindingsMap);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity3 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is never called.
        assertThat(testEntity3.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity3.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity3.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> testEntity1
     *     -> IfElseCondition
     *         -> testEntity2
     *         -> testEntity3
     * Test that the ifElseCondition entity runs only the second child entity if the condition is false.
     * Script is passed as a function in a variable. The function is invoked.
     * testEntity1 is used as a binding.
     */
    @Test
    public void testIfElseConditionForGroovyScriptFalseInvokeWithBinding() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final TestEntity testEntity1 = createChildTestEntity(testCase);
        testEntity1.setTestValue("5");
        // Create the Bindings
        final Map<String, Object> bindingsMap = new HashMap<String, Object>();
        bindingsMap.put("testEntity", testEntity1);
        
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY,
                "boolean myMethod() { return (testEntity.getTestValue().toInteger() > 10); }", null, "myMethod", null, bindingsMap);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity3 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is never called.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is called once.
        assertThat(testEntity3.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity3.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity3.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> IfElseCondition
     *         -> testEntity1
     *         -> testEntity2
     * Test that the ifElseCondition entity runs only the first child entity if the condition is true.
     * Script is passed as an function in a variable. The function is invoked and takes
     * arguments.
     * No bindings are used.
     */
    @Test
    public void testIfElseConditionForGroovyTrueInvokeUsingArguments() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        
        //Create the Arguments List.
        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add("5");
        argumentsList.add("10");
        
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY,
                "boolean myMethod(arg0, arg1) { return (arg0 < arg1); }", null, "myMethod", argumentsList, null);
        final TestEntity testEntity1 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is never called.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> IfElseCondition
     *         -> testEntity1
     *         -> testEntity2
     * Test that the ifElseCondition entity runs only the second child entity if the condition is false.
     * Script is passed as an function in a variable. The function is invoked and takes
     * arguments.
     * No bindings are used.
     */
    @Test
    public void testIfElseConditionForGroovyFalseInvokeUsingArguments() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        
        //Create the Arguments List.
        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add("10");
        argumentsList.add("5");
        
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY,
                "boolean myMethod(arg0, arg1) { return (arg0 < arg1); }", null, "myMethod", argumentsList, null);
        final TestEntity testEntity1 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is never called.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is called once.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> testEntity1
     *     -> IfElseCondition
     *         -> testEntity2
     *         -> testEntity3
     * Test that the ifElseCondition entity runs only the first child entity if the condition is true.
     * Script is passed as an function in a variable. The function is invoked and takes
     * arguments.
     * testEntity1 is used as a binding.
     */
    @Test
    public void testIfElseConditionForGroovyTrueInvokeUsingArgumentsAndBindings() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        
        final TestEntity testEntity1 = createChildTestEntity(testCase);
        testEntity1.setTestValue("5");
        // Create the Bindings
        final Map<String, Object> bindingsMap = new HashMap<String, Object>();
        bindingsMap.put("testEntity", testEntity1);
        
        //Create the Arguments List.
        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add("10");
        
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY,
                "boolean myMethod(arg0) { return (testEntity.getTestValue().toInteger() < arg0); }", null, "myMethod",
                argumentsList, bindingsMap);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity3 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is never called.
        assertThat(testEntity3.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity3.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity3.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> testEntity1
     *     -> IfElseCondition
     *         -> testEntity2
     *         -> testEntity3
     * Test that the ifElseCondition entity runs only the second child entity if the condition is false.
     * Script is passed as an function in a variable. The function is invoked and takes
     * arguments.
     * testEntity1 is used as a binding.
     */
    @Test
    public void testIfElseConditionForGroovyFalseInvokeUsingArgumentsAndBindings() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        
        final TestEntity testEntity1 = createChildTestEntity(testCase);
        testEntity1.setTestValue("5");
        // Create the Bindings
        final Map<String, Object> bindingsMap = new HashMap<String, Object>();
        bindingsMap.put("testEntity", testEntity1);
        
        //Create the Arguments List.
        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add("10");
        
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY,
                "boolean myMethod(arg0) { return (testEntity.getTestValue().toInteger() > arg0); }", null, "myMethod",
                argumentsList, bindingsMap);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity3 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is never called.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is called once.
        assertThat(testEntity3.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity3.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity3.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> IfElseCondition
     *         -> testEntity1
     *         -> testEntity2
     * Test that the ifElseCondition entity runs only the first child entity if the condition is true.
     * Script is passed as an function in a variable. The function is invoked and takes
     * arguments, one of which is itself an expression.
     * No bindings are used.
     */
    @Test
    public void testIfElseConditionForGroovyTrueInvokeUsingExpressionArguments() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        
        //Create the Arguments List.
        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add("(10 - 1)"); // This expression should get evaluated.
        argumentsList.add("10");
        
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY,
                "boolean myMethod(arg0, arg1) { return (arg0 < arg1); }", null, "myMethod", argumentsList, null);
        final TestEntity testEntity1 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is never called.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> IfElseCondition
     *         -> testEntity1
     *         -> testEntity2
     * Test that the ifElseCondition entity runs only the second child entity if the condition is false.
     * Script is passed as an function in a variable. The function is invoked and takes
     * arguments, one of which is itself an expression.
     * No bindings are used.
     */
    @Test
    public void testIfElseConditionForGroovyFalseInvokeUsingExpressionArguments() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        
        //Create the Arguments List.
        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add("(10 - 1)"); // This expression should get evaluated.
        argumentsList.add("10");
        
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY,
                "boolean myMethod(arg0, arg1) { return (arg0 > arg1); }", null, "myMethod", argumentsList, null);
        final TestEntity testEntity1 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is not called.
        assertThat(testEntity1.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity1.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity1.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is called once.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> testEntity1
     *     -> IfElseCondition
     *         -> testEntity2
     *         -> testEntity3
     * Test that the ifElseCondition entity runs only the first child entity if the condition is true.
     * Script is passed as a method in a variable. The method is invoked and takes
     * arguments, one of which is itself an expression.
     * testEntity1 is used as a binding.
     */
    @Test
    public void testIfElseConditionForGroovyTrueInvokeUsingExpressionArgumentsAndBindings() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        
        final TestEntity testEntity1 = createChildTestEntity(testCase);
        testEntity1.setTestValue("20");
        // Create the Bindings
        final Map<String, Object> bindingsMap = new HashMap<String, Object>();
        bindingsMap.put("testEntity", testEntity1);
        
        //Create the Arguments List.
        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add("(10 - 1)"); // This expression should get evaluated.
        argumentsList.add("10");
        
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY,
                "boolean myMethod(arg0, arg1) { return (arg0 + arg1 < testEntity.getTestValue().toInteger()); }", null,
                "myMethod", argumentsList, bindingsMap);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity3 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is never called.
        assertThat(testEntity3.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity3.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity3.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> testEntity1
     *     -> IfElseCondition
     *         -> testEntity2
     *         -> testEntity3
     * Test that the ifElseCondition entity runs only the second child entity if the condition is false.
     * Script is passed as a method in a variable. The method is invoked and takes
     * arguments, one of which is itself an expression.
     * testEntity1 is used as a binding.
     */
    @Test
    public void testIfElseConditionForGroovyFalseInvokeUsingExpressionArgumentsAndBindings() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        
        final TestEntity testEntity1 = createChildTestEntity(testCase);
        testEntity1.setTestValue("20");
        // Create the Bindings
        final Map<String, Object> bindingsMap = new HashMap<String, Object>();
        bindingsMap.put("testEntity", testEntity1);
        
        //Create the Arguments List.
        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add("(10 - 1)"); // This expression should get evaluated.
        argumentsList.add("10");
        
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY,
                "boolean myMethod(arg0, arg1) { return (arg0 + arg1 > testEntity.getTestValue().toInteger()); }", null,
                "myMethod", argumentsList, bindingsMap);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity3 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is not called.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is called once.
        assertThat(testEntity3.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity3.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity3.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> testEntity1
     *     -> IfElseCondition
     *         -> testEntity2
     *         -> testEntity3
     * Test that the ifElseCondition entity runs only the first child entity if the condition is true.
     * Script is passed as a file containing the script, which is a method.
     * The method is invoked and takes arguments, one of which is itself an expression.
     * testEntity1 is used as a binding.
     */
    @Test
    public void testIfElseConditionForGroovyTrueInvokeUsingFileWithExpressionArgumentsAndBindings() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        
        final TestEntity testEntity1 = createChildTestEntity(testCase);
        testEntity1.setTestValue("20");
        // Create the Bindings
        final Map<String, Object> bindingsMap = new HashMap<String, Object>();
        bindingsMap.put("testEntity", testEntity1);
        
        final String scriptString = "boolean myMethod(arg0, arg1) { return (arg0 + arg1 < testEntity.getTestValue().toInteger()); }";
        
        // Create the temporary script file and get the URL to it.
        final File file = createTempScriptFile(scriptString);
        URL fileUrl = null;
        try {
            fileUrl = file.toURI().toURL();
        } catch (MalformedURLException e) {
            fail("Could not locate temporary script file.", e);
        }
        
        //Create the Arguments List.
        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add("(10 - 1)"); // This expression should get evaluated.
        argumentsList.add("10");
        
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY,
                null, fileUrl, "myMethod", argumentsList, bindingsMap);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity3 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is never called.
        assertThat(testEntity3.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity3.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity3.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> testEntity1
     *     -> IfElseCondition
     *         -> testEntity2
     *         -> testEntity3
     * Test that the ifElseCondition entity runs only the second child entity if the condition is false.
     * Script is passed as a file containing the script, which is a method.
     * The method is invoked and takes arguments, one of which is itself an expression.
     * testEntity1 is used as a binding.
     */
    @Test
    public void testIfElseConditionForGroovyFalseInvokeUsingFileWithExpressionArgumentsAndBindings() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        
        final TestEntity testEntity1 = createChildTestEntity(testCase);
        testEntity1.setTestValue("20");
        // Create the Bindings
        final Map<String, Object> bindingsMap = new HashMap<String, Object>();
        bindingsMap.put("testEntity", testEntity1);
        
        final String scriptString = "boolean myMethod(arg0, arg1) { return (arg0 + arg1 > testEntity.getTestValue().toInteger()); }";
        
        // Create the temporary script file and get the URL to it.
        final File file = createTempScriptFile(scriptString);
        URL fileUrl = null;
        try {
            fileUrl = file.toURI().toURL();
        } catch (MalformedURLException e) {
            fail("Could not locate temporary script file.", e);
        }
        
        //Create the Arguments List.
        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add("(10 - 1)"); // This expression should get evaluated.
        argumentsList.add("10");
        
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY,
                null, fileUrl, "myMethod", argumentsList, bindingsMap);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity3 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is never called.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is called once.
        assertThat(testEntity3.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity3.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity3.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> testEntity1
     *     -> IfElseCondition
     *         -> testEntity2
     *         -> testEntity3
     * Test that the ifElseCondition entity runs only the first child entity if the condition is true.
     * Script is passed as both a file containing the script, which is a method, and also
     * as a script variable String. The file should take precedence.
     * The method is invoked and takes arguments, one of which is itself an expression.
     * testEntity1 is used as a binding.
     */
    @Test
    public void testIfElseConditionForGroovyTrueInvokeUsingFileAndScriptWithExpressionArgumentsAndBindings() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        
        final TestEntity testEntity1 = createChildTestEntity(testCase);
        testEntity1.setTestValue("20");
        // Create the Bindings
        final Map<String, Object> bindingsMap = new HashMap<String, Object>();
        bindingsMap.put("testEntity", testEntity1);
        
        // This one should get used in the evaluation.
        final String scriptStringTrue = "boolean myMethod(arg0, arg1) { return (arg0 + arg1 < testEntity.getTestValue().toInteger()); }";
        // This one should get ignored in the evaluation.
        final String scriptStringFalse = "boolean myMethod(arg0, arg1) { return (arg0 + arg1 >= testEntity.getTestValue().toInteger()); }";
        
        // Create the temporary script file and get the URL to it.
        final File file = createTempScriptFile(scriptStringTrue);
        URL fileUrl = null;
        try {
            fileUrl = file.toURI().toURL();
        } catch (MalformedURLException e) {
            fail("Could not locate temporary script file.", e);
        }
        
        //Create the Arguments List.
        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add("(10 - 1)"); // This expression should get evaluated.
        argumentsList.add("10");
        
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY,
                scriptStringFalse, fileUrl, "myMethod", argumentsList, bindingsMap);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity3 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is called once.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is never called.
        assertThat(testEntity3.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity3.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity3.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     -> testEntity1
     *     -> IfElseCondition
     *         -> testEntity2
     *         -> testEntity3
     * Test that the ifElseCondition entity runs only the second child entity if the condition is false.
     * Script is passed as both a file containing the script, which is a method, and also
     * as a script variable String. The file should take precedence.
     * The method is invoked and takes arguments, one of which is itself an expression.
     * testEntity1 is used as a binding.
     */
    @Test
    public void testIfElseConditionForGroovyFalseInvokeUsingFileAndScriptWithExpressionArgumentsAndBindings() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        
        final TestEntity testEntity1 = createChildTestEntity(testCase);
        testEntity1.setTestValue("20");
        // Create the Bindings
        final Map<String, Object> bindingsMap = new HashMap<String, Object>();
        bindingsMap.put("testEntity", testEntity1);
        
        // This one should get ignored in the evaluation.
        final String scriptStringTrue = "boolean myMethod(arg0, arg1) { return (arg0 + arg1 < testEntity.getTestValue().toInteger()); }";
        // This one should get used in the evaluation.
        final String scriptStringFalse = "boolean myMethod(arg0, arg1) { return (arg0 + arg1 >= testEntity.getTestValue().toInteger()); }";
        
        // Create the temporary script file and get the URL to it.
        final File file = createTempScriptFile(scriptStringFalse);
        URL fileUrl = null;
        try {
            fileUrl = file.toURI().toURL();
        } catch (MalformedURLException e) {
            fail("Could not locate temporary script file.", e);
        }
        
        //Create the Arguments List.
        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add("(10 - 1)"); // This expression should get evaluated.
        argumentsList.add("10");
        
        final IfElseCondition ifElseConditionEntity = createChildIfElseConditionEntity(testCase, LANGUAGE_GROOVY,
                scriptStringTrue, fileUrl, "myMethod", argumentsList, bindingsMap);
        final TestEntity testEntity2 = createChildTestEntity(ifElseConditionEntity);
        final TestEntity testEntity3 = createChildTestEntity(ifElseConditionEntity);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));

        // Start is never called.
        assertThat(testEntity2.sensors().get(TestEntity.START_COUNT)).isEqualTo(0);
        // Stop is never called.
        assertThat(testEntity2.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity2.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
        
        // Start is called once.
        assertThat(testEntity3.sensors().get(TestEntity.START_COUNT)).isEqualTo(1);
        // Stop is never called.
        assertThat(testEntity3.sensors().get(TestEntity.STOP_COUNT)).isEqualTo(0);
        // Restart is never called.
        assertThat(testEntity3.sensors().get(TestEntity.RESTART_COUNT)).isEqualTo(0);
    }

}
