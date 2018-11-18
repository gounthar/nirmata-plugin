
package io.jenkins.plugins.nirmata;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.*;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.google.common.base.Strings;

import hudson.Extension;
import hudson.model.Item;
import hudson.util.*;
import io.jenkins.plugins.nirmata.action.ActionType;
import io.jenkins.plugins.nirmata.model.Model;
import io.jenkins.plugins.nirmata.model.Status;
import io.jenkins.plugins.nirmata.util.NirmataClient;
import io.jenkins.plugins.nirmata.util.NirmataCredentials;
import jenkins.model.Jenkins;

public class DeleteEnvAppBuilder extends ActionBuilder {

    private final String _environment;
    private final String _application;
    private final Integer _timeout;

    public String getEnvironment() {
        return _environment;
    }

    public String getApplication() {
        return _application;
    }

    public Integer getTimeout() {
        return _timeout;
    }

    @DataBoundConstructor
    public DeleteEnvAppBuilder(String endpoint, String apikey, String environment, String application,
        Integer timeout) {
        super(endpoint, apikey);
        _environment = environment;
        _application = application;
        _timeout = timeout;
    }

    @Symbol("deleteAppInEnvironment")
    @Extension
    public static final class DescriptorImpl extends BuilderDescriptor {

        private NirmataCredentials credentials;
        private List<Model> environments;

        public DescriptorImpl() {
            super(DeleteEnvAppBuilder.class, "Delete App in Environment");
        }

        @Override
        public String getDisplayName() {
            return ActionType.DELETE_ENV_APP.toString();
        }

        public FormValidation doCheckEndpoint(@AncestorInPath Item project, @QueryParameter String endpoint) {
            credentials = new NirmataCredentials(project);

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

        public FormValidation doCheckApikey(@AncestorInPath Item project, @QueryParameter String endpoint,
            @QueryParameter String apikey) {
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
        public ListBoxModel doFillApikeyItems(@AncestorInPath Item project, @QueryParameter String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();
            if (project == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if (!project.hasPermission(Item.EXTENDED_READ)
                    && !project.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }

            List<StringCredentials> stringCredentials = credentials.getCredentials();
            return result.includeEmptyValue().withAll(stringCredentials).includeCurrentValue(credentialsId);
        }

        public ListBoxModel doFillEnvironmentItems(@AncestorInPath Item project, @QueryParameter String endpoint,
            @QueryParameter String apikey) {
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

        public ComboBoxModel doFillApplicationItems(@AncestorInPath Item project, @QueryParameter String endpoint,
            @QueryParameter String apikey, @QueryParameter String environment) {
            ComboBoxModel models = new ComboBoxModel();

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
                        for (Model model : applications) {
                            models.add(model.getName());
                        }
                    } else {
                        models.add("--- No applications found ---");
                    }
                }
            }

            return models;
        }
    }
}