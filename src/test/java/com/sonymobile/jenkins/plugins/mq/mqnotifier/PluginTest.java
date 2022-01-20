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

import hudson.matrix.Axis;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixProject;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.TextParameterDefinition;
import hudson.slaves.DumbSlave;
import org.hamcrest.Matchers;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

//CS IGNORE Check FOR NEXT 100 LINES. REASON: TestData

/**
 * Tests for MQ notifier plugin.
 *
 * @author Örjan Percy &lt;orjan.percy@sonymobile.com&gt;
 */
public class PluginTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private static final String EXCHANGE = "logs";
    private static final String ROUTING = "routingkey";
    private static final String URI = "amqp://localhost";
    private static final String MESSAGE = "secret message";

    /**
     * Called before class is setup.
     *
     * @throws Exception thrown
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        new Mocks.RabbitMQConnectionMock();
        new Mocks.RunListenerImplMock();
    }

    /**
     * Called before test is run.
     *
     * @throws Exception thrown
     */
    @Before
    public void setUp() throws Exception {
        Mocks.MESSAGES.clear();
        Mocks.COMPLETED.clear();
        Mocks.STARTED.clear();
    }

    /**
     * Test invalid connection by sending a message.
     */
    @Test
    public void testInvalidConnection() {
        MQConnection conn = MQConnection.getInstance();
        MQNotifierConfig config = MQNotifierConfig.getInstance();
        assertNotNull("No config available: MQNotifierConfig", config);
        config.setExchangeName(EXCHANGE);
        config.setServerUri(URI);
        config.setRoutingKey(ROUTING);
        config.setVirtualHost(null);
        config.setEnableNotifier(false);
        config.setEnableVerboseLogging(false);

        if (config != null && config.getEnableNotifier()) {
            conn.initialize(
                    config.getUserName(),
                    config.getUserPassword(),
                    config.getServerUri(),
                    config.getVirtualHost()
            );
            String message = new String(MESSAGE);

            conn.addMessageToQueue(config.getExchangeName(), config.getRoutingKey(), null, message.getBytes());
        }
        assertEquals("Unmatched number of messages", 0, Mocks.MESSAGES.size());
    }

    /**
     * Test valid connection by sending a message.
     */
    @Test
    public void testConnection() {
        MQConnection conn = MQConnection.getInstance();
        MQNotifierConfig config = MQNotifierConfig.getInstance();
        assertNotNull("No config available: MQNotifierConfig", config);
        config.setExchangeName(EXCHANGE);
        config.setServerUri(URI);
        config.setRoutingKey(ROUTING);
        config.setVirtualHost(null);
        config.setEnableNotifier(true);

        if (config != null && config.getEnableNotifier()) {
            conn.initialize(
                    config.getUserName(),
                    config.getUserPassword(),
                    config.getServerUri(),
                    config.getVirtualHost()
            );
            String message = new String(MESSAGE);

            conn.addMessageToQueue(config.getExchangeName(), config.getRoutingKey(), null, message.getBytes());
        }
        assertEquals("Unmatched number of messages", 1, Mocks.MESSAGES.size());
        assertThat("Unmatched message contents", Mocks.MESSAGES.get(0), is(MESSAGE));
    }

    /**
     * Test that building a project generates the intended build messages.
     *
     * @throws Exception thrown
     */
    @Test
    public void testBuildProject() throws Exception {
        String name = "qpwoeiruty";
        DumbSlave slave = j.createOnlineSlave();
        FreeStyleProject project = j.createFreeStyleProject(name);
        project.setAssignedNode(slave);
        j.buildAndAssertSuccess(project);

        assertEquals("Unmatched number", 1, Mocks.STARTED.size());
        assertEquals("Unmatched number", 1, Mocks.COMPLETED.size());
        assertThat(Mocks.STARTED.get(0), containsString(name));
        assertThat(Mocks.COMPLETED.get(0), containsString(name));
    }

    /**
     * Test that building two projects generates the intended build messages.
     *
     * @throws Exception thrown
     */
    @Test
    public void testBuildProject2() throws Exception {
        String name1 = "adsddlfkjgh";
        String name2 = "zmnxbcv";
        DumbSlave slave = j.createOnlineSlave();
        FreeStyleProject p1 = j.createFreeStyleProject(name1);
        FreeStyleProject p2 = j.createFreeStyleProject(name2);
        p1.setAssignedNode(slave);
        p2.setAssignedNode(slave);
        j.buildAndAssertSuccess(p1);
        j.buildAndAssertSuccess(p2);
        assertEquals("Unmatched number", 2, Mocks.STARTED.size());
        assertEquals("Unmatched number", 2, Mocks.COMPLETED.size());
        assertThat(Mocks.STARTED.get(0), containsString(name1));
        assertThat(Mocks.STARTED.get(1), containsString(name2));
        assertThat(Mocks.COMPLETED.get(0), containsString(name1));
        assertThat(Mocks.COMPLETED.get(1), containsString(name2));
    }

    /**
     * Test that building a matrix project generates the intended build messages.
     *
     * @throws Exception thrown
     */
    @Test
    public void testMatrixProject() throws Exception {
        String label = "label";
        String axis1 = "one";
        String axis2 = "two";
        MatrixProject project = j.createProject(MatrixProject.class);
        Axis axis = new Axis(label, axis1, axis2);
        project.setAxes(new AxisList(axis));

        DumbSlave slave = j.createOnlineSlave();
        project.setAssignedNode(slave);
        j.buildAndAssertSuccess(project);
        assertThat(Mocks.STARTED.toString(), containsString(axis1));
        assertThat(Mocks.STARTED.toString(), containsString(axis2));
        assertThat(Mocks.COMPLETED.toString(), containsString(axis1));
        assertThat(Mocks.COMPLETED.toString(), containsString(axis2));
    }

    /**
     * Tests that the publishMQMessage is registered and puts messages in the queue.
     *
     * @throws Exception thrown
     */
    @Test
    public void testPipelineStep() throws Exception {
        MQNotifierConfig config = MQNotifierConfig.getInstance();
        config.setEnableNotifier(true);

        String message = "{\"key\":\"value\"}";

        WorkflowJob job = j.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("publishMQMessage '" + message + "'", true));

        j.buildAndAssertSuccess(job);
        assertThat(Mocks.MESSAGES, Matchers.hasItem(message));
    }

    /**
     * Test that publishMQMessage correctly logs the custom message when verbose logging is on.
     *
     * @throws Exception thrown
     */
    @Test
    public void testPipelineStepLogsMessage() throws Exception {
        MQNotifierConfig config = MQNotifierConfig.getInstance();
        config.setEnableNotifier(true);
        config.setEnableVerboseLogging(true);

        String message = "{\"key\":\"value\"}";

        WorkflowJob job = j.createProject(WorkflowJob.class);
        job.setDefinition(new CpsFlowDefinition("publishMQMessage '" + message + "'", true));

        j.buildAndAssertSuccess(job);
        j.assertLogContains(
                "Posting JSON message to RabbitMQ:\n{\"key\": \"value\"}",
                job.getLastCompletedBuild()
        );
    }

    /**
     * Ensures that multi-line parameters are represented as a single parameter
     * in the MQ message.
     *
     * @throws Exception thrown
     */
    @Test
    public void testMultilineParameterInMessage() throws Exception {
        MQNotifierConfig config = MQNotifierConfig.getInstance();
        config.setEnableNotifier(true);

        FreeStyleProject p = j.createFreeStyleProject();
        p.addProperty(new ParametersDefinitionProperty(
                new TextParameterDefinition("FOO", "BAR\nBAZ", "Multiline parameter")));

        j.buildAndAssertSuccess(p);

        // Ensure that the parameter is a single entry:
        assertThat(Mocks.MESSAGES.get(0), containsString("\"FOO=BAR\\nBAZ\""));
    }

}
