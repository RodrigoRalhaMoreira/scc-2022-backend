package scc.srv.dataclasses;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a auctions questions and replies, should be named Messages but we
 * followed the given documentation.
 */

public class Question {
    @JsonProperty("id")
    private String id;
    @JsonProperty("auctionId")
    private String auctionId;
    @JsonProperty("userId")
    private String userId;
    @JsonProperty("message")
    private String message;
    @JsonProperty("reply")
    private String reply;

    
    public Question() {
        super();
    }

    public Question(String id, String auctionId, String userId, String message, String reply) {
        this.id = id;
        this.auctionId = auctionId;
        this.userId = userId;
        this.message = message;
        this.reply = reply;
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

    public String getMessage() {
        return message;
    }
    
    public String getReply() {
        return reply;
    }
    
    public void setReply(String reply) {
        this.reply = reply;
    }

    @Override
    public String toString() {
        return "Question [id=" + id + ", auctionId=" + auctionId + ", userId=" + userId + ", message="
                + message + ", reply=" + reply + "]";
    }

}
