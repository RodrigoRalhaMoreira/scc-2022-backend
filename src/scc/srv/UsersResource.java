package scc.srv;

import jakarta.ws.rs.*;
import scc.utils.Hash;

import java.util.Map;
import java.util.HashMap;

import jakarta.ws.rs.core.MediaType;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;

/**
 * Resource for managing users.
 */
@Path("/user")
public class UsersResource {
    private static CosmosDBLayer db_instance = CosmosDBLayer.getInstance();

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
        // Create the user to store in the db
        UserDAO dbUser = new UserDAO(user);
        db_instance.putUser(dbUser);
        return dbUser.getId();
    }

    /**
     * Updates a user. Throw an appropriate error message if
     * id does not exist.
     */
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public User updtate(@PathParam("user_id") String id) {
        return null;
    }

    /**
     * Deletes a user. Throw an appropriate error message if
     * id does not exist.
     */
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public User delete(@PathParam("user_id") String id) {
        return null;
    }
}
