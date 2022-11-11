package scc.srv;

import jakarta.ws.rs.*;

import jakarta.ws.rs.core.MediaType;

/**
 * Resource for managing auction.
 */
@Path("/auction")
public class AuctionsResource {

    private static CosmosDBLayer db_instance = CosmosDBLayer.getInstance();

    private static String AUCTION_NULL = "Null auction exception";
    private static String AUCTION_NOT_EXIST = "Auction does not exist";

    /**
     * Creates a new auction. The id of the auction is its hash.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(Auction auction) {
        if (auction == null) {
            System.out.println(AUCTION_NULL);
            return AUCTION_NULL;
        }
        // Create the user to store in the db
        AuctionDAO dbAuction = new AuctionDAO(auction);
        db_instance.putAuction(dbAuction);
        return dbAuction.getTitle();
    }

    /**
     * Updates an auction. Throw an appropriate error message if
     * id does not exist.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String update(Auction auction) {
        if (auction == null) {
            System.out.println(AUCTION_NULL);
            return AUCTION_NULL;
        }
        if (db_instance.getAuctionById(auction.getId()) == null) {
            System.out.println(AUCTION_NOT_EXIST);
            return AUCTION_NOT_EXIST;
        }
        AuctionDAO dbAuction = new AuctionDAO(auction);
        db_instance.updateAuction(dbAuction);
        return dbAuction.getId();
    }

    
}