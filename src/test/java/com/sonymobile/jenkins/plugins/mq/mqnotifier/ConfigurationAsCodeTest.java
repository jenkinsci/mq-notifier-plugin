/*
 *  The MIT License
 *
 *  Copyright 2022 Axis Communications AB. All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package com.sonymobile.jenkins.plugins.mq.mqnotifier;

import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import org.junit.ClassRule;
import org.junit.Test;

import static io.jenkins.plugins.casc.misc.Util.getUnclassifiedRoot;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class ConfigurationAsCodeTest {

    /**
     * Configure a Jenkins rule with configuration as code installed.
     */
    @ClassRule
    @ConfiguredWithCode("configuration-as-code.yml")
    public static JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    public void shouldSupportConfigurationAsCode() {
        MQNotifierConfig config = MQNotifierConfig.getInstance();
        /* assertAll not available in junit 4 */
        assertEquals("johndoe", config.getUserName());
        assertEquals("mq.test.com", config.getServerUri());
        assertEquals("jenkins", config.getRoutingKey());
        assertFalse(config.getEnableNotifier());
        assertFalse(config.getPersistentDelivery());
        assertTrue(config.getEnableVerboseLogging());
    }

    @Test
    public void shouldSupportConfigurationExport() throws Exception {
        MQNotifierConfig config = MQNotifierConfig.getInstance();
        config.setUserName("johndoe");
        config.setServerUri("mq.test.com");
        config.setExchangeName("test");
        config.setEnableNotifier(false);
        config.setRoutingKey("jenkins");
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());
        CNode mqNotifierAttr = getUnclassifiedRoot(context).get("mq-notifier");
        String exported = toYamlString(mqNotifierAttr).replaceFirst("password: \".*\"", "");
        String expected = toStringFromYamlFile(this, "expected-config.yml");
        assertEquals(expected, exported);
    }

}
