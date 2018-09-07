
package io.jenkins.plugins.nirmata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;

public abstract class ActionBuilder extends AbstractDescribableImpl<ActionBuilder> implements ExtensionPoint {

    private static final Logger logger = LoggerFactory.getLogger(ActionBuilder.class);

    private final String _endpoint;
    private final String _apikey;

    public String getEndpoint() {
        return _endpoint;
    }

    public String getApikey() {
        return _apikey;
    }

    protected ActionBuilder(String endpoint, String apikey) {
        _endpoint = endpoint;
        _apikey = apikey;
    }

}