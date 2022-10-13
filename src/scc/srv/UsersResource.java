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
    private static final String USERS_CONTAINER = "user";
    private static final String USERS_BLOB = "users";
    Map<String, User> users = new HashMap<String, User>();

    /**
     * Creates a new user.The id of the user is its hash.
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(User user) {
        if (user == null) {
            System.out.println("Null user exception");
        }
        // Store the user locally?? Is this necessary?? For faster accesses??
        users.put(Hash.of(user.getId()), user);

        // Create the user to store in the db
        UserDAO dbUser = new UserDAO(user);
        BinaryData binaryUser = BinaryData.fromString(dbUser.toString());

        // Get connection string in the storage access keys page
        String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=scc57943;AccountKey=tnWm4OfhzV9PO5PLULNE9bHQREnbCxGH0vLdjyE2y10l8IivZV4WeCe3jCuf4c+mltKSSW7RWB5a+AStWnQcTQ==;EndpointSuffix=core.windows.net";

        try {

            // Get container client
            BlobContainerClient containerClient = new BlobContainerClientBuilder()
                    .connectionString(storageConnectionString)
                    .containerName(USERS_CONTAINER)
                    .buildClient();

            // Get client to blob
            BlobClient blob = containerClient.getBlobClient(USERS_BLOB);

            // Upload contents from BinaryData (check documentation for other alternatives)
            blob.upload(binaryUser);

            System.out.println("User created : " + dbUser.getName());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return dbUser.getId();
    }

    /**
     * Updates a user. Throw an appropriate error message if
     * id does not exist.
     */
    @GET
    @Path("/update")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public User updtate(@PathParam("user_id") String id) {
        User u = users.get(id);
        if (u == null)
            throw new ServiceUnavailableException();
        return u;
    }

    /**
     * Deletes a user. Throw an appropriate error message if
     * id does not exist.
     */
    @GET
    @Path("/delete")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public User delete(@PathParam("user_id") String id) {
        User u = users.get(id);
        if (u == null)
            throw new ServiceUnavailableException();
        users.remove(id);
        return u;
    }
}
