package com.sonymobile.jenkins.plugins.mq.mqnotifier;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.testcontainers.containers.RabbitMQContainer;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
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
     * @return an RabbitMQ container singleton. A container is not started.
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
     * @param numberExpectedMessages stop after finding this many messages
     * @param secondsWaitPerMessage the max time to wait for a message
     * @param queueName the queue to listen to messages on
     *
     * @return a list of the found messages as JSONObjects
     */
    public static ArrayList<JSONObject> waitForMessages(
            MQConnection conn,
            int numberExpectedMessages,
            int secondsWaitPerMessage,
            String queueName
    ) throws IOException, InterruptedException {
        Channel channel = null;

        while (channel == null) {
            try {
                channel = conn.getConnection().createChannel();
                Thread.sleep(1000);
            }catch (Exception ignored){}
        }
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
            while(foundMessages.size() != numberExpectedMessages) {
                foundMessages.wait(Duration.ofSeconds(secondsWaitPerMessage).toMillis());
            }
            foundMessages.drainTo(foundMessagesArray);
        }
        return foundMessagesArray;
    }

    /**
     * Creates a list of x messages. Useful for generating messages during testing.
     *
     * @param count the number of messages to create
     *
     * @return list of messages
     */
    public static ArrayList<JSONObject> createMessages(int count) {
        return IntStream.range(1, count+1).mapToObj(i -> {
            JSONObject message = new JSONObject();
            message.put("test", "test" + i);
            return message;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

}
