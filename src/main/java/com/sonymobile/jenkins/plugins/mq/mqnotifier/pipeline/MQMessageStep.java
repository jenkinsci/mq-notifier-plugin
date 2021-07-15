/*
 *  The MIT License
 *
 *  Copyright 2018 Axis Communications AB
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
package com.sonymobile.jenkins.plugins.mq.mqnotifier.pipeline;

import com.sonymobile.jenkins.plugins.mq.mqnotifier.MQConnection;
import com.sonymobile.jenkins.plugins.mq.mqnotifier.MQNotifierConfig;
import hudson.Extension;
import hudson.model.TaskListener;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * Pipeline step to allowing publication of a MQ message.
 */
public class MQMessageStep extends Step {
    private final String json;

    /**
     * DataBoundConstructor.
     *
     * @param json mq message payload
     */
    @DataBoundConstructor
    public MQMessageStep(String json) {
        this.json = json;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(this, context);
    }

    /**
     * @return the json payload
     */
    public String getJson() {
        return json;
    }

    /**
     * Simple synchronous step execution.
     */
    private static class Execution extends SynchronousStepExecution<Void> {

        private transient MQMessageStep step;
        private static final long serialVersionUID = 1L;

        /**
         * Execution Constructor
         *
         * @param step    step
         * @param context the step context
         */
        protected Execution(@Nonnull MQMessageStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            TaskListener listener = getContext().get(TaskListener.class);
            MQNotifierConfig config = MQNotifierConfig.getInstance();

            JSONObject json;
            try {
                json = JSONObject.fromObject(step.getJson());
            } catch (JSONException jsonException) {
                listener.error("Not correct JSON: " + step.getJson());
                throw jsonException;
            }
            // It would be preferable if we could wait for the message to be sent to RabbitMQ and let the
            // user know in the build log if it could not be published for whatever reason. But every
            // message is put on a queue to be sent at a later point in time. Preferably we would be able
            // to get a Future<> back so that we could wait if we wanted. But that's not how the MQ
            // Notifier is built.
            if (config.getEnableVerboseLogging()) {
                listener.getLogger().println("Posting JSON message to RabbitMQ:\n" + json.toString(2));
            }
            MQConnection.getInstance().publish(json);
            return null;
        }
    }

    /**
     * Standard Descriptor.
     */
    @Extension
    public static class Descriptor extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.emptySet();
        }

        @Override
        public String getFunctionName() {
            return "publishMQMessage";
        }

        @Override
        public String getDisplayName() {
            return "Publish MQ Message";
        }
    }
}
