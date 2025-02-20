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
package org.apache.sling.feature.cpconverter.acl;

import java.io.File;
import java.io.FileInputStream;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.cpconverter.vltpkg.VaultPackageAssembler;

public final class DefaultAclManager implements AclManager {

    private static final String CONTENT_XML_FILE_NAME = ".content.xml";

    private static final String DEFAULT_TYPE = "sling:Folder";

    private final Set<String> preProvidedSystemUsers = new LinkedHashSet<>();

    private final Set<String> preProvidedPaths = new HashSet<String>();

    private final Set<String> systemUsers = new LinkedHashSet<>();

    private final Map<String, List<Acl>> acls = new HashMap<>();

    private List<String> nodetypeRegistrationSentences = new LinkedList<>();

    private Set<String> privileges = new LinkedHashSet<>();

    public boolean addSystemUser(String systemUser) {
        if (systemUser != null && !systemUser.isEmpty() && preProvidedSystemUsers.add(systemUser)) {
            return systemUsers.add(systemUser);
        }
        return false;
    }

    public Acl addAcl(String systemUser, String operation, String privileges, String path) {
        Acl acl = new Acl(operation, privileges, path);
        acls.computeIfAbsent(systemUser, k -> new LinkedList<>()).add(acl);
        return acl;
    }

    private void addPath(String path, Set<String> paths) {
        if (preProvidedPaths.add(path)) {
            paths.add(path);
        }

        int endIndex = path.lastIndexOf('/');
        if (endIndex > 0) {
            addPath(path.substring(0, endIndex), paths);
        }
    }

    public void addRepoinitExtension(VaultPackageAssembler packageAssembler, Feature feature) {
        Formatter formatter = null;
        try {
            formatter = new Formatter();

            if (!privileges.isEmpty()) {
                for (String privilege : privileges) {
                    formatter.format("register privilege %s%n", privilege);
                }
            }

            if (!nodetypeRegistrationSentences.isEmpty()) {
                formatter.format("register nodetypes%n")
                         .format("<<===%n");

                for (String nodetypeRegistrationSentence : nodetypeRegistrationSentences) {
                    if (nodetypeRegistrationSentence.isEmpty()) {
                        formatter.format("%n");
                    } else {
                        formatter.format("<< %s%n", nodetypeRegistrationSentence);
                    }
                }

                formatter.format("===>>%n");
            }

            // system users

            for (String systemUser : systemUsers) {
                List<Acl> authorizations = acls.remove(systemUser);

                // make sure all paths are created first

                addPaths(authorizations, packageAssembler, formatter);

                // create then the users

                formatter.format("create service user %s%n", systemUser);

                // finally add ACLs

                addAclStatement(formatter, systemUser, authorizations);
            }

            // all the resting ACLs can now be set

            for (Entry<String, List<Acl>> currentAcls : acls.entrySet()) {
                String systemUser = currentAcls.getKey();

                if (preProvidedSystemUsers.contains(systemUser)) {
                    List<Acl> authorizations = currentAcls.getValue();

                    // make sure all paths are created first

                    addPaths(authorizations, packageAssembler, formatter);

                    // finally add ACLs

                    addAclStatement(formatter, systemUser, authorizations);
                }
            }

            String text = formatter.toString();

            if (!text.isEmpty()) {
                Extension repoInitExtension = new Extension(ExtensionType.TEXT, Extension.EXTENSION_NAME_REPOINIT, true);
                repoInitExtension.setText(text);
                feature.getExtensions().add(repoInitExtension);
            }
        } finally {
            if (formatter != null) {
                formatter.close();
            }
        }
    }

    @Override
    public void addNodetypeRegistrationSentence(String nodetypeRegistrationSentence) {
        if (nodetypeRegistrationSentence != null) {
            nodetypeRegistrationSentences.add(nodetypeRegistrationSentence);
        }
    }

    @Override
    public void addPrivilege(String privilege) {
        privileges.add(privilege);
    }

    public void reset() {
        systemUsers.clear();
        acls.clear();
        nodetypeRegistrationSentences.clear();
        privileges.clear();
    }

    private void addPaths(List<Acl> authorizations, VaultPackageAssembler packageAssembler, Formatter formatter) {
        if (areEmpty(authorizations)) {
            return;
        }

        Set<String> paths = new TreeSet<String>();
        for (Acl authorization : authorizations) {
            addPath(authorization.getPath(), paths);
        }

        for (String path : paths) {
            File currentDir = packageAssembler.getEntry(path);
            String type = DEFAULT_TYPE;

            if (currentDir.exists()) {
                File currentContent = new File(currentDir, CONTENT_XML_FILE_NAME);
                if (currentContent.exists()) {
                    try (FileInputStream input = new FileInputStream(currentContent)) {
                        type = new PrimaryTypeParser(DEFAULT_TYPE).parse(input);
                    } catch (Exception e) {
                        throw new RuntimeException("A fatal error occurred while parsing the '"
                            + currentContent
                            + "' file, see nested exceptions: "
                            + e);
                    }
                }
            }

            formatter.format("create path (%s) %s%n", type, path);
        }
    }

    private static void addAclStatement(Formatter formatter, String systemUser, List<Acl> authorizations) {
        if (areEmpty(authorizations)) {
            return;
        }

        formatter.format("set ACL for %s%n", systemUser);

        for (Acl authorization : authorizations) {
            formatter.format("%s %s on %s",
                             authorization.getOperation(),
                             authorization.getPrivileges(),
                             authorization.getPath());

            if (!authorization.getRestrictions().isEmpty()) {
                formatter.format(" restriction(%s)",
                                 authorization.getRestrictions().stream().collect(Collectors.joining(",")));
            }

            formatter.format("%n");
        }

        formatter.format("end%n");
    }

    private static boolean areEmpty(List<Acl> authorizations) {
        return authorizations == null || authorizations.isEmpty();
    }

}
