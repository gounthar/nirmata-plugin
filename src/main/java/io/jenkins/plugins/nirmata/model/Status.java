
package io.jenkins.plugins.nirmata.model;

public class Status {

    private int _statusCode;
    private String _message;

    public Status(int statusCode, String message) {
        _statusCode = statusCode;
        _message = message;
    }

    public String getMessage() {
        return _message;
    }

    public void setMessage(String message) {
        _message = message;
    }

    public int getStatusCode() {
        return _statusCode;
    }

    public void setStatusCode(int statusCode) {
        _statusCode = statusCode;
    }

}
