package org.aksw.word2vecrestful;

import java.util.ArrayList;
import java.util.List;

import org.aksw.word2vecrestful.utils.Cfg;
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
	public void testNbmTime() {
		long startTime, diff;
		long totTime = 0;
		LOG.info("Starting InMemory Theta Model test!");
		Word2VecModel nbm = Word2VecFactory.getNormalBinModel();
		float[][] centroids = { TestConst.CENT1, TestConst.CENT2, TestConst.CENT3, TestConst.CENT4, TestConst.CENT5 };
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
		LOG.info(
				"Average query time for W2VNrmlMemModelKMeans is : " + (totTime / centroids.length) + " milliseconds");
		LOG.info("Predicted Words are :" + lrModelWords);
		float percVal = NrmlzdMdlPrfmncTester.calcPercScore(correctWords, lrModelWords);
		LOG.info("Score for Test is : " + percVal + "%");
		lrModelWords.clear();
	}
}
