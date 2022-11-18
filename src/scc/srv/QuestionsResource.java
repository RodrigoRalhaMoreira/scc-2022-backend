package scc.srv;

import scc.cache.RedisCache;

import jakarta.ws.rs.*;
import redis.clients.jedis.Jedis;
import jakarta.ws.rs.core.MediaType;
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
     * 
     * @throws JsonProcessingException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(Question question) throws JsonProcessingException {
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

    // Later improve this to get auctionId of the path
    /**
     * Reply to a question.
     * 
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    @Path("/{id}/reply")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String reply(Question question,
            @PathParam("id") String id) throws JsonMappingException, JsonProcessingException {
        if (question == null)
            return QUESTION_NULL;

        if (!question.getUserId().equals(getAuctionOwner(question.getAuctionId())))
            return ONLY_OWNER_ERROR;

        QuestionDAO dbReply = new QuestionDAO(question);
        jedis_instance.set("question:" + question.getId(), mapper.writeValueAsString(question));
        db_instance.putQuestion(dbReply);
        return dbReply.getId();
    }

    private String getAuctionOwner(String auctionId) throws JsonMappingException, JsonProcessingException {
        String auction_res = jedis_instance.get("auction:" + auctionId);
        if (auction_res != null) {
            Auction auction = mapper.readValue(auction_res, Auction.class);
            return auction.getOwnerId();
        }

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
