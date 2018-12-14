
package io.jenkins.plugins.nirmata.action;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.google.common.base.Strings;

import hudson.*;
import hudson.model.TaskListener;
import io.jenkins.plugins.nirmata.*;
import io.jenkins.plugins.nirmata.DeployEnvAppBuilder.DeployType;
import io.jenkins.plugins.nirmata.model.*;
import io.jenkins.plugins.nirmata.util.*;

public final class Action {

    private static final String RUNNING_STR = "running";
    private static final String ERROR_STR = "error";
    private static final String ID_STR = "id";
    private static final String RESOURCE_TYPE_STR = "resourceType";
    private static final String RESOURCE_NAME_STR = "resourceName";
    private static final String UNKNOWN_STR = "unknown";
    private static final String STATE_STR = "state";
    private static final String NAME_STR = "name";
    private static final String UPDATE_STR = "update";
    private static final String DELETED_STR = "deleted";
    private static final String APPLICATION_ID_STR = "applicationId";
    private static final String SEQUENCE_ID_STR = "sequenceId";
    private static final String COMPLETED_STR = "completed";
    private static final String FAILED_STR = "failed";
    private static final String RESOURCE_CHANGES_STR = "ResourceChanges";
    private static final String SUCCESS_STR = "Success";
    private static final String MESSAGE_STR = "message";
    private static final String CHANGES_REQUEST_ID_STR = "changesRequestId";

    private static final int NUM_OF_UNDERSCORES = 100;

    private final NirmataClient _client;
    private final FilePath _workspace;
    private final PrintStream _logger;
    private HTTPInfo _httpInfo;

    public Action(NirmataClient client, FilePath workspace, TaskListener listener) {
        _client = client;
        _workspace = workspace;
        _logger = listener.getLogger();
    }

    public NirmataClient getClient() {
        return _client;
    }

    public FilePath getWorkspace() {
        return _workspace;
    }

    public PrintStream getListener() {
        return _logger;
    }

    public hudson.model.Result buildStep(ActionBuilder builder) throws AbortException {
        printSeparator();

        if (builder instanceof UpdateEnvAppBuilder) {
            UpdateEnvAppBuilder updateBuilder = (UpdateEnvAppBuilder) builder;

            update(updateBuilder.getEnvironment(), updateBuilder.getApplication(), updateBuilder.getDirectories(),
                !updateBuilder.isIncludescheck() ? null : updateBuilder.getIncludes(),
                !updateBuilder.isExcludescheck() ? null : updateBuilder.getExcludes(), updateBuilder.getTimeout());

        } else if (builder instanceof UpdateCatAppBuilder) {
            UpdateCatAppBuilder updateBuilder = (UpdateCatAppBuilder) builder;

            update(updateBuilder.getCatalog(), updateBuilder.getDirectories(),
                !updateBuilder.isIncludescheck() ? null : updateBuilder.getIncludes(),
                !updateBuilder.isExcludescheck() ? null : updateBuilder.getExcludes(), updateBuilder.getTimeout());

        } else if (builder instanceof DeployEnvAppBuilder) {
            DeployEnvAppBuilder deployBuilder = (DeployEnvAppBuilder) builder;

            deploy(deployBuilder.getEnvironment(), deployBuilder.getCatalog(), deployBuilder.getApplication(),
                deployBuilder.getDirectories(), !deployBuilder.isIncludescheck() ? null : deployBuilder.getIncludes(),
                !deployBuilder.isExcludescheck() ? null : deployBuilder.getExcludes(), deployBuilder.getTimeout(),
                deployBuilder.getDeployType());

        } else if (builder instanceof DeleteEnvAppBuilder) {
            DeleteEnvAppBuilder deleteBuilder = (DeleteEnvAppBuilder) builder;

            delete(deleteBuilder.getEnvironment(), deleteBuilder.getApplication(), deleteBuilder.getTimeout());

        } else {
            throw new AbortException("Unknown action request!");
        }

        return verifyActionStatus(builder);
    }

    private hudson.model.Result verifyActionStatus(ActionBuilder builder) {
        hudson.model.Result result = hudson.model.Result.FAILURE;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            Payload payload = objectMapper.readValue(_httpInfo.getPayload(), Payload.class);
            if (payload.getStatus() == HttpServletResponse.SC_OK ||
                (payload.getStatus() == 0 && builder instanceof DeleteEnvAppBuilder)) {
                result = hudson.model.Result.SUCCESS;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    private void update(String catalog, String directories, String includes, String excludes, Integer timeout)
        throws AbortException {
        printInfo("Action: " + ActionType.UPDATE_CAT_APP.toString(),
            "Catalog: " + catalog,
            "Directories: " + directories,
            "Timeout: " + timeout,
            "Includes: " + includes,
            "Excludes: " + excludes);

        String applicationId = null;
        List<Model> catalogApplications = _client.getAppsFromCatalog().getModel();

        if (catalogApplications != null && !catalogApplications.isEmpty()) {
            for (Model model : catalogApplications) {
                if (model.getName().equals(catalog)) {
                    applicationId = model.getId();
                }
            }
        } else {
            printInfo("ERROR: Catalog applications list is empty");
        }

        if (!Strings.isNullOrEmpty(applicationId)) {
            List<String> listOfDirectories = FileOperations.getDirectories(_workspace.getRemote(), directories);
            List<String> listOfFiles = LocalRepo.getFilesInDirectory(listOfDirectories, includes, excludes);
            printFiles(listOfFiles);

            String yamlStr = FileOperations.appendFiles(listOfFiles);
            _httpInfo = _client.updateAppInCatalog(applicationId, yamlStr);

            printInfo(_httpInfo.toString());
        } else {
            throw new AbortException(
                String.format("Unable to update application in catalog, {%s}. ApplicationId is null",
                    catalog));
        }
    }

    @SuppressWarnings("unchecked")
    private void update(String environment, String application, String directories, String includes, String excludes,
        Integer timeout) throws AbortException {
        printInfo("Action: " + ActionType.UPDATE_ENV_APP.toString(),
            "Environment: " + environment,
            "Application: " + application,
            "Directories: " + directories,
            "Timeout: " + timeout,
            "Includes: " + includes,
            "Excludes: " + excludes);

        String environmentId = null;
        List<Model> environments = _client.getEnvironments().getModel();

        if (environments != null && !environments.isEmpty()) {
            for (Model model : environments) {
                if (model.getName().equals(environment)) {
                    environmentId = model.getId();
                }
            }
        } else {
            printInfo("ERROR: Environments list is empty");
        }

        if (Strings.isNullOrEmpty(environmentId)) {
            throw new AbortException(
                String.format("Unable to update application, {%s}. EnvironmentId is null", application));
        }

        String applicationId = null;
        List<Model> applications = _client.getAppsFromEnvironment(environmentId).getModel();

        if (applications != null && !applications.isEmpty()) {
            for (Model e : applications) {
                if (e.getRun().equals(application)) {
                    applicationId = e.getId();
                }
            }
        } else {
            printInfo("ERROR: Applications list is empty");
        }

        if (!Strings.isNullOrEmpty(applicationId)) {
            List<String> listOfDirectories = FileOperations.getDirectories(_workspace.getRemote(), directories);
            List<String> listOfFiles = LocalRepo.getFilesInDirectory(listOfDirectories, includes, excludes);
            printFiles(listOfFiles);

            String yamlStr = FileOperations.appendFiles(listOfFiles);

            _httpInfo = _client.updateAppInEnvironment(applicationId, yamlStr);
            String resultPayload = _httpInfo.getPayload();
            String changeRequestId = (String) getInfo(resultPayload, CHANGES_REQUEST_ID_STR);

            if (getInfo(resultPayload, MESSAGE_STR).equals(SUCCESS_STR) && changeRequestId == null) {
                for (Object val : (List<Object>) (getInfo(resultPayload, RESOURCE_CHANGES_STR))) {
                    String uuidStr = null;

                    for (Object obj : ((LinkedHashMap<Object, Object>) (val)).keySet()) {
                        if (((String) obj).equals("uuid")) {
                            Object uuid = ((LinkedHashMap<Object, Object>) val).get(obj);
                            uuidStr = uuid.toString();
                        }
                    }

                    if (StringUtils.isNotEmpty(uuidStr)) {
                        HTTPInfo changes = _client.getChangesFromResourceChange(uuidStr);
                        printInfo("ResourceChanges: " + changes.toString());

                        String payload = changes.getPayload();
                        String updatePolicy = getUpdatePolicy(environmentId, payload);
                        printInfo("Update Policy: " + updatePolicy);

                        checkUpdatePolicy(updatePolicy, applicationId, payload, timeout);
                    }
                }
            } else if (getInfo(resultPayload, MESSAGE_STR).equals(SUCCESS_STR) && changeRequestId != null) {
                HTTPInfo changes = _client
                    .getChangesFromChangeRequest((String) getInfo(resultPayload, CHANGES_REQUEST_ID_STR));
                printInfo("ChangeRequest: " + changes.toString());

                String payload = changes.getPayload();
                String updatePolicy = getUpdatePolicy(environmentId, payload);
                printInfo("Update Policy: " + updatePolicy);

                checkUpdatePolicy(updatePolicy, applicationId, payload, timeout);
            } else {
                throw new AbortException(
                    String.format("Unable to update application, {%s}", getInfo(resultPayload, MESSAGE_STR)));
            }
        } else {
            throw new AbortException(
                String.format("Unable to update application, {%s}. ApplicationId is null", application));
        }
    }

    private void deploy(String environment, String catalog, String application, String directories, String includes,
        String excludes, Integer timeout, String deployType) throws AbortException {

        if (DeployType.FILES.toString().equals(deployType)) {
            deployFromFiles(environment, application, directories, includes, excludes, timeout);

        } else if (DeployType.CATALOG.toString().equals(deployType)) {
            deployFromCatalog(environment, catalog, application, timeout);

        } else {
            throw new AbortException(
                String.format("Unable to deploy application, {%s} with directories, {%s} & cataog, {%s}", application,
                    directories, catalog));
        }
    }

    private void deployFromFiles(String environment, String application, String directories, String includes,
        String excludes, Integer timeout) throws AbortException {
        printInfo("Action: " + ActionType.DEPLOY_ENV_APP.toString(),
            "Environment: " + environment,
            "Application: " + application,
            "Timeout: " + timeout,
            "Directories: " + directories,
            "Includes: " + includes,
            "Excludes: " + excludes);

        List<String> listOfDirectories = FileOperations.getDirectories(_workspace.getRemote(), directories);
        List<String> listOfFiles = LocalRepo.getFilesInDirectory(listOfDirectories, includes, excludes);
        printFiles(listOfFiles);

        String yamlStr = FileOperations.appendFiles(listOfFiles);

        if (listOfFiles.size() == 0) {
            printInfo("There are no acceptable files in the directory");

        } else {
            String environmentId = null;
            List<Model> environments = _client.getEnvironments().getModel();

            if (environments != null && !environments.isEmpty()) {
                for (Model e : environments) {
                    if (e.getName().equals(environment)) {
                        environmentId = e.getId();
                    }
                }
            } else {
                printInfo("ERROR: Environments list is empty");
            }

            if (Strings.isNullOrEmpty(environmentId)) {
                throw new AbortException(
                    String.format("Unable to deploy application, {%s}. EnvironmentId is null", application));
            }

            _httpInfo = _client.deployAppFromFiles(environmentId, application, yamlStr);
            printInfo(_httpInfo.toString());

            if (getInfo(_httpInfo.getPayload(), MESSAGE_STR).equals(SUCCESS_STR)) {
                String taskState = checkSystemTaskState(timeout,
                    (String) getInfo(_httpInfo.getPayload(), SEQUENCE_ID_STR));
                printInfo("System Task State: " + taskState);

                if (taskState.equals(FAILED_STR)) {
                    printError((String) getInfo(_httpInfo.getPayload(), SEQUENCE_ID_STR));

                } else if (taskState.equals(COMPLETED_STR)) {
                    Response response = _client.getResponse(_httpInfo);

                    if (response != null) {
                        String status = checkStatus(timeout,
                            (String) getInfo(_httpInfo.getPayload(), APPLICATION_ID_STR));
                        printInfo("Action Status: " + status);
                    }
                }
            }
        }
    }

    private void deployFromCatalog(String environment, String catalog, String application, Integer timeout)
        throws AbortException {
        printInfo("Action: " + ActionType.DEPLOY_ENV_APP.toString(),
            "Environment: " + environment,
            "Catalog: " + catalog,
            "Application: " + application,
            "Timeout: " + timeout);

        String applicationId = null;
        List<Model> catalogApplications = _client.getAppsFromCatalog().getModel();

        if (catalogApplications != null && !catalogApplications.isEmpty()) {
            for (Model model : catalogApplications) {
                if (model.getName().equals(catalog)) {
                    applicationId = model.getId();
                }
            }
        } else {
            printInfo("ERROR: Applications list is empty");
        }

        if (!Strings.isNullOrEmpty(applicationId)) {
            _httpInfo = _client.deployAppFromCatalog(applicationId, environment, application);
            printInfo(_httpInfo.toString());

            Response response = _client.getResponse(_httpInfo);
            if (response != null) {
                String status = checkStatus(timeout, response.getResult().getId());
                printInfo("Action Status: " + status);
            }
        } else {
            throw new AbortException(
                String.format("Unable to deploy application in environment, {%s}. ApplicationId is null",
                    environment));
        }
    }

    private void delete(String environment, String application, Integer timeout) throws AbortException {
        printInfo("Action: " + ActionType.DELETE_ENV_APP.toString(),
            "Environment: " + environment,
            "Application: " + application,
            "Timeout: " + timeout);

        String environmentId = null;
        List<Model> environments = _client.getEnvironments().getModel();

        if (environments != null && !environments.isEmpty()) {
            for (Model model : environments) {
                if (model.getName().equals(environment)) {
                    environmentId = model.getId();
                }
            }
        } else {
            printInfo("ERROR: Environments list is empty");
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
            printInfo("ERROR: Applications list is empty");
        }

        if (!Strings.isNullOrEmpty(applicationId)) {
            _httpInfo = _client.deleteAppInEnvironment(applicationId);
            printInfo(_httpInfo.toString());

            String status = checkStatus(timeout, applicationId);
            printInfo("Action Status: " + (status == null ? DELETED_STR : status));
        } else {
            throw new AbortException(
                String.format("Unable to delete application, {%s}. ApplicationId is null", application));
        }
    }

    public Object getInfo(String result, String find) {
        String info = null;

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            if (result.charAt(0) == '[') {
                List<Map<String, Object>> data = objectMapper.readValue(result,
                    new TypeReference<List<Map<String, Object>>>() {});
                for (int i = 0; i < data.size(); i++) {
                    info = (String) data.get(i).get(find);
                }
            } else {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = objectMapper.readValue(result, Map.class);
                if (!(map.get(find) instanceof java.util.ArrayList)) {
                    info = (String) map.get(find);
                } else {
                    return map.get(find);
                }
            }
        } catch (Throwable t) {
            printInfo("Failed to parse input " + t.getMessage());
        }

        return info;
    }

    public String getUpdatePolicy(String environmentId, String info) {
        HTTPInfo updatePolicy = _client.getUpdatePolicy(environmentId);
        String policy = (String) getInfo(updatePolicy.getPayload(), "configUpdateAction");
        return policy;
    }

    public void checkUpdatePolicy(String updatePolicy, String applicationId, String changesPayload, Integer timeout) {
        if (!Strings.isNullOrEmpty(updatePolicy)) {
            if (updatePolicy.equals(UPDATE_STR)) {
                String sequenceId = (String) getInfo(changesPayload, SEQUENCE_ID_STR);
                String taskState = printSystemTasks(sequenceId, timeout);
                if (taskState.equals(COMPLETED_STR)) {
                    String statusResult = checkStatus(timeout, applicationId);
                    printInfo("State of application for given timeout: " + statusResult);
                } else if (taskState.equals(FAILED_STR)) {
                    printError((String) getInfo(changesPayload, SEQUENCE_ID_STR));
                } else {
                    printInfo("Timeout occurred. Current System Task State: " + taskState);
                }
            } else if (updatePolicy.equals("notify")) {
                printInfo("The user has been notified of the changes. "
                    + "%nUser must manually accept or decline the changes on the Nirmata platform.");
            }
        }
    }

    public String checkSystemTaskState(Integer timeout, String sequenceId) {
        long lastTime = 0;
        long timeDuration = 120000;
        long startTime = System.currentTimeMillis();

        if (timeout != 0) {
            timeDuration = timeout * 1000;
        }

        String task = null;
        String state = null;

        if (!Strings.isNullOrEmpty(_client.getSystemTasks(sequenceId).getPayload())) {
            do {
                if (System.currentTimeMillis() - lastTime > 15000) {
                    task = (String) getInfo(_client.getSystemTasks(sequenceId).getPayload(), NAME_STR);
                    state = (String) getInfo(_client.getSystemTasks(sequenceId).getPayload(), STATE_STR);

                    if (task == null || state == null) {
                        state = UNKNOWN_STR;
                        continue;
                    }

                    printInfo("Timestamp: " + LocalDateTime.now());
                    printInfo(String.format("System Task: %-20s System Task State: %-15s", task, state));

                    String completed = getCompletedCount(sequenceId);
                    printInfo("Number of completed subtasks: " + completed);
                    lastTime = System.currentTimeMillis();
                }
            } while (!state.equals(COMPLETED_STR) && !state.equals(FAILED_STR)
                && System.currentTimeMillis() - startTime < timeDuration);
        }

        return state;
    }

    public String getCompletedCount(String sequenceId) {
        List<Map<String, Object>> subTask = getSubTasks(_client.getSystemSubTasks(sequenceId).getPayload());
        int size = 0;
        int count = 0;

        if (subTask != null) {
            size = subTask.size();

            for (int i = 0; i < size; i++) {
                if (subTask.get(i).get(STATE_STR).equals(COMPLETED_STR)) {
                    count++;
                }
            }
        }

        return count + "/" + size;
    }

    public String printSystemTasks(String sequenceId, Integer timeout) {
        long lastTime = 0;
        long timeDuration = 120000;
        long startTime = System.currentTimeMillis();

        if (timeout != 0) {
            timeDuration = timeout * 1000;
        }

        String systemTasks = _client.getSystemTasks(sequenceId).getPayload();
        String taskName, taskType, taskId, taskState = null;
        String subTasks = _client.getSystemSubTasks(sequenceId).getPayload();
        String subTaskName, subTaskType, subTaskId, subTaskState = null;
        List<Map<String, Object>> data = getSubTasks(subTasks);

        if (!Strings.isNullOrEmpty(_client.getSystemTasks(sequenceId).getPayload())) {
            do {

                if (System.currentTimeMillis() - lastTime > 15000) {
                    systemTasks = _client.getSystemTasks(sequenceId).getPayload();
                    taskName = (String) getInfo(systemTasks, RESOURCE_NAME_STR);
                    taskType = (String) getInfo(systemTasks, RESOURCE_TYPE_STR);
                    taskId = (String) getInfo(systemTasks, ID_STR);
                    taskState = (String) getInfo(systemTasks, STATE_STR);

                    if (taskState == null) {
                        taskState = UNKNOWN_STR;
                        continue;
                    }

                    subTasks = _client.getSystemSubTasks(sequenceId).getPayload();
                    data = getSubTasks(subTasks);

                    printInfo("Timestamp: " + LocalDateTime.now());
                    printInfo("SystemTask: ");
                    printInfo(String.format("ResourceName: %-14s ResourceType: %-14s TaskId: %-38s TaskState: %-12s",
                        taskName, taskType, taskId, taskState));
                    printInfo("    Subtasks:");

                    for (int i = 0; i < data.size(); i++) {
                        subTaskName = (String) data.get(i).get(RESOURCE_NAME_STR);
                        subTaskType = (String) data.get(i).get(RESOURCE_TYPE_STR);
                        subTaskId = (String) data.get(i).get(ID_STR);
                        subTaskState = (String) data.get(i).get(STATE_STR);
                        printInfo(String.format(
                            "ResourceName: %-14s ResourceType: %-14s TaskId: %-38s TaskState: %-12s", subTaskName,
                            subTaskType, subTaskId, subTaskState));
                    }

                    lastTime = System.currentTimeMillis();
                }
            } while (!taskState.equals(COMPLETED_STR) && !taskState.equals(FAILED_STR)
                && (System.currentTimeMillis() - startTime < timeDuration));
        }

        return taskState;
    }

    public void printError(String sequenceId) {
        String taskError = (String) getInfo(_client.getSystemTasks(sequenceId).getPayload(), ERROR_STR);
        List<Map<String, Object>> subTasks = getSubTasks(_client.getSystemSubTasks(sequenceId).getPayload());

        printInfo("System Task Error: " + taskError);

        for (int i = 0; i < subTasks.size(); i++) {
            if (subTasks.get(i).get(STATE_STR).equals(FAILED_STR)) {
                String subTaskError = (String) subTasks.get(i).get(ERROR_STR);
                String afterDecoding = StringEscapeUtils.unescapeHtml(subTaskError);
                printInfo("Sub Task Error: " + afterDecoding);
            }
        }
    }

    public List<Map<String, Object>> getSubTasks(String info) {
        List<Map<String, Object>> data = null;

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            data = objectMapper.readValue(info, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Throwable t) {
            printInfo("Failed to get subTasks" + t.getMessage());
        }

        return data;
    }

    public String checkStatus(Integer timeout, String applicationID) {
        long timeInterval = 5000;
        long timeDuration = 120000;
        long startTime = System.currentTimeMillis();
        String status = null;
        String executionStatus = null;

        if (timeout != 0) {
            timeDuration = timeout * 1000;
        }

        printInfo(
            String.format("%n%-20s%-30s%-30s", "Time (secs)", "Application State", "Application Execution State"));

        while (System.currentTimeMillis() - startTime < timeDuration) {
            try {
                Result environmentApplications = _client.getAppStateInEnvironment(applicationID).getResult();
                Result environmentApplicationsExec = _client.getAppsExecutionStateFromEnvironment(applicationID)
                    .getResult();

                if (environmentApplications != null) {
                    if (environmentApplications.getId().equals(applicationID)) {
                        status = environmentApplications.getState();
                        executionStatus = environmentApplicationsExec.getExecutionState();

                        _logger
                            .println(String.format("%-20d%-30s%-30s", (System.currentTimeMillis() - startTime) / 1000,
                                status, executionStatus));

                        if (status.equals(RUNNING_STR) && (System.currentTimeMillis() - startTime > 1000)) {
                            return status;
                        }
                    }
                } else {
                    printInfo("ERROR: Environment applications list is empty");
                    status = null;
                    break;
                }

                Thread.sleep(timeInterval);
            } catch (InterruptedException e) {
                printInfo("ERROR: Thread InterruptedException: " + e.getMessage());
            }
        }

        return status;
    }

    private void printSeparator() {
        for (int i = 0; i++ < NUM_OF_UNDERSCORES;) {
            _logger.print('-');
        }
    }

    private void printInfo(String... vargs) {
        _logger.println();
        for (String varg : vargs) {
            _logger.println(varg);
        }
    }

    private void printFiles(List<String> listOfFiles) {
        _logger.println("List of " + listOfFiles.size() + " files:");
        for (String file : listOfFiles) {
            _logger.println(file);
        }
    }

}
