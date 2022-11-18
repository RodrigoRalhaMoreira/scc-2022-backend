package scc.srv;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MediaType;
import com.azure.cosmos.util.CosmosPagedIterable;

/**
 * Resource for managing questions.
 */
@Path("/auction/{id}/question")
public class QuestionsResource {
    private static final String QUESTION_NULL = "Error creating null question";
    private static final String ONLY_OWNER_ERROR = "Only owner can reply to questions";
    private static final String AUCTION_ERROR = "Auction does not exist";
    private static final String USER_NOT_EXISTS = "Error non-existent user";
    private static final String AUCTION_NOT_EXISTS = "Error non-existent auction";
    private static final String USER_NOT_AUTH = "User not authenticated";

    private static CosmosDBLayer db_instance;
    private UsersResource users;

    // Improvements to be made. If we have "id" of auction as PathParam we should
    // not have to pass it as param in POST request.

    public QuestionsResource() {
        db_instance = CosmosDBLayer.getInstance();
        for(Object resource : MainApplication.getSingletonsSet())  {
            if(resource instanceof UsersResource)
                users = (UsersResource) resource;
        }
    }

    /**
     * Creates a new question.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(@CookieParam("scc:session") Cookie session, Question question) {

        if (question == null)
            return QUESTION_NULL;

        try {
            users.checkCookieUser(session, question.getUserId());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return e.getMessage();
        }

        if (!userExistsInDB(question.getUserId()))
            return USER_NOT_EXISTS;

        // this does not make sense we're only doing this for the moment
        if (!auctionExistsInDB(question.getAuctionId()))
            return AUCTION_NOT_EXISTS;

        // Create the question to store in the db
        QuestionDAO dbquestion = new QuestionDAO(question);

        db_instance.putQuestion(dbquestion);
        return dbquestion.getId();
    }

    // Later improve this to get auctionId of the path
    /**
     * Reply to a question.
     */
    @Path("/{id}/reply")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String reply(@QueryParam("scc:session") Cookie session, Question question,
            @PathParam("id") String id) {
        if (question == null)
            return QUESTION_NULL;
        
        try {
            users.checkCookieUser(null, question.getUserId());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return e.getMessage();
        }

        if (!question.getUserId().equals(getAuctionOwner(question.getAuctionId())))
            return ONLY_OWNER_ERROR;

        QuestionDAO dbReply = new QuestionDAO(question);
        db_instance.putQuestion(dbReply);
        return dbReply.getId();
    }

    private String getAuctionOwner(String auctionId) {
        CosmosPagedIterable<AuctionDAO> auctionsIt = db_instance.getAuctionById(auctionId);
        if (!auctionsIt.iterator().hasNext())
            return AUCTION_ERROR; // this should never happen.
        AuctionDAO auc = auctionsIt.iterator().next();
        return auc.getOwnerId();
    }

    private boolean userExistsInDB(String userId) {
        CosmosPagedIterable<UserDAO> usersIt = db_instance.getUserById(userId);
        return usersIt.iterator().hasNext();
    }

    private boolean auctionExistsInDB(String auctionId) {
        CosmosPagedIterable<AuctionDAO> auctionIt = db_instance.getAuctionById(auctionId);
        return auctionIt.iterator().hasNext();
    }
}
