package com.sonymobile.jenkins.plugins.mq.mqnotifier.pipeline.MQMessageStep

import lib.FormTagLib

def f = namespace(FormTagLib)

f.entry(field: 'json',
        title: 'JSON Message',
        description: 'JSON Message to be sent to the RabbitMQ server.') {
    f.textbox()
}