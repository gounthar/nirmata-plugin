
package io.jenkins.plugins.nirmata;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import io.jenkins.plugins.nirmata.action.Action;
import io.jenkins.plugins.nirmata.util.NirmataClient;
import io.jenkins.plugins.nirmata.util.NirmataCredentials;
import jenkins.security.MasterToSlaveCallable;
import jenkins.tasks.SimpleBuildStep;

public class NirmataPlugin extends Builder implements SimpleBuildStep, Serializable {

    private static final long serialVersionUID = 6253017462895236976L;
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
        if (_builder != null) {
            NirmataCredentials credentials = new NirmataCredentials();
            Optional<StringCredentials> credential = credentials.getCredential(_builder.getApikey());
            String apiKey = credential.get().getSecret().getPlainText();

            if (workspace != null && listener != null && apiKey != null) {
                Result result = launcher.getChannel().call(new ExecuteAction(_builder, workspace, listener, apiKey));
                if (result != Result.SUCCESS) {
                    run.setResult(result);
                }

                return;
            }
        }

        throw new AbortException("Unable to execute task with NULL parameters!");
    }

    @Symbol("nirmata")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @SuppressWarnings("rawtypes")
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.NirmataPlugin_DescriptorImpl_DisplayName();
        }

    }

    private static class ExecuteAction extends MasterToSlaveCallable<Result, AbortException> {

        private static final long serialVersionUID = -8107559201979285317L;
        private final ActionBuilder _builder;
        private final FilePath _workspace;
        private final TaskListener _listener;
        private final String _apiKey;

        public ExecuteAction(ActionBuilder builder, FilePath workspace, TaskListener listener, String apiKey) {
            _builder = builder;
            _workspace = workspace;
            _listener = listener;
            _apiKey = apiKey;
        }

        @Override
        public Result call() throws AbortException {
            NirmataClient client = new NirmataClient(_builder.getEndpoint(), _apiKey);
            Action action = new Action(client, _workspace, _listener);

            return action.buildStep(_builder);
        }
    }
}