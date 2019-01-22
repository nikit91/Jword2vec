package org.aksw.word2vecrestful.word2vec;

import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dice_research.topicmodeling.commons.sort.AssociativeSort;

/**
 * Class to encapsulate word2vec in-memory model and expose methods to perform
 * search on the model. (Only works with Normalized Model)
 * 
 * This class selects {@link W2VNrmlMemModelBinSrchCached#compareVecCount}
 * vectors (1 mean vector and others on basis Map iterator) and then calculates
 * the cosine similarity of all words in model to those vectors.
 * 
 * It uses the knowledge about pre-processed similarities with
 * {@link W2VNrmlMemModelBinSrchCached#comparisonVecs} to narrow down the search
 * of closest word for the user specified vector.
 * 
 * @author Nikit
 *
 */
public class W2VNrmlMemModelBinSrchCached extends W2VNrmlMemModelBinSrch implements GenWord2VecModel {

	public W2VNrmlMemModelBinSrchCached(Map<String, float[]> word2vec, int vectorSize, int compareVecCount,
			int bucketCount) throws IOException {
		super(word2vec, vectorSize, compareVecCount, bucketCount);
	}

	public W2VNrmlMemModelBinSrchCached(Map<String, float[]> word2vec, int vectorSize) throws IOException {
		super(word2vec, vectorSize);
	}

	public static Logger LOG = LogManager.getLogger(W2VNrmlMemModelBinSrchCached.class);
	/**
	 * NOTE: this cache is useful only if the same float[] array instance/object is used to query
	 */
	private Map<float[], String> closestWordCache = new ConcurrentHashMap<>();
	private Map<float[], Integer> closestWordHit = new ConcurrentSkipListMap<>();
	public static final int CACHE_SIZE = 10000;
	
	private int cacheSize = CACHE_SIZE;

	/**
	 * Method to fetch the closest word entry for a given vector using cosine
	 * similarity
	 * 
	 * @param vector - vector to find closest word to
	 * @param subKey - key to subset if any
	 * @return closest word to the given vector alongwith it's vector
	 */
	protected String getClosestEntry(float[] vector, String subKey) {
		String closestEntry = closestWordCache.get(vector);
		if(closestEntry == null) {
			closestEntry = super.getClosestEntry(vector, subKey);
			closestWordCache.put(vector, closestEntry);
			closestWordHit.put(vector, 1);
		}else {
			closestWordHit.put(vector, closestWordHit.get(vector)+1);
		}
		return closestEntry;
		
	}

	public int getCacheSize() {
		return cacheSize;
	}

	public void setCacheSize(int cacheSize) {
		this.cacheSize = cacheSize;
	}
	
	

}
