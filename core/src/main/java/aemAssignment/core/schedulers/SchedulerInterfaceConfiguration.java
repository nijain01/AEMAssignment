package aemAssignment.core.schedulers;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/*
* OSGi configuration for Scheduler for Sending Mails for Pending Action Items
 */

@ObjectClassDefinition(name="Pending Actions Mail Scheduler Configuration")
public @interface SchedulerInterfaceConfiguration {
    @AttributeDefinition(
        name = "Enabled",
        description = "Enable/Disable the Scheduled Service",
        type = AttributeType.BOOLEAN
    )
    boolean service_enabled() default true;

    @AttributeDefinition(
        name = "Cron expression defining when this Scheduled Service will run",
        description = "[every minute = 0 * * * * ?], [12:01am daily = 0 1 0 ? * *]; see www.cronmaker.com",
        type = AttributeType.STRING
    )
    String scheduler_expression() default "0 * * * * ?";

    @AttributeDefinition(
        name = "Template Path",
        description = "Path of the Email Template",
        type = AttributeType.STRING
    )
    String templatePath() default "/etc/notification/aep/email/pending-action-items.txt";

    @AttributeDefinition(
        name = "From Address",
        description = "Email Address from which mail needs to send to users",
        type = AttributeType.STRING
    )
    String fromAddress() default "no-reply@test.com";

    @AttributeDefinition(
        name = "Context PAth",
        description = "Context Path under which the CQ/Sling launchpad webapp is running",
        type = AttributeType.STRING
    )
    String context_path() default "http://localhost:4502";

    @AttributeDefinition(
        name = "Pending Duration",
        description = "Days after which reminder mail needs to send to user if action is not taken after assignment",
        type = AttributeType.STRING
    )
    String pendingDuration() default "1";

    @AttributeDefinition(
        name = "Context Expiration Duration",
        description = "Days before which reminder mail needs to send to user reminding that content is due for expiration(off time) within X days",
        type = AttributeType.STRING
    )
    String contentExpirationDuration() default "100";
}