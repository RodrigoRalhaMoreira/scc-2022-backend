package scc.serverless.main;

import com.microsoft.azure.functions.annotation.*;
import scc.srv.cosmosdb.CosmosDBLayer;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with Timer Trigger.
 */
public class CosmosDBFunction {

	private static CosmosDBLayer db_instance;

	@FunctionName("bidInAuction")
	public void updateAuctionWithBid(
			@CosmosDBTrigger(name = "items", databaseName = "scc23groupdrt", collectionName = "bids", connectionStringSetting = "AzureCosmosDBConnection") String[] items,
			final ExecutionContext context) {

		/*
		 * AuctionDAO auctionDAO =
		 * db_instance.getAuctionById(bid.getAuctionId()).iterator().next();
		 * auctionDAO.setWinnigBid(bid);
		 * db_instance.putAuction(auctionDAO);
		 */
		for (String s : items)
			context.getLogger().info(s + "\n\n");
	}

}
