/*
 *  The MIT License
 *
 *  Copyright 2016 Sony Mobile Communications Inc. All rights reserved.
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

import com.sonymobile.jenkins.plugins.mq.mqnotifier.providers.MQDataProvider;
import hudson.Extension;
import hudson.model.Label;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;
import net.sf.json.JSONObject;

/**
 * Receives notifications about when tasks are submitted to the queue and publishes
 * messages on configured MQ server.
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 */
@Extension
public class QueueListenerImpl extends QueueListener {
    /**
     * Populates the json with common data for Queue items.
     *
     * @param json The resulting JSONObject
     * @param i queue item
     */
    public void populateCommon(JSONObject json, Queue.Item i) {
        json.put(Util.KEY_URL, Util.getJobUrl(i));
        json.put(Util.KEY_PROJECT_NAME, Util.getFullName(i.task));
        json.put(Util.KEY_MASTER_FQDN, Util.getHostName());

        Label assignedLabel = i.getAssignedLabel();
        json.put(Util.KEY_DEQUEUE_ALLOCATED_LABEL,
                assignedLabel != null ? assignedLabel.getDisplayName() : Util.VALUE_DEQUEUE_NO_LABEL);
        json.put(Util.LISTENER_TYPE, "queue");
    }

    @Override
    public void onEnterWaiting(Queue.WaitingItem wi) {
        JSONObject json = new JSONObject();
        json.put(Util.KEY_STATE, Util.VALUE_ADDED_TO_QUEUE);
        populateCommon(json, wi);
        for (MQDataProvider mqDataProvider : MQDataProvider.all()) {
            mqDataProvider.provideEnterWaitingQueueData(wi, json);
        }
        MQConnection.getInstance().publish(json);
    }

    @Override
    public void onLeft(Queue.LeftItem li) {
        JSONObject json = new JSONObject();
        json.put(Util.KEY_STATE, Util.VALUE_REMOVED_FROM_QUEUE);
        if (li.isCancelled()) {
            json.put(Util.KEY_DEQUEUE_REASON, Util.VALUE_CANCELLED);
        } else {
            json.put(Util.KEY_DEQUEUE_REASON, Util.VALUE_BUILDING);
            json.put(Util.KEY_DEQUEUE_TIME_SPENT, System.currentTimeMillis() - li.getInQueueSince());
        }
        populateCommon(json, li);

        for (MQDataProvider mqDataProvider : MQDataProvider.all()) {
            mqDataProvider.provideLeftQueueData(li, json);
        }
        MQConnection.getInstance().publish(json);
    }
}
