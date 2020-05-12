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

import com.sonymobile.jenkins.plugins.mq.mqnotifier.providers.MQDataProvider;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import net.sf.json.JSONObject;


/**
 * Receives notifications about builds and publish messages on configured MQ server.
 *
 * @author Örjan Percy &lt;orjan.percy@sonymobile.com&gt;
 */
@Extension
public class RunListenerImpl extends RunListener<Run> {
    /**
     * Constructor for RunListenerImpl.
     */
    public RunListenerImpl() {
        super(Run.class);
    }

    @Override
    public void onStarted(Run r, TaskListener listener) {
        JSONObject json = new JSONObject();
        json.put(Util.KEY_STATE, Util.VALUE_STARTED);
        json.put(Util.KEY_URL, Util.getJobUrl(r));
        json.put(Util.KEY_PROJECT_NAME, r.getParent().getFullName());
        json.put(Util.KEY_BUILD_NR, r.getNumber());
        json.put(Util.KEY_MASTER_FQDN, Util.getHostName());

        for (MQDataProvider mqDataProvider : MQDataProvider.all()) {
            mqDataProvider.provideStartRunData(r, json);
        }
        MQConnection.getInstance().publish(json);
    }

    @Override
    public void onCompleted(Run r, TaskListener listener) {
        if (r instanceof AbstractBuild) {
            onDone(r);
        }
    }

    @Override
    public void onFinalized(Run r) {
        if (!(r instanceof AbstractBuild)) {
            onDone(r);
        }
    }

    @Override
    public void onDeleted(Run r) {
        if (r instanceof AbstractBuild) {
            // Deleting a Job does not fire the RunListener.onDeleted event for its Runs
            // https://issues.jenkins-ci.org/browse/JENKINS-26708
            JSONObject json = new JSONObject();
            json.put(Util.KEY_STATE, Util.VALUE_DELETED);
            json.put(Util.KEY_URL, Util.getJobUrl(r));
            json.put(Util.KEY_PROJECT_NAME, r.getParent().getFullName());
            json.put(Util.KEY_BUILD_NR, r.getNumber());
            json.put(Util.KEY_MASTER_FQDN, Util.getHostName());
            json.put(Util.KEY_STATUS, Util.VALUE_DELETED);
            MQConnection.getInstance().publish(json);
        }
    }

    /**
     * Collect the relevant information from the Run and publish it.
     *
     * @param r The run
     */
    private void onDone(Run r) {
        JSONObject json = new JSONObject();
        json.put(Util.KEY_STATE, Util.VALUE_COMPLETED);
        json.put(Util.KEY_URL, Util.getJobUrl(r));
        json.put(Util.KEY_PROJECT_NAME, r.getParent().getFullName());
        json.put(Util.KEY_BUILD_NR, r.getNumber());
        json.put(Util.KEY_BUILD_DURATION, r.getDuration());
        json.put(Util.KEY_MASTER_FQDN, Util.getHostName());
        String status = "";
        Result res = r.getResult();
        if (res != null) {
            status = res.toString();
        }
        json.put(Util.KEY_STATUS, status);
        for (MQDataProvider mqDataProvider : MQDataProvider.all()) {
            mqDataProvider.provideCompletedRunData(r, json);
        }
        MQConnection.getInstance().publish(json);
    }
}
