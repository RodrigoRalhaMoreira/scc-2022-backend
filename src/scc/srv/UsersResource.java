package scc.srv;

import scc.cache.RedisCache;

import jakarta.ws.rs.*;
import redis.clients.jedis.Jedis;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

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

    private static CosmosDBLayer db_instance;
    private static Jedis jedis_instance;
    private ObjectMapper mapper;

    private MediaResource media;

    public UsersResource() {
        db_instance = CosmosDBLayer.getInstance();
        jedis_instance = RedisCache.getCachePool().getResource();
        mapper = new ObjectMapper();

        for (Object resource : MainApplication.getSingletonsSet())
            if (resource instanceof MediaResource) {
                media = (MediaResource) resource;
                break;
            }
    }

    /**
     * Creates a new user.The id of the user is its hash.
     * 
     * @throws JsonProcessingException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(User user) throws JsonProcessingException {
        if (user == null)
            return USER_NULL;

        // verify if imgId exists
        if (!media.verifyImgId(user.getPhotoId())) {
            return IMG_NOT_EXIST;
        }

        String res = jedis_instance.get("user:" + user.getId());
        if (res != null)
            return USER_ALREADY_EXISTS;

        UserDAO userDao = new UserDAO(user);

        jedis_instance.set("user:" + user.getId(), mapper.writeValueAsString(user));

        db_instance.putUser(userDao);
        return userDao.getId();
    }

    /**
     * Updates a user. Throw an appropriate error message if
     * id does not exist.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String update(@CookieParam("scc:session") Cookie session, User user) {

        if (user == null) {
            return USER_NULL;
        }

        try {
            checkCookieUser(session, user.getId());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return e.getMessage();
        }

        // verify if imgId exists
        if (!media.verifyImgId(user.getPhotoId())) {
            System.out.println(IMG_NOT_EXIST);
            return IMG_NOT_EXIST;
        }

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
            UserDAO user = db_instance.getUserById(session.getValue()).iterator().next();
            user.setDeletedUser();
            // a trigger is called upon this update to handle user auctions and bids and
            // delete him after
            db_instance.updateUser(user);
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
            jedis_instance.setex("session:" + uid, 300, mapper.writeValueAsString(new Session(uid, user.getId())));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return Response.ok().cookie(cookie).build();
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

        List<String> auctions = new ArrayList<>();

        Iterator<AuctionDAO> it = db_instance.getAuctionUserFollow(id).iterator();
        while (it.hasNext())
            auctions.add(it.next().toAuction().toString());

        return auctions;
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

        Session s = mapper.readValue(jedis_instance.get("session:" + session.getValue()), Session.class);

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
}
