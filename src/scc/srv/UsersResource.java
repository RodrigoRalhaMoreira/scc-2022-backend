package scc.srv;

import jakarta.ws.rs.*;
import redis.clients.jedis.Jedis;
import jakarta.ws.rs.core.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import scc.cache.RedisCache;

/**
 * Resource for managing users.
 */
@Path("/user")
public class UsersResource {
    private static final String UPDATE_ERROR = "Error updating non-existent user";
    private static final String DELETE_ERROR = "Error deleting non-existent user";

    private static CosmosDBLayer db_instance;
    private static Jedis jedis_instance;
    ObjectMapper mapper;

    public UsersResource() {
        db_instance = CosmosDBLayer.getInstance();
        jedis_instance = RedisCache.getCachePool().getResource();
        mapper = new ObjectMapper();
    }

    /**
     * Creates a new user.The id of the user is its hash.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(User user) {
        if (user == null) {
            System.out.println("Null user exception");
        }
        UserDAO userDao = new UserDAO(user);
        try {
            jedis_instance.set("user:" + user.getId(), mapper.writeValueAsString(userDao));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Create the user to store in the db
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
            System.out.println("Null user exception");
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
        if (db_instance.getUserById(user.getId()) != null) {
            db_instance.updateUser(userDao);
            return userDao.getId();
        }
        System.out.println("User does not exist");
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
        long removed = jedis_instance.del("user:" + id);
        if (removed == 1) {
            db_instance.delUserById(id);
            return id;
        }
        if (db_instance.getUserById(id) != null) {
            db_instance.delUserById(id);
            return id;
        }
        System.out.println("User does not exist");
        return DELETE_ERROR;
    }
}
