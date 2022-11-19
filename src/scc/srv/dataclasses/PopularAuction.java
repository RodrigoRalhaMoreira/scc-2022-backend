package scc.srv.dataclasses;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PopularAuction {
    
    @JsonProperty("id")
    private String id;
    @JsonProperty("count")
    private int count;

    public PopularAuction() {
        super();
    }

    public PopularAuction(String id, int count) {
        this.id = id;
        this.count = count;
    }

    public String getId() {
        return id;
    }

    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "PopularAuction [id=" + id + ", count=" + count + "]";
    }
}