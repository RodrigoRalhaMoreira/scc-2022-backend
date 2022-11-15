package scc.srv;

import scc.cache.RedisCache;

import jakarta.ws.rs.*;
import redis.clients.jedis.Jedis;
import jakarta.ws.rs.core.MediaType;

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

    private static final String USER_NULL = "Error creating null user";
    private static final String UPDATE_ERROR = "Error updating non-existent user";
    private static final String DELETE_ERROR = "Error deleting non-existent user";
    private static final String INVALID_LOGIN = "UserId or password incorrect";
    private static final String IMG_NOT_EXIST = "Image does not exist";
    private static final String ALREADY_AUTH = "User already authenticated";

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
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(User user) {
        
        if (user == null)
            return USER_NULL;
        
        //verify if imgId exists
        if (!media.verifyImgId(user.getPhotoId())) {
            System.out.println(IMG_NOT_EXIST);
            return IMG_NOT_EXIST;
        }

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
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String update(User user) {
        if (user == null) {
            return USER_NULL;
        }
        
        //verify if imgId exists
        if (!media.verifyImgId(user.getPhotoId())) {
            System.out.println(IMG_NOT_EXIST);
            return IMG_NOT_EXIST;
        }
        
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
     */
    @Path("/auth")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String login(Login login) {

        
        UserDAO user = null;
        if (!userExistsInDB(login.getId())) {
            return INVALID_LOGIN;
        } else {
            user = db_instance.getUserById(login.getId()).iterator().next();

            if (!user.getPwd().equals(login.getPwd()))
                return INVALID_LOGIN;
            
            if (db_instance.getLoginById(login.getId()).iterator().hasNext())
                return ALREADY_AUTH;
            
            LoginDAO loginDAO = new LoginDAO(login);

            db_instance.putLogin(loginDAO);
            return loginDAO.getId();
        }
    }

    private boolean userExistsInDB(String userId) {
        CosmosPagedIterable<UserDAO> usersIt = db_instance.getUserById(userId);
        return usersIt.iterator().hasNext();
    }
}
