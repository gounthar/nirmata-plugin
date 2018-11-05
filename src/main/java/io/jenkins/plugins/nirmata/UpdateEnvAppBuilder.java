
package io.jenkins.plugins.nirmata;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.google.common.base.Strings;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.jenkins.plugins.nirmata.action.ActionType;
import io.jenkins.plugins.nirmata.model.Model;
import io.jenkins.plugins.nirmata.model.Status;
import io.jenkins.plugins.nirmata.util.NirmataClient;
import io.jenkins.plugins.nirmata.util.NirmataCredentials;
import jenkins.model.Jenkins;

public class UpdateEnvAppBuilder extends ActionBuilder {

    private final String _environment;
    private final String _application;
    private final Integer _timeout;
    private final String _directories;
    private final boolean _includescheck;
    private final String _includes;
    private final boolean _excludescheck;
    private final String _excludes;

    public String getEnvironment() {
        return _environment;
    }

    public String getApplication() {
        return _application;
    }

    public Integer getTimeout() {
        return _timeout;
    }

    public String getDirectories() {
        return _directories;
    }

    public boolean isIncludescheck() {
        return _includescheck;
    }

    public String getIncludes() {
        return _includes;
    }

    public boolean isExcludescheck() {
        return _excludescheck;
    }

    public String getExcludes() {
        return _excludes;
    }

    @DataBoundConstructor
    public UpdateEnvAppBuilder(String endpoint, String apikey, String environment, String application, Integer timeout,
        String directories, boolean includescheck, String includes, boolean excludescheck, String excludes) {
        super(endpoint, apikey);
        _environment = environment;
        _application = application;
        _timeout = timeout;
        _directories = directories;
        _includescheck = includescheck;
        _includes = includes;
        _excludescheck = excludescheck;
        _excludes = excludes;
    }

    @Symbol("updateAppInEnvironment")
    @Extension
    public static final class DescriptorImpl extends BuilderDescriptor {

        private static final NirmataCredentials credentials = new NirmataCredentials();
        private static List<Model> environments;

        public DescriptorImpl() {
            super(UpdateEnvAppBuilder.class, "Update App in Environment");
        }

        @Override
        public String getDisplayName() {
            return ActionType.UPDATE_ENV_APP.toString();
        }

        public FormValidation doCheckEndpoint(@QueryParameter String endpoint) {
            if (!Strings.isNullOrEmpty(endpoint)) {
                NirmataClient client = new NirmataClient(endpoint, null);
                Status status = client.getEnvironments().getStatus();

                return (status != null && status.getStatusCode() != HttpServletResponse.SC_UNAUTHORIZED)
                    ? FormValidation.error(String.format("%s (%s)", status.getMessage(), status.getStatusCode()))
                    : FormValidation.ok();
            } else {
                return FormValidation.warning("Endpoint is required");
            }
        }

        public FormValidation doCheckApikey(@QueryParameter String endpoint, @QueryParameter String apikey) {
            if (!Strings.isNullOrEmpty(apikey) && credentials.getCredential(apikey).isPresent()) {
                Optional<StringCredentials> credential = credentials.getCredential(apikey);
                NirmataClient client = new NirmataClient(endpoint, credential.get().getSecret().getPlainText());
                Status status = client.getEnvironments().getStatus();

                return (status != null && status.getStatusCode() != HttpServletResponse.SC_OK)
                    ? FormValidation.error(String.format("%s (%s)", status.getMessage(), status.getStatusCode()))
                    : FormValidation.ok();
            } else {
                return FormValidation.warning("API key is required");
            }
        }

        @SuppressWarnings("deprecation")
        public ListBoxModel doFillApikeyItems() {
            if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                return new ListBoxModel();
            }

            List<StringCredentials> stringCredentials = credentials.getCredentials();
            return new StandardListBoxModel().includeEmptyValue().withAll(stringCredentials);
        }

        public ListBoxModel doFillEnvironmentItems(@QueryParameter String endpoint, @QueryParameter String apikey) {
            ListBoxModel models = new ListBoxModel();
            if (Strings.isNullOrEmpty(endpoint) || Strings.isNullOrEmpty(apikey)) {
                return models;
            }

            Optional<StringCredentials> credential = credentials.getCredential(apikey);
            NirmataClient client = new NirmataClient(endpoint, credential.get().getSecret().getPlainText());
            environments = client.getEnvironments().getModel();
            Status status = client.getEnvironments().getStatus();

            if (status.getStatusCode() == HttpServletResponse.SC_OK) {
                if (environments != null && !environments.isEmpty()) {
                    models.add(new ListBoxModel.Option("Select environment", null, false));

                    for (Model model : environments) {
                        models.add(model.getName());
                    }
                } else {
                    models.add(new ListBoxModel.Option("--- No environments found ---", null, false));
                }
            }

            return models;
        }

        public ListBoxModel doFillApplicationItems(@QueryParameter String endpoint, @QueryParameter String apikey,
            @QueryParameter String environment) {
            ListBoxModel models = new ListBoxModel();

            if (Strings.isNullOrEmpty(endpoint) || Strings.isNullOrEmpty(apikey) || Strings.isNullOrEmpty(environment)
                || environments == null) {
                return models;
            }

            String environmentId = null;
            for (Model model : environments) {
                if (model.getName().equals(environment)) {
                    environmentId = model.getId();
                }
            }

            if (!Strings.isNullOrEmpty(environmentId)) {
                Optional<StringCredentials> credential = credentials.getCredential(apikey);
                NirmataClient client = new NirmataClient(endpoint, credential.get().getSecret().getPlainText());
                List<Model> applications = client.getAppsFromEnvironment(environmentId).getModel();
                Status status = client.getAppsFromEnvironment(environmentId).getStatus();

                if (status.getStatusCode() == HttpServletResponse.SC_OK) {
                    if (applications != null && !applications.isEmpty()) {
                        models.add(new ListBoxModel.Option("Select application", null, false));

                        for (Model model : applications) {
                            models.add(model.getRun());
                        }
                    } else {
                        models.add(new ListBoxModel.Option("--- No applications found ---", null, false));
                    }
                }
            }

            return models;
        }
    }

}