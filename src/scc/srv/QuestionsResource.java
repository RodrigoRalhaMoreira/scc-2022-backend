package scc.srv;

import jakarta.ws.rs.*;

import jakarta.ws.rs.core.MediaType;

/**
 * Resource for managing questions.
 */
@Path("auction/{id}/question")
public class QuestionsResource {
    private static CosmosDBLayer db_instance = CosmosDBLayer.getInstance();

    /**
     * Creates a new question.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String create(Question question) {
        if (question == null) {
            System.out.println("Null user exception");
        }
        // Create the question to store in the db
        QuestionDAO dbquestion = new QuestionDAO(question);
        db_instance.putQuestion(dbquestion);
        return dbquestion.getQuestionId();
    }

    /**
     * Reply to a question.
     */
    @Path("/{questionId}/reply")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String reply(Question question, @PathParam("questionId") String questionId) {
        if (question == null) {
            System.out.println("Null user exception");
        }

        // code to get auction
        // auction = getAuction(questionId)
        if (question.getUserId() != auction.getOwner()) {
            System.out.println("Not the owner");
            return "Only owner can reply to questions";
        }

        // Create the user to store in the db
        QuestionDAO dbReply = new QuestionDAO(question);
        db_instance.putQuestion(dbReply);
        return dbReply.getQuestionId();
    }
}
