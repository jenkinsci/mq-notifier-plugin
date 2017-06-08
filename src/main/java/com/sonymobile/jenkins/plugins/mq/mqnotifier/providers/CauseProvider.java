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

import hudson.Extension;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Run;
import hudson.model.TopLevelItem;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

/**
 * Provides information about the causes for a build.
 *
 * @author Tomas Westling &lt;tomas.westling@axis.com&gt;
 */
@Extension
public class CauseProvider extends MQDataProvider {

    /**Causes Key. */
    public static final String KEY_CAUSES = "causes";

    @Override
    public void provideStartRunData(Run run, JSONObject json) {

        List<String> causes = new LinkedList<String>();
        for (Object o : run.getCauses()) {
            causes.add(o.getClass().getSimpleName());
        }

        CauseAction causeAction = run.getAction(CauseAction.class);
        if (causeAction != null) {
            for (Cause cause : causeAction.getCauses()) {
                if (cause instanceof Cause.UpstreamCause) {
                    Cause.UpstreamCause upstreamCause = (Cause.UpstreamCause)cause;
                    causes.add(upstreamCause.getShortDescription());
                    TopLevelItem item = Jenkins.getInstance().getItem(upstreamCause.getUpstreamProject());
                    if (item != null && item instanceof MatrixProject) {
                        //Find the build
                        MatrixBuild mb = ((MatrixProject)item).getBuildByNumber(upstreamCause.getUpstreamBuild());
                        causes.add(mb.getUrl());
                    }
                }
            }
        }
        Cause.RemoteCause remoteCause = (Cause.RemoteCause)run.getCause(Cause.RemoteCause.class);
        if (remoteCause != null) {
            causes.add(remoteCause.getShortDescription());
        }
        Cause.UserIdCause userIdCause = (Cause.UserIdCause)run.getCause(Cause.UserIdCause.class);
        if (userIdCause != null) {
            causes.add(userIdCause.getShortDescription());
        }
        json.put(KEY_CAUSES, causes.toString());
    }
}
