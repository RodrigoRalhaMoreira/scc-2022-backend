package scc.srv.resources;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import scc.srv.cosmosdb.CosmosDBLayer;
import scc.srv.cosmosdb.models.AuctionDAO;
import scc.srv.cosmosdb.models.QuestionDAO;
import scc.srv.cosmosdb.models.UserDAO;
import scc.srv.dataclasses.Question;

import java.lang.reflect.Field;

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
    
    private static final String NULL_FIELD_EXCEPTION = "Null %s exception";

    private static CosmosDBLayer db_instance;

    // Improvements to be made. If we have "id" of auction as PathParam we should
    // not have to pass it as param in POST request.

    public QuestionsResource() {
        db_instance = CosmosDBLayer.getInstance();
    }

    /**
     * Creates a new question.
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(Question question) throws IllegalArgumentException, IllegalAccessException {
        
        String error = checkQuestion(question);
        
        if(error != null)
            return error;

        // Create the question to store in the db
        QuestionDAO dbquestion = new QuestionDAO(question);

        db_instance.putQuestion(dbquestion);
        return dbquestion.getId();
    }

    // Later improve this to get auctionId of the path
    /**
     * Reply to a question.
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    @Path("/{id}/reply")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String reply(Question question, @PathParam("id") String id) throws IllegalArgumentException, IllegalAccessException {
        
        // Winning bids start by default with the value of null
        String error = checkQuestion(question);
        
        if(error != null)
            return error;

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
    
    private String checkQuestion(Question question) throws IllegalArgumentException, IllegalAccessException{
        
        if (question == null)
            return QUESTION_NULL;
        
        // verify that fields are different from null
        for (Field f : question.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (f.get(question) == null)
                return String.format(NULL_FIELD_EXCEPTION, f.getName());
        }
        
        if (!userExistsInDB(question.getUserId()))
            return USER_NOT_EXISTS;

        // this does not make sense we're only doing this for the moment
        if (!auctionExistsInDB(question.getAuctionId()))
            return AUCTION_NOT_EXISTS;
        
        return null;
    }
}
