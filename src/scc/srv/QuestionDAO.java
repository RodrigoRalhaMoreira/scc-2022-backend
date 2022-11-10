package scc.srv;

/**
 * Represents a Question, as stored in the database
 */
public class QuestionDAO {
	private String _rid;
	private String _ts;
	private String questionId;
	private String auctionId;
	private String userId;
	private String message;

	public QuestionDAO() {
	}

	public QuestionDAO(Question q) {
		this(q.getQuestionId(), q.getAuctionId(), q.getUserId(), q.getMessage());
	}

	public QuestionDAO(String questionId, String auctionId, String userId, String message) {
		super();
		this.questionId = questionId;
		this.auctionId = auctionId;
		this.userId = userId;
		this.message = message;
	}

	public String get_rid() {
		return _rid;
	}

	public void set_rid(String _rid) {
		this._rid = _rid;
	}

	public String get_ts() {
		return _ts;
	}

	public void set_ts(String _ts) {
		this._ts = _ts;
	}

	public String getQuestionId() {
		return questionId;
	}

	public String getAuctionId() {
		return auctionId;
	}

	public String getUserId() {
		return userId;
	}

	public String getMessage() {
		return message;
	}

	public Question toQuestion() {
		return new Question(questionId, auctionId, userId, message);
	}

	@Override
	public String toString() {
		return "QuestionDAO [_rid=" + _rid + ", _ts=" + _ts + ", questionId=" + questionId + ", auctionId=" + auctionId
				+ ", userId=" + userId + ", message=" + message + "]";
	}
}
