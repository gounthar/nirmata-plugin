
package io.jenkins.plugins.nirmata.model;

import java.util.List;

public class Response {

    private Status _status;
    private List<Model> _model;
    private Result _result;

    public Status getStatus() {
        return _status;
    }

    public void setStatus(Status status) {
        _status = status;
    }

    public List<Model> getModel() {
        return _model;
    }

    public void setModel(List<Model> model) {
        _model = model;
    }

    public Result getResult() {
        return _result;
    }

    public void setResult(Result result) {
        _result = result;
    }

}
