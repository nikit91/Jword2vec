package upb.dice.rcc.tool.rmthd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.aksw.word2vecrestful.utils.Cfg;
import org.aksw.word2vecrestful.word2vec.GenWord2VecModel;
import org.aksw.word2vecrestful.word2vec.W2VNrmlMemModelBinSrch;
import org.aksw.word2vecrestful.word2vec.Word2VecFactory;
import org.aksw.word2vecrestful.word2vec.Word2VecModel;
import org.apache.log4j.PropertyConfigurator;

import com.fasterxml.jackson.core.JsonProcessingException;

import upb.dice.rcc.snglscr.finder.RfsTopCosSimSum;
import upb.dice.rcc.tool.PublicationWordSetExtractor;
import upb.dice.rcc.tool.RccNounPhraseLabelPair;

/**
 * Class to demonstrate the usage of methods to find the closest research
 * methods
 * 
 * @author nikitsrivastava
 *
 */
public class RsrchMthdFinderMain {

	/**
	 * Method to demonstrate example usage
	 * 
	 * @param args
	 * @throws JsonProcessingException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// initialize logger
		PropertyConfigurator.configure(Cfg.LOG_FILE);
		// init w2v model
		Word2VecModel genModel = Word2VecFactory.get();

		Word2VecModel nrmlRsrchMthdMdl = Word2VecFactory.getNrmlRsrchMthdModel();

		final GenWord2VecModel memModel = new W2VNrmlMemModelBinSrch(nrmlRsrchMthdMdl.word2vec,
				nrmlRsrchMthdMdl.vectorSize);
		memModel.process();
		// Init wordset extractor
		PublicationWordSetExtractor wordSetExtractor = new PublicationWordSetExtractor(genModel);

		RfsTopCosSimSum finder = new RfsTopCosSimSum(genModel, memModel, wordSetExtractor);

		String pubFileDirPath = "data/rcc/ext_publications/";
		File pubFileDir = new File(pubFileDirPath);
		Map<String, Set<String>> resIdMap = new HashMap<>();
		for (final File fileEntry : pubFileDir.listFiles()) {

			RccNounPhraseLabelPair tmpPair = finder.findClosestResearchField(fileEntry);
			String fldId = tmpPair.getClosestWord();
			Set<String> fileNameSet = resIdMap.get(fldId);
			if (fileNameSet == null) {
				fileNameSet = new HashSet<>();
				resIdMap.put(fldId, fileNameSet);
			}
			fileNameSet.add(fileEntry.getName());
		}

		RsrchMthdPrinter.printResults(resIdMap);

	}

}
