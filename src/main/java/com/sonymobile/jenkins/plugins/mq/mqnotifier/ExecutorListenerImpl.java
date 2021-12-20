/*
 *  The MIT License
 *
 *  Copyright 2021 Axis Communications AB. All rights reserved.
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

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Executor;
import hudson.model.ExecutorListener;
import hudson.model.Queue;
import net.sf.json.JSONObject;
import org.slf4j.LoggerFactory;

/**
 * A listener for task related events from executors.
 */
@Extension
public class ExecutorListenerImpl implements ExecutorListener {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ExecutorListenerImpl.class);

    /**
     * Populates the json with common data for Executor and Task items.
     *
     * @param json The resulting JSONObject
     * @param e    executor
     * @param t    task
     */
    public void populateCommon(JSONObject json, Executor e, Queue.Task t) {
        json.put(Util.EXECUTOR_TYPE, e.getClass().getSimpleName());
        json.put(Util.EXECUTOR_NAME, e.getName());
        FilePath workspace = e.getCurrentWorkspace();
        json.put(Util.EXECUTOR_WORKSPACE, workspace != null ? workspace.getRemote() : "NO_WORKSPACE");

        json.put(Util.ELAPSED_TIME, e.getElapsedTime());
        json.put(Util.IDLE_START, e.getIdleStartMilliseconds());
        json.put(Util.KEY_DEQUEUE_TIME_SPENT, e.getTimeSpentInQueue());
        json.put(Util.EXECUTOR_OWNER, e.getOwner().getName());

        json.put(Util.TASK_NAME, t.getName());
        json.put(Util.KEY_DEQUEUE_ALLOCATED_LABEL,
                t.getAssignedLabel() != null ? t.getAssignedLabel().getDisplayName() : Util.VALUE_DEQUEUE_NO_LABEL);

        json.put(Util.TASK_URL, Util.getTaskUrl(t));
        json.put(Util.TASK_IS_CONCURRENT, t.isConcurrentBuild());
        json.put(Util.TASK_OWNER_NAME, t.getOwnerTask().getDisplayName());
        json.put(Util.TASK_OWNER_URL, Util.getTaskUrl(t.getOwnerTask()));

        json.put(Util.KEY_PROJECT_NAME, Util.getFullName(t));
        json.put(Util.KEY_MASTER_FQDN, Util.getHostName());

        json.put(Util.LISTENER_TYPE, "executor");
    }

    @Override
    public void taskStarted(Executor executor, Queue.Task task) {
        LOGGER.debug("taskStarted");
        JSONObject json = new JSONObject();
        populateCommon(json, executor, task);
        json.put(Util.KEY_STATE, "TASK_STARTED");
        MQConnection.getInstance().publish(json);
    }

    @Override
    public void taskAccepted(Executor executor, Queue.Task task) {
        LOGGER.debug("taskAccepted");
        JSONObject json = new JSONObject();
        populateCommon(json, executor, task);
        json.put(Util.KEY_STATE, "TASK_ACCEPTED");
        MQConnection.getInstance().publish(json);
    }

    @Override
    public void taskCompleted(Executor executor, Queue.Task task, long durationMS) {
        LOGGER.debug("taskCompleted");
        JSONObject json = new JSONObject();
        populateCommon(json, executor, task);
        json.put(Util.KEY_STATE, "TASK_COMPLETED");
        json.put(Util.TASK_DURATION, durationMS);
        MQConnection.getInstance().publish(json);
    }

    @Override
    public void taskCompletedWithProblems(Executor executor, Queue.Task task, long durationMS, Throwable problems) {
        LOGGER.debug("taskCompletedWithProblems");
        JSONObject json = new JSONObject();
        populateCommon(json, executor, task);
        json.put("state", "TASK_COMPLETED");
        json.put(Util.TASK_DURATION, durationMS);
        json.put(Util.PROBLEMS, problems.getMessage());
        MQConnection.getInstance().publish(json);
    }
}
