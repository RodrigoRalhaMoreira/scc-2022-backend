package scc.srv.cosmosdb.models;

import scc.srv.dataclasses.Question;

/**
 * Represents a Question, as stored in the database
 */
public class QuestionDAO {
	private String _rid;
	private String _ts;
	private String id;
	private String auctionId;
	private String userId;
	private String message;

	public QuestionDAO() {
	}

	public QuestionDAO(Question q) {
		this(q.getId(), q.getAuctionId(), q.getUserId(), q.getMessage());
	}

	public QuestionDAO(String id, String auctionId, String userId, String message) {
		super();
		this.id = id;
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

	public String getId() {
		return id;
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
		return new Question(id, auctionId, userId, message);
	}

	@Override
	public String toString() {
		return "QuestionDAO [_rid=" + _rid + ", _ts=" + _ts + ", id=" + id + ", auctionId=" + auctionId
				+ ", userId=" + userId + ", message=" + message + "]";
	}
}
