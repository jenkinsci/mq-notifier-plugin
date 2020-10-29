package com.sonymobile.jenkins.plugins.mq.mqnotifier;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.junit.AssumptionViolatedException;
import org.testcontainers.containers.RabbitMQContainer;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility methods for running e.g. integration tests and configuration setup.
 *
 * @author Hampus Johansson
 */
public final class TestUtil {

    public static final String EXCHANGE = "jenkins";
    public static final String QUEUE_NAME = "test-queue";
    public static final int PORT = 5672;

    private static RabbitMQContainer defaultMQContainer = null;

    /**
     * Creates a default RabbitMQ container to run tests against. It declares an Exchange (EXCHANGE)
     * with a binding to a queue (QUEUE_NAME). No routing queue should be specified. The container may
     * be started by, for example, using a jUnit @Rule.
     *
     * @return an RabbitMQ container singleton. The container is not started.
     */
    public static RabbitMQContainer getDefaultMQContainer() {
        if (defaultMQContainer == null) {
            defaultMQContainer = new RabbitMQContainer("rabbitmq:3-management")
                    .withExposedPorts(PORT)
                    .withExposedPorts(15672)
                    .withExchange(EXCHANGE, "direct")
                    .withQueue(QUEUE_NAME)
                    .withBinding(EXCHANGE, QUEUE_NAME);
        }
        return defaultMQContainer;
    }

    /**
     * Set common configuration values, intended for use together with the RabbitMQ container.
     *
     * @param config A configuration object to set default configuration for.
     */
    public static void setDefaultConfig(MQNotifierConfig config) {
        config.setUserName("guest");
        config.setUserPassword(Secret.fromString("guest"));
        config.setExchangeName(EXCHANGE);
        config.setRoutingKey("");
        config.setVirtualHost(null);
        config.setEnableNotifier(true);
    }

    /**
     * Wait for x messages on the queue to be published.
     *
     * @param conn A connection to be used for connecting to RabbitMQ
     * @param stopAtMessage stop after finding this many messages
     * @param maxWaitTime the max time to wait for a message
     * @param queueName the queue to listen to messages on
     *
     * @return a list of the found messages as JSONObjects
     */
    public static ArrayList<JSONObject> waitForMessages(
            MQConnection conn,
            int stopAtMessage,
            int maxWaitTime,
            String queueName
    ) throws IOException, InterruptedException {
        Channel channel = conn.getConnection().openChannel().get();
        LinkedBlockingQueue<JSONObject> foundMessages = new LinkedBlockingQueue<>();
        ArrayList<JSONObject> foundMessagesArray = new ArrayList<>();

        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(
                    String consumerTag,
                    Envelope envelope,
                    AMQP.BasicProperties properties,
                    byte[] body) throws IOException {

                JSONObject message = JSONObject.fromObject(new String(body));
                foundMessages.offer(message);
                synchronized (foundMessages) {
                    foundMessages.notify();
                }
            }
        };

        channel.basicConsume(queueName, true, consumer);

        synchronized (foundMessages) {
            while(foundMessages.size() != stopAtMessage) {
                foundMessages.wait(Duration.ofSeconds(maxWaitTime).toMillis());
            }
            foundMessages.drainTo(foundMessagesArray);
        }
        return foundMessagesArray;
    }

    /**
     * Publish a list of messages to RabbitMQ.
     *
     * @param messages the messages, as JSONObjects to be published to RabbitMQ
     * @param conn A connection to be used for connecting to RabbitMQ and to publish messages on
     * @param waitTime the max time to wait for all messages to be published
     *
     * @throws InterruptedException if all messages couldn't be publish within the wait time
     */
    public static void sendMessagesWithinTimeframe(ArrayList<JSONObject> messages, MQConnection conn, int waitTime) throws InterruptedException {
        messages.forEach(conn::publish);
        if (!waitUntil(Duration.ofSeconds(waitTime), () -> conn.getSizeOutstandingConfirms() == 0)) {
            throw new AssumptionViolatedException("All messages could not be confirmed within the timeframe");
        }
    }

    /**
     * Creates a list of x messages. Useful for generating messages during testing.
     *
     * @param numberOfMessages the number of messages to create
     *
     * @return list of messages
     */
    public static ArrayList<JSONObject> createMessages(int numberOfMessages) {
        return IntStream.range(1, numberOfMessages+1).mapToObj(i -> {
            JSONObject message = new JSONObject();
            message.put("test", "test" + i);
            return message;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Busy wait until a condition becomes true
     *
     * @param timeout a timeout not to wait longer than.
     * @param condition a condition to evaluate. Proceed if condition is true
     *
     * @return the return value of the condition
     * @throws InterruptedException if the sleep is interrupted
     */
    public static boolean waitUntil(Duration timeout, BooleanSupplier condition) throws InterruptedException {
        int waited = 0;
        while (!condition.getAsBoolean() && waited < timeout.toMillis()) {
            Thread.sleep(100L);
            waited += 100;
        }
        return condition.getAsBoolean();
    }

}
