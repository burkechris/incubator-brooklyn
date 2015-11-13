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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.entity.ImplementedBy;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.entity.trait.Startable;
import org.apache.brooklyn.util.core.flags.SetFromFlag;

import com.google.common.collect.Maps;

/**
 * Entity that runs an arbitrary script or expression written in one of a number of
 * JSR-223 compliant scripting languages.
 * E.g. AppleScript, groovy, javascript, python.
 * 
 * Other entities may be bound and used in the script or expression.
 * 
 * A named function may be invoked on the script. This function may have arguments provided.
 * The arguments will also be evaluated before being passed to the function so these may
 * also be expressions if desired. These expressions must be written in the same language
 * as the script itself.
 * 
 * Note that the specification states that script engines are not required to support
 * the Invocable interface.
 * The Nashorn (javascript) engine included in Java does but, before writing code
 * that invokes functions, you should verify that the engine for the language in which
 * your script is written supports the Invocable interface.
 * This means that invoking a function on a script is entirely dependent on the language
 * engine used and, in these cases, you may be limited to using expressions only.
 * 
 * A file may be specified in place of the script. Only one or other should be specified.
 * In the case of both being provided, the file will take precedence.
 * This should be specified in the form of a URL.
 *
 * @author Chris Burke
 */
@ImplementedBy(value = RunScriptImpl.class)
public interface RunScript extends Entity, Startable {

    ConfigKey<String> SCRIPT = ConfigKeys.newStringConfigKey("script", "The script or expression to run.");

    ConfigKey<URL> FILE = ConfigKeys.newConfigKey(URL.class, "script.url",
            "The location of the script or expression to run. This overrides the script variable.");

    @SetFromFlag(nullable = false)
    ConfigKey<String> LANGUAGE = ConfigKeys.newStringConfigKey("language",
            "The scripting language used by the expression or script.");

    ConfigKey<String> INVOKE_FUNCTION = ConfigKeys.newStringConfigKey("invoke",
            "The function to invoke on the script.");

    @SuppressWarnings("rawtypes")
    ConfigKey<List> ARGUMENTS = ConfigKeys.newConfigKey(List.class, "args",
            "Arguments to be used when invoking a function.", new ArrayList<Object>());

    @SuppressWarnings("rawtypes")
    ConfigKey<Map> BINDINGS = ConfigKeys.newConfigKey(Map.class, "bindings", "Bindings to be used in the expression.",
            Maps.newLinkedHashMap());
}
