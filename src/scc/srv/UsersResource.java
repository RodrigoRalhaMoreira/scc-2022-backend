package scc.srv;

import scc.cache.RedisCache;

import jakarta.ws.rs.*;
import redis.clients.jedis.Jedis;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Resource for managing users.
 */
@Path("/user")
public class UsersResource {

    private static final String USER_NULL = "Error creating null user";
    private static final String UPDATE_ERROR = "Error updating non-existent user";
    private static final String DELETE_ERROR = "Error deleting non-existent user";
    private static final String IMG_NOT_EXIST = "Image does not exist";
    private static final String INVALID_LOGIN = "UserId or password incorrect";
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

        if(!checkAuth(user.getId()))
            return USER_NOT_AUTH;

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

        if(!checkAuth(id))
            return USER_NOT_AUTH;

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

        if(!checkAuth(id))
            throw new NotAuthorizedException(USER_NOT_AUTH);

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
    public Response auth(Login login) {

        UserDAO user = null;

        if (!userExistsInDB(login.getId())) {
            throw new NotAuthorizedException(INVALID_LOGIN);
        } else {
            user = db_instance.getUserById(login.getId()).iterator().next();
            
            if (db_instance.getLoginById(login.getId()).iterator().hasNext())
                throw new NotAuthorizedException(ALREADY_AUTH);

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
                jedis_instance.set("session:"+ user.getId(), mapper.writeValueAsString(new Session(uid, user.getId())));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            /*     
            LoginDAO loginDAO = new LoginDAO(login);

            db_instance.putLogin(loginDAO);*/   

            return Response.ok().cookie(cookie).build();
        }
    }

    @Path("/{id}/following")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> following(@PathParam("id") String id) {

        if(!checkAuth(id))
            throw new NotAuthorizedException(USER_NOT_AUTH);

        List<String> auctions = new ArrayList<>(); 
        
        Iterator<AuctionDAO> it = db_instance.getAuctionUserFollow(id).iterator();
        while(it.hasNext())
            auctions.add(it.next().toAuction().toString());
        
        return auctions;
    }

    public static boolean checkAuth(String userId) {
        String res = jedis_instance.get("session:" + userId);
        return res != null;
    }

    private boolean userExistsInDB(String userId) {
        CosmosPagedIterable<UserDAO> usersIt = db_instance.getUserById(userId);
        return usersIt.iterator().hasNext();
    }
}
