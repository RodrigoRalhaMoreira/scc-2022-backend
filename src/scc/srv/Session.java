package scc.srv;

public class Session {

    private String uuid;
    private String userId;

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
}
