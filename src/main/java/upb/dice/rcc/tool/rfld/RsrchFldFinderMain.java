package upb.dice.rcc.tool.rfld;

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

import upb.dice.rcc.tool.finder.RccFinderTopCosSimSum;
import upb.dice.rcc.tool.finder.RccNounPhrasePair;
import upb.dice.rcc.tool.rfld.generator.RsrchFldMdlGnrtrCsv;

public class RsrchFldFinderMain {

	public static int idIndx = RsrchFldMdlGnrtrCsv.DEFAULT_ID_INDX;
	public static int resColIndx = 4;

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

		Word2VecModel nrmlRsrchFldMdl = Word2VecFactory.getNrmlRsrchFldModel();
		final GenWord2VecModel memModel = new W2VNrmlMemModelBinSrch(nrmlRsrchFldMdl.word2vec,
				nrmlRsrchFldMdl.vectorSize);
		memModel.process();

		RccFinderTopCosSimSum finder = new RccFinderTopCosSimSum(genModel, memModel);

		String pubFileDirPath = "data/rcc/ext_publications/";
		File pubFileDir = new File(pubFileDirPath);
		Map<String, Set<String>> resIdMap = new HashMap<>();
		for (final File fileEntry : pubFileDir.listFiles()) {

			RccNounPhrasePair tmpPair = finder.findClosestResearchField(fileEntry);
			String fldId = tmpPair.getClosestWord();
			Set<String> fileNameSet = resIdMap.get(fldId);
			if (fileNameSet == null) {
				fileNameSet = new HashSet<>();
				resIdMap.put(fldId, fileNameSet);
			}
			fileNameSet.add(fileEntry.getName());
		}

		RsrchFieldPrinter.printResults(resIdMap, idIndx, resColIndx);

	}

}
