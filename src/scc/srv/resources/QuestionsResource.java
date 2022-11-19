package scc.srv.resources;

import scc.cache.RedisCache;
import scc.cosmosdb.CosmosDBLayer;
import scc.cosmosdb.models.AuctionDAO;
import scc.cosmosdb.models.QuestionDAO;
import scc.cosmosdb.models.UserDAO;
import scc.srv.MainApplication;
import scc.srv.dataclasses.Auction;
import scc.srv.dataclasses.Question;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Cookie;
import redis.clients.jedis.Jedis;
import jakarta.ws.rs.core.MediaType;

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
    private static final String REPLY_ALREADY_DONE = "Only one reply can be made for a question";
    private static final String ALREADY_EXISTS_DB = "Id already exists in the DataBase";
    private static final String AUCTION_ID_NOT_EXISTS_DB = "Auction does not exist in the DataBase";
    private static final String SAME_OWNER = "Owner can not ask a question in his auction";
    private static final String NULL_FIELD_EXCEPTION = "Null %s exception";
    private static final int DEFAULT_REDIS_EXPIRE = 600;

    private static CosmosDBLayer db_instance;
    private static Jedis jedis_instance;
    private ObjectMapper mapper;
    private UsersResource users;

    // Improvements to be made. If we have "id" of auction as PathParam we should
    // not have to pass it as param in POST request.

    public QuestionsResource() {
        db_instance = CosmosDBLayer.getInstance();
        jedis_instance = RedisCache.getCachePool().getResource();
        mapper = new ObjectMapper();
        for (Object resource : MainApplication.getSingletonsSet()) {
            if (resource instanceof UsersResource)
                users = (UsersResource) resource;
        }
    }

    /**
     * Creates a new question.
     * 
     * @throws JsonProcessingException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(@CookieParam("scc:session") Cookie session, Question question,
            @PathParam("id") String auctionId)
            throws JsonProcessingException, IllegalArgumentException, IllegalAccessException {

        try {
            users.checkCookieUser(session, question.getUserId());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return e.getMessage();
        }

        String error = checkQuestion(question);

        if (error != null)
            return error;

        if (getAuctionOwner(auctionId).equals(question.getUserId()))
            return SAME_OWNER;

        // Create the question to store in the db
        QuestionDAO dbquestion = new QuestionDAO(question);
        jedis_instance.setex("question:" + question.getId(), DEFAULT_REDIS_EXPIRE, mapper.writeValueAsString(question));

        db_instance.putQuestion(dbquestion);
        return dbquestion.getMessage();
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> listAuctionQuestions(@PathParam("id") String auctionId)
            throws IllegalArgumentException, IllegalAccessException {

        List<String> list = new ArrayList<>();

        if (!auctionExistsInDB(auctionId)) {
            list.add(AUCTION_ID_NOT_EXISTS_DB);
            return list;
        }

        Iterator<QuestionDAO> it = db_instance.getQuestionsByAuctionId(auctionId).iterator();

        if (it.hasNext())
            list.add(((QuestionDAO) it.next()).toQuestion().toString());

        return list;
    }

    @Path("/{id}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Question listAuctionQuestion(@PathParam("id") String auctionId,
            @PathParam("id") String questionId) throws IllegalArgumentException, IllegalAccessException {

        if (!auctionExistsInDB(auctionId))
            return null;

        if (!questionExistsInDB(questionId))
            return null;

        return getQuestionById(questionId);
    }

    /**
     * Reply to a question.
     * 
     * @throws JsonProcessingException
     * @throws JsonMappingException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Path("/{id}/reply")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String reply(@CookieParam("scc:session") Cookie session, Question question,
            @PathParam("id") String auctionId, @PathParam("id") String questionId)
            throws JsonMappingException, JsonProcessingException, IllegalArgumentException, IllegalAccessException {

        try {
            users.checkCookieUser(null, question.getUserId());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return e.getMessage();
        }

        String error = checkQuestion(question);

        if (error != null)
            return null;

        if (!question.getUserId().equals(getAuctionOwner(auctionId))) // question.getAuctionId()
            return ONLY_OWNER_ERROR;

        Question questioned = getQuestionById(questionId);

        String reply = questioned.getReply();
        String newReply = question.getMessage();

        if (reply != null)
            return REPLY_ALREADY_DONE;

        // Updates the question in redis with the new reply and stores the new question
        // (reply)
        try {
            String res = jedis_instance.get("question:" + questioned.getId());
            if (res != null) {
                questioned.setReply(newReply);
                jedis_instance.setex("question:" + questioned.getId(), DEFAULT_REDIS_EXPIRE,
                        mapper.writeValueAsString(questioned));
                jedis_instance.setex("question:" + question.getId(), DEFAULT_REDIS_EXPIRE,
                        mapper.writeValueAsString(question));
            }
        } catch (Exception e) {
            // TODO: handle exception
        }

        // Updates the question in the database with the new reply and stores the new
        // question (reply)
        QuestionDAO dbQuestion = new QuestionDAO(questioned);
        dbQuestion.setReply(newReply);
        db_instance.updateQuestion(dbQuestion);

        QuestionDAO dbReply = new QuestionDAO(question);
        db_instance.putQuestion(dbReply);
        return dbReply.getMessage();
    }

    // --------------------------------------------------- PRIVATE METHODS
    // ----------------------------------------

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

    private Question getQuestionById(String id) {
        Iterator<QuestionDAO> auctionsIt = db_instance.getQuestionsById(id).iterator();
        while (auctionsIt.hasNext())
            return auctionsIt.next().toQuestion();
        return null;
    }

    private boolean questionExistsInDB(String questionId) {
        CosmosPagedIterable<QuestionDAO> questionsIt = db_instance.getQuestionsById(questionId);
        return questionsIt.iterator().hasNext();
    }

    private boolean userExistsInDB(String userId) {
        CosmosPagedIterable<UserDAO> usersIt = db_instance.getUserById(userId);
        return usersIt.iterator().hasNext();
    }

    private boolean auctionExistsInDB(String auctionId) {
        CosmosPagedIterable<AuctionDAO> auctionIt = db_instance.getAuctionById(auctionId);
        return auctionIt.iterator().hasNext();
    }

    private String checkQuestion(Question question) throws IllegalArgumentException, IllegalAccessException {

        if (question == null)
            return QUESTION_NULL;

        if (questionExistsInDB(question.getId()))
            return ALREADY_EXISTS_DB;

        // verify that fields are different from null
        for (Field f : question.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (f.get(question) == null && !f.getName().equals("reply"))
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
