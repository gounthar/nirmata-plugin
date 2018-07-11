
package io.jenkins.plugins.nirmata;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(UpdateEnvAppBuilder.class);

    private final String environment;
    private final String application;
    private final String timeout;
    private final String directories;
    private final boolean includescheck;
    private final String includes;
    private final boolean excludescheck;
    private final String excludes;

    public String getEnvironment() {
        return environment;
    }

    public String getApplication() {
        return application;
    }

    public String getTimeout() {
        return timeout;
    }

    public String getDirectories() {
        return directories;
    }

    public boolean isIncludescheck() {
        return includescheck;
    }

    public String getIncludes() {
        return includes;
    }

    public boolean isExcludescheck() {
        return excludescheck;
    }

    public String getExcludes() {
        return excludes;
    }

    @DataBoundConstructor
    public UpdateEnvAppBuilder(String endpoint, String apikey, String environment, String application, String timeout,
        String directories, boolean includescheck, String includes, boolean excludescheck, String excludes) {
        super(endpoint, apikey);
        this.environment = environment;
        this.application = application;
        this.timeout = timeout;
        this.directories = directories;
        this.includescheck = includescheck;
        this.includes = includes;
        this.excludescheck = excludescheck;
        this.excludes = excludes;
    }

    @Symbol("Update App in Environment")
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

        public FormValidation doCheckTimeout(@QueryParameter int timeout) {
            return timeout >= 0 && timeout <= 20 ? FormValidation.ok()
                : FormValidation.error("Timeout cannot be less than 0 or greater than 20 mins");
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
                if (environments != null) {
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
                    if (!(applications == null || applications.isEmpty())) {
                        for (Model model : applications) {
                            models.add(model.getName());
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