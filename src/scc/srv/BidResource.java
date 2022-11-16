package scc.srv;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Resource for managing bids.
 */
@Path("/auction/{id}/bid")
public class BidResource {
    
    private static final String BID_NULL = "Null bid exception";
    private static final String USER_NOT_EXISTS = "User does not exist";
    private static final String AUCTION_NOT_EXISTS = "Auction does not exist";
    
    private static final String NEGATIVE_VALUE = "value can not be negative or zero";
    private static final String NULL_FIELD_EXCEPTION = "Null %s exception";
    private static final String LOWER_BIDVALUE = "Current bid value has to be higher than "
                                                                    + "current winning bid value for that auction";
    
    private static CosmosDBLayer db_instance;
    private static Jedis jedis_instance;  
    private ObjectMapper mapper;
    
    private AuctionsResource auctions;
    
    // Improvements to be made. If we have "id" of auction as PathParam we should
    // not have to pass it as param in POST request.

    public BidResource() {
        db_instance = CosmosDBLayer.getInstance();
        jedis_instance = RedisCache.getCachePool().getResource();
        mapper = new ObjectMapper();
        
        for(Object resource : MainApplication.getSingletonsSet()) 
            if(resource instanceof AuctionsResource)
                auctions = (AuctionsResource) resource;
    }

    /**
     * Creates a new bid.
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(Bid bid) throws IllegalArgumentException, IllegalAccessException {
        
        String result = checkBid(bid);
        
        if(result != null)
            return result;
        
        Bid auctionWinningBid = auctions.getAuctionWinningBid(bid.getAuctionId());
        
        // bid value has to be higher than current winning bid for that auction
        if(auctionWinningBid != null && auctionWinningBid.getValue() >= bid.getValue())
            return LOWER_BIDVALUE;
        
        Auction auction = auctions.getAuctionById(bid.getAuctionId());
        AuctionDAO dbAuction = new AuctionDAO(auction);
        
        try {
            // No need to read from the cache, only write it
            dbAuction.setWinnigBid(bid);
            jedis_instance.set("auction:" + auction.getId(), mapper.writeValueAsString(dbAuction));
            db_instance.updateAuction(dbAuction);
            
        } catch (Exception e) {
            // TODO: handle exception
        }
        
        db_instance.updateAuction(dbAuction);
        
        // Create the bid to store in the database
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
    
    // ----------------------------------------------  PRIVATE METHODS  ---------------------------------------------

    private boolean userExistsInDB(String userId) {
        CosmosPagedIterable<UserDAO> usersIt = db_instance.getUserById(userId);
        return usersIt.iterator().hasNext();
    }

    private boolean auctionExistsInDB(String auctionId) {
        CosmosPagedIterable<AuctionDAO> auctionIt = db_instance.getAuctionById(auctionId);
        return auctionIt.iterator().hasNext();
    }
    
    private String checkBid(Bid bid) throws IllegalArgumentException, IllegalAccessException{
        
        if (bid == null)
            return BID_NULL;
        
        // verify that fields are different from null
        for (Field f : bid.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (f.get(bid) == null)
                return String.format(NULL_FIELD_EXCEPTION, f.getName());
        }
        
        if (bid.getValue() <= 0)
            return NEGATIVE_VALUE;
        
        if (!userExistsInDB(bid.getUserId()))
            return USER_NOT_EXISTS;

        // this does not make sense we're only doing this for the moment
        if (!auctionExistsInDB(bid.getAuctionId()))
            return AUCTION_NOT_EXISTS;
        
        return null;
    }
}
