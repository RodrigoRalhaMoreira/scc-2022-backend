package scc.srv;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Login {

    @JsonProperty("id")
    private String id;
    @JsonProperty("pwd")
    private String pwd;
    
    public Login() {
        super();
    }

    public Login(String id, String pwd) {
        this.id = id;
        this.pwd = pwd; 
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
        return "Login [id=" + id + ", pwd=" + pwd + "]";
    }

}
