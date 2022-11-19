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
    @JsonProperty("amount")
    private int amount;

    public Bid() {
        super();
    }

    public Bid(String id, String auctionId, String userId, int amount) {
        this.id = id;
        this.auctionId = auctionId;
        this.userId = userId;
        this.amount = amount;
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

    public int getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "Bid [id=" + id + ", auctionId=" + auctionId + ", userId=" + userId + ", amount="
                + amount + "]";
    }

}
