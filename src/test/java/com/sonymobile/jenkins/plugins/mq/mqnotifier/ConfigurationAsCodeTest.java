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
        assertEquals("hampus", config.getUserName());
        assertEquals("elasticsearch.tools.axis.com", config.getServerUri());
        assertEquals("jenkins", config.getRoutingKey());
        assertFalse(config.getEnableNotifier());
        assertFalse(config.getPersistentDelivery());
        assertTrue(config.getEnableVerboseLogging());
    }

    @Test
    public void shouldSupportConfigurationExport() throws Exception {
        MQNotifierConfig config = MQNotifierConfig.getInstance();
        config.setUserName("hampus");
        config.setServerUri("elasticsearch.tools.axis.com");
        config.setExchangeName("test");
        config.setEnableNotifier(false);
        config.setRoutingKey("jenkins");
        ConfigurationContext context = new ConfigurationContext(ConfiguratorRegistry.get());
        CNode mqNotifierAttr = getUnclassifiedRoot(context).get("mqNotifier");
        String exported = toYamlString(mqNotifierAttr).replaceFirst("password: \".*\"", "");
        String expected = toStringFromYamlFile(this, "expected-config.yml");
        assertEquals(expected, exported);
    }

}
