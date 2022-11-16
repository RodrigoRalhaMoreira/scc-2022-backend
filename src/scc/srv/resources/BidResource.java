package scc.srv.resources;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.srv.MainApplication;
import scc.srv.cosmosdb.CosmosDBLayer;
import scc.srv.cosmosdb.models.AuctionDAO;
import scc.srv.cosmosdb.models.BidDAO;
import scc.srv.cosmosdb.models.UserDAO;
import scc.srv.dataclasses.Auction;
import scc.srv.dataclasses.Bid;
import scc.srv.dataclasses.User;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Resource for managing bids.
 */
@Path("/auction/{id}/bid")
public class BidResource {
    
    private static final String BID_NULL = "Null bid exception";
    private static final String USER_NOT_EXISTS = "User does not exist";
    private static final String AUCTION_NOT_EXISTS = "Auction does not exist";
    
    private static final String NEGATIVE_VALUE = "Value can not be negative or zero";
    private static final String NULL_FIELD_EXCEPTION = "Null %s exception";
    private static final String SAME_OWNER = "Owner of the bid can not bid on his own auction";
    private static final String AUCTION_NOT_OPEN = "Can only bid in an open auction";
    private static final String LOWER_THAN_MIN_VALUE = "Value can not be lower than auction's minValue";
    private static final String LOWER_BIDVALUE = "Current bid value has to be higher than current winning bid value for that auction";
    
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
     * @throws JsonProcessingException 
     * @throws JsonMappingException 
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(Bid bid) throws IllegalArgumentException, IllegalAccessException, JsonMappingException, JsonProcessingException {
        
        String result = checkBid(bid);
        
        if(result != null)
            return result; 
        
        String res = jedis_instance.get("auction:" + bid.getAuctionId());
        Auction redis_auction = mapper.readValue(res, Auction.class); //Auction
        
        Bid auctionWinningBid = redis_auction.getWinnigBid();
        String auctionOwnerId = redis_auction.getOwnerId();
        String auctionStatus = redis_auction.getStatus();
        int auctionMinPrice = redis_auction.getMinPrice(); 
        
        // bid value has to be higher than current winning bid for that auction
        if(auctionWinningBid != null && auctionWinningBid.getValue() >= bid.getValue())
            return LOWER_BIDVALUE;
        
        if(auctionOwnerId.equals(bid.getUserId()))
            return SAME_OWNER;
        
        if(!auctionStatus.equals("open"))
            return AUCTION_NOT_OPEN;
        
        if(bid.getValue() < auctionMinPrice)
            return LOWER_THAN_MIN_VALUE;
        
        AuctionDAO dbAuction = new AuctionDAO(redis_auction);
        
        try {
            dbAuction.setWinnigBid(bid);
            jedis_instance.set("auction:" + redis_auction.getId(), mapper.writeValueAsString(redis_auction));
            jedis_instance.set("bid:" + bid.getId(), mapper.writeValueAsString(bid));
        } catch (Exception e) {
            // TODO: handle exception
        }
        
        // Updated the auction in the database
        db_instance.updateAuction(dbAuction);
        
        // AuctionDAO dbAuction = new AuctionDAO(redis_auction);
        BidDAO dbBid = new BidDAO(bid);
        // Create the bid to store in the database
        db_instance.putBid(dbBid);
        return dbBid.getId();
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
        if (!auctionExistsInRedis(id) || !auctionExistsInDB(id))
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
    
    private boolean userExistsInRedis(String userId) {
        String res = jedis_instance.get("user:" + userId);
        
        try {
            User redis_user = mapper.readValue(res, User.class);
            return redis_user != null;
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        return false;
    }

    private boolean auctionExistsInRedis(String auctionId) {
        String res = jedis_instance.get("auction:" + auctionId);
        
        try {
            Auction redis_auction = mapper.readValue(res, Auction.class);
            return redis_auction != null;
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        return false;
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
        
        if (!userExistsInRedis(bid.getUserId()) || !userExistsInDB(bid.getUserId()))
            return USER_NOT_EXISTS;
        
        if (!auctionExistsInRedis(bid.getAuctionId()) || !auctionExistsInDB(bid.getAuctionId()))
            return AUCTION_NOT_EXISTS;
        
        return null;
    }
}
