package scc.srv;

import jakarta.ws.rs.*;

import jakarta.ws.rs.core.MediaType;

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
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String update(User user) {
        if (user == null) {
            System.out.println("Null user exception");
        }
        if (db_instance.getUserById(user.getId()) == null)
            System.out.println("User does not exist");
        UserDAO dbUser = new UserDAO(user);
        db_instance.updateUser(dbUser);
        return dbUser.getId();
    }

    /**
     * Deletes a user. Throw an appropriate error message if
     * id does not exist.
     */
    @Path("/{id}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public String delete(@PathParam("id") String id) {
        System.out.println(id);
        if (db_instance.getUserById(id) == null)
            System.out.println("User does not exist");
        db_instance.delUserById(id);
        return id;
    }
}
