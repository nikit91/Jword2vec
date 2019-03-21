package org.aksw.word2vecrestful;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.utils.Word2VecMath;
import org.aksw.word2vecrestful.word2vec.GenWord2VecModel;
import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelUnitVecBeta;
import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelUnitVecExtItr;
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

	@Test
	public void testNbmTime() throws IOException {
		LOG.info("Starting InMemory Theta Model test!");
		// Word2VecModel nbm = Word2VecFactory.getNormalBinModel();
		Word2VecModel nbm = getNrmlzdTestModel();
		float queryVec[] = new float[100];
		for(int i=0;i<100;i++) {
			queryVec[i] = 1;
		}	
		// float[][] centroids = TEST_CENTROIDS;
		float[][] centroids = {queryVec};
		LOG.info("Starting BruteForce-Model Test");
		List<String> correctWords = NrmlzdThetaMdlPrfmncTester.getCorrectWords(centroids, nbm);
		LOG.info("Correct Words are :" + correctWords);
		
		int bucketSize = 1000000;
		
		LOG.info("Initializing W2VNrmlMemModelUnitVecExtItr Model");
		GenWord2VecModel memModel = new W2VNrmlMemModelUnitVecExtItr(nbm.word2vec, nbm.vectorSize, bucketSize);
		float perc1 = testModel(memModel, centroids, correctWords, "W2VNrmlMemModelUnitVecExtItr");
		
		LOG.info("Initializing W2VNrmlMemModelUnitVecBeta Model");
		memModel = new W2VNrmlMemModelUnitVecBeta(nbm.word2vec, nbm.vectorSize, bucketSize);
		float perc2 = testModel(memModel, centroids, correctWords, "W2VNrmlMemModelUnitVecBeta");
		
		assertEquals(100, perc2, 0);
		assertTrue(perc2>=perc1);
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
	
	
	public static Word2VecModel getNrmlzdTestModel() {
		int vectorSize = 100;
		Map<String, float[]> wordMap = new HashMap<>();
		float[] correctVec = new float[vectorSize];
		float[] wrongVec = new float[vectorSize];
		for(int i=0;i<vectorSize;i++) {
			correctVec[i] = 1;
			if((i+1)%2==0) {
				wrongVec[i] = 0.96f;
			}else {
				wrongVec[i] = 1.04f;
			}
		}
		correctVec[0] = 1.2f;
		wordMap.put("correct", Word2VecMath.normalize(correctVec));
		wordMap.put("wrong", Word2VecMath.normalize(wrongVec));
		return new Word2VecModel(wordMap, vectorSize);
	}
	
}
