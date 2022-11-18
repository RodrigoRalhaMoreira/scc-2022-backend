package scc.srv.cosmosdb.models;

import scc.srv.dataclasses.PopularAuction;

public class PopularAuctionDAO {
    
    private String _rid;
    private String _ts;
    private String id;
    private int count;
    
    public PopularAuctionDAO() {}
    
    public PopularAuctionDAO(PopularAuction pa) {
        this(pa.getId(), pa.getCount());
    }
    
    public PopularAuctionDAO(String id, int count) {
        super();
        this.id = id;
        this.count = count;
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

    public int getCount() {
        return count;
    }

    public PopularAuction toPopularAuction() {
        return new PopularAuction(id, count);
    }

    @Override
    public String toString() {
        return "PopularAuctionDAO [_rid=" + _rid + ", _ts=" + _ts + ", id=" + id + ", count=" + count + "]";
    }
}
