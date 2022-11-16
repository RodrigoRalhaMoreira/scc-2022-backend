package scc.srv;

import java.util.Iterator;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;

public class CosmosDBLayer {
	private static final String CONNECTION_URL = "https://scc23groupdrt.documents.azure.com:443/";
	private static final String DB_KEY = "nNaQx90GgUrilUlFIx9N1B7zv8wzblpSczL4IGGbFNIt5Q2YiOImwWUxIwieZmXbE3ELDhKSDSlbACDbwYwY4A==";
	private static final String DB_NAME = "scc23dbgroupdrt";

	private static CosmosDBLayer instance;

	public static synchronized CosmosDBLayer getInstance() {
		if (instance != null)
			return instance;

		CosmosClient client = new CosmosClientBuilder()
				.endpoint(CONNECTION_URL)
				.key(DB_KEY)
				// .directMode()
				.gatewayMode()
				// replace by .directMode() for better performance
				.consistencyLevel(ConsistencyLevel.SESSION)
				.connectionSharingAcrossClientsEnabled(true)
				.contentResponseOnWriteEnabled(true)
				.buildClient();
		instance = new CosmosDBLayer(client);
		return instance;

	}

	private CosmosClient client;
	private CosmosDatabase db;
	private CosmosContainer users;
	private CosmosContainer questions;
	private CosmosContainer auctions;
	private CosmosContainer bids;
	private CosmosContainer login;

	public CosmosDBLayer(CosmosClient client) {
		this.client = client;
	}

	private synchronized void init() {
		if (db != null)
			return;
		db = client.getDatabase(DB_NAME);
		users = db.getContainer("users");
		auctions = db.getContainer("auctions");
		questions = db.getContainer("questions");
		bids = db.getContainer("bids");
		login = db.getContainer("login");

	}

	public CosmosItemResponse<Object> delUserById(String id) {
		init();
		PartitionKey key = new PartitionKey(id);
		return users.deleteItem(id, key, new CosmosItemRequestOptions());
	}

	public CosmosItemResponse<Object> delUser(UserDAO user) {
		init();
		return users.deleteItem(user, new CosmosItemRequestOptions());
	}

	public CosmosItemResponse<UserDAO> putUser(UserDAO user) {
		init();
		return users.createItem(user);
	}

	public CosmosItemResponse<QuestionDAO> putQuestion(QuestionDAO question) {
		init();
		return questions.createItem(question);
	}

	public CosmosItemResponse<UserDAO> updateUser(UserDAO user) {
		init();
		return users.upsertItem(user);
	}

	public CosmosPagedIterable<UserDAO> getUserById(String id) {
		init();
		return users.queryItems("SELECT * FROM users WHERE users.id=\"" + id + "\"", new CosmosQueryRequestOptions(),
				UserDAO.class);
	}

	public CosmosPagedIterable<UserDAO> getUsers() {
		init();
		return users.queryItems("SELECT * FROM users ", new CosmosQueryRequestOptions(), UserDAO.class);
	}

	public CosmosPagedIterable<AuctionDAO> getAuctionsByUserId(String id) {
		init();
		return auctions.queryItems("SELECT * FROM auctions WHERE auctions.ownerId=\"" + id + "\"", new CosmosQueryRequestOptions(), AuctionDAO.class);
	}

	public void close() {
		client.close();
	}

	public CosmosItemResponse<AuctionDAO> putAuction(AuctionDAO auction) {
		init();
		return auctions.createItem(auction);
	}

	public CosmosItemResponse<BidDAO> putBid(BidDAO bid) {
		init();
		return bids.createItem(bid);
	}

	public CosmosPagedIterable<BidDAO> getBidById(String id) {
		init();
		return auctions.queryItems("SELECT * FROM bids WHERE bids.id=\"" + id + "\"",
				new CosmosQueryRequestOptions(),
				BidDAO.class);
	}

	public CosmosPagedIterable<BidDAO> getBidsByAuctionId(String id) {
		init();
		return bids.queryItems("SELECT * FROM bids WHERE bids.auctionId=\"" + id + "\"",
				new CosmosQueryRequestOptions(),
				BidDAO.class);
	}

	public CosmosPagedIterable<AuctionDAO> getAuctionById(String id) {
		init();
		return auctions.queryItems("SELECT * FROM auctions WHERE auctions.id=\"" + id + "\"",
				new CosmosQueryRequestOptions(),
				AuctionDAO.class);
	}

	public CosmosItemResponse<AuctionDAO> updateAuction(AuctionDAO dbAuction) {
		init();
		return auctions.upsertItem(dbAuction);
	}

    public CosmosItemResponse<LoginDAO> putLogin(LoginDAO loginDAO) {
		init();
		return login.createItem(loginDAO);
    }

    public CosmosPagedIterable<LoginDAO> getLoginById(String id) {
		init();
        return login.queryItems("SELECT * FROM login WHERE login.id=\"" + id + "\"",
				new CosmosQueryRequestOptions(),
				LoginDAO.class);
    }

	public CosmosPagedIterable<AuctionDAO> getCloseAuctions() {
		init();
        CosmosPagedIterable<AuctionDAO> cpi = auctions.queryItems("SELECT * FROM auctions WHERE auction.status=\""+ AuctionStatus.OPEN.getStatus() + "\"" + "AND auctions.endTime <= GetCurrentDateTime()",
				new CosmosQueryRequestOptions(),
				AuctionDAO.class);
		Iterator<AuctionDAO> it = cpi.iterator();
		while(it.hasNext()) {
			AuctionDAO auction = it.next();
			auction.setStatus(AuctionStatus.CLOSE.getStatus());
		}
		return cpi;
	}
}
