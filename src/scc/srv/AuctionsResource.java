package scc.srv;

import java.util.Iterator;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.*;

import jakarta.ws.rs.core.MediaType;
import redis.clients.jedis.Jedis;
import scc.cache.RedisCache;

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
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(Auction auction) {
        
        // Winning bids start by default with the value of null
        
        String result = checkAuction(auction);
        
        if(result != null)
            return result;
        
        else {
            
            // Status special verification when creating an auction
            if (!auction.getStatus().equals(AuctionStatus.OPEN.getStatus())) 
                result = INVALID_STATUS; 
            
            // Create the user to store in the database and cache
            AuctionDAO dbAuction = new AuctionDAO(auction);
    
            try {
                jedis_instance.set("auction:" + auction.getId(), mapper.writeValueAsString(dbAuction));
            } catch (Exception e) {
                // TODO: handle exception
            }
    
            db_instance.putAuction(dbAuction);
            return dbAuction.getTitle();
        }
    }

    /**
     * Updates an auction. Throw an appropriate error message if
     * id does not exist.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String update(Auction auction) {
        
        String result = checkAuction(auction);
        
        if(result != null)
            return result;
        
        else {

            // Status special verification when updating an auction
            if(!isValidStatus(auction.getStatus()))
                return INVALID_STATUS;
    
            AuctionDAO dbAuction = new AuctionDAO(auction);
    
            try {
                String res = jedis_instance.get("auction:"+ auction.getId());
                if (res != auction.getId() && res != null) {
                    jedis_instance.set("auction:" + auction.getId(), mapper.writeValueAsString(dbAuction));
                    db_instance.updateAuction(dbAuction);
                    return dbAuction.getId();
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
    
            if (!db_instance.getAuctionById(auction.getId()).iterator().hasNext()) {
                System.out.println(AUCTION_NOT_EXIST);
                return AUCTION_NOT_EXIST;
            } 
            
            else {
                db_instance.updateAuction(dbAuction);
                return dbAuction.getId();
            }
        }
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
    
    private String checkAuction(Auction auction) {
        
        String result = null;
        
        if (auction == null) 
            result = AUCTION_NULL;
        
        else if (auction.getId() == null)
            result = NULL_ID;
        
        else if (auction.getTitle() == null)
            result = NULL_TITLE;
        
        else if (auction.getImgId() == null)
            result = NULL_IMAGE;
        
        else if (auction.getOwnerId() == null)
            result = NULL_OWNERID;
        
        else if (auction.getEndTime() == null)
            result = NULL_ENDTIME;
        
        else if (auction.getStatus() == null)
            result = NULL_STATUS;
        
        else if (auction.getMinPrice() <= 0)
            result = NEGATIVE_MINPRICE;
        
        else if (!db_instance.getUserById(auction.getOwnerId()).iterator().hasNext())
            result = USER_NOT_EXIST;
        
        //verify if imgId exists in the database
        else if (!media.verifyImgId(auction.getImgId()))
            result = IMG_NOT_EXIST;
        
        return result;
    }
}