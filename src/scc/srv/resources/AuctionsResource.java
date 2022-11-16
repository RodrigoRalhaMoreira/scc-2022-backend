package scc.srv.resources;

import java.lang.reflect.Field;
import java.util.Iterator;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.*;

import jakarta.ws.rs.core.MediaType;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;
import scc.srv.MainApplication;
import scc.srv.cosmosdb.CosmosDBLayer;
import scc.srv.cosmosdb.models.AuctionDAO;
import scc.srv.dataclasses.Auction;
import scc.srv.dataclasses.AuctionStatus;
import scc.srv.dataclasses.Bid;

/**
 * Resource for managing auction.
 */
@Path("/auction")
public class AuctionsResource {

    private static CosmosDBLayer db_instance;
    private static Jedis jedis_instance;
    
    private ObjectMapper mapper;
    private MediaResource media;

    private static final String AUCTION_NULL = "Null auction exception";
    private static final String AUCTION_NOT_EXIST = "Auction does not exist";
    private static final String USER_NOT_EXIST = "User does not exist";
    private static final String IMG_NOT_EXIST = "Image does not exist";
    private static final String INVALID_STATUS = "Invalid status";

    private static final String NULL_ID = "Null id exception";
    private static final String NULL_TITLE = "Null title exception";
    private static final String NULL_IMAGE = "Null imageId exception";
    private static final String NULL_OWNERID = "Null ownerId exception";
    private static final String NULL_ENDTIME = "Null endTime exception";
    private static final String NULL_STATUS = "Null status exception";
    private static final String NEGATIVE_MINPRICE = "minPrice can not be negative or zero";
    private static final String NULL_FIELD_EXCEPTION = "Null %s exception";
    
    
    public AuctionsResource() {
        db_instance = CosmosDBLayer.getInstance();
        jedis_instance = RedisCache.getCachePool().getResource();
        mapper = new ObjectMapper();
        
        for(Object resource : MainApplication.getSingletonsSet()) 
            if(resource instanceof MediaResource)
                media = (MediaResource) resource;
    } 

    /**
     * Creates a new auction. The id of the auction is its hash.
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(Auction auction) throws IllegalArgumentException, IllegalAccessException {
        
        // Winning bids start by default with the value of null
        String error = checkAuction(auction);
        
        if(error != null)
            return error;
         
        // Status special verification when creating an auction
        if (!auction.getStatus().equals(AuctionStatus.OPEN.getStatus())) 
            return INVALID_STATUS; 
        
        try {
            jedis_instance.set("auction:" + auction.getId(), mapper.writeValueAsString(auction));
        } catch (Exception e) {
            // TODO: handle exception
        }
        
        // Create the user to store in the database and cache
        AuctionDAO dbAuction = new AuctionDAO(auction);
        db_instance.putAuction(dbAuction);
        return dbAuction.getTitle();
    }
    
    /**
     * Updates an auction. Throw an appropriate error message if
     * id does not exist.
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String update(Auction auction) throws IllegalArgumentException, IllegalAccessException {
        
        String error = checkAuction(auction);
        
        if(error != null)
            return error;
        
        // Actual status
        String currentStatus = getStatusAuction(auction.getId());
        int currentStatusValue = getStatusValue(currentStatus); // Value
        
        // New Status
        String newStatus = auction.getStatus(); // or dbAuction --> works as well
        int newStatusValue = getStatusValue(newStatus); // Value
        
        // Status special verification when updating an auction
        if(!isValidStatus(auction.getStatus()) || currentStatusValue > newStatusValue)
            return INVALID_STATUS;
        
        // Checks if auctionId exists
        if (!db_instance.getAuctionById(auction.getId()).iterator().hasNext()) 
            return AUCTION_NOT_EXIST;

        AuctionDAO dbAuction = new AuctionDAO(auction);
        
        try {
            String res = jedis_instance.get("auction:" + auction.getId());
            if (res != auction.getId() && res != null) {
                jedis_instance.set("auction:" + auction.getId(), mapper.writeValueAsString(dbAuction));
                db_instance.updateAuction(dbAuction);
                return "RES VALUE ---> " + res;
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        
        db_instance.updateAuction(dbAuction);
        return dbAuction.getId();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getAuction(@PathParam("id") String id) {
        Iterator<AuctionDAO> it = db_instance.getAuctionById(id).iterator();
        if (it.hasNext())
            return ((((AuctionDAO) it.next()).toAuction()).toString());
        return null;
    }

    // -----------------------------------------------------  PRIVATE METHODS ---------------------------------------------


    private boolean isValidStatus(String status) {
        return ((status.equals(AuctionStatus.OPEN.getStatus()) || 
                status.equals(AuctionStatus.CLOSE.getStatus()) || 
                status.equals(AuctionStatus.DELETED.getStatus())) ? true : false);
    }
    
    private int getStatusValue(String status) {
        
        for(AuctionStatus auctionStatus : AuctionStatus.values())
            if(status.equals(auctionStatus.getStatus()))
                return auctionStatus.getValue();
        
        return -1;
    }
    
    public Bid getAuctionWinningBid(String id) {
        Iterator<AuctionDAO> it = db_instance.getAuctionById(id).iterator();
        if (it.hasNext())
            return ((((AuctionDAO) it.next()).toAuction().getWinnigBid()));
        return null;
    }
    
    private String getStatusAuction(String id) {
        Iterator<AuctionDAO> it = db_instance.getAuctionById(id).iterator();
        if (it.hasNext())
            return ((((AuctionDAO) it.next()).toAuction().getStatus()));
        return null;
    }
    
    public Auction getAuctionById(String id) {
        Iterator<AuctionDAO> it = db_instance.getAuctionById(id).iterator();
        if (it.hasNext())
            return ((((AuctionDAO) it.next()).toAuction()));
        return null;
    }
    
    private String checkAuction(Auction auction) throws IllegalArgumentException, IllegalAccessException {

        if (auction == null)
            return AUCTION_NULL;

        // verify that fields are different from null
        for (Field f : auction.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (f.get(auction) == null && !f.getName().matches("description|winningBid"))
                return String.format(NULL_FIELD_EXCEPTION, f.getName());
        }

        if (auction.getMinPrice() <= 0)
            return NEGATIVE_MINPRICE;

        if (!db_instance.getUserById(auction.getOwnerId()).iterator().hasNext())
            return USER_NOT_EXIST;

        // verify if imgId exists in the database
        if (!media.verifyImgId(auction.getImgId()))
            return IMG_NOT_EXIST;
        
        return null;
    }
}