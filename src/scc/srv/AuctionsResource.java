package scc.srv;

import jakarta.ws.rs.*;

import jakarta.ws.rs.core.MediaType;

/**
 * Resource for managing users.
 */
@Path("/auction")
public class AuctionsResource {

    private static CosmosDBLayer db_instance = CosmosDBLayer.getInstance();

    /**
     * Creates a new user.The id of the user is its hash.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(Auction auction) {
        if (auction == null) {
            System.out.println("Null auction exception");
        }
        // Create the user to store in the db
        AuctionDAO dbAuction = new AuctionDAO(auction);
        db_instance.putAuction(dbAuction);
        return dbAuction.getTitle();
    }

    /**
     * Updates a user. Throw an appropriate error message if
     * id does not exist.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String update(Auction auction) {
        if (auction == null) {
            System.out.println("Null auction exception");
        }
        if (db_instance.getAuctionById(auction.getId()) == null)
            System.out.println("Auction does not exist");
        AuctionDAO dbAuction = new AuctionDAO(auction);
        db_instance.updateAuction(dbAuction);
        return dbAuction.getId();
    }

    
}