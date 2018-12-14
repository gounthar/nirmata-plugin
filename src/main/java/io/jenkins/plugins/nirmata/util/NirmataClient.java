
package io.jenkins.plugins.nirmata.util;

import java.net.URLEncoder;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.entity.*;
import org.slf4j.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

import hudson.AbortException;
import io.jenkins.plugins.nirmata.model.*;

public class NirmataClient {

    private static final Logger logger = LoggerFactory.getLogger(NirmataClient.class);

    private static final String GET_ENV_API = "/environments/api/Environment?fields=name,id";
    private static final String GET_APPS_FROM_ENV_API = "/environments/api/Environment/%s/applications?fields=name,run,id";
    private static final String GET_APPS_FROM_CAT_API = "/catalog/api/applications?fields=name,id";
    private static final String GET_APPS_STATE_FROM_ENV_API = "/environments/api/applications/%s?fields=id,name,state";
    private static final String GET_APPS_EXECUTION_STATE_FROM_ENV_API = "/environments/api/applications/%s?fields=id,name,executionState";
    private static final String DELETE_APPS_FROM_CAT_API = "/environments/api/applications/%s";
    private static final String DEPLOY_APPS_FROM_CAT_API = "/catalog/api/applications/%s/run";
    private static final String DEPLOY_APPS_FROM_YAML_API = "/environments/api/Environment/%s/createApplication?run=%s";
    private static final String UPDATE_APPS_FROM_ENV_API = "/environments/api/applications/%s/update";
    private static final String UPDATE_APPS_FROM_CAT_API = "/catalog/api/Application/%s/import";
    private static final String CONTENT_YAML_TYPE = "text/yaml";
    private static final String CONTENT_JSON_TYPE = "application/json";
    private static final String CONTENT_APP_YAML_TYPE = "application/yaml";
    private static final String NIRMATA_STR = "NIRMATA-API ";
    private static final String GET_CHANGE_REQUEST = "/environments/api/changeRequests/%s?fields=name,id,changes,state,user,changeId,sequenceId";
    private static final String GET_RESOURCE_CHANGE_REQUEST = "/environments/api/resourceChange/%s?fields=namespace,id,source,state,sequenceId,yamlBeforeChange,yamlAfterChange,resourceName,sequenceId,resourceKind";
    private static final String GET_UPDATE_POLICY = "/environments/api/environments/%s/updatePolicy";
    private static final String GET_SYSTEM_TASKS = "/environments/api/systemTasks?fields=id,resourceName,resourceType,name,state,error,subtasks&query=";
    private static final String GET_SYSTEM_SUBTASKS = "/environments/api/systemSubTasks?fields=id,resourceName,resourceType,name,state,error&query=";

    private String _endpoint;
    private String _apiKey;

    @SuppressWarnings("unused")
    private NirmataClient() {

    }

    public NirmataClient(String endpoint, String apiKey) {
        _endpoint = endpoint;
        _apiKey = apiKey;
    }

    public Response getEnvironments() {
        Response response = null;

        try {
            String uri = String.format("https://%s%s", _endpoint, GET_ENV_API);

            HTTPInfo httpInfo = HttpClient.doGet(uri, CONTENT_YAML_TYPE, NIRMATA_STR + _apiKey);
            response = getResponse(httpInfo);
        } catch (Exception e) {
            logger.error("Error encountered while getting environments, {}", e);
        }

        return response;
    }

    public Response getResponse(HTTPInfo httpInfo) {
        Response response = new Response();

        try {
            response.setStatus(new Status(httpInfo.getStatusCode(), httpInfo.getMessage()));
            if (httpInfo.getStatusCode() == HttpServletResponse.SC_OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                try {
                    response.setModel(
                        objectMapper.readValue(httpInfo.getPayload(), new TypeReference<List<Model>>() {}));
                } catch (Exception e) {
                    response.setResult(
                        objectMapper.readValue(httpInfo.getPayload(), Result.class));
                }
            }
        } catch (Exception e) {
            logger.error("Unable to read Response object", e);
        }

        return response;
    }

    public Response getAppsFromEnvironment(String environmentId) {
        Response response = null;

        try {
            String api = String.format(GET_APPS_FROM_ENV_API, environmentId);
            String uri = String.format("https://%s%s", _endpoint, api);

            HTTPInfo httpInfo = HttpClient.doGet(uri, CONTENT_YAML_TYPE, NIRMATA_STR + _apiKey);
            response = getResponse(httpInfo);
        } catch (Exception e) {
            logger.error("Error encountered while getting apps from environment, {}", e);
        }

        return response;
    }

    public Response getAppsFromCatalog() {
        Response response = null;

        try {
            String uri = String.format("https://%s%s", _endpoint, GET_APPS_FROM_CAT_API);

            HTTPInfo httpInfo = HttpClient.doGet(uri, CONTENT_YAML_TYPE, NIRMATA_STR + _apiKey);
            response = getResponse(httpInfo);
        } catch (Exception e) {
            logger.error("Error encountered while getting apps from catalog, {}", e);
        }

        return response;
    }

    public Response getAppStateInEnvironment(String applicationID) {
        Response response = null;

        try {
            String api = String.format(GET_APPS_STATE_FROM_ENV_API, applicationID);
            String uri = String.format("https://%s%s", _endpoint, api);

            HTTPInfo httpInfo = HttpClient.doGet(uri, CONTENT_YAML_TYPE, NIRMATA_STR + _apiKey);
            response = getResponse(httpInfo);
        } catch (Exception e) {
            logger.error("Error encountered while getting apps state from environment, {}", e);
        }

        return response;
    }

    public Response getAppsExecutionStateFromEnvironment(String applicationID) {
        Response response = null;

        try {
            String api = String.format(GET_APPS_EXECUTION_STATE_FROM_ENV_API, applicationID);
            String uri = String.format("https://%s%s", _endpoint, api);

            HTTPInfo httpInfo = HttpClient.doGet(uri, CONTENT_YAML_TYPE, NIRMATA_STR + _apiKey);
            response = getResponse(httpInfo);
        } catch (Exception e) {
            logger.error("Error encountered while getting apps execution state from environment, {}", e);
        }

        return response;
    }

    public HTTPInfo getChangesFromChangeRequest(String changeRequestID) {
        HTTPInfo httpInfo = null;

        try {
            String api = String.format(GET_CHANGE_REQUEST, changeRequestID);
            String uri = String.format("https://%s%s", _endpoint, api);

            httpInfo = HttpClient.doGet(uri, CONTENT_YAML_TYPE, NIRMATA_STR + _apiKey);
        } catch (Exception e) {
            logger.error("Error encountered while getting changes from ChangeRequest, {}", e);
        }

        return httpInfo;
    }

    public HTTPInfo getChangesFromResourceChange(String resourceChangeId) {
        HTTPInfo httpInfo = null;

        try {
            String api = String.format(GET_RESOURCE_CHANGE_REQUEST, resourceChangeId);
            String uri = String.format("https://%s%s", _endpoint, api);

            httpInfo = HttpClient.doGet(uri, CONTENT_YAML_TYPE, NIRMATA_STR + _apiKey);
        } catch (Exception e) {
            logger.error("Error encountered while getting changes from ChangeRequest, {}", e);
        }

        return httpInfo;
    }

    public HTTPInfo getSystemTasks(String sequenceId) {
        HTTPInfo httpInfo = null;

        try {
            String query = String.format("{\"sequenceId\":\"%s\"}", sequenceId);
            String uri = String.format("https://%s%s%s", _endpoint, GET_SYSTEM_TASKS,
                URLEncoder.encode(query, "UTF-8"));

            httpInfo = HttpClient.doGet(uri, CONTENT_YAML_TYPE, NIRMATA_STR + _apiKey);
        } catch (Exception e) {
            logger.error("Error encountered while getting System Tasks, {}", e);
        }

        return httpInfo;
    }

    public HTTPInfo getSystemSubTasks(String sequenceId) {
        HTTPInfo httpInfo = null;

        try {
            String query = String.format("{\"sequenceId\":\"%s\"}", sequenceId);
            String uri = String.format("https://%s%s%s", _endpoint, GET_SYSTEM_SUBTASKS,
                URLEncoder.encode(query, "UTF-8"));

            httpInfo = HttpClient.doGet(uri, CONTENT_YAML_TYPE, NIRMATA_STR + _apiKey);
        } catch (Exception e) {
            logger.error("Error encountered while getting System Sub Tasks, {}", e);
        }

        return httpInfo;
    }

    public HTTPInfo getUpdatePolicy(String environmentID) {
        HTTPInfo httpInfo = null;

        try {
            String api = String.format(GET_UPDATE_POLICY, environmentID);
            String uri = String.format("https://%s%s", _endpoint, api);

            httpInfo = HttpClient.doGet(uri, CONTENT_YAML_TYPE, NIRMATA_STR + _apiKey);
        } catch (Exception e) {
            logger.error("Error encountered while getting update policy, {}", e);
        }

        return httpInfo;
    }

    public HTTPInfo deleteAppInEnvironment(String applicationId) throws AbortException {
        HTTPInfo httpInfo = null;

        try {
            String api = String.format(DELETE_APPS_FROM_CAT_API, applicationId);
            String uri = String.format("https://%s%s", _endpoint, api);

            httpInfo = HttpClient.doDelete(uri, CONTENT_YAML_TYPE, NIRMATA_STR + _apiKey);
        } catch (Exception e) {
            throw new AbortException(String.format(
                "Error encountered while deleting app (%s) from environment with Exception (%s)", applicationId, e));
        }

        return httpInfo;
    }

    public HTTPInfo deployAppFromCatalog(String applicationId, String envName, String appName)
        throws AbortException {
        HTTPInfo httpInfo = null;

        try {
            String payload = String.format("{" + "\"run\": \"%s\", " + "\"environment\": \"%s\"" + "}", appName,
                envName);
            StringEntity entity = new StringEntity(payload, ContentType.APPLICATION_FORM_URLENCODED);

            String api = String.format(DEPLOY_APPS_FROM_CAT_API, applicationId);
            String uri = String.format("https://%s%s", _endpoint, api);

            httpInfo = HttpClient.doPost(uri, CONTENT_JSON_TYPE, NIRMATA_STR + _apiKey, entity);
        } catch (Exception e) {
            throw new AbortException(String.format(
                "Error encountered while deploying app (%s) in environment with Exception (%s)", applicationId, e));
        }

        return httpInfo;
    }

    public HTTPInfo deployAppFromFiles(String environmentId, String appName, String yamlStr) throws AbortException {
        HTTPInfo httpInfo = null;

        try {
            StringEntity entity = new StringEntity(yamlStr, ContentType.APPLICATION_OCTET_STREAM);

            String api = String.format(DEPLOY_APPS_FROM_YAML_API, environmentId, appName);
            String uri = String.format("https://%s%s", _endpoint, api);

            httpInfo = HttpClient.doPost(uri, CONTENT_APP_YAML_TYPE, NIRMATA_STR + _apiKey, entity);
        } catch (Exception e) {
            throw new AbortException(String.format(
                "Error encountered while deploying app (%s) in environment with Exception (%s)", appName, e));
        }

        return httpInfo;
    }

    public HTTPInfo updateAppInEnvironment(String applicationId, String yamlStr)
        throws AbortException {
        HTTPInfo httpInfo = null;

        try {
            StringEntity entity = new StringEntity(yamlStr, ContentType.APPLICATION_OCTET_STREAM);
            entity.setChunked(true);

            String api = String.format(UPDATE_APPS_FROM_ENV_API, applicationId);
            String uri = String.format("https://%s%s", _endpoint, api);

            httpInfo = HttpClient.doPost(uri, CONTENT_YAML_TYPE, NIRMATA_STR + _apiKey, entity);
        } catch (Exception e) {
            throw new AbortException(String.format(
                "Error encountered while updating app (%s) in environment with Exception (%s)", applicationId, e));
        }

        return httpInfo;
    }

    public HTTPInfo updateAppInCatalog(String applicationId, String yamlStr) throws AbortException {
        HTTPInfo httpInfo = null;

        try {
            StringEntity entity = new StringEntity(yamlStr, ContentType.APPLICATION_OCTET_STREAM);
            entity.setChunked(true);

            String api = String.format(UPDATE_APPS_FROM_CAT_API, applicationId);
            String uri = String.format("https://%s%s", _endpoint, api);

            httpInfo = HttpClient.doPost(uri, CONTENT_YAML_TYPE, NIRMATA_STR + _apiKey, entity);
        } catch (Exception e) {
            throw new AbortException(String
                .format("Error encountered while updating app (%s) in catalog with Exception (%s)", applicationId, e));
        }

        return httpInfo;
    }
}
