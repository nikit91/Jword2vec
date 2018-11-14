package upb.dice.rcc.tool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import upb.dice.rcc.tool.finder.RccFinder;

/**
 * Class to help execute the RccFinder to find research fields and methods for
 * the publications in a given directory
 * 
 * @author nikitsrivastava
 *
 */
public class RccMainThread extends RccMain {

	public static final long SLEEP_DURATION = 10000; // 10 seconds

	public RccMainThread(File rFldInputFile, File rMthdInputFile, File rFldOutputFile, File rMthdOutputFile)
			throws IOException {
		super(rFldInputFile, rMthdInputFile, rFldOutputFile, rMthdOutputFile);
	}

	/**
	 * Method to demonstrate example usage
	 * 
	 * @param args
	 * @throws JsonProcessingException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void main(String[] args) {
		// Publications directory
		String pubDirPath = args[0];
		// Research field output file path
		String rFldOutputFilePath = args[1];
		// Research method output file path
		String rMthdOutputFilePath = args[2];
		// Research field input file path
		String rFldInputFilePath = args[3];
		// Research method input file path
		String rMthdInputFilePath = args[4];

		File rFldInputFile = new File(rFldInputFilePath);
		File rMthdInputFile = new File(rMthdInputFilePath);
		// Research field output file
		File rFldOutputFile = new File(rFldOutputFilePath);
		// Research method output file
		File rMthdOutputFile = new File(rMthdOutputFilePath);
		try {
			// init the main
			RccMain rccMain = new RccMainThread(rFldInputFile, rMthdInputFile, rFldOutputFile, rMthdOutputFile);
			rccMain.processEntries(pubDirPath);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void processEntries(String pubDirPath) throws Exception {
		TLOG.logTime(1);
		// fetch closest entries
		this.fetchWriteClosestEntries(pubDirPath);
		// write the closest entries to file

		TLOG.printTime(1, "Total Fetch time (Methods and Fields)");
	}

	/**
	 * Method to read all the publication files in a given directory and write the
	 * research field and method associated with them
	 * 
	 * @param pubDirPath - path to the publications directory
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void fetchWriteClosestEntries(String pubDirPath) throws IOException, InterruptedException {

		File pubFileDir = new File(pubDirPath);
		Map<String, RccNounPhraseLabelPair> rfldMap = new HashMap<>();
		Map<String, RccNounPhraseLabelPair> rmthdMap = new HashMap<>();
		int count = 0;
		// Create threads for all publications
		for (final File fileEntry : pubFileDir.listFiles()) {
			PublicationWordSetExtractor extractor = new PublicationWordSetExtractor(genModel);
			FinderThread rfldThrd = new FinderThread(rFldFinder, fileEntry, rfldMap, extractor);
			FinderThread rmthdThrd = new FinderThread(rMthdFinder, fileEntry, rmthdMap, extractor);
			// Execute the threads
			Thread finderThread = new Thread(rfldThrd);
			finderThread.start();
			finderThread = new Thread(rmthdThrd);
			finderThread.start();
			count++;
		}
		// method to wait for finder processing to end
		waitWriteEntries(rfldMap, rmthdMap, count);

	}

	private void waitWriteEntries(Map<String, RccNounPhraseLabelPair> rfldMap,
			Map<String, RccNounPhraseLabelPair> rmthdMap, int count)
			throws JsonGenerationException, JsonMappingException, IOException, InterruptedException {
		TLOG.logTime(2);
		while (true) {
			if (rfldMap.size() == count && rmthdMap.size() == count) {
				for (String keyEntry : rmthdMap.keySet()) {
					// Save research field
					this.saveRfldEntry(keyEntry, rfldMap.get(keyEntry));
					// Save research method
					this.saveRmthdEntry(keyEntry, rmthdMap.get(keyEntry));
				}
				this.writeClosestEntries(rFldOutputFile, rMthdOutputFile);
				break;
			} else {
				TLOG.printTime(2, "wait method");
				Thread.sleep(SLEEP_DURATION);
			}
		}
	}

	static class FinderThread implements Runnable {
		private RccFinder finder;
		private File fileEntry;
		private Map<String, RccNounPhraseLabelPair> pairMap;
		private PublicationWordSetExtractor extractor;

		public FinderThread(RccFinder finder, File fileEntry, Map<String, RccNounPhraseLabelPair> pairMap,
				PublicationWordSetExtractor extractor) {
			super();
			this.finder = finder;
			this.fileEntry = fileEntry;
			this.pairMap = pairMap;
			this.extractor = extractor;
		}

		@Override
		public void run() {
			try {
				LOG.info("thread started for: " + fileEntry.getName());
				pairMap.put(fileEntry.getName(), finder.findClosestResearchField(fileEntry, extractor));
			} catch (IOException e) {
				LOG.error("finder failed for: " + fileEntry.getName());
				e.printStackTrace();
			}

		}

	}

}
