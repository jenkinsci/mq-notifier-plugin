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
package com.sonymobile.jenkins.plugins.mq.mqnotifier;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.PossibleAuthenticationFailureException;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.net.URISyntaxException;

/**
 * Adds the MQ notifier plugin configuration to the system config page.
 *
 * @author Örjan Percy &lt;orjan.percy@sonymobile.com&gt;
 */
@Extension
@Symbol("mqNotifier")
public final class MQNotifierConfig extends GlobalConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MQNotifierConfig.class);
    private final String[] schemes = {"amqp", "amqps"};
    private static final String SERVER_URI = "serverUri";
    private static final String USERNAME = "userName";
    private static final String PASSWORD = "userPassword";

    /* The status whether the plugin is enabled */
    private boolean enableNotifier;

    /* Whether to use verbose logging or not */
    private boolean enableVerboseLogging;

    /* The MQ server URI */
    private String serverUri;
    private String userName;
    private Secret userPassword;

    /* The notifier plugin sends messages to an exchange which will push the messages to one or several queues.*/
    private String exchangeName;

    /* The virtual host which the connection intends to operate within. */
    private String virtualHost;

    /* Messages will be sent with a routing key which allows messages to be delivered to queues that are bound with a
     * matching binding key. The routing key must be a list of words, delimited by dots.
     */
    private String routingKey;
    /* Messages delivered to durable queues will be logged to disk if persistent delivery is set. */
    private boolean persistentDelivery;
    /* Application id that can be read by the consumer (optional). */
    private String appId;

    /**
     * Creates an instance with specified parameters.
     *
     * @param enableNotifier        if this plugin is enabled
     * @param serverUri             the server uri
     * @param userName              the user name
     * @param userPassword          the user password
     * @param exchangeName          the name of the exchange
     * @param virtualHost           the name of the virtual host
     * @param routingKey            the routing key
     * @param persistentDelivery    if using persistent delivery mode
     * @param appId                 the application id
     * @param enableVerboseLogging  if verbose logging is enabled
     */
    @DataBoundConstructor
    public MQNotifierConfig(boolean enableNotifier, String serverUri, String userName, Secret userPassword,
                            String exchangeName, String virtualHost, String routingKey, boolean persistentDelivery,
                            String appId, boolean enableVerboseLogging) {
        this.enableNotifier = enableNotifier;
        this.serverUri = serverUri;
        this.userName = userName;
        this.userPassword = userPassword;
        this.exchangeName = exchangeName;
        this.virtualHost = virtualHost;
        this.routingKey = routingKey;
        this.persistentDelivery = persistentDelivery;
        this.appId = appId;
        this.enableVerboseLogging = enableVerboseLogging;
    }

    /**
     * Load configuration on invoke.
     */
    public MQNotifierConfig() {
        this.enableNotifier = false;        // default value
        this.persistentDelivery = true;     // default value
        this.enableVerboseLogging = true;   // default value
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
        req.bindJSON(this, formData);
        save();
        MQConnection.getInstance().initialize(userName, userPassword, serverUri, virtualHost);
        return true;
    }

    /**
     * Gets whether this plugin is enabled or not.
     *
     * @return true if this plugin is enabled.
     */
    public boolean getEnableNotifier() {
        return this.enableNotifier;
    }

    /**
     * Sets flag whether this plugin is enabled or not.
     *
     * @param enableNotifier true if this plugin is enabled.
     */
    public void setEnableNotifier(boolean enableNotifier) {
        this.enableNotifier = enableNotifier;
    }

    /**
     * Gets whether verbose logging is enabled or not.
     *
     * @return true if verbose logging is enabled.
     */
    public boolean getEnableVerboseLogging() {
        return this.enableVerboseLogging;
    }

    /**
     * Sets flag whether verbose logging is enabled or not.
     *
     * @param enableVerboseLogging true if this verbose logging is enabled.
     */
    public void setEnableVerboseLogging(boolean enableVerboseLogging) {
        this.enableVerboseLogging = enableVerboseLogging;
    }

    /**
     * Gets URI for MQ server.
     *
     * @return the URI.
     */
    public String getServerUri() {
        return this.serverUri;
    }

    /**
     * Sets URI for MQ server.
     *
     * @param serverUri the URI.
     */
    public void setServerUri(final String serverUri) {
        this.serverUri = StringUtils.strip(StringUtils.stripToNull(serverUri), "/");
    }

    /**
     * Gets user name.
     *
     * @return the user name.
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * Sets user name.
     *
     * @param userName the user name.
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Gets user password.
     *
     * @return the user password.
     */
    public Secret getUserPassword() {
        return this.userPassword;
    }

    /**
     * Sets user password.
     *
     * @param userPassword the user password.
     */
    public void setUserPassword(Secret userPassword) {
        this.userPassword = userPassword;
    }

    /**
     * Gets this extension's instance.
     * <p>
     * If {@link jenkins.model.Jenkins#getInstance()} isn't available
     * or the plugin class isn't registered null will be returned.
     *
     * @return the instance of this extension.
     */
    public static MQNotifierConfig getInstance() {
        return MQNotifierConfig.all().get(MQNotifierConfig.class);
    }

    /**
     * Gets the exchange name.
     *
     * @return the exchange name.
     */
    public String getExchangeName() {
        return this.exchangeName;
    }

    /**
     * Sets the exchange name.
     *
     * @param exchangeName the exchange name.
     */
    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    /**
     * Gets the virtual host name.
     *
     * @return the virtual host name.
     */
    public String getVirtualHost() {
        return this.virtualHost;
    }

    /**
     * Sets the virtual host name.
     *
     * @param virtualHost the exchange name.
     */
    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    /**
     * Gets the routing key.
     *
     * @return the routing key.
     */
    public String getRoutingKey() {
        return this.routingKey;
    }

    /**
     * Sets the routing key.
     *
     * @param routingKey the routing key.
     */
    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    /**
     * Returns true if persistentDelivery is to be used.
     *
     * @return if persistentDelivery is to be used.
     */
    public boolean getPersistentDelivery() {
        return this.persistentDelivery;
    }

    /**
     * Sets persistent delivery mode.
     *
     * @param pd if persistentDelivery is to be used.
     */
    public void setPersistentDelivery(boolean pd) {
        this.persistentDelivery = pd;
    }

    /**
     * Returns application id.
     *
     * @return application id.
     */
    public String getAppId() {
        return this.appId;
    }

    /**
     * Sets application id.
     *
     * @param appId Application id to use
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    @Override
    public String getDisplayName() {
        return "MQ Notifier Plugin";
    }

    /**
     * Tests connection to the server URI.
     *
     * @param uri the URI.
     * @param name the user name.
     * @param pw the user password.
     * @return FormValidation object that indicates ok or error.
     * @throws javax.servlet.ServletException Exception for servlet.
     */
    @RequirePOST
    public FormValidation doTestConnection(@QueryParameter(SERVER_URI) final String uri,
                                           @QueryParameter(USERNAME) final String name,
                                           @QueryParameter(PASSWORD) final Secret pw) throws ServletException {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        UrlValidator urlValidator = new UrlValidator(getInstance().schemes, UrlValidator.ALLOW_LOCAL_URLS);
        FormValidation result = FormValidation.ok();
        if (urlValidator.isValid(uri)) {
            try {
                ConnectionFactory conn = new ConnectionFactory();
                conn.setUri(uri);
                if (StringUtils.isNotEmpty(name)) {
                    conn.setUsername(name);
                    if (StringUtils.isNotEmpty(Secret.toString(pw))) {
                        conn.setPassword(Secret.toString(pw));
                    }
                }
                conn.newConnection();
            } catch (URISyntaxException e) {
                result = FormValidation.error("Invalid Uri");
            } catch (PossibleAuthenticationFailureException e) {
                result = FormValidation.error("Authentication Failure");
            } catch (Exception e) {
                result = FormValidation.error(e.getMessage());
            }
        } else {
            result = FormValidation.error("Invalid Uri");
        }
        return result;

    }

}
