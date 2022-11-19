package scc.srv.dataclasses;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecentAuction {
    
    @JsonProperty("id")
    private String id;
    @JsonProperty("time_difference")
    private String timeDifference;

    public RecentAuction() {
        super();
    }

    public RecentAuction(String id, String timeDifference) {
        this.id = id;
        this.timeDifference = timeDifference;
    }

    public String getId() {
        return id;
    }
    
    public String getTimeDifference() {
        return timeDifference;
    }

    @Override
    public String toString() {
        return "RecentAuction [id=" + id + ", timeDifference=" + timeDifference + "]";
    }
}