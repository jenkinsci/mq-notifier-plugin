/*
 *  The MIT License
 *
 *  Copyright 2017 Axis Communications AB. All rights reserved.
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
package com.sonymobile.jenkins.plugins.mq.mqnotifier.providers;

import hudson.ExtensionPoint;
import hudson.model.Executor;
import hudson.model.Queue;
import hudson.model.Run;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import java.util.List;

/**
 * Provides data for the notifier to send.
 *
 * @author Tomas Westling &lt;tomas.westling@axis.com&gt;
 */
public abstract class MQDataProvider implements ExtensionPoint {

    /**
     * Provides data for when an item enters the queue.
     *
     * @param wi the {@link hudson.model.Queue.WaitingItem} that has entered the queue.
     * @param json the json object that we should add information to.
     */
    public void provideEnterWaitingQueueData(Queue.WaitingItem wi, JSONObject json) {
    }

    /**
     * Provides data for when an item leaves the queue.
     *
     * @param li the {@link hudson.model.Queue.LeftItem} that has left the queue.
     * @param json the json object that we should add information to.
     */
    public void provideLeftQueueData(Queue.LeftItem li, JSONObject json) {
    }

    /**
     * Provides data for when a Run starts.
     *
     * @param run the {@link hudson.model.Run} that has started running.
     * @param json the json object that we should add information to.
     */
    public void provideStartRunData(Run run, JSONObject json) {
    }

    /**
     * Provides data for when a Run is completed.
     *
     * @param run the {@link hudson.model.Run} that has completed running.
     * @param json the json object that we should add information to.
     */
    public void provideCompletedRunData(Run run, JSONObject json) {
    }

    /**
     *  Provides data for when a task is accepted
     *
     * @param executor The executor.
     * @param task The task.
     * @param json the json object that we should add information to.
     */
    public void provideTaskAcceptedData(Executor executor, Queue.Task task, JSONObject json) {
    }

    /**
     *  Provides data for when a task is started
     *
     * @param executor The executor.
     * @param task The task.
     * @param json the json object that we should add information to.
     */
    public void provideTaskStartedData(Executor executor, Queue.Task task, JSONObject json) {
    }

    /**
     *  Provides data for when a task is completed
     *
     * @param executor The executor.
     * @param task The task.
     * @param durationMS The number of milliseconds that the task took to complete.
     * @param json the json object that we should add information to.
     */
    public void provideTaskCompletedData(Executor executor, Queue.Task task, long durationMS, JSONObject json) {
    }

    /**
     *  Provides data for when a task is completed with problems
     *
     * @param executor The executor.
     * @param task The task.
     * @param durationMS The number of milliseconds that the task took to complete.
     * @param problems The exception that was thrown.
     * @param json the json object that we should add information to.
     */
    public void provideTaskCompletedWithProblemsData(Executor executor, Queue.Task task, long durationMS, Throwable problems, JSONObject json) {
    }

    /**
     * Returns all MQDataProvider for this Jenkins instance.
     * @return all the MQDataProviders.
     */
    public static List<MQDataProvider> all() {
        return Jenkins.getInstance().getExtensionList(MQDataProvider.class);
    }
}
