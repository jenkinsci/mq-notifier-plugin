package com.sonymobile.jenkins.plugins.mq.mqnotifier;

import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.jvnet.hudson.test.JenkinsRule;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.Network.newNetwork;


import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Integration tests for the MQConnection
 *
 * @author Hampus Johansson
 */
public class ConnectionIntegrationTest {

    private static final String TOXIPROXY_NETWORK_ALIAS = "toxiproxy";

    RabbitMQContainer defaultMQContainer = TestUtil.getDefaultMQContainer();
    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Rule
    public Network network = newNetwork();

    public ToxiproxyContainer toxiproxy = new ToxiproxyContainer()
            .withNetwork(network)
            .withNetworkAliases(TOXIPROXY_NETWORK_ALIAS);

    @Rule
    public TestRule chain = RuleChain
            .outerRule(defaultMQContainer.withNetwork(network))
            .around(toxiproxy)
            .around(new JenkinsRule());

    /**
     * Get the toxiproxy for the MQ container
     */
    private ToxiproxyContainer.ContainerProxy getProxy() {
        return toxiproxy.getProxy(defaultMQContainer, TestUtil.PORT);
    }

    /**
     * Creates a connection to rabbitMQ (through toxiproxy) and configure exchanges, passwords etc.
     */
    @Before
    public void createProxyConnection() {
        MQNotifierConfig config = MQNotifierConfig.getInstance();
        assertNotNull("No config available: MQNotifierConfig", config);
        TestUtil.setDefaultConfig(config);
        config.setServerUri(formatProxyServerUri());

        MQConnection conn = MQConnection.getInstance();
        conn.initialize(config.getUserName(), config.getUserPassword(), config.getServerUri(), config.getVirtualHost());
    }

    /**
     * Format an uri to the toxiproxy.
     */
    private String formatProxyServerUri() {
        ToxiproxyContainer.ContainerProxy proxy = getProxy();
        final String ipAddressViaToxiproxy = proxy.getContainerIpAddress();
        final int portViaToxiproxy = proxy.getProxyPort();
        return "amqp://" + ipAddressViaToxiproxy + ":" + portViaToxiproxy;
    }

    /**
     * Test that the MQNotifier plugin sends messages correctly.
     */
    @Test
    public void testSentMessagesHasCorrectFormat() throws IOException, InterruptedException {
        MQConnection conn = MQConnection.getInstance();
        ArrayList<JSONObject> expectedMessages = TestUtil.createMessages(10);
        expectedMessages.forEach(conn::publish);
        ArrayList<JSONObject> actualMessages = TestUtil.waitForMessages(conn, 10, 10, TestUtil.QUEUE_NAME);
        assertEquals(expectedMessages, actualMessages);
    }

    /**
     * Test that the MQNotifier won't lose messages when the connection is closed.
     */
    @Test
    public void testSendMessagesHandlesClosedConnection() throws InterruptedException, IOException {
        MQConnection conn = MQConnection.getInstance();
        ArrayList<JSONObject> expectedMessages = TestUtil.createMessages(1000);

        getProxy().setConnectionCut(true);
        executor.submit(() -> {
            expectedMessages.forEach(conn::publish);
        });
        Thread.sleep(1000);
        getProxy().setConnectionCut(false);
        ArrayList<JSONObject> actualMessages = TestUtil.waitForMessages(conn, 1000, 40, TestUtil.QUEUE_NAME);
        assertEquals(expectedMessages, actualMessages);
    }

}
