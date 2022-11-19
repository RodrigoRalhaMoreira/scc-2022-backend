package scc.srv.resources;

import scc.cache.RedisCache;
import scc.cosmosdb.CosmosDBLayer;
import scc.cosmosdb.models.AuctionDAO;
import scc.cosmosdb.models.BidDAO;
import scc.cosmosdb.models.QuestionDAO;
import scc.cosmosdb.models.UserDAO;
import scc.srv.MainApplication;
import scc.srv.dataclasses.Login;
import scc.srv.dataclasses.Session;
import scc.srv.dataclasses.User;

import java.util.UUID;
import java.util.List;
import jakarta.ws.rs.*;
import java.util.Iterator;
import java.util.ArrayList;
import java.lang.reflect.Field;
import jakarta.ws.rs.core.Cookie;
import redis.clients.jedis.Jedis;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.MediaType;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Resource for managing users.
 */
@Path("/user")
public class UsersResource {

    private static final int DEFAULT_REDIS_EXPIRE = 600;
    private static final String NOT_AUTH = "User not auth";
    private static final String IMG_NOT_EXIST = "Image does not exist";
    private static final String USER_NULL = "Error creating null user";
    private static final String NULL_FIELD_EXCEPTION = "Null %s exception";
    private static final String USER_ALREADY_EXISTS = "UserId already exists";
    private static final String INVALID_LOGIN = "UserId or password incorrect";
    private static final String UPDATE_ERROR = "Error updating non-existent user";
    private static final String DELETE_ERROR = "Error deleting non-existent user";

    private ObjectMapper mapper;
    private MediaResource media;
    private static Jedis jedis_instance;
    private static CosmosDBLayer db_instance;

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
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(User user) throws IllegalArgumentException, IllegalAccessException {

        String error = checkUser(user);

        if (error != null)
            return error;

        String res = jedis_instance.get("user:" + user.getId());
        if (res != null)
            return USER_ALREADY_EXISTS;

        UserDAO userDao = new UserDAO(user);
        try {
            jedis_instance.setex("user:" + user.getId(), DEFAULT_REDIS_EXPIRE, mapper.writeValueAsString(user));
        } catch (Exception e) {
            e.printStackTrace();
        }

        db_instance.putUser(userDao);
        return userDao.getId();
    }

    /**
     * Updates a user. Throw an appropriate error message if
     * id does not exist.
     * 
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String update(@CookieParam("scc:session") Cookie session, User user)
            throws IllegalArgumentException, IllegalAccessException {

        try {
            checkCookieUser(session, user.getId());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return e.getMessage();
        }

        String error = checkUser(user);

        if (error != null)
            return error;

        UserDAO userDao = new UserDAO(user);
        try {
            String res = jedis_instance.get("user:" + user.getId());
            if (res != null) {
                jedis_instance.setex("user:" + user.getId(), DEFAULT_REDIS_EXPIRE, mapper.writeValueAsString(user));
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
        if (!db_instance.getUserById(login.getId()).iterator().next().getPwd().equals(login.getPwd()))
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
            jedis_instance.setex("session:" + uid, DEFAULT_REDIS_EXPIRE,
                    mapper.writeValueAsString(new Session(uid, user.getId())));
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
        while (it.hasNext())
            openAuctions.add(it.next().toAuction().toString());

        return openAuctions;
    }

    @Path("/{id}/following")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> following(@CookieParam("scc:session") Cookie session, @PathParam("id") String id) {

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

        Iterator<AuctionDAO> itwb = db_instance.getAuctionUserFollow(id).iterator();
        while (itwb.hasNext())
            winningBidAuctions.add("  Winning Bidded Auction Id: " + itwb.next().getWinnigBid().getAuctionId());

        Iterator<BidDAO> itb = db_instance.getBidsByUserId(id).iterator();
        while (itb.hasNext())
            bidAuctions.add("  Bidded Auction Id: " + itb.next().getAuctionId());

        Iterator<QuestionDAO> itq = db_instance.getQuestionsByUserId(id).iterator();
        while (itq.hasNext())
            questionsAuctions.add("  Questioned Auction Id: " + itq.next().getAuctionId());

        List<String> newList = new ArrayList<>();

        newList.addAll(winningBidAuctions);
        newList.addAll(bidAuctions);
        newList.addAll(questionsAuctions);

        return newList;
    }

    /**
     * Throws exception if not appropriate user for operation on Auction
     * 
     * @throws Exception
     */
    public String checkCookieUser(Cookie session, String id)
            throws Exception {

        if (session == null || session.getValue() == null)
            throw new Exception("No session initialized");

        Session s = null;
        String session_res = jedis_instance.get("session:" + session.getValue());
        if (session_res == null)
            return NOT_AUTH;
        s = mapper.readValue(session_res, Session.class);

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

    private String checkUser(User user) throws IllegalArgumentException, IllegalAccessException {

        if (user == null)
            return USER_NULL;

        // verify that fields are different from null excepts channelIds
        for (Field f : user.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (f.get(user) == null && !f.getName().matches("channelIds"))
                return String.format(NULL_FIELD_EXCEPTION, f.getName());
        }

        // verifies if imageId exists
        if (!media.verifyImgId(user.getPhotoId()))
            return IMG_NOT_EXIST;

        return null;
    }
}
