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
package com.sonymobile.jenkins.plugins.mq.mqnotifier.MQNotifierConfig;

def f = namespace("/lib/form")
def l = "/plugin/mq-notifier/"

f.section(title: "MQ Notifier") {

    f.entry(title: "Enable Notifier", help: l+"help-enable-notifier.html") {
        f.checkbox(field: "enableNotifier", checked: instance.enableNotifier)
    }
    f.entry(title: "MQ URI", field: "serverUri", help: l+"help-amqp-uri.html") {
        f.textbox("value":instance.serverUri)
    }
    f.entry(title: "User name", field: "userName", help: l+"help-user-name.html") {
        f.textbox("value":instance.userName)
    }
    f.entry(title: "Password", field: "userPassword", help: l+"help-user-password.html") {
        f.password("value":instance.userPassword)
    }
    descriptor = instance.descriptor
    f.validateButton(title: "Test Connection", progress: "Trying to connect...", method: "testConnection",
            with: "serverUri,userName,userPassword")

    f.entry(title: "Exchange Name", field: "exchangeName", help: l+"help-exchange-name.html") {
        f.textbox("value":instance.exchangeName)
    }
    f.entry(title: "Virtual host", field: "virtualHost", help: l+"help-virtual-host.html") {
        f.textbox("value":instance.virtualHost)
    }
    f.entry(title: "Routing Key", field: "routingKey", help: l+"help-routing-key.html") {
        f.textbox("value":instance.routingKey)
    }
    f.entry(title: "Application Id", field: "appId", help: l+"help-application-id.html") {
        f.textbox("value":instance.appId)
    }
    f.entry(title: "Persistent Delivery mode", help: l+"help-persistent-delivery.html") {
        f.checkbox(field: "persistentDelivery", checked: instance.persistentDelivery)
    }
    f.entry(title: "Enable verbose logging", help: l+"help-enable-verbose-logging.html") {
        f.checkbox(field: "enableVerboseLogging", checked: instance.enableVerboseLogging)
    }
}
