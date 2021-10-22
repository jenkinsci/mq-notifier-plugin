## Change Log

### Version 1.2.8 (released Nov 16, 2018)

Pipeline Step to allow publishing of arbitrary messages

### Version 1.2.7 (released Sep 25, 2018)

[Fix security issue](https://jenkins.io/security/advisory/2018-09-25/#SECURITY-972)

### Version 1.2.6 (released Mar 06, 2018)

Refactoring some code to easier allow other plugins to easier send
standalone messages to RabbitMQ.

### Version 1.2.5 (released Mar 06, 2018)
   
Add an option to disable/enable the plugin (Thanks Huaxing Sun)

**Please note:** After upgrade and initial restart the plugin will not
be enabled. Go to Configure Jenkins -\> Enable Notifier in the 'MQ
Notifier' section.

### Version 1.2.4 (released Oct 02, 2017)

Add "Build Duration" time to the published AMQP event.

### Version 1.2.3 (released June 08, 2017)

Added a null check to fix NullPointerExceptions in the CauseProvider

### Version 1.2.2 (released May 22, 2017)

- Collect additional build and host data (project names, jenkins master fqdn)  
- Collect data about time spent in queue and assigned label

### Version 1.2.1 (released Feb 16, 2017)

Added an extension point for providing data to the notifier

### Version 1.2.0 (released Dec 22, 2016)

Support for pipeline projects

### Version 1.1.5 (released Dec 09, 2016)

- Added internal queue for when connection to MQ is down.  
- Several fixes regarding saving of the configuration.  
- Build parameters added to all of the existing messages.

### Version 1.0 (released May 18, 2016)

Initial release