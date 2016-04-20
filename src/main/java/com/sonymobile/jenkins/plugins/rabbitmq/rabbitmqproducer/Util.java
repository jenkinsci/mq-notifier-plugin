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
package com.sonymobile.jenkins.plugins.rabbitmq.rabbitmqproducer;

import hudson.Functions;
import hudson.model.Queue;
import hudson.model.Run;
import jenkins.model.Jenkins;

/**
 * Constants and helper functions.
 * @author Tomas Westling &lt;tomas.westling@sonymobile.com&gt;
 */
public final class Util {

    /**Url Key. */
    public static final String KEY_URL = "url";
    /**State Key. */
    public static final String KEY_STATE = "state";
    /**Causes Key. */
    public static final String KEY_CAUSES = "causes";
    /**Dequeue Reason Key. */
    public static final String KEY_DEQUEUE_REASON = "dequeue_reason";
    /**Status Key. */
    public static final String KEY_STATUS = "status";
    /**Queued Value. */
    public static final String VALUE_ADDED_TO_QUEUE = "QUEUED";
    /**Dequeued Value. */
    public static final String VALUE_REMOVED_FROM_QUEUE = "DEQUEUED";
    /**Cancelled Value. */
    public static final String VALUE_CANCELLED = "CANCELLED";
    /**Building Value. */
    public static final String VALUE_BUILDING = "BUILDING";
    /**Started Value. */
    public static final String VALUE_STARTED = "STARTED";
    /**Completed Value. */
    public static final String VALUE_COMPLETED = "COMPLETED";
    /**Deleted Value. */
    public static final String VALUE_DELETED = "DELETED";
    /**Content Type. */
    public static final String CONTENT_TYPE = "application/json";

    /**
     * Utility classes should not have a public or default constructor.
     */
    private Util() {
    }

    /**
     * Get the job url for use with the REST api.
     *
     * @param item The queue item.
     * @return The url.
     *
     */
    public static String getJobUrl(Queue.Item item) {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null && jenkins.getRootUrl() != null) {
            return Functions.joinPath(jenkins.getRootUrl(), item.task.getUrl());
        } else {
            return item.task.getUrl();
        }
    }

    /**
     * Get the job url for use with the REST api.
     *
     * @param r The started build.
     * @return The url.
     *
     */
    public static String getJobUrl(Run r) {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null && jenkins.getRootUrl() != null) {
            return Functions.joinPath(jenkins.getRootUrl(), r.getUrl());
        } else {
            return r.getUrl();
        }
    }
}
