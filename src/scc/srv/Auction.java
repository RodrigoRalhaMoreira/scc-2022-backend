package scc.srv;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an Auction
 */

public class Auction {

    @JsonProperty("id")
    private String id;
    @JsonProperty("title")
    private String title;
    @JsonProperty("description")
    private String description;
    @JsonProperty("imgId")
    private String imgId;
    @JsonProperty("ownerId")
    private String ownerId;
    @JsonProperty("endTime")
    private Date endTime;
    @JsonProperty("minPrice")
    private int minPrice;
    @JsonProperty("winningBid")
    private Bid winnigBid;
    @JsonProperty("status")
    private String status;

    public Auction() {
        super();
    }

    public Auction(String id, String title, String desription, String imgId, String ownerId, Date endTime, int minPrice, Bid winningBid, String status) {
        this.id = id;
        this.title = title;
        this.description = desription;
        this.imgId = imgId;
        this.ownerId = ownerId;
        this.endTime = endTime;
        this.minPrice = minPrice;
        this.winnigBid = winningBid;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImgId() {
        return imgId;
    }

    public void setImgId(String imgId) {
        this.imgId = imgId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public void setOwner(String ownerId) {
        this.ownerId = ownerId;
    }

    public int getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(int minPrice) {
        this.minPrice = minPrice;
    }

    public Bid getWinnigBid() {
        return winnigBid;
    }

    public void setWinnigBid(Bid winnigBid) {
        this.winnigBid = winnigBid;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
	public String toString() {
		return "Auction [id= " + id + ", title=" + title + ", description=" + description + ", imgId=" + imgId
				+ ", ownerId=" + ownerId + ", endTime=" + endTime +  ", minPrice=" + minPrice + ", winnigBid=" + winnigBid +", status="+ status + "]";
	}
}