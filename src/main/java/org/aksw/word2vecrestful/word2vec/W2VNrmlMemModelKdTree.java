package org.aksw.word2vecrestful.word2vec;

import java.util.Map;
import java.util.Map.Entry;

import org.aksw.word2vecrestful.subset.DataSubsetProvider;
import org.aksw.word2vecrestful.tool.kdtree.KDTree;
import org.aksw.word2vecrestful.tool.kdtree.KeySizeException;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Class to encapsulate word2vec in-memory model and expose methods to perform
 * search on the model
 * 
 * @author Nikit
 *
 */
public class W2VNrmlMemModelKdTree implements GenWord2VecModel {
	public static Logger LOG = LogManager.getLogger(GenWord2VecModel.class);

	private Map<String, float[]> word2vec;
	private int vectorSize;
	// for future use
	@SuppressWarnings("unused")
	private DataSubsetProvider dataSubsetProvider;
	
	private KDTree kdTree;

	public W2VNrmlMemModelKdTree(final Map<String, float[]> word2vec, final int vectorSize) {
		this.word2vec = word2vec;
		this.vectorSize = vectorSize;

	}

	@Override
	public void process() {
		this.dataSubsetProvider = new DataSubsetProvider();
		//TODO : Generate the KDTree here
		kdTree = new KDTree(vectorSize);
		try {
			for(Entry<String, float[]> entry : word2vec.entrySet()) {
				kdTree.insert(entry.getValue(), entry.getKey());
			}
		} catch(Exception ex) {
			ex.printStackTrace();
			LOG.error(ex);
		}
	}

	/**
	 * Method to fetch the closest word entry for a given vector using cosine
	 * similarity
	 * 
	 * @param vector
	 *            - vector to find closest word to
	 * 
	 * @return closest word to the given vector alongwith it's vector
	 */
	@Override
	public String getClosestEntry(float[] vector) {
		return getClosestEntry(vector, null);
	}

	/**
	 * Method to fetch the closest word entry for a given vector using cosine
	 * similarity
	 * 
	 * @param vector
	 *            - vector to find closest word to
	 * @param subKey
	 *            - key to subset if any
	 * @return closest word to the given vector alongwith it's vector
	 */
	@Override
	public String getClosestSubEntry(float[] vector, String subKey) {
		return getClosestEntry(vector, subKey);
	}

	/**
	 * Method to fetch the closest word entry for a given vector using cosine
	 * similarity
	 * 
	 * @param vector
	 *            - vector to find closest word to
	 * @param subKey
	 *            - key to subset if any
	 * @return closest word to the given vector alongwith it's vector
	 */
	private String getClosestEntry(float[] vector, String subKey) {
		// Normalize incoming vector
		vector = Word2VecMath.normalize(vector);
		String result = null;
		try {
			result = (String) kdTree.nearest(vector);
		} catch (KeySizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Method to fetch vectorSize
	 * 
	 * @return - vectorSize
	 */
	@Override
	public int getVectorSize() {
		return this.vectorSize;
	}

	/**
	 * Method to fetch word2vec map
	 * 
	 * @return - word2vec map
	 */
	public Map<String, float[]> getWord2VecMap() {
		return this.word2vec;
	}

}
