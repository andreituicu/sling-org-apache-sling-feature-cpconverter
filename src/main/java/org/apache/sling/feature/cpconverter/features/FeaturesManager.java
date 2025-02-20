/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature.cpconverter.features;

import java.util.Dictionary;

import org.apache.sling.feature.Feature;

public interface FeaturesManager {

    void init(String groupId, String artifactId, String version);

    Feature getTargetFeature();

    Feature getRunMode(String runMode);

    void addArtifact(String runMode,
                     String groupId,
                     String artifactId,
                     String version,
                     String classifier,
                     String type);

    void addConfiguration(String runMode, String pid, Dictionary<String, Object> configurationProperties);

    void serialize() throws Exception;

}
