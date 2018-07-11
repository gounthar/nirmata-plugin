
package io.jenkins.plugins.nirmata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;

public abstract class ActionBuilder extends AbstractDescribableImpl<ActionBuilder> implements ExtensionPoint {

    private static final Logger logger = LoggerFactory.getLogger(DeleteEnvAppBuilder.class);

    private final String endpoint;
    private final String apikey;

    public String getEndpoint() {
        return endpoint;
    }

    public String getApikey() {
        return apikey;
    }

    protected ActionBuilder(String endpoint, String apikey) {
        this.endpoint = endpoint;
        this.apikey = apikey;
    }

}