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
 * Tests for the RunScript entity.
 * 
 * @author Chris Burke
 */
public class RunScriptTest {

    private static final String LANGUAGE_GROOVY = "groovy";
    private static final String LANGUAGE_JAVASCRIPT = "javascript";
    private static final String LANGUAGE_NON_EXISTENT = "klingon";
    
    private static final String RESULT = "RESULT";
    private static final String ARGUMENT_1 = "ONE";
    private static final String ARGUMENT_2 = "NIL";
    
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
     * Creates a RunScript entity as a child of the specified parent entity.
     * 
     * @param parentEntity The parent entity for the child test entity.
     * @param language The language in which the script has been written.
     * @param script The actual script to run.
     * @param file The location of the script file. Overrides script variable.
     * @param invokeFunction The function to invoke on the script. Optional.
     * @param arguments Arguments to pass to the function if required.
     * @param bindings Entity bindings used by the script.
     * @return The child RunScript entity.
     */
    private RunScript createChildRunScriptEntity(final Entity parentEntity,
                                                 final String language,
                                                 final String script,
                                                 final URL file,
                                                 final String invokeFunction,
                                                 final List<String> arguments,
                                                 final Map<String, Object> bindings) {
        final List<String> args = arguments == null ? new ArrayList<String>() : arguments;
        final Map<String, Object> bindingsMap = bindings == null ? new LinkedHashMap<String, Object>() : bindings;
        final RunScript runScriptEntity = parentEntity.addChild(EntitySpec.create(RunScript.class)
                .configure(RunScript.LANGUAGE, language)
                .configure(RunScript.SCRIPT, script)
                .configure(RunScript.FILE, file)
                .configure(RunScript.INVOKE_FUNCTION, invokeFunction)
                .configure(RunScript.ARGUMENTS, args)
                .configure(RunScript.BINDINGS, bindingsMap));
        return runScriptEntity;   
    }
    
    /**
     * Tests that an exception is thrown when requesting a language for which there is no ScriptEngine.
     */
    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testWhenNonExistentLanguage() {
        final String expectedExceptionMessage = "Cannot find the ScriptEngine for [" + LANGUAGE_NON_EXISTENT + "].";
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));

        // Create the RunScript entity.
        createChildRunScriptEntity(testCase, LANGUAGE_NON_EXISTENT, null, null, null, null, null);

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
     *     --> testEntity1
     *     --> runScript
     * Test that the runScript entity runs the script.
     * Script is passed as an expression in a variable. testEntity1 is used as a Binding.
     */
    @Test
    public void testRunScriptForJavascriptExpression() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        TestEntity testEntity1 = createChildTestEntity(testCase);
        // Create the Bindings
        final Map<String, Object> bindingsMap = new HashMap<String, Object>();
        bindingsMap.put("testEntity", testEntity1);
        
        //Create the Script String.
        final String scriptString = "if (Boolean(10 > 9)) { testEntity.setTestValue('" + RESULT + "'); }";
        
        //Create the RunScript entity.
        createChildRunScriptEntity(testCase, LANGUAGE_JAVASCRIPT, scriptString, null, null, null, bindingsMap);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));
        
        // Check that the script worked.
        assertThat(testEntity1.getTestValue()).hasToString(RESULT);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     --> testEntity1
     *     --> runScript
     * Test that the runScript entity runs the script.
     * Script is passed as an expression in a variable. testEntity1 is used as a Binding.
     */
    @Test
    public void testRunScriptForGroovyExpression() {
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        TestEntity testEntity1 = createChildTestEntity(testCase);
        // Create the Bindings
        final Map<String, Object> bindingsMap = new HashMap<String, Object>();
        bindingsMap.put("testEntity", testEntity1);
        
        //Create the Script String.
        final String scriptString = "if (10 > 9) { testEntity.setTestValue(\"" + RESULT + "\"); }";
        
        //Create the RunScript entity.
        createChildRunScriptEntity(testCase, LANGUAGE_GROOVY, scriptString, null, null, null, bindingsMap);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));
        
        // Check that the script worked.
        assertThat(testEntity1.getTestValue()).hasToString(RESULT);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     --> testEntity1
     *     --> runScript
     * Test that the runScript entity runs the script.
     * Script is passed as an expression in a variable. testEntity1 is used as a Binding.
     * Script uses an invoked function which contains arguments.
     */
    @Test
    public void testRunScriptForJavascriptScriptFunctionWithArguments() {
        final String expectedResultString = RESULT + " - " + ARGUMENT_1 + ":" + ARGUMENT_2;
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        TestEntity testEntity1 = createChildTestEntity(testCase);
        // Create the Bindings
        final Map<String, Object> bindingsMap = new HashMap<String, Object>();
        bindingsMap.put("testEntity", testEntity1);
        
        //Create the Script String.
        final String scriptString = "function myFunction(arg0, arg1) { if (Boolean(10 > 9)) { testEntity.setTestValue('"
                + RESULT + " - ' + arg0 + ':' + arg1); } }";

        //Create the Arguments List.
        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add(ARGUMENT_1);
        argumentsList.add(ARGUMENT_2);
        
        //Create the RunScript entity.
        createChildRunScriptEntity(testCase, LANGUAGE_JAVASCRIPT, scriptString, null, "myFunction", argumentsList, bindingsMap);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));
        
        // Check that the script worked.
        assertThat(testEntity1.getTestValue()).hasToString(expectedResultString);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     --> testEntity1
     *     --> runScript
     * Test that the runScript entity runs the script.
     * Script is passed as an expression in a variable. testEntity1 is used as a Binding.
     * Script uses an invoked function which contains arguments. One of the arguments is
     * itself an expression which should get evaluated before being passed to the invoked function.
     */
    @Test
    public void testRunScriptForJavascriptScriptFunctionWithExpressionArgument() {
        final String expectedResultString = RESULT + " - " + ARGUMENT_1 + ":9";
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        TestEntity testEntity1 = createChildTestEntity(testCase);
        // Create the Bindings
        final Map<String, Object> bindingsMap = new HashMap<String, Object>();
        bindingsMap.put("testEntity", testEntity1);
        
        //Create the Script String.
        final String scriptString = "function myFunction(arg0, arg1) { if (Boolean(10 > 9)) { testEntity.setTestValue('"
                + RESULT + " - ' + arg0 + ':' + arg1); } }";

        //Create the Arguments List.
        final String expressionArgument = "(8 + 1)";
        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add(ARGUMENT_1);
        argumentsList.add(expressionArgument);
        
        //Create the RunScript entity.
        createChildRunScriptEntity(testCase, LANGUAGE_JAVASCRIPT, scriptString, null, "myFunction", argumentsList, bindingsMap);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));
        
        // Check that the script worked.
        assertThat(testEntity1.getTestValue()).hasToString(expectedResultString);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     --> testEntity1
     *     --> runScript
     * Test that the runScript entity runs the script.
     * Script is passed in as a location to file which contains the script. testEntity1 is used as a Binding.
     * Script uses an invoked function which contains arguments.
     */
    @Test
    public void testRunScriptForJavascriptScriptFileFunctionWithArguments() {
        final String expectedResultString = RESULT + " - " + ARGUMENT_1 + ":" + ARGUMENT_2;
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        TestEntity testEntity1 = createChildTestEntity(testCase);
        // Create the Bindings
        final Map<String, Object> bindingsMap = new HashMap<String, Object>();
        bindingsMap.put("testEntity", testEntity1);
        
        final String scriptString = "function myFunction(arg0, arg1) { if (Boolean(10 > 9)) { testEntity.setTestValue('"
                + RESULT + " - ' + arg0 + ':' + arg1); } }";
        
        final File file = createTempScriptFile(scriptString);

        //Create the Arguments List.
        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add(ARGUMENT_1);
        argumentsList.add(ARGUMENT_2);
        
        // Create the RunScript entity.
        URL fileUrl = null;
        try {
            fileUrl = file.toURI().toURL();
        } catch (MalformedURLException e) {
            fail("Could not locate temporary script file.", e);
        }
        createChildRunScriptEntity(testCase, LANGUAGE_JAVASCRIPT, null, fileUrl, "myFunction", argumentsList, bindingsMap);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));
        
        // Check that the script worked.
        assertThat(testEntity1.getTestValue()).hasToString(expectedResultString);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     --> runScript
     * Script is passed in as a location to file which contains the script.
     * Test that an exception is thrown when the file location does not point to a file.
     */
    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testRunScriptForScriptFileNotFound() {
        final String fileLocation = "/tmp/idonotexist.script";
        final String expectedExceptionMessage = "FileNotFoundException: " + fileLocation + " (No such file or directory)";
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final File file = new File(fileLocation);

        // Create the RunScript entity.
        URL fileUrl = null;
        try {
            fileUrl = file.toURI().toURL();
        } catch (MalformedURLException e) {
            fail("Could not locate temporary script file.", e);
        }
        createChildRunScriptEntity(testCase, LANGUAGE_JAVASCRIPT, null, fileUrl, null, null, null);
        
        try {
            app.start(ImmutableList.of(app.newSimulatedLocation()));
        } catch (Exception e) {
            assertThat(e.getCause().getMessage()).contains(expectedExceptionMessage);
            throw e;
        }
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
     *     --> testEntity1
     *     --> runScript
     * Test that the runScript entity runs the script.
     * Script is passed as an expression in a variable. testEntity1 is used as a Binding.
     * Script uses an invoked function which contains arguments.
     */
    @Test
    public void testRunScriptForGroovyScriptMethodWithArguments() {
        final String expectedResultString = RESULT + " - " + ARGUMENT_1 + ":" + ARGUMENT_2;
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        final TestEntity testEntity1 = createChildTestEntity(testCase);
        // Create the Bindings
        final Map<String, Object> bindingsMap = new HashMap<String, Object>();
        bindingsMap.put("testEntity", testEntity1);
        
        //Create the Script String.
        final String scriptString = "void myMethod(arg0, arg1) { if (10 > 9) { testEntity.setTestValue('"
                + RESULT + " - ' + arg0 + ':' + arg1); } }";

        //Create the Arguments List.
        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add(ARGUMENT_1);
        argumentsList.add(ARGUMENT_2);
        
        //Create the RunScript entity.
        createChildRunScriptEntity(testCase, LANGUAGE_GROOVY, scriptString, null, "myMethod", argumentsList, bindingsMap);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));
        
        // Check that the script worked.
        assertThat(testEntity1.getTestValue()).hasToString(expectedResultString);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     --> testEntity1
     *     --> runScript
     * Test that the runScript entity runs the script.
     * Script is passed as an expression in a variable. testEntity1 is used as a Binding.
     * Script uses an invoked function which contains arguments. One of the arguments is
     * itself an expression which should get evaluated before being passed to the invoked function.
     */
    @Test
    public void testRunScriptForGroovyScriptFunctionWithExpressionArgument() {
        final String expectedResultString = RESULT + " - " + ARGUMENT_1 + ":9";
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        TestEntity testEntity1 = createChildTestEntity(testCase);
        // Create the Bindings
        final Map<String, Object> bindingsMap = new HashMap<String, Object>();
        bindingsMap.put("testEntity", testEntity1);
        
        //Create the Script String.
        final String scriptString = "void myMethod(arg0, arg1) { if (10 > 9) { testEntity.setTestValue('"
                + RESULT + " - ' + arg0 + ':' + arg1); } }";

        //Create the Arguments List.
        final String expressionArgument = "(8 + 1)";
        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add(ARGUMENT_1);
        argumentsList.add(expressionArgument);
        
        //Create the RunScript entity.
        createChildRunScriptEntity(testCase, LANGUAGE_GROOVY, scriptString, null, "myMethod", argumentsList, bindingsMap);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));
        
        // Check that the script worked.
        assertThat(testEntity1.getTestValue()).hasToString(expectedResultString);
    }
    
    /**
     * We'll have test entities like:
     * testCase
     *     --> testEntity1
     *     --> runScript
     * Test that the runScript entity runs the script.
     * Script is passed in as a location to file which contains the script. testEntity1 is used as a Binding.
     * Script uses an invoked function which contains arguments.
     */
    @Test
    public void testRunScriptForGroovyScriptFileMethodWithArguments() {
        final String expectedResultString = RESULT + " - " + ARGUMENT_1 + ":" + ARGUMENT_2;
        final TestCase testCase = app.createAndManageChild(EntitySpec.create(TestCase.class));
        TestEntity testEntity1 = createChildTestEntity(testCase);
        // Create the Bindings
        final Map<String, Object> bindingsMap = new HashMap<String, Object>();
        bindingsMap.put("testEntity", testEntity1);
        
        final String scriptString = "void myMethod(arg0, arg1) { if (10 > 9) { testEntity.setTestValue('"
                + RESULT + " - ' + arg0 + ':' + arg1); } }";
        
        //Create the Script File.
        final File file = createTempScriptFile(scriptString);

        //Create the Arguments List.
        List<String> argumentsList = new ArrayList<String>();
        argumentsList.add(ARGUMENT_1);
        argumentsList.add(ARGUMENT_2);
        
        // Create the RunScript entity.
        URL fileUrl = null;
        try {
            fileUrl = file.toURI().toURL();
        } catch (MalformedURLException e) {
            fail("Could not locate temporary script file.", e);
        }
        createChildRunScriptEntity(testCase, LANGUAGE_GROOVY, null, fileUrl, "myMethod", argumentsList, bindingsMap);
        
        app.start(ImmutableList.of(app.newSimulatedLocation()));
        
        // Check that the script worked.
        assertThat(testEntity1.getTestValue()).hasToString(expectedResultString);
    }

}
