
package io.jenkins.plugins.nirmata;

import java.io.IOException;
import java.util.Optional;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import io.jenkins.plugins.nirmata.action.Action;
import io.jenkins.plugins.nirmata.util.NirmataClient;
import io.jenkins.plugins.nirmata.util.NirmataCredentials;
import jenkins.tasks.SimpleBuildStep;

public class NirmataPlugin extends Builder implements SimpleBuildStep {

    private static final Logger logger = LoggerFactory.getLogger(NirmataPlugin.class);

    private final ActionBuilder _builder;

    public ActionBuilder getBuilder() {
        return _builder;
    }

    @DataBoundConstructor
    public NirmataPlugin(ActionBuilder builder) {
        _builder = builder;
    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
        throws InterruptedException, IOException {
        NirmataCredentials credentials = new NirmataCredentials();
        Optional<StringCredentials> credential = credentials.getCredential(_builder.getApikey());
        NirmataClient client = new NirmataClient(_builder.getEndpoint(), credential.get().getSecret().getPlainText());

        Action action = new Action(client, workspace, listener);
        action.buildStep(_builder);
    }

    @Symbol("nirmata")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.NirmataPlugin_DescriptorImpl_DisplayName();
        }

    }

}