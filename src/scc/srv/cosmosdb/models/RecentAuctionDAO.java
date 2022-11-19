package scc.srv.cosmosdb.models;

import scc.srv.dataclasses.RecentAuction;

public class RecentAuctionDAO {
    
    private String _rid;
    private String _ts;
    private String id;
    private String timeDifference;
    
    public RecentAuctionDAO() {}
    
    public RecentAuctionDAO(RecentAuction ra) {
        this(ra.getId(), ra.getTimeDifference());
    }
    
    public RecentAuctionDAO(String id, String timeDifference) {
        super();
        this.id = id;
        this.timeDifference = timeDifference;
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

    public String getTimeDifference() {
        return timeDifference;
    }

    public RecentAuction toRecentAuction() {
        return new RecentAuction(id, timeDifference);
    }

    @Override
    public String toString() {
        return "RecentAuctionDAO [_rid=" + _rid + ", _ts=" + _ts + ", id=" + id + ", timeDifference=" + timeDifference + "]";
    }

}