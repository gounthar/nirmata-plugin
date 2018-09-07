
package io.jenkins.plugins.nirmata.model;

public class Model {

    private String _id;
    private String _name;
    private String _run;

    public final String getId() {
        return _id;
    }

    public final void setId(String id) {
        _id = id;
    }

    public final String getName() {
        return _name;
    }

    public final void setName(String name) {
        _name = name;
    }

    public final String getRun() {
        return _run;
    }

    public final void setRun(String run) {
        _run = run;
    }
}
