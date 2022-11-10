package scc.srv;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a auctions questions and replies, should be named Messages but we
 * followed the given documentation.
 */

public class Question {
    @JsonProperty("questionId")
    private String questionId;
    @JsonProperty("auctionId")
    private String auctionId;
    @JsonProperty("userId")
    private String userId;
    @JsonProperty("message")
    private String message;

    public Question() {
        super();
    }

    public Question(String questionId, String auctionId, String userId, String message) {
        this.questionId = questionId;
        this.auctionId = auctionId;
        this.userId = userId;
        this.message = message;
    }

    public String getQuestionId() {
        return questionId;
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

    @Override
    public String toString() {
        return "Question [questionId=" + questionId + ", auctionId=" + auctionId + ", userId=" + userId + ", message="
                + message + "]";
    }

}
