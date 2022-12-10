package scc.cosmosdb.models;

import scc.srv.dataclasses.Bid;

/**
 * Represents a Bid, as stored in the database
 */
public class BidDAO {
    private String _rid;
    private String _ts;
    private String id;
    private String auctionId;
    private String userId;
    private float amount;

    public BidDAO() {
    }

    public BidDAO(Bid b) {
        this(b.getId(), b.getAuctionId(), b.getUserId(), b.getAmount());
    }

    public BidDAO(String id, String auctionId, String userId, float amount) {
        super();
        this.id = id;
        this.auctionId = auctionId;
        this.userId = userId;
        this.amount = amount;
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

    public String getAuctionId() {
        return auctionId;
    }

    public String getUserId() {
        return userId;
    }

    public float getAmount() {
        return amount;
    }

    public Bid toBid() {
        return new Bid(id, auctionId, userId, amount);
    }

    @Override
    public String toString() {
        return "BidDAO [_rid=" + _rid + ", _ts=" + _ts + ", id=" + id + ", auctionId=" + auctionId
                + ", userId=" + userId + ", amount=" + amount + "]";
    }
}
