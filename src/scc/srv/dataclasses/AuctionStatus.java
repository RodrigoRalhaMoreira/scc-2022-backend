package scc.srv.dataclasses;

public enum AuctionStatus {
    
    OPEN("open", 0),
    CLOSE("closed", 1),  
    DELETED("deleted", 2);

    private String status;
    private int value;

    AuctionStatus(String status, int value) {
        this.status = status;
        this.value = value;
    }

    public String getStatus() {
        return status;
    }
    
    public int getValue() {
        return value;
    }
}