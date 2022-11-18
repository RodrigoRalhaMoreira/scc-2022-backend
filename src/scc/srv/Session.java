package scc.srv;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Session {

    @JsonProperty("uuid")
    private String uuid;
    @JsonProperty("userId")
    private String userId;

    public Session() {
        super();
    }

    public Session(String uuid, String userId) {
        this.uuid = uuid;
        this.userId = userId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "Session [uuid= " + uuid + ", userId=" + userId + "]";
    }
}
