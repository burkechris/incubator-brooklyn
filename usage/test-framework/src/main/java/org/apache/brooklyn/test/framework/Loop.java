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

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.entity.ImplementedBy;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.entity.trait.Startable;
import org.apache.brooklyn.util.core.flags.SetFromFlag;

/**
 * Entity that loops through its children the specified number of times.
 * Exceptions may be ignored so that looping continues if children fail.
 *
 * @author Chris Burke
 */
@ImplementedBy(value = LoopImpl.class)
public interface Loop extends Entity, Startable {

    ConfigKey<Boolean> IGNORE_EXCEPTIONS = ConfigKeys.newBooleanConfigKey("ignore.exceptions",
            "If false, an exception whilst looping will abort the loop. Otherwise the exception will be ignored and looping will continue.",
            Boolean.FALSE);

    @SetFromFlag(nullable = false)
    ConfigKey<Integer> LOOP_COUNT = ConfigKeys.newIntegerConfigKey("count",
            "Number of times to loop through the children.");

}
