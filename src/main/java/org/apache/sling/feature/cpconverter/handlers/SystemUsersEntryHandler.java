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
package org.apache.sling.feature.cpconverter.handlers;

import java.io.InputStream;

import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.Archive.Entry;
import org.apache.sling.feature.cpconverter.ContentPackage2FeatureModelConverter;
import org.apache.sling.feature.cpconverter.shared.AbstractJcrNodeParser;
import org.xml.sax.Attributes;

public final class SystemUsersEntryHandler extends AbstractRegexEntryHandler {

    public SystemUsersEntryHandler() {
        super("/jcr_root/home/users/.*/\\.content.xml");
    }

    @Override
    public void handle(String path, Archive archive, Entry entry, ContentPackage2FeatureModelConverter converter)
            throws Exception {
        SystemUserParser systemUserParser = new SystemUserParser(converter);
        try (InputStream input = archive.openInputStream(entry)) {
            systemUserParser.parse(input);
        }
    }

    private static final class SystemUserParser extends AbstractJcrNodeParser<Void> {

        private final static String REP_SYSTEM_USER = "rep:SystemUser";

        private final static String REP_AUTHORIZABLE_ID = "rep:authorizableId";

        private final ContentPackage2FeatureModelConverter converter;

        public SystemUserParser(ContentPackage2FeatureModelConverter converter) {
            super(REP_SYSTEM_USER);
            this.converter = converter;
        }

        @Override
        protected void onJcrRootElement(String uri, String localName, String qName, Attributes attributes) {
            String authorizableId = attributes.getValue(REP_AUTHORIZABLE_ID);
            if (authorizableId != null && !authorizableId.isEmpty()) {
                converter.getAclManager().addSystemUser(authorizableId);
            }
        }

        @Override
        protected Void getParsingResult() {
            return null;
        }

    }

}
