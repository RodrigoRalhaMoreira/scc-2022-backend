package scc.srv;

import java.util.Date;

public class AuctionDAO {

    private String _rid;
    private String _ts;
    private String id;
    private String title;
    private String description;
    private String imgId;
    private String ownerId;
    private Date endTime;
    private int minPrice;
    private int winnigBid;
    private String status;

    public AuctionDAO() {}

    public AuctionDAO(Auction a) {
        this(a.getId(), a.getTitle(), a.getDescription(), a.getImgId(), a.getOwnerId(), a.getEndTime(), a.getMinPrice(), a.getWinnigBid(), a.getStatus());
    }

    public AuctionDAO(String id, String title, String desription, String imgId, String ownerId, Date endTime, int minPrice, int winningBid, String status) {
        super();
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
    
	public String get_rid() {
		return _rid;
	}

	public void set_rid(String _rid) {
		this._rid = _rid;
	}

	public String get_ts() {
		return _ts;
	}

	public void set_ts(String _ts) {
		this._ts = _ts;
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

    public void setImg(String imgId) {
        this.imgId = imgId;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwner(String ownerId) {
        this.ownerId = ownerId;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public int getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(int minPrice) {
        this.minPrice = minPrice;
    }

    public int getWinnigBid() {
        return winnigBid;
    }

    public void setWinnigBid(int winnigBid) {
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
		return "AuctionDAO [_rid=" + _rid + ", _ts=" + _ts + ", id= " + id +", title=" + title + ", description=" + description + ", imgId=" + imgId
				+ ", ownerId=" + ownerId +  ", endTime=" + endTime + ", minPrice=" + minPrice + ", winnigBid=" + winnigBid +", status="+ status + "]";
	}
}