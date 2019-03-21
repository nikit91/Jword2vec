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
	
	protected double bucketSize;
	protected String currentImpl;
	public W2VNrmlMemModelUnitVec(final Map<String, float[]> word2vec, final int vectorSize, int bucketCount) throws IOException {
		super(word2vec, vectorSize, vectorSize, bucketCount);
		bucketSize = (2d)/(Double.valueOf(bucketCount));
		currentImpl = "Unit Vector";
	}

	@Override
	public void process() throws IOException {
		LOG.info("Process from "+currentImpl+" called");
		generateComparisonVecs();
		// Initialize Arrays
		genAllCosineSim();
	}

	private void generateComparisonVecs() {
		for (int i = 0; i < vectorSize; i++) {
			float[] compareVec = new float[vectorSize];
			compareVec[i] = 1;
			comparisonVecs[i] = compareVec;
		}
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
			boolean wordNotFound = true;
			boolean midEmpty;
			int ringRad = -1;
			BitSet midBs;
			//New Addition
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
					if(csBucketContainer[i][indx]!=null) {
						curBs.or(csBucketContainer[i][indx]);
					}
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
				if (!midEmpty) {
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

	/**
	 * Method to or with all the bitsets in a given range of a bucket from a given
	 * offset
	 * 
	 * @param bIndx     - index of the bucket in bucket array
	 * @param range     - range of buckets to or with including both left and right
	 *                  sides
	 * @param offset    - offset of buckets , 0 if none
	 * @param bucketArr - array of buckets
	 * @param curBs     - bitset to perform or operation on
	 */
	protected void orWithNeighbours(int bIndx, int range, int offset, BitSet[] bucketArr, BitSet curBs) {
		int rNbr = bIndx + offset + 1;
		int lNbr = bIndx - offset - 1;
		int contLen = bucketArr.length;
		int rRangeAdd = rNbr + range;
		int rLim = rRangeAdd > contLen ? contLen : rRangeAdd; // exclusive
		int lRangeSub = lNbr - range - 1;
		int lLim = lRangeSub < 0 ? 0 : lRangeSub; // inclusive
		BitSet temp;
		while (true) {
			if (rNbr < rLim) {
				temp = bucketArr[rNbr];
				orOperation(temp, curBs);
			}
			if (lNbr >= lLim) {
				temp = bucketArr[lNbr];
				orOperation(temp, curBs);
			}
			rNbr++;
			lNbr--;
			if (rNbr >= rLim && lNbr < lLim) {
				break;
			}
		}
	}

}
