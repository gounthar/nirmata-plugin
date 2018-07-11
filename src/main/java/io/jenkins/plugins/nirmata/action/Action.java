
package io.jenkins.plugins.nirmata.action;

import java.util.List;

import com.google.common.base.Strings;

import hudson.AbortException;
import hudson.FilePath;
import hudson.model.TaskListener;
import io.jenkins.plugins.nirmata.ActionBuilder;
import io.jenkins.plugins.nirmata.DeleteEnvAppBuilder;
import io.jenkins.plugins.nirmata.DeployEnvAppBuilder;
import io.jenkins.plugins.nirmata.UpdateCatAppBuilder;
import io.jenkins.plugins.nirmata.UpdateEnvAppBuilder;
import io.jenkins.plugins.nirmata.model.HTTPInfo;
import io.jenkins.plugins.nirmata.model.Model;
import io.jenkins.plugins.nirmata.model.Response;
import io.jenkins.plugins.nirmata.model.Result;
import io.jenkins.plugins.nirmata.util.FileOperations;
import io.jenkins.plugins.nirmata.util.LocalRepo;
import io.jenkins.plugins.nirmata.util.NirmataClient;

public final class Action {

    private static final int NUM_OF_RETRIES = 15;

    private final NirmataClient _client;
    private final FilePath _workspace;
    private final TaskListener _listener;
    private String _status = null;

    public Action(NirmataClient client, FilePath workspace, TaskListener listener) {
        _client = client;
        _workspace = workspace;
        _listener = listener;
    }

    public NirmataClient getClient() {
        return _client;
    }

    public FilePath getWorkspace() {
        return _workspace;
    }

    public TaskListener getListener() {
        return _listener;
    }

    public void buildStep(ActionBuilder builder) throws AbortException {
        if (builder instanceof UpdateEnvAppBuilder) {
            UpdateEnvAppBuilder updateBuilder = (UpdateEnvAppBuilder) builder;

            String appendedDirectoryPath = FileOperations.appendBasePath(_workspace.getRemote(),
                updateBuilder.getDirectories());

            update(updateBuilder.getEnvironment(), updateBuilder.getApplication(), appendedDirectoryPath,
                !updateBuilder.isIncludescheck() ? null : updateBuilder.getIncludes(),
                !updateBuilder.isExcludescheck() ? null : updateBuilder.getExcludes(), updateBuilder.getTimeout());

        } else if (builder instanceof UpdateCatAppBuilder) {
            UpdateCatAppBuilder updateBuilder = (UpdateCatAppBuilder) builder;

            String appendedDirectoryPath = FileOperations.appendBasePath(_workspace.getRemote(),
                updateBuilder.getDirectories());

            update(updateBuilder.getCatalog(), appendedDirectoryPath,
                !updateBuilder.isIncludescheck() ? null : updateBuilder.getIncludes(),
                !updateBuilder.isExcludescheck() ? null : updateBuilder.getExcludes(), updateBuilder.getTimeout());

        } else if (builder instanceof DeployEnvAppBuilder) {
            DeployEnvAppBuilder deployBuilder = (DeployEnvAppBuilder) builder;

            deploy(deployBuilder.getEnvironment(), deployBuilder.getCatalog(), deployBuilder.getApplication(),
                deployBuilder.getTimeout());

        } else if (builder instanceof DeleteEnvAppBuilder) {
            DeleteEnvAppBuilder deleteBuilder = (DeleteEnvAppBuilder) builder;

            delete(deleteBuilder.getEnvironment(), deleteBuilder.getApplication(), deleteBuilder.getTimeout());

        } else {
            throw new AbortException("Unknown action request!");
        }
    }

    private void printActionInfo(String... vargs) {
        _listener.getLogger().println();
        for (String varg : vargs) {
            _listener.getLogger().println(varg);
        }
        _listener.getLogger().println();
    }

    private void update(String catalog, String directories, String includes, String excludes, String timeout)
        throws AbortException {
        printActionInfo("Action: " + ActionType.UPDATE_CAT_APP.toString(),
            "Catalog: " + catalog,
            "Directories: " + directories,
            "Timeout: " + timeout,
            "Includes: " + includes,
            "Excludes: " + excludes);

        String applicationId = null;
        List<Model> catalogApplications = _client.getAppsFromCatalog().getModel();

        if (catalogApplications != null && !catalogApplications.isEmpty()) {
            for (Model e : catalogApplications) {
                if (e.getName().equals(catalog)) {
                    applicationId = e.getId();
                }
            }
        } else {
            _listener.getLogger().println("ERROR: Catalog applications list is empty");
        }

        if (!Strings.isNullOrEmpty(applicationId)) {
            List<String> listOfDirectories = FileOperations.getList(directories);
            List<String> listOfFiles = LocalRepo.getFilesInDirectory(listOfDirectories, includes, excludes);
            String yamlStr = FileOperations.appendFiles(listOfFiles);

            HTTPInfo result = _client.updateAppInCatalog(applicationId, yamlStr);
            printActionInfo(result.toString());
        } else {
            throw new AbortException(
                String.format("Unable to update application in catalog, {%s}. ApplicationId is null",
                    catalog));
        }
    }

    private void update(String environment, String application, String directories, String includes, String excludes,
        String timeout) throws AbortException {
        printActionInfo("Action: " + ActionType.UPDATE_ENV_APP.toString(),
            "Environment: " + environment,
            "Application: " + application,
            "Directories: " + directories,
            "Timeout: " + timeout,
            "Includes: " + includes,
            "Excludes: " + excludes);

        String environmentId = null;
        List<Model> environments = _client.getEnvironments().getModel();

        if (environments != null && !environments.isEmpty()) {
            for (Model e : environments) {
                if (e.getName().equals(environment)) {
                    environmentId = e.getId();
                }
            }
        } else {
            _listener.getLogger().println("ERROR: Environments list is empty");
        }

        if (Strings.isNullOrEmpty(environmentId)) {
            throw new AbortException(
                String.format("Unable to update application, {%s}. EnvironmentId is null", application));
        }

        String applicationId = null;
        List<Model> applications = _client.getAppsFromEnvironment(environmentId).getModel();

        if (applications != null && !applications.isEmpty()) {
            for (Model e : applications) {
                if (e.getName().equals(application)) {
                    applicationId = e.getId();
                }
            }
        } else {
            _listener.getLogger().println("ERROR: Applications list is empty");
        }

        if (!Strings.isNullOrEmpty(applicationId)) {
            List<String> listOfDirectories = FileOperations.getList(directories);
            List<String> listOfFiles = LocalRepo.getFilesInDirectory(listOfDirectories, includes, excludes);
            String yamlStr = FileOperations.appendFiles(listOfFiles);

            HTTPInfo result = _client.updateAppInEnvironment(applicationId, yamlStr);
            printActionInfo(result.toString());

            String status = getStatus(timeout, applicationId);
            printActionInfo("Action Status: " + status);
        } else {
            throw new AbortException(
                String.format("Unable to update application, {%s}. ApplicationId is null", application));
        }
    }

    private void deploy(String environment, String catalog, String application, String timeout) throws AbortException {
        printActionInfo("Action: " + ActionType.DEPLOY_ENV_APP.toString(),
            "Environment: " + environment,
            "Catalog: " + catalog,
            "Application: " + application,
            "Timeout: " + timeout);

        String applicationId = null;
        List<Model> catalogApplications = _client.getAppsFromCatalog().getModel();

        if (catalogApplications != null && !catalogApplications.isEmpty()) {
            for (Model e : catalogApplications) {
                if (e.getName().equals(catalog)) {
                    applicationId = e.getId();
                }
            }
        } else {
            _listener.getLogger().println("ERROR: Applications list is empty");
        }

        if (!Strings.isNullOrEmpty(applicationId)) {
            HTTPInfo result = _client.deployAppInEnvironment(applicationId, environment, application);
            printActionInfo(result.toString());

            Response response = _client.getResponse(result);
            if (response != null) {
                String status = getStatus(timeout, response.getResult().getId());
                printActionInfo("Action Status: " + status);
            }
        } else {
            throw new AbortException(
                String.format("Unable to depoly application in environment, {%s}. ApplicationId is null",
                    environment));
        }
    }

    private void delete(String environment, String application, String timeout) throws AbortException {
        printActionInfo("Action: " + ActionType.DELETE_ENV_APP.toString(),
            "Environment: " + environment,
            "Application: " + application,
            "Timeout: " + timeout);

        String environmentId = null;
        List<Model> environments = _client.getEnvironments().getModel();

        if (environments != null && !environments.isEmpty()) {
            for (Model e : environments) {
                if (e.getName().equals(environment)) {
                    environmentId = e.getId();
                }
            }
        } else {
            _listener.getLogger().println("ERROR: Environments list is empty");
        }

        if (Strings.isNullOrEmpty(environmentId)) {
            throw new AbortException(
                String.format("Unable to delete application, {%s}. EnvironmentId is null", application));
        }

        String applicationId = null;
        List<Model> applications = _client.getAppsFromEnvironment(environmentId).getModel();

        if (applications != null && !applications.isEmpty()) {
            for (Model e : applications) {
                if (e.getName().equals(application)) {
                    applicationId = e.getId();
                }
            }
        } else {
            _listener.getLogger().println("ERROR: Applications list is empty");
        }

        if (!Strings.isNullOrEmpty(applicationId)) {
            HTTPInfo result = _client.deleteAppInEnvironment(applicationId);
            printActionInfo(result.toString());

            String status = getStatus(timeout, applicationId);
            printActionInfo("Action Status: " + status);
        } else {
            throw new AbortException(
                String.format("Unable to delete application, {%s}. ApplicationId is null", application));
        }
    }

    public String getStatus(String timeout, String applicationID) {
        long timeInterval = Integer.parseInt(timeout) * 1000;
        int retries = 0;
        boolean flag = true;

        while (flag && retries++ < NUM_OF_RETRIES) {
            try {
                Result appStatus = _client.getAppStateInEnvironment(applicationID).getResult();

                _listener.getLogger().print(".");
                if (appStatus != null) {
                    if (appStatus.getId().equals(applicationID)) {
                        _status = appStatus.getState();
                        if (!_status.equals("executing")) {
                            flag = false;
                        }
                    }
                } else {
                    _listener.getLogger().println(
                        String.format("%nERROR: Unable to retrieve state of application, {%s}", applicationID));
                    flag = false;
                    _status = null;
                }

                Thread.sleep(timeInterval);
            } catch (InterruptedException e) {
                _listener.getLogger().println("ERROR: " + e.getMessage());
            }
        }

        return _status;
    }
}
