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

import org.apache.brooklyn.api.entity.ImplementedBy;

/**
 * Entity that contains two children.
 * It will run its first child if a particular condition is met, otherwise it will run its second child.
 * Any other children are ignored.
 * 
 * This condition can be an expression or script in one of a number of
 * JSR-223 compliant scripting languages.
 *
 * @author Chris Burke
 */
@ImplementedBy(value = IfElseConditionImpl.class)
public interface IfElseCondition extends IfCondition {
}
