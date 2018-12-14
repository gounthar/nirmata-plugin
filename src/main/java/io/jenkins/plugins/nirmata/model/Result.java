
package io.jenkins.plugins.nirmata.model;

public class Result {

    private String _status;
    private String _message;
    private String _application;
    private String _run;
    private String _id;
    private String _name;
    private String _state;
    private String _execState;

    public String getStatus() {
        return _status;
    }

    public void setStatus(String status) {
        _status = status;
    }

    public String getMessage() {
        return _message;
    }

    public void setMessage(String message) {
        _message = message;
    }

    public String getApplication() {
        return _application;
    }

    public void setApplication(String application) {
        _application = application;
    }

    public String getRun() {
        return _run;
    }

    public void setRun(String run) {
        _run = run;
    }

    public String getId() {
        return _id;
    }

    public void setId(String id) {
        _id = id;
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public String getState() {
        return _state;
    }

    public void setState(String state) {
        _state = state;
    }

    public String getExecutionState() {
        return _execState;
    }

    public void setExecutionState(String execState) {
        this._execState = execState;
    }

}
