/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.internal.serialization.filter;

import static org.apache.commons.lang3.JavaVersion.JAVA_1_8;
import static org.apache.commons.lang3.JavaVersion.JAVA_9;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtMost;
import static org.apache.geode.distributed.ConfigurationProperties.HTTP_SERVICE_PORT;
import static org.apache.geode.distributed.ConfigurationProperties.JMX_MANAGER;
import static org.apache.geode.distributed.ConfigurationProperties.JMX_MANAGER_PORT;
import static org.apache.geode.distributed.ConfigurationProperties.JMX_MANAGER_START;
import static org.apache.geode.distributed.ConfigurationProperties.LOG_FILE;
import static org.apache.geode.internal.AvailablePortHelper.getRandomAvailableTCPPort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.rules.TemporaryFolder;

import org.apache.geode.cache.Cache;
import org.apache.geode.distributed.ServerLauncher;
import org.apache.geode.management.ManagementService;
import org.apache.geode.management.internal.SystemManagementService;
import org.apache.geode.test.junit.rules.CloseableReference;

public class ServerLauncherJmxSerialFilterIntegrationTest {

  private static final String NAME = "server";
  private static final String JMX_SERIAL_FILTER_PROPERTY =
      "jmx.remote.rmi.server.serial.filter.pattern";

  private File workingDirectory;
  private int jmxPort;
  private String openMBeanFilterPattern;

  @Rule
  public CloseableReference<ServerLauncher> server = new CloseableReference<>();
  @Rule
  public RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    workingDirectory = temporaryFolder.newFolder(NAME);
    jmxPort = getRandomAvailableTCPPort();
    openMBeanFilterPattern = new OpenMBeanFilterPattern().pattern();
  }

  @Test
  public void configuresJmxSerialFilter_onJava9orGreater() {
    assumeThat(isJavaVersionAtLeast(JAVA_9)).isTrue();

    server.set(new ServerLauncher.Builder()
        .setMemberName(NAME)
        .setDisableDefaultServer(true)
        .setWorkingDirectory(workingDirectory.getAbsolutePath())
        .set(HTTP_SERVICE_PORT, "0")
        .set(JMX_MANAGER, "true")
        .set(JMX_MANAGER_PORT, String.valueOf(jmxPort))
        .set(JMX_MANAGER_START, "true")
        .set(LOG_FILE, new File(workingDirectory, NAME + ".log").getAbsolutePath())
        .build())
        .get()
        .start();

    assertThat(isJmxManagerStarted())
        .isTrue();
    assertThat(System.getProperty(JMX_SERIAL_FILTER_PROPERTY))
        .as(JMX_SERIAL_FILTER_PROPERTY)
        .isEqualTo(openMBeanFilterPattern);
  }

  @Test
  public void configuresJmxSerialFilter_whenPropertyIsEmpty_onJava9orGreater() {
    assumeThat(isJavaVersionAtLeast(JAVA_9)).isTrue();

    System.setProperty(JMX_SERIAL_FILTER_PROPERTY, "");

    server.set(new ServerLauncher.Builder()
        .setMemberName(NAME)
        .setDisableDefaultServer(true)
        .setWorkingDirectory(workingDirectory.getAbsolutePath())
        .set(HTTP_SERVICE_PORT, "0")
        .set(JMX_MANAGER, "true")
        .set(JMX_MANAGER_PORT, String.valueOf(jmxPort))
        .set(JMX_MANAGER_START, "true")
        .set(LOG_FILE, new File(workingDirectory, NAME + ".log").getAbsolutePath())
        .build())
        .get()
        .start();

    assertThat(isJmxManagerStarted())
        .isTrue();
    assertThat(System.getProperty(JMX_SERIAL_FILTER_PROPERTY))
        .as(JMX_SERIAL_FILTER_PROPERTY)
        .isEqualTo(openMBeanFilterPattern);
  }

  @Test
  public void doesNotChangeJmxSerialFilter_whenPropertyIsNotEmpty_onJava9orGreater() {
    assumeThat(isJavaVersionAtLeast(JAVA_9)).isTrue();

    String existingSerialFilter = "!*";
    System.setProperty(JMX_SERIAL_FILTER_PROPERTY, existingSerialFilter);

    server.set(new ServerLauncher.Builder()
        .setMemberName(NAME)
        .setDisableDefaultServer(true)
        .setWorkingDirectory(workingDirectory.getAbsolutePath())
        .set(HTTP_SERVICE_PORT, "0")
        .set(JMX_MANAGER, "true")
        .set(JMX_MANAGER_PORT, String.valueOf(jmxPort))
        .set(JMX_MANAGER_START, "true")
        .set(LOG_FILE, new File(workingDirectory, NAME + ".log").getAbsolutePath())
        .build())
        .get()
        .start();

    assertThat(isJmxManagerStarted())
        .isTrue();
    assertThat(System.getProperty(JMX_SERIAL_FILTER_PROPERTY))
        .as(JMX_SERIAL_FILTER_PROPERTY)
        .isEqualTo(existingSerialFilter);
  }

  @Test
  public void doesNotConfigureJmxSerialFilter_onJava8() {
    assumeThat(isJavaVersionAtMost(JAVA_1_8)).isTrue();

    server.set(new ServerLauncher.Builder()
        .setMemberName(NAME)
        .setDisableDefaultServer(true)
        .setWorkingDirectory(workingDirectory.getAbsolutePath())
        .set(HTTP_SERVICE_PORT, "0")
        .set(JMX_MANAGER, "true")
        .set(JMX_MANAGER_PORT, String.valueOf(jmxPort))
        .set(JMX_MANAGER_START, "true")
        .set(LOG_FILE, new File(workingDirectory, NAME + ".log").getAbsolutePath())
        .build())
        .get()
        .start();

    assertThat(isJmxManagerStarted())
        .isTrue();
    assertThat(System.getProperty(JMX_SERIAL_FILTER_PROPERTY))
        .as(JMX_SERIAL_FILTER_PROPERTY)
        .isNull();
  }

  @Test
  public void doesNotConfigureJmxSerialFilter_whenPropertyIsEmpty_onJava8() {
    assumeThat(isJavaVersionAtMost(JAVA_1_8)).isTrue();

    System.setProperty(JMX_SERIAL_FILTER_PROPERTY, "");

    server.set(new ServerLauncher.Builder()
        .setMemberName(NAME)
        .setDisableDefaultServer(true)
        .setWorkingDirectory(workingDirectory.getAbsolutePath())
        .set(HTTP_SERVICE_PORT, "0")
        .set(JMX_MANAGER, "true")
        .set(JMX_MANAGER_PORT, String.valueOf(jmxPort))
        .set(JMX_MANAGER_START, "true")
        .set(LOG_FILE, new File(workingDirectory, NAME + ".log").getAbsolutePath())
        .build())
        .get()
        .start();

    assertThat(isJmxManagerStarted())
        .isTrue();
    assertThat(System.getProperty(JMX_SERIAL_FILTER_PROPERTY))
        .as(JMX_SERIAL_FILTER_PROPERTY)
        .isEqualTo("");
  }

  @Test
  public void doesNotConfigureJmxSerialFilter_whenPropertyIsNotEmpty_onJava8() {
    assumeThat(isJavaVersionAtMost(JAVA_1_8)).isTrue();

    String existingSerialFilter = "!*";
    System.setProperty(JMX_SERIAL_FILTER_PROPERTY, existingSerialFilter);

    server.set(new ServerLauncher.Builder()
        .setMemberName(NAME)
        .setDisableDefaultServer(true)
        .setWorkingDirectory(workingDirectory.getAbsolutePath())
        .set(HTTP_SERVICE_PORT, "0")
        .set(JMX_MANAGER, "true")
        .set(JMX_MANAGER_PORT, String.valueOf(jmxPort))
        .set(JMX_MANAGER_START, "true")
        .set(LOG_FILE, new File(workingDirectory, NAME + ".log").getAbsolutePath())
        .build())
        .get()
        .start();

    assertThat(isJmxManagerStarted())
        .isTrue();
    assertThat(System.getProperty(JMX_SERIAL_FILTER_PROPERTY))
        .as(JMX_SERIAL_FILTER_PROPERTY)
        .isEqualTo(existingSerialFilter);
  }

  private SystemManagementService getSystemManagementService() {
    Cache cache = server.get().getCache();
    return (SystemManagementService) ManagementService.getManagementService(cache);
  }

  private boolean isJmxManagerStarted() {
    return getSystemManagementService().isManager();
  }
}