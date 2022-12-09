package scc.cache;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisCache {
	private static final String RedisHostname = System.getenv("redis");

	// private static final String RedisKey =
	// "NxvshIdAYlZKTmNMLPXsAkFzykHbllbodAzCaG1aILk=";

	private static JedisPool instance;

	public synchronized static JedisPool getCachePool() {

		System.out.println("REDIS HOSTNAME: " + RedisHostname);

		if (instance != null)
			return instance;
		final JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxTotal(128);
		poolConfig.setMaxIdle(128);
		poolConfig.setMinIdle(16);
		poolConfig.setTestOnBorrow(true);
		poolConfig.setTestOnReturn(true);
		poolConfig.setTestWhileIdle(true);
		poolConfig.setNumTestsPerEvictionRun(3);
		poolConfig.setBlockWhenExhausted(true);
		instance = new JedisPool(poolConfig, RedisHostname, 6379, 1000, false);
		return instance;

	}
}
