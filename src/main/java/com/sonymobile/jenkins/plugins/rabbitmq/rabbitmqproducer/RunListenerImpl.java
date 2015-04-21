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
import hudson.Extension;
import hudson.Functions;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.model.AbstractBuild;
import hudson.model.Cause;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.TopLevelItem;
import hudson.model.listeners.RunListener;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

/**
 * Receives notifications about builds and publish messages on configured RabbitMQ server.
 *
 * @author Örjan Percy &lt;orjan.percy@sonymobile.com&gt;
 */
@Extension
public class RunListenerImpl extends RunListener<Run> {

    private static final String KEY_URL = "url";
    private static final String KEY_STATE = "state";
    private static final String KEY_CAUSES = "causes";
    private static final String VALUE_STARTED = "STARTED";
    private static final String VALUE_COMPLETED = "COMPLETED";
    private static final String VALUE_DELETED = "DELETED";
    private static final String KEY_STATUS = "status";
    private static final String CONTENT_TYPE = "application/json";
    private static RabbitMQProducerConfig config;

    /**
     * Constructor for RunListenerImpl.
     */
    public RunListenerImpl() {
        super(Run.class);
    }

    /**
     * Get the job url for use with the REST api.
     *
     * @param r The started build.
     * @return The url.
     *
     */
    private String getJobUrl(Run r) {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null && jenkins.getRootUrl() != null) {
            return Functions.joinPath(jenkins.getRootUrl(), r.getUrl());
        } else {
            return r.getUrl();
        }
    }

    @Override
    public void onStarted(Run r, TaskListener listener) {
        if (r instanceof AbstractBuild) {
            AbstractBuild<?, ?> build = (AbstractBuild<?, ?>)r;
            List<String> causes = new LinkedList<String>();
            for (Object o : build.getCauses()) {
                causes.add(o.getClass().getSimpleName());
            }

            Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause)r.getCause(Cause.UpstreamCause.class);
            if (upstreamCause != null) {
                causes.add(upstreamCause.getShortDescription());

                TopLevelItem item = Jenkins.getInstance().getItem(upstreamCause.getUpstreamProject());
                if (item != null && item instanceof MatrixProject) {
                    //Find the build
                    MatrixBuild mb = ((MatrixProject)item).getBuildByNumber(upstreamCause.getUpstreamBuild());
                    causes.add(mb.getUrl());
                }
            }
            Cause.RemoteCause remoteCause = (Cause.RemoteCause)r.getCause(Cause.RemoteCause.class);
            if (remoteCause != null) {
                causes.add(remoteCause.getShortDescription());
            }
            Cause.UserIdCause userIdCause = (Cause.UserIdCause)r.getCause(Cause.UserIdCause.class);
            if (userIdCause != null) {
                causes.add(userIdCause.getShortDescription());
            }

            JSONObject json = new JSONObject();
            json.put(KEY_STATE, VALUE_STARTED);
            json.put(KEY_URL, getJobUrl(r));
            json.put(KEY_CAUSES, causes.toString());
            publish(json);
        }
    }

    @Override
    public void onCompleted(Run r, TaskListener listener) {
        if (r instanceof AbstractBuild) {
            AbstractBuild<?, ?> build = (AbstractBuild<?, ?>)r;
            JSONObject json = new JSONObject();
            json.put(KEY_STATE, VALUE_COMPLETED);
            json.put(KEY_URL, getJobUrl(r));
            String status = "";
            Result res = build.getResult();
            if (res != null) {
                status = res.toString();
            }
            json.put(KEY_STATUS, status);
            publish(json);
        }
    }
    @Override
    public void onDeleted(Run r) {
        if (r instanceof AbstractBuild) {
            // Deleting a Job does not fire the RunListener.onDeleted event for its Runs
            // https://issues.jenkins-ci.org/browse/JENKINS-26708
            JSONObject json = new JSONObject();
            json.put(KEY_STATE, VALUE_DELETED);
            json.put(KEY_URL, getJobUrl(r));
            json.put(KEY_STATUS, VALUE_DELETED);
            publish(json);
        }
    }

    /**
     * Publish json message on configured RabbitMQ server.
     *
     * @param json the message in json format
     */
    private void publish(JSONObject json) {
        if (config == null) {
            config = RabbitMQProducerConfig.get();
        }
        if (config != null) {
            AMQP.BasicProperties.Builder bob = new AMQP.BasicProperties.Builder();
            int dm = 1;
            if (config.getPersistentDelivery()) {
                dm = 2;
            }
            bob.appId(config.getAppId());
            bob.deliveryMode(dm);
            bob.contentType(CONTENT_TYPE);
            RabbitMQConnection.getInstance().send(config.getExchangeName(), config.getRoutingKey(), bob.build(),
                    json.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
