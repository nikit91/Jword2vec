package org.aksw.word2vecrestful.word2vec;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Class to encapsulate word2vec in-memory model and expose methods to perform
 * search on the model. (Only works with Normalized Model)
 * 
 * This class selects {@link W2VNrmlMemModelUnitVec#compareVecCount} vectors
 * (centroids of the KMeans result on the model vectors) and then calculates the
 * cosine similarity of all words in model to those vectors.
 * 
 * It uses the knowledge about pre-processed similarities with
 * {@link W2VNrmlMemModelUnitVec#comparisonVecs} to narrow down the search of
 * closest word for the user specified vector.
 * 
 * @author Nikit
 *
 */
public class W2VNrmlMemModelUnitVec extends W2VNrmlMemModelBinSrch {
	public static Logger LOG = LogManager.getLogger(GenWord2VecModel.class);

	public W2VNrmlMemModelUnitVec(final Map<String, float[]> word2vec, final int vectorSize) throws IOException {
		super(word2vec, vectorSize, vectorSize, 20);
	}

	@Override
	public void process() throws IOException {
		LOG.info("Process from UnitVector called");
		generateComparisonVecs();
		// Initialize Arrays
		genAllCosineSim();
	}
	
	private void generateComparisonVecs() {
		for(int i=0;i<vectorSize;i++) {
			float[] compareVec = new float[vectorSize];
			compareVec[i] = 1;
			comparisonVecs[i] = compareVec;
		}
	}
	

}
