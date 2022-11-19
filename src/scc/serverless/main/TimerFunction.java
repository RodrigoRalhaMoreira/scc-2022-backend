package scc.serverless.main;

import java.text.SimpleDateFormat;
import java.util.*;
import com.microsoft.azure.functions.annotation.*;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;
import scc.cache.RedisCache;
import scc.cosmosdb.CosmosDBLayer;
import scc.cosmosdb.models.AuctionDAO;
import scc.srv.dataclasses.Auction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;

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
	public void cosmosFunction(@TimerTrigger(name = "closeAuctionTrigger", schedule = "*/1 * * * * *") String timerInfo,
			ExecutionContext context) {

		context.getLogger().info("Timer is triggered CLOSE AUCTION: " + timerInfo);
		try {
			jedis_instance.incr("cnt:timer");
			jedis_instance.set("serverless-time",
					new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(new Date()));
		} catch (Exception e) {
			context.getLogger().info(e.getMessage());
		}

		Iterator<AuctionDAO> it = db_instance.getCloseAuctions().iterator();
		jedis_instance.configRewrite();
		try {
			while (it.hasNext()) {
				AuctionDAO auction = it.next();
				jedis_instance.set("auction:" + auction.getId(), mapper.writeValueAsString(auction));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FunctionName("auctionsAboutToClose")
	public void auctionsAboutToClose(
			@TimerTrigger(name = "auctionsAboutToClose", schedule = "30 * */5 * * *") String timerInfo,
			ExecutionContext context) {

		context.getLogger().info("Timer is triggered AUCTIONS ABOUT TO CLOSE: " + timerInfo);
		/* 
		try {
			jedis_instance.incr("cnt:timer");
			jedis_instance.set("serverless-time",
					new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(new Date()));
		} catch (Exception e) {
			context.getLogger().info(e.getMessage());
		}*/

		//Set<String> redis_auctions = jedis_instance.keys("auction:*");

		Iterator<AuctionDAO> it = db_instance.getAuctionsAboutToClose().iterator();
		while(it.hasNext()) {
			Auction auction = it.next().toAuction();
			try {
				jedis_instance.set("auction:" + auction.getId(),  mapper.writeValueAsString(auction)
				, SetParams.setParams().ex(86400).nx());
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}