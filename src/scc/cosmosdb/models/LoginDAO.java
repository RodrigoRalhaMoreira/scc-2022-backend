package scc.cosmosdb.models;

import scc.srv.dataclasses.Login;

public class LoginDAO {

    private String _rid;
    private String _ts;
    private String id;
    private String pwd;

    public LoginDAO() {
    }

    public LoginDAO(Login l) {
        this(l.getId(), l.getPwd());
    }

    public LoginDAO(String id, String pwd) {
        super();
        this.id = id;
        this.pwd = pwd;
    }

    public String get_rid() {
        return _rid;
    }

    public void set_rid(String _rid) {
        this._rid = _rid;
    }

    public String get_ts() {
        return _ts;
    }

    public void set_ts(String _ts) {
        this._ts = _ts;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    @Override
    public String toString() {
        return "LoginDAO [_rid=" + _rid + ", _ts=" + _ts + ", id=" + id + ", pwd=" + pwd + "]";
    }
}
