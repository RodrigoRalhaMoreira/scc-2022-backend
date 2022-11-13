package scc.srv;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.azure.cosmos.util.CosmosPagedIterable;

/**
 * Resource for managing bids.
 */
@Path("/auction/{id}/bid")
public class BidResource {
    private static final String BID_NULL = "Error creating null bid";
    private static final String USER_NOT_EXISTS = "Error non-existent user";
    private static final String AUCTION_NOT_EXISTS = "Error non-existent auction";

    private static CosmosDBLayer db_instance;

    // Improvements to be made. If we have "id" of auction as PathParam we should
    // not have to pass it as param in POST request.

    public BidResource() {
        db_instance = CosmosDBLayer.getInstance();
    }

    /**
     * Creates a new bid.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(Bid bid) {
        if (bid == null)
            return BID_NULL;

        if (!userExistsInDB(bid.getUserId()))
            return USER_NOT_EXISTS;

        // this does not make sense we're only doing this for the moment
        if (!auctionExistsInDB(bid.getAuctionId()))
            return AUCTION_NOT_EXISTS;

        // Create the bid to store in the db
        BidDAO dbbid = new BidDAO(bid);

        db_instance.putBid(dbbid);
        return dbbid.getId();
    }

    /**
     * Get all the bids for this auction.
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> list(@PathParam("id") String id) {
        List<String> bids = new ArrayList<>();

        // this does not make sense we're only doing this for the moment
        if (!auctionExistsInDB(id))
            return bids;

        CosmosPagedIterable<BidDAO> bidsIterable = db_instance.getBidsByAuctionId(id);
        Iterator<BidDAO> bidsIt = bidsIterable.iterator();
        while (bidsIt.hasNext()) {
            bids.add(bidsIt.next().toString());
        }
        return bids;
    }

    private boolean userExistsInDB(String userId) {
        CosmosPagedIterable<UserDAO> usersIt = db_instance.getUserById(userId);
        return usersIt.iterator().hasNext();
    }

    private boolean auctionExistsInDB(String auctionId) {
        CosmosPagedIterable<AuctionDAO> auctionIt = db_instance.getAuctionById(auctionId);
        return auctionIt.iterator().hasNext();
    }
}
