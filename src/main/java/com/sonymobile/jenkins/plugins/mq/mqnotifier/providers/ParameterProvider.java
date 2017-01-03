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
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Queue;
import hudson.model.Run;
import net.sf.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

/**
 * Provides the notifier with build parameters.
 */
@Extension
public class ParameterProvider extends MQDataProvider {

    /**Params Key. */
    public static final String KEY_PARAMETERS = "parameters";

    @Override
    public void provideStartRunData(Run run, JSONObject json) {
        addRunParametersToJSON(run, json);
    }

    @Override
    public void provideCompletedRunData(Run run, JSONObject json) {
        addRunParametersToJSON(run, json);
    }

    @Override
    public void provideEnterWaitingQueueData(Queue.WaitingItem wi, JSONObject json) {
        addQueueParametersToJson(wi, json);
    }

    @Override
    public void provideLeftQueueData(Queue.LeftItem li, JSONObject json) {
        super.provideLeftQueueData(li, json);
    }

    /**
     * Adds parameters for a Run.
     * @param run the Run we are getting parameters from.
     * @param json the JSON object to add data to.
     */
    private void addRunParametersToJSON(Run run, JSONObject json) {
        List<String> parameters = new LinkedList<String>();
        ParametersAction parametersAction = run.getAction(ParametersAction.class);
        if (parametersAction != null) {
            List<ParameterValue> parameterValues = parametersAction.getParameters();
            if (parameterValues != null) {
                for (ParameterValue parameterValue : parameterValues) {
                    parameters.add(parameterValue.getName() + "=" + parameterValue.getValue());
                }
            }
        }
        json.put(KEY_PARAMETERS, parameters);
    }

    /**
     * Adds parameters for a {@link Queue.Item}s
     * @param item the Item we are getting parameters from.
     * @param json the JSON object to add data to.
     */
    private void addQueueParametersToJson(Queue.Item item, JSONObject json) {
        String[] parametersArray = new String[0];
        String parameters = item.getParams();
        if (parameters.length() > 0) {
            parametersArray = parameters.substring(1).split("\n");   // Remove leading '\n'.
        }
        json.put(KEY_PARAMETERS, parametersArray);
    }
}
