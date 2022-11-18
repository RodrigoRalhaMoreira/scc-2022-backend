package scc.srv.dataclasses;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecentAuction {
    
    @JsonProperty("id")
    private String id;
    @JsonProperty("endTime")
    private String endTime;

    public RecentAuction() {
        super();
    }

    public RecentAuction(String id, String endTime) {
        this.id = id;
        this.endTime = endTime;
    }

    public String getId() {
        return id;
    }
    
    public String getEndTime() {
        return endTime;
    }

    @Override
    public String toString() {
        return "RecentAuction [id=" + id + ", endTime=" + endTime + "]";
    }
}
