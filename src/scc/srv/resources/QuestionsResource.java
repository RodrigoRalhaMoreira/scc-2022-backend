package scc.srv.resources;

import jakarta.ws.rs.*;
import redis.clients.jedis.Jedis;
import jakarta.ws.rs.core.MediaType;
import scc.srv.cosmosdb.CosmosDBLayer;
import scc.srv.cosmosdb.models.AuctionDAO;
import scc.srv.cosmosdb.models.QuestionDAO;
import scc.srv.cosmosdb.models.RecentAuctionDAO;
import scc.srv.cosmosdb.models.UserDAO;
import scc.srv.dataclasses.Question;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

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
    private static Jedis jedis_instance;
    private ObjectMapper mapper;

    // Improvements to be made. If we have "id" of auction as PathParam we should
    // not have to pass it as param in POST request.

    public QuestionsResource() {
        db_instance = CosmosDBLayer.getInstance();
        jedis_instance = RedisCache.getCachePool().getResource();
        mapper = new ObjectMapper();
    }

    /**
     * Creates a new question.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(Question question) {
        if (question == null)
            return QUESTION_NULL;

        if (!userExistsInDB(question.getUserId()))
            return USER_NOT_EXISTS;

        // this does not make sense we're only doing this for the moment
        if (!auctionExistsInDB(question.getAuctionId()))
            return AUCTION_NOT_EXISTS;

        // Create the question to store in the db
        QuestionDAO dbquestion = new QuestionDAO(question);
        jedis_instance.set("question:" + question.getId(), mapper.writeValueAsString(question));

        db_instance.putQuestion(dbquestion);
        return dbquestion.getId();
    }
    
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listAuctionQuestions(Question question) throws IllegalArgumentException, IllegalAccessException {
        
        String error = checkQuestion(question);
        
        if(error != null)
            return null;
        
        List<String> list = new ArrayList<>();

        Iterator<QuestionDAO> it = db_instance.getQuestionsByAuctionId(question.getAuctionId()).iterator();
        if (it.hasNext()) 
            list.add(((QuestionDAO) it.next()).toQuestion().toString());
        
        return list;
    }
    
    // Later improve this to get auctionId of the path
    /**
     * Reply to a question.
     */
    @Path("/{id}/reply")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String reply(Question question,
            @PathParam("id") String id) {
        if (question == null)
            return QUESTION_NULL;

        if (!question.getUserId().equals(getAuctionOwner(question.getAuctionId())))
            return ONLY_OWNER_ERROR;

        QuestionDAO dbReply = new QuestionDAO(question);
        jedis_instance.set("question:" + question.getId(), mapper.writeValueAsString(question));
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
