/*
 *  The MIT License
 *
 *  Copyright 2015 Sony Mobile Communications Inc. All rights reserved.
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
package com.sonymobile.jenkins.plugins.rabbitmq.rabbitmqproducer;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AlreadyClosedException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import hudson.util.Secret;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Creates a RabbitMQ connection.
 *
 * @author Örjan Percy &lt;orjan.percy@sonymobile.com&gt;
 */
public final class RabbitMQConnection implements ShutdownListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitMQConnection.class);
    private static final int HEARTBEAT_INTERVAL = 30;

    private String userName;
    private Secret userPassword;
    private String serverUri;
    private String virtualHost;
    private Connection connection = null;
    private Channel channel = null;

    /**
     * Lazy-loaded singleton using the initialization-on-demand holder pattern.
     */
    private RabbitMQConnection() { }

    /**
     * Is only executed on {@link #getInstance()} invocation.
     */
    private static class LazyRabbit {
        private static final RabbitMQConnection INSTANCE = new RabbitMQConnection();
        private static final ConnectionFactory CF = new ConnectionFactory();
    }

    /**
     * Gets the instance.
     *
     * @return the instance
     */
    public static RabbitMQConnection getInstance() {
        return LazyRabbit.INSTANCE;
    }

    /**
     * Gets the connection factory that will enable a connection to the AMQP server.
     *
     * @return the connection factory
     */
    private ConnectionFactory getConnectionFactory() {
        if (LazyRabbit.CF != null) {
            try {
                // Try to recover the topology along with the connection.
                LazyRabbit.CF.setAutomaticRecoveryEnabled(true);
                // set requested heartbeat interval, in seconds
                LazyRabbit.CF.setRequestedHeartbeat(HEARTBEAT_INTERVAL);
                LazyRabbit.CF.setUri(serverUri);
                if (StringUtils.isNotEmpty(virtualHost)) {
                    LazyRabbit.CF.setVirtualHost(virtualHost);
                }
            } catch (KeyManagementException e) {
                LOGGER.error("KeyManagementException: ", e);
            } catch (NoSuchAlgorithmException e) {
                LOGGER.error("NoSuchAlgorithmException: ", e);
            } catch (URISyntaxException e) {
                LOGGER.error("URISyntaxException: ", e);
            }
            if (StringUtils.isNotEmpty(userName)) {
                LazyRabbit.CF.setUsername(userName);
                if (StringUtils.isNotEmpty(Secret.toString(userPassword))) {
                    LazyRabbit.CF.setPassword(Secret.toString(userPassword));
                }
            }
        }
        return LazyRabbit.CF;
    }

    /**
     * Gets the connection.
     *
     * @return the connection.
     */
    public Connection getConnection() {
        if (connection == null) {
            try {
                connection = getConnectionFactory().newConnection();
                connection.addShutdownListener(this);
            } catch (IOException e) {
                LOGGER.warn("Connection refused", e);
            }
        }
        return connection;
    }

    /**
     * Initializes this instance with supplied values.
     *
     * @param name the user name
     * @param password the user password
     * @param uri the server uri
     * @param vh the virtual host
     */
    public void initialize(String name, Secret password, String uri, String vh) {
        userName = name;
        userPassword = password;
        serverUri = uri;
        virtualHost = vh;
    }

    /**
     * Sends a message.
     * *
     * @param exchange the exchange to publish the message to
     * @param routingKey the routing key
     * @param props other properties for the message - routing headers etc
     * @param body the message body
     */
    public void send(String exchange, String routingKey, AMQP.BasicProperties props, byte[] body) {
        if (exchange == null) {
            LOGGER.error("Invalid configuration, exchange must not be null.");
            return;
        }
        try {
            if (channel == null || !channel.isOpen()) {
                channel = getConnection().createChannel();
                if (! getConnection().getAddress().isLoopbackAddress()) {
                    channel.exchangeDeclarePassive(exchange);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Cannot create channel", e);
            channel = null; // reset
        } catch (ShutdownSignalException e) {
            LOGGER.error("Cannot create channel", e);
            channel = null; // reset
        }
        if (channel != null) {
            try {
                channel.basicPublish(exchange, routingKey, props, body);
            } catch (IOException e) {
                LOGGER.error("Cannot publish message", e);
            } catch (AlreadyClosedException e) {
                LOGGER.error("Connection is already closed", e);
            }
        }
    }

    @Override
    public void shutdownCompleted(ShutdownSignalException cause) {
        if (cause.isHardError()) {
            if (!cause.isInitiatedByApplication()) {
                LOGGER.warn("RabbitMQ connection was suddenly disconnected.");
                try {
                    if (connection != null && connection.isOpen()) {
                        connection.close();
                    }
                    if (channel != null && channel.isOpen()) {
                        channel.close();
                    }
                } catch (IOException e) {
                    LOGGER.error("IOException: ", e);
                } catch (AlreadyClosedException e) {
                    LOGGER.error("AlreadyClosedException: ", e);
                } finally {
                    channel = null;
                    connection = null;
                }
            }
        } else {
            LOGGER.warn("RabbitMQ channel was suddenly disconnected.");
        }
    }
}
