package scc.srv.resources;

import scc.cache.RedisCache;
import scc.srv.MainApplication;
import scc.srv.Session;
import scc.srv.cosmosdb.CosmosDBLayer;
import scc.srv.cosmosdb.models.AuctionDAO;
import scc.srv.cosmosdb.models.BidDAO;
import scc.srv.cosmosdb.models.QuestionDAO;
import scc.srv.cosmosdb.models.UserDAO;
import scc.srv.dataclasses.Login;
import scc.srv.dataclasses.User;
import jakarta.ws.rs.*;
import redis.clients.jedis.Jedis;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Resource for managing users.
 */
@Path("/user")
public class UsersResource {

    private static final String USER_NULL = "Error creating null user";
    private static final String IMG_NOT_EXIST = "Image does not exist";
    private static final String UPDATE_ERROR = "Error updating non-existent user";
    private static final String DELETE_ERROR = "Error deleting non-existent user";
    private static final String INVALID_LOGIN = "UserId or password incorrect";
    private static final String USER_ALREADY_EXISTS = "UserId already exists";
    private static final String LOGIN_NULL = "Login null exception";
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

        for (Object resource : MainApplication.getSingletonsSet())
            if (resource instanceof MediaResource)
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
    public User create(User user) throws IllegalArgumentException, IllegalAccessException {
        
        String error = checkUser(user);
        
        if(error != null)
            return null; // error;

        String res = jedis_instance.get("user:" + user.getId());
        if (res != null)
            return null; // USER_ALREADY_EXISTS;

        UserDAO userDao = new UserDAO(user);
        try {
            jedis_instance.set("user:" + user.getId(), mapper.writeValueAsString(user));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        db_instance.putUser(userDao);
        return userDao.toUser();
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
    public String update(@CookieParam("scc:session") Cookie session, User user) throws IllegalArgumentException, IllegalAccessException {

        try {
            checkCookieUser(session, user.getId());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return e.getMessage();
        }

        String error = checkUser(user);
        
        if(error != null)
            return error;

        UserDAO userDao = new UserDAO(user);
        try {
            String res = jedis_instance.get("user:" + user.getId());
            if (res != null) {
                jedis_instance.set("user:" + user.getId(), mapper.writeValueAsString(user));
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
    public String delete(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {

        try {
            checkCookieUser(session, id);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return e.getMessage();
        }

        int removed = 0;
        if (userExistsInDB(id)) {
            db_instance.delUserById(id);
            removed = 1;
        }
        
        jedis_instance.del("user:" + id);
        return removed > 0 ? id : DELETE_ERROR;
    }

    // not sure if we want this information cached (map with string(UserId) ->
    // set<Auctions>)
    @Path("/{id}/auctions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getAuctionsOfUser(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {

        try {
            checkCookieUser(session, id);
        } catch (Exception e) {
            List<String> error = new ArrayList<String>();
            error.add(e.getMessage());
            return error;
        }

        List<String> auctions = new ArrayList<>();
        Iterator<AuctionDAO> it = db_instance.getAuctionsByUserId(id).iterator();
        while (it.hasNext()) {
            auctions.add((it.next().toAuction()).toString());
        }
        return auctions;
    }

    /**
     * Login Method
     * 
     * @param login
     * @return
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    @Path("/auth")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response auth(Login login) {

        UserDAO user = null;

        if (!userExistsInDB(login.getId()))
            throw new NotAuthorizedException(INVALID_LOGIN);
        if(!db_instance.getUserById(login.getId()).iterator().next().getPwd().equals(login.getPwd()))
            throw new NotAuthorizedException(INVALID_LOGIN);
            
            user = db_instance.getUserById(login.getId()).iterator().next();

            String uid = UUID.randomUUID().toString();

                NewCookie cookie = new NewCookie.Builder("scc:session")
                                        .value(uid)
                                        .path("/")
                                        .comment("sessionid")
                                        .maxAge(3600)
                                        .secure(false)
                                        .httpOnly(true)
                                        .build();
                
            try {
                jedis_instance.setex("session:"+ uid, 300, mapper.writeValueAsString(new Session(uid, user.getId())));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return Response.ok().cookie(cookie).build();
    }
    
    @Path("/{id}/auctions?status=\"OPEN\"")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> openAuctionsUserList(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {

        try {
            checkCookieUser(session, id);
        } catch (Exception e) {
            List<String> error = new ArrayList<String>();
            error.add(e.getMessage());
            return error;
        }

        List<String> openAuctions = new ArrayList<>(); 
        
        Iterator<AuctionDAO> it = db_instance.getOpenAuctions(id).iterator();
        while(it.hasNext())
            openAuctions.add(it.next().toAuction().toString());
        
        return openAuctions;
    }

    @Path("/{id}/following")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> following(@CookieParam("scc:session") Cookie session,  @PathParam("id") String id) {

        try {
            checkCookieUser(session, id);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            List<String> error = new ArrayList<String>();
            error.add(e.getMessage());
            return error;
        }

        List<String> winningBidAuctions = new ArrayList<>(); 
        List<String> bidAuctions = new ArrayList<>(); 
        List<String> questionsAuctions = new ArrayList<>(); 
        
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
        /**
    * Throws exception if not appropriate user for operation on Auction
         * @throws Exception
    */
    public String checkCookieUser(Cookie session, String id)
        throws Exception {

        if (session == null || session.getValue() == null)
           throw new Exception("No session initialized");

        Session s = null;

        try {
            s = mapper.readValue(jedis_instance.get("session:" + session.getValue()), Session.class);
        } catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (s == null || s.getUserId() == null || s.getUserId().length() == 0)
            throw new Exception("No valid session initialized");
        if (!s.getUserId().equals(id))
            throw new Exception("Invalid user : " + s.getUserId());
        return s.toString();
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
}
