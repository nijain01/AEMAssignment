package aemAssignment.core.services;


import org.apache.jackrabbit.core.config.ConfigurationException;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
* OSGi Configuration for number of days for short text to appear
 */

@Component(
    immediate = true,
    name = "AEP Duration Configuration for Link Collection Component",
    configurationPid = "aemAssignment.core.services.AEPShortTextDurationPresetsConfiguration",
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    service = AEPShortTextDurationPresetsConfiguration.class
    //property ={
    // "service.description=Short Text Duration Configuration for AEP",
    //}
    )
@Designate(ocd = AEPShortTextDurationPresetsConfiguration.Configuration.class)
public class AEPShortTextDurationPresetsConfiguration {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private String shortTextDuration;

    /**
     * @throws ConfigurationException
     */
    @Activate
    public void activate(Configuration configuration) {
        log.info("Inside AEPShortTextDurationPresetsConfiguration");
        this.shortTextDuration = configuration.short_text_duration();
        log.info("short Text: " + this.shortTextDuration);
    }

    /**
     * @return shortTextDuration
     */
    public String getShortTextDuration() {
        return shortTextDuration;
    }

    @ObjectClassDefinition(name="AEP Short Text OSGi Configuration")
    public @interface Configuration {
        @AttributeDefinition(
            name = "Short Text Duration",
            description = "Short Text Duration to be added in days",
            type = AttributeType.STRING
        )
        String short_text_duration() default "1";
    }
}
