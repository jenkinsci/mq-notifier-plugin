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
import hudson.model.Run;
import hudson.model.TaskListener;
import mockit.Mock;
import mockit.MockUp;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

//CS IGNORE Javadoc FOR NEXT 50 LINES. REASON: TestData

/**
 * Test mocks.
 *
 * @author Örjan Percy &lt;orjan.percy@sonymobile.com&gt;
 */
public final class Mocks {

    /**
     * Store received info.
     */
    public static final List<String> MESSAGES = new CopyOnWriteArrayList<String>();
    public static final List<String> STARTED = new CopyOnWriteArrayList<String>();
    public static final List<String> COMPLETED = new CopyOnWriteArrayList<String>();

    // private constructor to avoid unnecessary instantiation of the class
    private Mocks() { }

    /**
     * Mock the connection.
     */
    public static final class RabbitMQConnectionMock extends MockUp<RabbitMQConnection> {

        @Mock
        public void send(String exchangeName, String routingKey, AMQP.BasicProperties props, byte[] body) {
            String str = new String(body);
            MESSAGES.add(str);
        }
    }

    /**
     * Mock the RunListenerImpl.
     */
    public static final class RunListenerImplMock extends MockUp<RunListenerImpl> {
        @Mock
        public void onStarted(Run r, TaskListener listener) {
            STARTED.add(r.toString());
        }

        @Mock
        public void onCompleted(Run r, TaskListener listener) {
            COMPLETED.add(r.toString());
        }
    }
}
