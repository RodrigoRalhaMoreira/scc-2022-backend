package scc.srv;

public enum AuctionStatus {
    CLOSE("closed"), OPEN("open"), DELETED("deleted");

    private String status;

    AuctionStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
