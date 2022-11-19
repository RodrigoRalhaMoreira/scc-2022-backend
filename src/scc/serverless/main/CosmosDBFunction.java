package scc.serverless.main;

import com.microsoft.azure.functions.annotation.*;

import scc.cosmosdb.CosmosDBLayer;
import scc.cosmosdb.models.UserDAO;

import com.google.gson.Gson;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with Timer Trigger.
 */
public class CosmosDBFunction {

	private static CosmosDBLayer db_instance;

	@FunctionName("userSoftDelete")
	public void updateAuctionWithBid(
			@CosmosDBTrigger(name = "items", databaseName = "scc23groupdrt", collectionName = "users", connectionStringSetting = "AccountEndpoint=https://scc23groupdrt.documents.azure.com:443/;AccountKey=nNaQx90GgUrilUlFIx9N1B7zv8wzblpSczL4IGGbFNIt5Q2YiOImwWUxIwieZmXbE3ELDhKSDSlbACDbwYwY4A==;") String[] items,
			final ExecutionContext context) {

		context.getLogger().info("COSMOS is triggered USER DELETE: " + items);
		for (String s : items) {
			context.getLogger().info("ITEM: " + s);
			Gson g = new Gson();
			UserDAO user = g.fromJson(s, UserDAO.class);
			context.getLogger().info("USER: " + user.toString());
		}
	}

}
