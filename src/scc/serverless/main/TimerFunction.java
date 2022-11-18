package scc.serverless.main;

import java.util.*;
import com.microsoft.azure.functions.annotation.*;
import redis.clients.jedis.Jedis;
import scc.srv.AuctionDAO;
import scc.srv.CosmosDBLayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with Timer Trigger.
 */
public class TimerFunction {

	private static CosmosDBLayer db_instance;
	private static Jedis jedis_instance;
	private ObjectMapper mapper;

	@FunctionName("close-auction")
	public void cosmosFunction(@TimerTrigger(name = "close-auction", schedule = "30 */1 * * * *") String timerInfo,
			ExecutionContext context) {
		/*
		 * try (Jedis jedis = RedisCache.getCachePool().getResource()) {
		 * jedis.incr("cnt:timer");
		 * jedis.set("serverless-time", new
		 * SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z").format(new Date()));
		 * }
		 */

		Iterator<AuctionDAO> it = db_instance.getCloseAuctions().iterator();

		try {
			while (it.hasNext()) {
				AuctionDAO auction = it.next();
				jedis_instance.set("auction:" + auction.getId(), mapper.writeValueAsString(auction));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
