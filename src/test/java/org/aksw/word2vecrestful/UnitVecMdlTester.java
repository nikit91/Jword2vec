package org.aksw.word2vecrestful;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.word2vec.GenWord2VecModel;
import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelUnitVecBeta;
import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelUnitVecExtItr;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;

public class UnitVecMdlTester {
	static {
		PropertyConfigurator.configure(Cfg.LOG_FILE);
	}
	public static Logger LOG = LogManager.getLogger(UnitVecMdlTester.class);
	public static final float[][] TEST_CENTROIDS = { TestConst.CENT1, TestConst.CENT2, TestConst.CENT3, TestConst.CENT4,
			TestConst.CENT5, TestConst.CENT6, TestConst.CENT7, TestConst.CENT8, TestConst.CENT9, TestConst.CENT10,
			TestConst.CENT11, TestConst.CENT12, TestConst.CENT13, TestConst.CENT14, TestConst.CENT15, TestConst.CENT16,
			TestConst.CENT17, TestConst.CENT18, TestConst.CENT19, TestConst.CENT20 };

	@Test
	public void testNbmTime() throws IOException {
		LOG.info("Starting InMemory Theta Model test!");
		Word2VecModel nbm = Word2VecFactory.getNormalBinModel();
		float[][] centroids = TEST_CENTROIDS;
		LOG.info("Starting BruteForce-Model Test");
		List<String> correctWords = NrmlzdThetaMdlPrfmncTester.getCorrectWords(centroids, nbm);
		LOG.info("Correct Words are :" + correctWords);
		
		LOG.info("Initializing W2VNrmlMemModelUnitVecExtItr Model");
		GenWord2VecModel memModel = new W2VNrmlMemModelUnitVecExtItr(nbm.word2vec, nbm.vectorSize, 50);
		float perc1 = testModel(memModel, centroids, correctWords, "W2VNrmlMemModelUnitVecExtItr");
		
		LOG.info("Initializing W2VNrmlMemModelUnitVecBeta Model");
		memModel = new W2VNrmlMemModelUnitVecBeta(nbm.word2vec, nbm.vectorSize, 50);
		float perc2 = testModel(memModel, centroids, correctWords, "W2VNrmlMemModelUnitVecBeta");
		
		assertEquals(perc2, 100, 0);
		assertTrue(perc2>perc1);
	}
	
	private float testModel(GenWord2VecModel memModel, float[][] centroids, List<String> correctWords, String modelName) throws IOException {
		long startTime, diff;
		long totTime = 0;
		memModel.process();
		List<String> lrModelWords = new ArrayList<>();

		LOG.info("Starting "+modelName+" Test");

		for (int i = 0; i < centroids.length; i++) {
			LOG.info("Sending query for Centroid " + (i + 1));
			startTime = System.currentTimeMillis();
			lrModelWords.add(memModel.getClosestEntry(centroids[i]));
			diff = System.currentTimeMillis() - startTime;
			totTime += diff;
			LOG.info("Query time recorded for Centroid " + (i + 1) + " is " + diff + " milliseconds.");
		}
		LOG.info("Average query time for "+modelName+" is : " + (totTime / centroids.length) + " milliseconds");
		LOG.info("Predicted Words are :" + lrModelWords);
		float percVal = NrmlzdThetaMdlPrfmncTester.calcPercScore(correctWords, lrModelWords);
		LOG.info("Score for Test is : " + percVal + "%");
		lrModelWords.clear();
		return percVal;
		
	}
}
