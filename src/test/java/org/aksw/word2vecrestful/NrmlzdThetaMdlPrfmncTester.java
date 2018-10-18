package org.aksw.word2vecrestful;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelKMeans;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;

import nikit.test.TestConst;

public class NrmlzdThetaMdlPrfmncTester {
	static {
		PropertyConfigurator.configure(Cfg.LOG_FILE);
	}
	public static Logger LOG = LogManager.getLogger(NrmlzdThetaMdlPrfmncTester.class);
	public static final float[][] TEST_CENTROIDS = { TestConst.CENT1, TestConst.CENT2, TestConst.CENT3, TestConst.CENT4,
			TestConst.CENT5, TestConst.CENT6, TestConst.CENT7, TestConst.CENT8, TestConst.CENT9, TestConst.CENT10,
			TestConst.CENT11, TestConst.CENT12, TestConst.CENT13, TestConst.CENT14, TestConst.CENT15, TestConst.CENT16,
			TestConst.CENT17, TestConst.CENT18, TestConst.CENT19, TestConst.CENT20 };
	public static final String[] TEST_WORDS = { "cat", "dog", "airplane", "road", "kennedy", "rome", "human", "disney",
			"machine", "intelligence", "palaeontology", "surgeon", "amazon", "jesus", "gold", "atlantis", "ronaldo",
			"pele", "scissors", "lizard" };
	/*
	 * @Test public void testNbmTime() { long startTime, diff; long totTime = 0;
	 * LOG.info("Starting InMemory Theta Model test!"); Word2VecModel nbm =
	 * Word2VecFactory.getNormalBinModel(); float[][] centroids = { TestConst.CENT1,
	 * TestConst.CENT2, TestConst.CENT3, TestConst.CENT4, TestConst.CENT5 };
	 * LOG.info("Starting BruteForce-Model Test"); List<String> correctWords =
	 * getCorrectWords(centroids, nbm); LOG.info("Correct Words are :" +
	 * correctWords); LOG.info("Initializing Theta Model"); final
	 * W2VNrmlMemModelTheta memModel = new W2VNrmlMemModelTheta(nbm.word2vec,
	 * nbm.vectorSize); List<String> lrModelWords = new ArrayList<>();
	 * 
	 * LOG.info("Starting Theta-Model Test"); for (int mult = 10; mult < 1000; mult
	 * += 10) { LOG.info("Testing with multplier: " + mult);
	 * memModel.updateGMultiplier(mult);
	 * 
	 * for (int i = 0; i < centroids.length; i++) {
	 * LOG.info("Sending query for Centroid " + (i + 1)); startTime =
	 * System.currentTimeMillis();
	 * lrModelWords.add(memModel.getClosestEntry(centroids[i])); diff =
	 * System.currentTimeMillis() - startTime; totTime += diff;
	 * LOG.info("Query time recorded for Centroid " + (i + 1) + " is " + diff +
	 * " milliseconds."); }
	 * LOG.info("Average query time for W2VNrmlMemModelTheta is : " + (totTime /
	 * centroids.length) + " milliseconds"); LOG.info("Predicted Words are :" +
	 * lrModelWords); float percVal =
	 * NrmlzdMdlPrfmncTester.calcPercScore(correctWords, lrModelWords);
	 * LOG.info("Score for Test is : " + percVal + "%"); lrModelWords.clear(); } }
	 */

	@Test
	public void testNbmTime() throws IOException {
		long startTime, diff;
		long totTime = 0;
		LOG.info("Starting InMemory Theta Model test!");
		Word2VecModel nbm = Word2VecFactory.getNormalBinModel();
		float[][] centroids = TEST_CENTROIDS;
		//float[][] centroids = fetchWordsVec(TEST_WORDS, nbm);
		LOG.info("Starting BruteForce-Model Test");
		List<String> correctWords = NrmlzdMdlPrfmncTester.getCorrectWords(centroids, nbm);
		LOG.info("Correct Words are :" + correctWords);
		LOG.info("Initializing W2VNrmlMemModelKMeans Model");
		final W2VNrmlMemModelKMeans memModel = new W2VNrmlMemModelKMeans(nbm.word2vec, nbm.vectorSize);
		List<String> lrModelWords = new ArrayList<>();

		LOG.info("Starting W2VNrmlMemModelKMeans Test");

		for (int i = 0; i < centroids.length; i++) {
			LOG.info("Sending query for Centroid " + (i + 1));
			startTime = System.currentTimeMillis();
			lrModelWords.add(memModel.getClosestEntry(centroids[i]));
			diff = System.currentTimeMillis() - startTime;
			totTime += diff;
			LOG.info("Query time recorded for Centroid " + (i + 1) + " is " + diff + " milliseconds.");
		}
		LOG.info("Average query time for W2VNrmlMemModelKMeans is : " + (totTime / centroids.length) + " milliseconds");
		LOG.info("Predicted Words are :" + lrModelWords);
		float percVal = NrmlzdMdlPrfmncTester.calcPercScore(correctWords, lrModelWords);
		LOG.info("Score for Test is : " + percVal + "%");
		lrModelWords.clear();

		String word1 = "By_Jonas_Elmerraji";
		String word2 = "%_#F########_3v.jsn";
		float[] word1Vec = nbm.word2vec.get("By_Jonas_Elmerraji");
		float[] word2Vec = nbm.word2vec.get("%_#F########_3v.jsn");
		LOG.info("Cosine Similarity between " + word1 + " & " + word2 + " is : "
				+ Word2VecMath.cosineSimilarity(word1Vec, word2Vec));
		LOG.info("Cosine Similarity between " + word1 + " & Centroid19 is : "
				+ Word2VecMath.cosineSimilarity(TestConst.CENT19, word1Vec));
		LOG.info("Cosine Similarity between " + word2 + " & Centroid19 is : "
				+ Word2VecMath.cosineSimilarity(TestConst.CENT19, word2Vec));
	}

	private static float[][] fetchWordsVec(String[] words, Word2VecModel nbm) {
		float[][] resVec = new float[words.length][300];
		for (int i = 0; i < words.length; i++) {
			resVec[i] = nbm.word2vec.get(words[i]);
		}
		return resVec;
	}
}
