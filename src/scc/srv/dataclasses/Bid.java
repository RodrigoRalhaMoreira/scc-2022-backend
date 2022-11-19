package scc.srv.dataclasses;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an auction bid.
 */

public class Bid {
    @JsonProperty("id")
    private String id;
    @JsonProperty("auctionId")
    private String auctionId;
    @JsonProperty("userId")
    private String userId;
    @JsonProperty("value")
    private int value;

    public Bid() {
        super();
    }

    public Bid(String id, String auctionId, String userId, int value) {
        this.id = id;
        this.auctionId = auctionId;
        this.userId = userId;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getUserId() {
        return userId;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Bid [id=" + id + ", auctionId=" + auctionId + ", userId=" + userId + ", value="
                + value + "]";
    }

}
