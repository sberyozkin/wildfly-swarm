/**
 * Copyright 2015-2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.swarm.keycloak.mpjwt.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.wildfly.swarm.bootstrap.util.BootstrapProperties;
import org.wildfly.swarm.spi.api.DeploymentProcessor;
import org.wildfly.swarm.spi.api.JARArchive;
import org.wildfly.swarm.spi.runtime.annotations.DeploymentScoped;

@DeploymentScoped
public class KeycloakMicroprofileJwtArchivePreparer implements DeploymentProcessor {

    private static final Logger log = Logger.getLogger(KeycloakMicroprofileJwtArchivePreparer.class);

    @Inject
    public KeycloakMicroprofileJwtArchivePreparer(Archive archive) {
    }

    @Override
    public void process() throws IOException {
        InputStream keycloakJsonStream = getKeycloakJsonFromClasspath("keycloak.json");
        if (keycloakJsonStream != null) {
            try {
                Module module = Module.getBootModuleLoader().loadModule("org.wildfly.swarm.keycloak.mpjwt:deployment");
                Class<?> use = module.getClassLoader()
                    .loadClass("org.wildfly.swarm.keycloak.mpjwt.deployment.KeycloakJWTCallerPrincipalFactory");
                Method m = use.getDeclaredMethod("createDeploymentFromStream", InputStream.class);
                m.invoke(null, keycloakJsonStream);
            } catch (Throwable ex) {
                log.warn("keycloak.json resource is not available", ex);
            }
        }
    }

    private static InputStream getKeycloakJsonFromClasspath(String resourceName) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream keycloakJson = cl.getResourceAsStream(resourceName);
        if (keycloakJson == null) {

            String appArtifact = System.getProperty(BootstrapProperties.APP_ARTIFACT);

            if (appArtifact != null) {
                try (InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream("_bootstrap/" + appArtifact)) {
                    Archive<?> tmpArchive = ShrinkWrap.create(JARArchive.class);
                    tmpArchive.as(ZipImporter.class).importFrom(in);
                    Node jsonNode = tmpArchive.get(resourceName);
                    if (jsonNode == null) {
                        jsonNode = getKeycloakJsonNodeFromWebInf(tmpArchive, resourceName, true);
                    }
                    if (jsonNode == null) {
                        jsonNode = getKeycloakJsonNodeFromWebInf(tmpArchive, resourceName, false);
                    }
                    if (jsonNode != null && jsonNode.getAsset() != null) {
                        keycloakJson = jsonNode.getAsset().openStream();
                    }
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return keycloakJson;
    }

    private static Node getKeycloakJsonNodeFromWebInf(Archive<?> tmpArchive, String resourceName,
            boolean useForwardSlash) {
        String webInfPath = useForwardSlash ? "/WEB-INF" : "WEB-INF";
        if (!resourceName.startsWith("/")) {
            resourceName = "/" + resourceName;
        }
        Node jsonNode = tmpArchive.get(webInfPath + resourceName);
        if (jsonNode == null) {
            jsonNode = tmpArchive.get(webInfPath + "/classes" + resourceName);
        }
        return jsonNode;
    }

}
