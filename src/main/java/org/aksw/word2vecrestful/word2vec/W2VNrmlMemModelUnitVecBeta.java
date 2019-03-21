package org.aksw.word2vecrestful.word2vec;

import java.io.IOException;
import java.util.BitSet;
import java.util.Map;

import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Class to encapsulate word2vec in-memory model and expose methods to perform
 * search on the model. (Only works with Normalized Model)
 * 
 * This class selects {@link W2VNrmlMemModelUnitVecBeta#compareVecCount} vectors
 * (centroids of the KMeans result on the model vectors) and then calculates the
 * cosine similarity of all words in model to those vectors.
 * 
 * It uses the knowledge about pre-processed similarities with
 * {@link W2VNrmlMemModelUnitVecBeta#comparisonVecs} to narrow down the search of
 * closest word for the user specified vector.
 * 
 * @author Nikit
 *
 */
public class W2VNrmlMemModelUnitVecBeta extends W2VNrmlMemModelUnitVec {
	public static Logger LOG = LogManager.getLogger(GenWord2VecModel.class);

	public W2VNrmlMemModelUnitVecBeta(final Map<String, float[]> word2vec, final int vectorSize, int bucketCount) throws IOException {
		super(word2vec, vectorSize, bucketCount);
		currentImpl+= " Beta";
	}
	
	protected int computeGamma(float[] qVec, int lMult) {
		//Assuming normalized vector
		int gamma = 0;
		double lambda = bucketSize*lMult;
		float minVal  = Word2VecMath.getMin(qVec);
		// double alpha = Word2VecMath.norm(qVec);
		double alpha = 1d;
		double lSq = lambda*lambda;
		
		double a = (lSq*alpha-minVal*minVal);
		double b = 2*minVal*alpha*(lSq-1);
		double c = (lSq-1d)*alpha*alpha;
		
		double[] betaArr = Word2VecMath.quadraticEquationRoots(a, b, c);
		
		double beta = betaArr[0];
		double delta1 = calcDelta(beta, minVal);
		beta = betaArr[1];
		double delta2 = calcDelta(beta, minVal);
		
		double delta = Math.abs(delta1>delta2?delta1:delta2);
		
		gamma = (int) Math.ceil(delta/bucketSize);
		return gamma<1?1:gamma;
	}
	
	protected double calcDelta(double beta, double minVal) {
		return (1d+minVal*beta)/(Math.sqrt(1d+beta*beta+2*minVal*beta));
	}
	/**
	 * Method to fetch the closest word entry for a given vector using cosine
	 * similarity
	 * 
	 * @param vector - vector to find closest word to
	 * @param subKey - key to subset if any
	 * @return closest word to the given vector alongwith it's vector
	 */
	protected String getClosestEntry(float[] vector, String subKey) {
		String closestWord = null;
		try {
			// Normalize incoming vector
			vector = Word2VecMath.normalize(vector);
			int gamma =1;
			boolean wordNotFound = true;
			boolean midEmpty;
			int ringRad = -1;
			BitSet midBs;
			//New Addition
			boolean extraItr = true;
			while (wordNotFound) {
				midEmpty = false;
				ringRad++;
				LOG.info("Ring Radius: " + ringRad);
				// calculate cosine similarity of all distances
				float[] curCompVec;
				midBs = new BitSet(word2vec.size());
				BitSet finBitSet = null;
				for (int i = 0; i < compareVecCount; i++) {
					curCompVec = comparisonVecs[i];
					double cosSimVal = Word2VecMath.cosineSimilarityNormalizedVecs(curCompVec, vector);
					int indx = getBucketIndex(cosSimVal);
					BitSet curBs = new BitSet(word2vec.size());
					// calculate middle bitset
					curBs.or(csBucketContainer[i][indx]);
					if (ringRad > 0) {
						orWithNeighbours(indx, ringRad, 0, csBucketContainer[i], curBs);
					}
					if (i == 0) {
						midBs.or(curBs);
						finBitSet = curBs;
					} else {
						midBs.and(curBs);
					}
					if (midBs.cardinality() == 0) {
						midEmpty = true;
						break;
					}
					orWithNeighbours(indx, 1, ringRad, csBucketContainer[i], curBs);
					if (i > 0) {
						finBitSet.and(curBs);
					}
				}
				if(!midEmpty && extraItr) {
					extraItr = false;
					gamma = computeGamma(vector, ringRad>1?ringRad:1);
					// minus one to balance the ++ effect of next iteration
					ringRad+=gamma-1;
				}
				else if (!midEmpty) {
					int nearbyWordsCount = finBitSet.cardinality();
					LOG.info("Number of nearby words: " + nearbyWordsCount);
					int[] nearbyIndexes = new int[nearbyWordsCount];
					int j = 0;
					for (int i = finBitSet.nextSetBit(0); i >= 0; i = finBitSet.nextSetBit(i + 1), j++) {
						// operate on index i here
						nearbyIndexes[j] = i;
						if (i == Integer.MAX_VALUE) {
							break; // or (i+1) would overflow
						}
					}
					closestWord = findClosestWord(nearbyIndexes, vector);
					wordNotFound = false;
				}

			}

		} catch (Exception e) {
			LOG.error("Exception has occured while finding closest word.");
			e.printStackTrace();
		}
		LOG.info("Closest word found is: " + closestWord);
		return closestWord;
	}

}
