package scc.serverless.main;

import scc.cache.RedisCache;
import scc.cosmosdb.CosmosDBLayer;
import scc.cosmosdb.models.AuctionDAO;
import scc.srv.dataclasses.Auction;

import java.util.*;
import redis.clients.jedis.Jedis;
import com.microsoft.azure.functions.*;
import redis.clients.jedis.params.SetParams;
import com.microsoft.azure.functions.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Azure Functions with Timer Trigger.
 */
public class TimerFunction {

	private static CosmosDBLayer db_instance;
	private static Jedis jedis_instance;
	private ObjectMapper mapper;

	public TimerFunction() {
		db_instance = CosmosDBLayer.getInstance();
		jedis_instance = RedisCache.getCachePool().getResource();
		mapper = new ObjectMapper();
	}

	@FunctionName("closeAuction")
	public void cosmosFunction(
			@TimerTrigger(name = "closeAuctionTrigger", schedule = "30 * */2 * * *") String timerInfo,
			ExecutionContext context) {

		db_instance.closeAuctions();
	}

	@FunctionName("auctionsAboutToClose")
	public void auctionsAboutToClose(
			@TimerTrigger(name = "auctionsAboutToClose", schedule = "30 * */5 * * *") String timerInfo,
			ExecutionContext context) {

		context.getLogger().info("Timer is triggered AUCTIONS ABOUT TO CLOSE: " + timerInfo);

		Iterator<AuctionDAO> it = db_instance.getAuctionsAboutToClose().iterator();
		while (it.hasNext()) {
			Auction auction = it.next().toAuction();
			try {
				jedis_instance.set("auction:" + auction.getId(), mapper.writeValueAsString(auction),
						SetParams.setParams().ex(86400).nx());
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}