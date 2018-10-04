package org.aksw.word2vecrestful.word2vec;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import org.aksw.word2vecrestful.utils.ClusterableVec;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import nikit.test.TimeLogger;

/**
 * Class to encapsulate word2vec in-memory model and expose methods to perform
 * search on the model
 * 
 * @author Nikit
 *
 */
public class W2VNrmlMemModelKMeans implements GenWord2VecModel {
	public static Logger LOG = LogManager.getLogger(GenWord2VecModel.class);

	private Map<String, float[]> word2vec;
	private int vectorSize;
	private float[][] comparisonVecs = null;
	private String[] wordArr;
	private float[][] vecArr;
	private int compareVecCount = 100;
	private int bucketCount = 10;
	private BitSet[][] csBucketContainer;
	// TODO : Remove this
	private TimeLogger tl = new TimeLogger();

	public W2VNrmlMemModelKMeans(final Map<String, float[]> word2vec, final int vectorSize) {
		this.word2vec = word2vec;
		this.vectorSize = vectorSize;
		comparisonVecs = new float[compareVecCount][vectorSize];
		csBucketContainer = new BitSet[compareVecCount][bucketCount];
		LOG.info("Starting Generation of comparison vectors!");
		// Fetch comparison vectors
		generateComparisonVectors();
		LOG.info("Comparison vectors generated. Building buckets.");
		// Initialize Arrays
		processCosineSim();

	}

	private void generateComparisonVectors() {
		KMeansPlusPlusClusterer<ClusterableVec> clusterer = new KMeansPlusPlusClusterer<>(compareVecCount, 10);
		List<ClusterableVec> vecList = new ArrayList<>();
		for (float[] vec : word2vec.values()) {
			vecList.add(getClusterablePoint(vec));
		}
		List<CentroidCluster<ClusterableVec>> compVecList = clusterer.cluster(vecList);
		int i = 0;
		for (CentroidCluster<ClusterableVec> entry : compVecList) {
			Clusterable centroid = entry.getCenter();
			float[] fCentroid = Word2VecMath.convertDoublesToFloats(centroid.getPoint());
			comparisonVecs[i] = fCentroid;
			i++;
		}
	}

	public static ClusterableVec getClusterablePoint(float[] vec) {
		return new ClusterableVec(vec);
	}

	private void processCosineSim() {
		double cosSimVal;
		this.wordArr = new String[word2vec.size()];
		this.vecArr = new float[word2vec.size()][vectorSize];
		int i = 0;
		for (String word : word2vec.keySet()) {
			wordArr[i] = word;
			float[] vec = word2vec.get(word);
			vecArr[i] = vec;
			for (int j = 0; j < compareVecCount; j++) {
				BitSet[] comparisonVecBuckets = csBucketContainer[j];
				cosSimVal = Word2VecMath.cosineSimilarityNormalizedVecs(comparisonVecs[j], vec);
				// Setting bitset for the comparison vec
				setValToBucket(i, cosSimVal, comparisonVecBuckets);
			}
			i++;
		}
	}

	private int getBucketIndex(double cosineSimVal) {
		Double dIndx = ((bucketCount - 1d) / 2d) * (cosineSimVal + 1d);
		return Math.round(dIndx.floatValue());
	}

	private void setValToBucket(int wordIndex, double cosSimVal, BitSet[] meanComparisonVecBuckets) {
		int bucketIndex = getBucketIndex(cosSimVal);
		BitSet bitset = meanComparisonVecBuckets[bucketIndex];
		if (bitset == null) {
			bitset = new BitSet(word2vec.size());
			meanComparisonVecBuckets[bucketIndex] = bitset;
		}
		bitset.set(wordIndex);
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
		String closestWord = null;
		try {
			// Normalize incoming vector
			vector = Word2VecMath.normalize(vector);
			// calculate cosine similarity of all distances
			float[] curCompVec;
			BitSet finBitSet = null;
			tl.logTime(1);
			for (int i = 0; i < compareVecCount; i++) {
				curCompVec = comparisonVecs[i];
				double cosSimVal = Word2VecMath.cosineSimilarityNormalizedVecs(curCompVec, vector);
				int indx = getBucketIndex(cosSimVal);
				BitSet curBs = new BitSet(word2vec.size());
				curBs.or(csBucketContainer[i][indx]);
				int temIndx = indx + 1;
				if (temIndx < csBucketContainer[i].length && csBucketContainer[i][temIndx] != null) {
					curBs.or(csBucketContainer[i][temIndx]);
				}
				temIndx = indx - 1;
				if (temIndx > -1 && csBucketContainer[i][temIndx] != null) {
					curBs.or(csBucketContainer[i][temIndx]);
				}
				if (i == 0) {
					finBitSet = curBs;
				} else {
					finBitSet.and(curBs);
				}
			}
			tl.printTime(1, "Setting Bits");
			tl.logTime(1);
			int[] nearbyIndexes = new int[finBitSet.cardinality()];
			int j = 0;
			for (int i = finBitSet.nextSetBit(0); i >= 0; i = finBitSet.nextSetBit(i + 1), j++) {
				// operate on index i here
				nearbyIndexes[j] = i;
				if (i == Integer.MAX_VALUE) {
					break; // or (i+1) would overflow
				}
			}
			tl.printTime(1, "Extracting words");
			tl.logTime(1);
			closestWord = findClosestWord(nearbyIndexes, vector);
			tl.printTime(1, "finding closest word");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return closestWord;
	}

	private String findClosestWord(int[] nearbyIndexes, float[] vector) {
		double minDist = -2;
		String minWord = null;
		double tempDist;
		for (int indx : nearbyIndexes) {
			float[] wordvec = vecArr[indx];
			tempDist = getSqEucDist(vector, wordvec, minDist);
			if (tempDist != -1) {
				minWord = wordArr[indx];
				minDist = tempDist;
			}
		}
		return minWord;
	}

	/**
	 * Method to find the squared value of euclidean distance between two vectors if
	 * it is less than the provided minimum distance value, otherwise return -1
	 * 
	 * @param arr1
	 *            - first vector
	 * @param arr2
	 *            - second vector
	 * @param minDist
	 *            - minimum distance constraint
	 * @return squared euclidean distance between two vector or -1
	 */
	private double getSqEucDist(float[] arr1, float[] arr2, double minDist) {
		double dist = 0;
		for (int i = 0; i < vectorSize; i++) {
			dist += Math.pow(arr1[i] - arr2[i], 2);
			if (minDist != -2 && dist > minDist)
				return -1;
		}
		return dist;
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