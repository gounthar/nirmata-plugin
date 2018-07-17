
package io.jenkins.plugins.nirmata;

import hudson.model.Descriptor;

public class BuilderDescriptor extends Descriptor<ActionBuilder> {

    private transient String _displayName;

    public BuilderDescriptor(Class<? extends ActionBuilder> clazz, String displayName) {
        super(clazz);
        _displayName = displayName;
    }

    @Override
    public String getDisplayName() {
        return _displayName;
    }

    @Override
    public String getConfigPage() {
        if (!getClass().equals(BuilderDescriptor.class)) {
            return super.getConfigPage();
        }
        return getViewPage(BuilderDescriptor.class, "config.jelly");
    }
}