package org.aksw.word2vecrestful.utils;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class TimeLogger {
	public static Logger LOG = LogManager.getLogger(TimeLogger.class);
	private Map<Integer, Long> strtLog = new HashMap<>();
	
	public void logTime(int id) {
		strtLog.put(id, System.currentTimeMillis());
	}
	
	public void printTime(int id, String procName) {
		long diff = System.currentTimeMillis() - strtLog.get(id);
		LOG.info("Time recorded for '" + procName + "' is "
				+ diff + " milliseconds.");
	}

}
