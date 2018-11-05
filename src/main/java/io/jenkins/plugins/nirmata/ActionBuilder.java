
package io.jenkins.plugins.nirmata;

import java.io.Serializable;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;

public abstract class ActionBuilder extends AbstractDescribableImpl<ActionBuilder>
    implements ExtensionPoint, Serializable {

    private static final long serialVersionUID = 9092451084647459564L;
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