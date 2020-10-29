package com.sonymobile.jenkins.plugins.mq.mqnotifier;

import net.sf.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.jvnet.hudson.test.JenkinsRule;
import org.testcontainers.containers.RabbitMQContainer;

import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Integration tests for the MQConnection
 *
 * @author Hampus Johansson
 */
public class ConnectionIntegrationTest {

    RabbitMQContainer defaultMQContainer = TestUtil.getDefaultMQContainer();

    @Rule
    public TestRule chain = RuleChain
            .outerRule(defaultMQContainer)
            .around(new JenkinsRule());

    /**
     * Test that the MQNotifier plugin sends messages correctly.
     */
    @Test
    public void testSentMessagesHasCorrectFormat() throws IOException, InterruptedException {
        MQNotifierConfig config = MQNotifierConfig.getInstance();
        assertNotNull("No config available: MQNotifierConfig", config);
        TestUtil.setDefaultConfig(config);
        config.setServerUri(defaultMQContainer.getAmqpUrl());

        MQConnection conn = MQConnection.getInstance();
        conn.initialize(config.getUserName(), config.getUserPassword(), config.getServerUri(), config.getVirtualHost());

        ArrayList<JSONObject> expectedMessages = TestUtil.createMessages(10);
        TestUtil.sendMessagesWithinTimeframe(expectedMessages, conn, 10);
        ArrayList<JSONObject> actualMessages = TestUtil.waitForMessages(conn, 10, 10, TestUtil.QUEUE_NAME);
        assertEquals(expectedMessages, actualMessages);
    }

}
