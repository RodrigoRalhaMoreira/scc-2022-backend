package scc.srv.resources;

import scc.cache.RedisCache;
import scc.srv.MainApplication;
import scc.srv.cosmosdb.CosmosDBLayer;
import scc.srv.cosmosdb.models.AuctionDAO;
import scc.srv.cosmosdb.models.BidDAO;
import scc.srv.cosmosdb.models.LoginDAO;
import scc.srv.cosmosdb.models.QuestionDAO;
import scc.srv.cosmosdb.models.UserDAO;
import scc.srv.dataclasses.Login;
import scc.srv.dataclasses.User;
import jakarta.ws.rs.*;
import redis.clients.jedis.Jedis;
import jakarta.ws.rs.core.MediaType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Resource for managing users.
 */
@Path("/user")
public class UsersResource {

    private static final String USER_NULL = "User null exception";
    private static final String LOGIN_NULL = "Login null exception";
    private static final String UPDATE_ERROR = "Error updating non-existent user";
    private static final String DELETE_ERROR = "Error deleting non-existent user";
    private static final String IMG_NOT_EXIST = "Image does not exist";
    private static final String INVALID_LOGIN = "UserId or password incorrect";
    private static final String NULL_FIELD_EXCEPTION = "Null %s exception";
    private static final String ALREADY_AUTH = "User already authenticated";
    private static final String USER_NOT_AUTH = "User not authenticated";

    private static CosmosDBLayer db_instance;
    private static Jedis jedis_instance;
    
    private ObjectMapper mapper;
    private MediaResource media;

    public UsersResource() {
        db_instance = CosmosDBLayer.getInstance();
        jedis_instance = RedisCache.getCachePool().getResource();
        mapper = new ObjectMapper();
        
        for(Object resource : MainApplication.getSingletonsSet())
            if(resource instanceof MediaResource)
                media = (MediaResource) resource;
    }

    /**
     * Creates a new user.The id of the user is its hash.
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(User user) throws IllegalArgumentException, IllegalAccessException {
        
        String error = checkUser(user);
        
        if(error != null)
            return error;

        UserDAO userDao = new UserDAO(user);
        try {
            jedis_instance.set("user:" + user.getId(), mapper.writeValueAsString(userDao));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        db_instance.putUser(userDao);
        return userDao.getId();
    }

    /**
     * Updates a user. Throw an appropriate error message if
     * id does not exist.
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String update(User user) throws IllegalArgumentException, IllegalAccessException {
        
        String error = checkUser(user);
        
        if(error != null)
            return error;
        
        UserDAO userDao = new UserDAO(user);
        try {
            String res = jedis_instance.get("user:" + user.getId());
            if (res != user.getId() && res != null) {
                jedis_instance.set("user:" + user.getId(), mapper.writeValueAsString(userDao));
                db_instance.updateUser(userDao);
                return userDao.getId();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        if (userExistsInDB(user.getId())) {
            db_instance.updateUser(userDao);
            return userDao.getId();
        }
        
        return UPDATE_ERROR;
    }

    /**
     * Deletes a user. Throw an appropriate error message if
     * id does not exist.
     */
    @Path("/{id}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public String delete(@PathParam("id") String id) {
        int removed = 0;
        
        if (userExistsInDB(id)) {
            db_instance.delUserById(id);
            removed = 1;
        }
        
        jedis_instance.del("user:" + id);
        return removed > 0 ? id : DELETE_ERROR;
    }

    @Path("/{id}/auctions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getAuctionsOfUser(@PathParam("id") String id) {
        List<String> auctions = new ArrayList<>();
        Iterator<AuctionDAO> it = db_instance.getAuctionsByUserId(id).iterator();
        while(it.hasNext()) {
            auctions.add((it.next().toAuction()).toString());
        }
        return auctions;
    }

    /**
     * Login Method
     * @param login
     * @return
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    @Path("/auth")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String login(Login login) throws IllegalArgumentException, IllegalAccessException {
        
       UserDAO user = null;
       String error = checkLoginUser(login);
        
       if(error != null)
           return error;
        
       else {
            
           user = db_instance.getUserById(login.getId()).iterator().next();

           if (!user.getPwd().equals(login.getPwd()))
                return INVALID_LOGIN;
   
            LoginDAO loginDAO = new LoginDAO(login);

            db_instance.putLogin(loginDAO);
            return loginDAO.getId();
       }
    }
    
    @Path("/{id}/auctions?status=\"OPEN\"")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> openAuctionsUserList(@PathParam("id") String id) {

        if(!checkAuth(id))
            throw new NotAuthorizedException(USER_NOT_AUTH);

        List<String> openAuctions = new ArrayList<>(); 
        
        Iterator<AuctionDAO> it = db_instance.getOpenAuctions(id).iterator();
        while(it.hasNext())
            openAuctions.add(it.next().toAuction().toString());
        
        return openAuctions;
    }

    /**
     * We considered as following auctions those which the user did a bid on an auction or
     * asked a question
     * 
     * @param id - id of the user
     * @return
     */
    @Path("/{id}/following")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> following(@PathParam("id") String id) {

        if(!checkAuth(id))
            throw new NotAuthorizedException(USER_NOT_AUTH);

        List<String> winningBidAuctions = new ArrayList<>(); 
        List<String> bidAuctions = new ArrayList<>(); 
        List<String> questionsAuctions = new ArrayList<>(); 
         // null
        
        Iterator<AuctionDAO> itwb = db_instance.getAuctionUserFollow(id).iterator(); // getAuctionUserFollow(id)
        while(itwb.hasNext())
            winningBidAuctions.add("  Winning Bidded Auction Id: " + itwb.next().getWinnigBid().getAuctionId()); // .toBid().toString());
        
        Iterator<BidDAO> itb = db_instance.getBidsByUserId(id).iterator(); // getAuctionUserFollow(id)
        while(itb.hasNext())
            bidAuctions.add("  Bidded Auction Id: " + itb.next().getAuctionId()); // .toBid().toString());
        
        Iterator<QuestionDAO> itq = db_instance.getQuestionsByUserId(id).iterator(); // getAuctionUserFollow(id)
        while(itq.hasNext())
            questionsAuctions.add("  Questioned Auction Id: " + itq.next().getAuctionId()); //itq.next().getAuctionId()); // .toQuestion().toString());
        
        List<String> newList = new ArrayList<>();
        
        newList.addAll(winningBidAuctions);
        newList.addAll(bidAuctions);
        newList.addAll(questionsAuctions);
        
        return newList;
    }

    private boolean userExistsInDB(String userId) {
        CosmosPagedIterable<UserDAO> usersIt = db_instance.getUserById(userId);
        return usersIt.iterator().hasNext();
    }
    
    private String checkUser(User user) throws IllegalArgumentException, IllegalAccessException{
        
        if (user == null)
            return USER_NULL;
        
        // verify that fields are different from null excepts channelIds
        for (Field f : user.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (f.get(user) == null && !f.getName().matches("channelIds"))
                return String.format(NULL_FIELD_EXCEPTION, f.getName());
        }
        
        //verifies if imageId exists
        if (!media.verifyImgId(user.getPhotoId()))
            return IMG_NOT_EXIST;
        
        return null;
    }
    
    public static boolean checkAuth(String userId) {
        String res = jedis_instance.get("session:" + userId);
        return res != null;
    }
    
    private String checkLoginUser(Login login) throws IllegalArgumentException, IllegalAccessException{
        
        if (login == null)
            return LOGIN_NULL;
        
        // verify that fields are different from null
        for (Field f : login.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (f.get(login) == null)
                return String.format(NULL_FIELD_EXCEPTION, f.getName());
        }
        
        if (!userExistsInDB(login.getId())) 
            return INVALID_LOGIN;
            
        return null;
    }
}
