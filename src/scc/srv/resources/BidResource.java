package scc.srv.resources;

import scc.cache.RedisCache;
import scc.srv.dataclasses.Bid;
import scc.srv.MainApplication;
import scc.cosmosdb.CosmosDBLayer;
import scc.srv.dataclasses.Auction;
import scc.cosmosdb.models.AuctionDAO;
import scc.cosmosdb.models.BidDAO;
import scc.srv.dataclasses.AuctionStatus;

import java.util.List;
import jakarta.ws.rs.*;
import java.util.Iterator;
import java.util.ArrayList;
import java.lang.reflect.Field;
import jakarta.ws.rs.core.Cookie;
import redis.clients.jedis.Jedis;
import jakarta.ws.rs.core.MediaType;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Resource for managing bids.
 */
@Path("/auction")
public class BidResource {

    private static final String BID_NULL = "Null bid exception";
    private static final String NULL_FIELD_EXCEPTION = "Null %s exception";
    private static final String AUCTION_NOT_EXISTS = "Auction does not exist";
    private static final String BID_ALREADY_EXISTS = "AuctionId already exists";
    private static final String AUCTION_NOT_OPEN = "Can only bid in an open auction";
    private static final String NEGATIVE_VALUE = "value can not be negative or zero";
    private static final String SAME_OWNER = "Owner of the bid can not bid on his own auction";
    private static final String LOWER_THAN_MIN_VALUE = "Value can not be lower than auction's minValue";
    private static final String LOWER_BIDVALUE = "Current bid value has to be higher than current winning bid value for that auction";
    private static final int DEFAULT_REDIS_EXPIRE = 600;

    private UsersResource users;
    private ObjectMapper mapper;
    private static Jedis jedis_instance;
    private static CosmosDBLayer db_instance;

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
     * @throws Exception
     */
    @Path("/{id}/bid")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Bid create(@CookieParam("scc:session") Cookie session, Bid bid)
            throws Exception {

        // users.checkCookieUser(session, bid.getUserId());
        checkBid(bid);

        String res = jedis_instance.get("auction:" + bid.getAuctionId());
        Auction auction;
        if (res == null) {
            AuctionDAO newAuction = getAuctionInDB(bid.getAuctionId());

            if (newAuction == null)
                throw new Exception("Without Auction");

            auction = newAuction.toAuction();
        } else
            auction = mapper.readValue(res, Auction.class); // Auction

        if (auction.getWinningBid() != null && auction.getWinningBid().getAmount() >= bid.getAmount())
            throw new Exception("Invalid Amount");

        if (auction.getOwnerId().equals(bid.getUserId()))
            throw new Exception("Same user");

        if (!auction.getStatus().equals(AuctionStatus.OPEN.getStatus()))
            throw new Exception("Auction Closed");

        if (bid.getAmount() < auction.getMinPrice())
            throw new Exception("Invalid Amount 2");

        // Updates the auction in the database
        auction.setWinningBid(bid);
        AuctionDAO dbAuction = new AuctionDAO(auction);
        db_instance.updateAuction(dbAuction);

        // bid value has to be higher than current winning bid for that auction
        jedis_instance.setex("auction:" + auction.getId(), DEFAULT_REDIS_EXPIRE,
                mapper.writeValueAsString(auction));
        jedis_instance.setex("bid:" + bid.getId(), DEFAULT_REDIS_EXPIRE, mapper.writeValueAsString(bid));

        // Create the bid to store in the database
        BidDAO dbbid = new BidDAO(bid);
        db_instance.putBid(dbbid);
        return bid;

    }

    /**
     * Get all the bids for this auction.
     */
    @Path("/{id}/bid")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> list(@PathParam("id") String id) {

        List<String> bids = new ArrayList<>();

        // this does not make sense we're only doing this for the moment
        if (getAuctionInDB(id) == null)
            return bids;

        Iterator<BidDAO> bidsIt = db_instance.getBidsByAuctionId(id).iterator();

        while (bidsIt.hasNext())
            bids.add(bidsIt.next().toBid().toString());

        return bids;
    }

    // PRIVATE METHODS

    /*
     * Returns auctions from DB with auctionId or null
     * 
     */
    private AuctionDAO getAuctionInDB(String auctionId) {
        CosmosPagedIterable<AuctionDAO> auctionIt = db_instance.getAuctionById(auctionId);
        if (auctionIt.iterator().hasNext())
            return auctionIt.iterator().next();
        return null;
    }

    /**
     * Checks if a bid is valid
     * 
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     **/

    private void checkBid(Bid bid) throws IllegalArgumentException, IllegalAccessException, Exception {

        if (bid == null)
            throw new Exception("Bid null");

        String res = jedis_instance.get("bid:" + bid.getId());
        if (res != null)
            throw new Exception(res + "WTF");
        if (db_instance.getBidById(bid.getId()).iterator().hasNext())
            throw new Exception(res + "BLABLABLA");
        // verify that fields are different from null
        for (Field f : bid.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (f.get(bid) == null)
                throw new Exception(String.format(NULL_FIELD_EXCEPTION, f.getName()));
            ;
        }

        if (bid.getAmount() <= 0)
            throw new Exception(NEGATIVE_VALUE);
    }
}
