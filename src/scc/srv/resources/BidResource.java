package scc.srv.resources;

import scc.cache.RedisCache;
import scc.cosmosdb.CosmosDBLayer;
import scc.cosmosdb.models.AuctionDAO;
import scc.cosmosdb.models.BidDAO;
import scc.cosmosdb.models.UserDAO;
import scc.srv.MainApplication;
import scc.srv.dataclasses.Auction;
import scc.srv.dataclasses.AuctionStatus;
import scc.srv.dataclasses.Bid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import redis.clients.jedis.Jedis;

import java.lang.reflect.Field;
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
    private static final String NEGATIVE_VALUE = "value can not be negative or zero";
    private static final String NULL_FIELD_EXCEPTION = "Null %s exception";
    private static final String SAME_OWNER = "Owner of the bid can not bid on his own auction";
    private static final String AUCTION_NOT_OPEN = "Can only bid in an open auction";
    private static final String LOWER_THAN_MIN_VALUE = "Value can not be lower than auction's minValue";
    private static final String LOWER_BIDVALUE = "Current bid value has to be higher than current winning bid value for that auction";
    private static final int DEFAULT_REDIS_EXPIRE = 600;

    private static CosmosDBLayer db_instance;
    private UsersResource users;
    private static Jedis jedis_instance;
    private ObjectMapper mapper;

    // Improvements to be made. If we have "id" of auction as PathParam we should
    // not have to pass it as param in POST request.

    public BidResource() {
        db_instance = CosmosDBLayer.getInstance();
        jedis_instance = RedisCache.getCachePool().getResource();
        mapper = new ObjectMapper();
        for (Object resource : MainApplication.getSingletonsSet()) {
            if (resource instanceof UsersResource)
                users = (UsersResource) resource;
        }
    }

    /**
     * Creates a new bid.
     * 
     * @throws JsonProcessingException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(@CookieParam("scc:session") Cookie session, Bid bid)
            throws JsonProcessingException, IllegalArgumentException, IllegalAccessException {

        /**
         * TODO
         * REALLY IMPORTANT -->>> ACTUAL BID HAS TO BE GREATER THAT THE WINNING BID AT
         * THE MOMENT
         * IF WINNING BID IS CURRENTLY NULL (-> WINNING BID = BID.GETVALUE())
         */

        try {
            users.checkCookieUser(session, bid.getUserId());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return e.getMessage();
        }

        String result = checkBid(bid);

        if (result != null)
            return result;

        String res = jedis_instance.get("auction:" + bid.getAuctionId());
        Auction redis_auction = mapper.readValue(res, Auction.class); // Auction

        Bid auctionWinningBid = redis_auction.getWinningBid();
        String auctionOwnerId = redis_auction.getOwnerId();
        String auctionStatus = redis_auction.getStatus();
        int auctionMinPrice = redis_auction.getMinPrice();

        // bid value has to be higher than current winning bid for that auction
        if(auctionWinningBid != null && auctionWinningBid.getAmount() >= bid.getAmount())
            return LOWER_BIDVALUE;

        if (auctionOwnerId.equals(bid.getUserId()))
            return SAME_OWNER;
        
        if(!auctionStatus.equals(AuctionStatus.OPEN.getStatus()))
            return AUCTION_NOT_OPEN;
        
        if(bid.getAmount() < auctionMinPrice)
            return LOWER_THAN_MIN_VALUE;

        jedis_instance.setex("auction:" + redis_auction.getId(), DEFAULT_REDIS_EXPIRE,
                mapper.writeValueAsString(redis_auction));
        jedis_instance.setex("bid:" + bid.getId(), DEFAULT_REDIS_EXPIRE, mapper.writeValueAsString(bid));

        // Updates the auction in the database
        AuctionDAO dbAuction = new AuctionDAO(redis_auction);
        dbAuction.setWinnigBid(bid);
        db_instance.updateAuction(dbAuction);

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

        Iterator<BidDAO> bidsIt = db_instance.getBidsByAuctionId(id).iterator();

        while (bidsIt.hasNext())
            bids.add(bidsIt.next().toBid().toString());

        return bids;
    }

    // --------------------------------------------------------- PRIVATE METHODS
    // ------------------------------------

    private boolean userExistsInDB(String userId) {
        CosmosPagedIterable<UserDAO> usersIt = db_instance.getUserById(userId);
        return usersIt.iterator().hasNext();
    }

    private boolean auctionExistsInDB(String auctionId) {
        CosmosPagedIterable<AuctionDAO> auctionIt = db_instance.getAuctionById(auctionId);
        return auctionIt.iterator().hasNext();
    }

    /**
     * private boolean userExistsInRedis(String userId) {
     * String res = jedis_instance.get("user:" + userId);
     * 
     * try {
     * User redis_user = mapper.readValue(res, User.class);
     * return redis_user != null;
     * } catch (JsonProcessingException e) {
     * // TODO Auto-generated catch block
     * e.printStackTrace();
     * }
     * return false;
     * }
     * 
     * 
     * private boolean auctionExistsInRedis(String auctionId) {
     * String res = jedis_instance.get("auction:" + auctionId);
     * 
     * try {
     * Auction redis_auction = mapper.readValue(res, Auction.class);
     * return redis_auction != null;
     * } catch (JsonProcessingException e) {
     * // TODO Auto-generated catch block
     * e.printStackTrace();
     * }
     * return false;
     * }
     * 
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     **/

    private String checkBid(Bid bid) throws IllegalArgumentException, IllegalAccessException {

        if (bid == null)
            return BID_NULL;

        String res = jedis_instance.get("bid:" + bid.getId());
        if (res != null)
            return BID_ALREADY_EXISTS;

        // verify that fields are different from null
        for (Field f : bid.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (f.get(bid) == null)
                return String.format(NULL_FIELD_EXCEPTION, f.getName());
        }
        
        if (bid.getAmount() <= 0)
            return NEGATIVE_VALUE;

        if (!userExistsInDB(bid.getUserId()))
            return USER_NOT_EXISTS;

        // this does not make sense we're only doing this for the moment
        if (!auctionExistsInDB(bid.getAuctionId()))
            return AUCTION_NOT_EXISTS;

        return null;
    }
}
