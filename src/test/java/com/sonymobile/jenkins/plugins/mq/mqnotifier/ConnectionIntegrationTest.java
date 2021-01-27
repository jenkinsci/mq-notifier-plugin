package com.sonymobile.jenkins.plugins.mq.mqnotifier;

import eu.rekawek.toxiproxy.model.ToxicDirection;
import eu.rekawek.toxiproxy.model.toxic.Timeout;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.jvnet.hudson.test.JenkinsRule;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.ToxiproxyContainer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.testcontainers.containers.Network.newNetwork;

/**
 * Integration tests for the MQConnection.
 *
 * @author Hampus Johansson
 */
@SuppressWarnings({"checkstyle:magicnumber", "checkstyle:javadocvariable"})
public class ConnectionIntegrationTest {

    private static final String TOXIPROXY_NETWORK_ALIAS = "toxiproxy";
    private static final int DEFAULT_MESSAGE_WAIT = 10;

    RabbitMQContainer defaultMQContainer = TestUtil.getDefaultMQContainer();
    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Rule
    public Network network = newNetwork();

    private ToxiproxyContainer toxiproxy = new ToxiproxyContainer()
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
     * Creates a connection to RabbitMQ through toxiproxy and configures exchanges, passwords etc.
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
     * Clean up outstanding messages before running new tests.
     */
    @Before
    public void clearOutstandingConfirms() {
        MQConnection.getInstance().clearOutstandingConfirms();
    }

    /**
     * Format an URI to toxiproxy, which will be forwarded to RabbitMQ.
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
        int messageCount = 10;
        ArrayList<JSONObject> expectedMessages = TestUtil.createMessages(messageCount);
        expectedMessages.forEach(conn::publish);
        ArrayList<JSONObject> actualMessages = TestUtil.waitForMessages(
                conn,
                messageCount,
                DEFAULT_MESSAGE_WAIT,
                TestUtil.QUEUE_NAME
        );
        assertEquals(expectedMessages, actualMessages);
    }

    /**
     * Test that the MQNotifier plugin receive ACKs.
     */
    @Test
    public void testSentMessagesReceiveACKs() throws IOException, InterruptedException {
        MQConnection conn = MQConnection.getInstance();
        int messageCount = 25;
        ArrayList<JSONObject> expectedMessages = TestUtil.createMessages(messageCount);
        expectedMessages.forEach(conn::publish);
        TestUtil.waitForMessages(
                conn,
                messageCount,
                DEFAULT_MESSAGE_WAIT,
                TestUtil.QUEUE_NAME
        );
        Thread.sleep(2000); // Make sure the ACKs have some time to get processed.
        assertEquals(0, conn.getSizeOutstandingConfirms());
    }

    /**
     * Test that the MQNotifier won't lose messages when the connection is closed.
     */
    @Test
    public void testSendMessagesHandlesClosedConnection() throws InterruptedException, IOException {
        MQConnection conn = MQConnection.getInstance();
        int messageCount = 1000;
        ArrayList<JSONObject> expectedMessages = TestUtil.createMessages(messageCount);

        getProxy().setConnectionCut(true);
        executor.submit(() -> {
            expectedMessages.forEach(conn::publish);
        });
        Thread.sleep(1000);
        getProxy().setConnectionCut(false);
        ArrayList<JSONObject> actualMessages = TestUtil.waitForMessages(
                conn,
                messageCount,
                DEFAULT_MESSAGE_WAIT,
                TestUtil.QUEUE_NAME
        );
        assertEquals(expectedMessages, actualMessages);
    }

    /**
     * Test that the MQNotifier won't lose messages when upstream is closed, i.e. no acks.
     */
    @Test
    public void testSendMessagesHandlesUpstreamTimeout() throws InterruptedException, IOException {
        MQConnection conn = MQConnection.getInstance();
        int messageCount = 1000;
        ArrayList<JSONObject> expectedMessages = TestUtil.createMessages(messageCount);
        getProxy().toxics().timeout("timeout", ToxicDirection.UPSTREAM, 8000);
        executor.submit(() -> {
            expectedMessages.forEach(conn::publish);
        });
        Thread.sleep(8000);
        getProxy().toxics().get("timeout", Timeout.class).remove();
        ArrayList<JSONObject> actualMessages = TestUtil.waitForMessages(
                conn,
                messageCount,
                DEFAULT_MESSAGE_WAIT,
                TestUtil.QUEUE_NAME
        );
        assertEquals(expectedMessages, actualMessages);
    }

    /**
     * Test that the MQNotifier won't lose messages when the connection flickers.
     */
    @Test
    public void testSendMessagesHandlesConnectionFlicker() throws InterruptedException, IOException {
        MQConnection conn = MQConnection.getInstance();
        int messageCount = 1000;
        ArrayList<JSONObject> expectedMessages = TestUtil.createMessages(messageCount);
        executor.submit(() -> {
            expectedMessages.subList(0, 500).forEach(conn::publish);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            expectedMessages.subList(messageCount / 2, messageCount).forEach(conn::publish);
        });
        Thread.sleep(1000);
        getProxy().setConnectionCut(true);
        Thread.sleep(5000);
        getProxy().setConnectionCut(false);
        ArrayList<JSONObject> actualMessages = TestUtil.waitForMessages(
                conn,
                messageCount,
                DEFAULT_MESSAGE_WAIT,
                TestUtil.QUEUE_NAME
        );
        assertEquals(expectedMessages, actualMessages);
    }

    /**
     * Test that the MQNotifier won't lose messages on high latency.
     */
    @Test
    public void testSendMessagesHandlesLatency() throws InterruptedException, IOException {
        MQConnection conn = MQConnection.getInstance();
        int messageCount = 2;
        ArrayList<JSONObject> expectedMessages = TestUtil.createMessages(messageCount);
        getProxy().toxics().latency("latency", ToxicDirection.UPSTREAM, 2000).setJitter(2000);
        executor.submit(() -> {
            expectedMessages.forEach(conn::publish);
        });
        ArrayList<JSONObject> actualMessages = TestUtil.waitForMessages(
                conn,
                messageCount,
                30,
                TestUtil.QUEUE_NAME);
        assertEquals(expectedMessages, actualMessages);
    }

}
