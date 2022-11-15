package scc.srv;

import scc.cache.RedisCache;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Resource for managing bids.
 */
@Path("/auction/{id}/bid")
public class BidResource {

    private static final String BID_NULL = "Null bid exception";
    private static final String USER_NOT_EXISTS = "User does not exist";
    private static final String AUCTION_NOT_EXISTS = "Auction does not exist";
    private static final String BID_ALREADY_EXISTS = "AuctionId already exists";

    private static final String NULL_ID = "Null id exception";
    private static final String NULL_AUCTIONID = "Null auctionId exception";
    private static final String NULL_USERID = "Null userId exception";
    private static final String NEGATIVE_VALUE = "value can not be negative or zero";

    private static CosmosDBLayer db_instance;
    private static Jedis jedis_instance;
    private ObjectMapper mapper;

    // Improvements to be made. If we have "id" of auction as PathParam we should
    // not have to pass it as param in POST request.

    public BidResource() {
        db_instance = CosmosDBLayer.getInstance();
        jedis_instance = RedisCache.getCachePool().getResource();
        mapper = new ObjectMapper();
    }

    /**
     * Creates a new bid.
     * 
     * @throws JsonProcessingException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(Bid bid) throws JsonProcessingException {

        /**
         * TODO
         * REALLY IMPORTANT -->>> ACTUAL BID HAS TO BE GREATER THAT THE WINNING BID AT
         * THE MOMENT
         * IF WINNING BID IS CURRENTLY NULL (-> WINNING BID = BID.GETVALUE())
         */

        String error = checkBid(bid);

        if (error != null)
            return error;

        jedis_instance.set("bid:" + bid.getId(), mapper.writeValueAsString(bid));

        // Create the bid to store in the database
        BidDAO dbbid = new BidDAO(bid);

        db_instance.putBid(dbbid);
        return dbbid.getId();

    }

    // Another one that we have to decide wether we want this info on cache or not.
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

    // ---------------------------------------------- PRIVATE METHODS
    // ---------------------------------------------

    private boolean userExistsInDB(String userId) {
        CosmosPagedIterable<UserDAO> usersIt = db_instance.getUserById(userId);
        return usersIt.iterator().hasNext();
    }

    private boolean auctionExistsInDB(String auctionId) {
        CosmosPagedIterable<AuctionDAO> auctionIt = db_instance.getAuctionById(auctionId);
        return auctionIt.iterator().hasNext();
    }

    private String checkBid(Bid bid) {

        String result = null;

        if (bid == null)
            result = BID_NULL;

        String res = jedis_instance.get("bid:" + bid.getId());
        if (res != null)
            return BID_ALREADY_EXISTS;

        else if (bid.getId() == null)
            result = NULL_ID;

        else if (bid.getAuctionId() == null)
            result = NULL_AUCTIONID;

        else if (bid.getUserId() == null)
            result = NULL_USERID;

        else if (bid.getValue() <= 0)
            result = NEGATIVE_VALUE;

        else if (!userExistsInDB(bid.getUserId()))
            return USER_NOT_EXISTS;

        // this does not make sense we're only doing this for the moment
        else if (!auctionExistsInDB(bid.getAuctionId()))
            return AUCTION_NOT_EXISTS;

        return result;
    }
}
