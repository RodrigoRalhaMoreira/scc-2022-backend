package scc.srv;

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

    private static String AUCTION_NULL = "Null auction exception";
    private static String AUCTION_NOT_EXIST = "Auction does not exist";
    private static String USER_NOT_EXIST = "User does not exist";
    private static String IMG_NOT_EXIST = "Image does not exist";

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
        if (auction == null) {
            System.out.println(AUCTION_NULL);
            return AUCTION_NULL;
        }
        if (!db_instance.getUserById(auction.getOwnerId()).iterator().hasNext()) {
            System.out.println(USER_NOT_EXIST);
            return USER_NOT_EXIST;
        }
        
        //verify if imgId exists
        if (!media.verifyImgId(auction.getImgId())) {
            System.out.println(IMG_NOT_EXIST);
            return IMG_NOT_EXIST;
        }

        // Create the user to store in the db and cache
        AuctionDAO dbAuction = new AuctionDAO(auction);

        try {
            jedis_instance.set("auction:" + auction.getId(), mapper.writeValueAsString(dbAuction));
        } catch (Exception e) {
            // TODO: handle exception
        }

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

        if (!db_instance.getUserById(auction.getOwnerId()).iterator().hasNext()) {
            System.out.println(USER_NOT_EXIST);
            return USER_NOT_EXIST;
        }
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
        } else {
            db_instance.updateAuction(dbAuction);
            return dbAuction.getId();
        }
    }
    
}