package scc.srv.cosmosdb.models;

import scc.srv.dataclasses.RecentAuction;

public class RecentAuctionDAO {
    
    private String _rid;
    private String _ts;
    private String id;
    private String endTime;
    
    public RecentAuctionDAO() {}
    
    public RecentAuctionDAO(RecentAuction ra) {
        this(ra.getId(), ra.getEndTime());
    }
    
    public RecentAuctionDAO(String id, String endTime) {
        super();
        this.id = id;
        this.endTime = endTime;
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

    public String getEndTime() {
        return endTime;
    }

    public RecentAuction toRecentAuction() {
        return new RecentAuction(id, endTime);
    }

    @Override
    public String toString() {
        return "RecentAuctionDAO [_rid=" + _rid + ", _ts=" + _ts + ", id=" + id + ", endTime=" + endTime + "]";
    }

}
