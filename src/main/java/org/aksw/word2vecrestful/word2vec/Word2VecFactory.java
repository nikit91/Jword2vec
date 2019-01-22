package org.aksw.word2vecrestful.word2vec;

import java.io.File;

import org.aksw.word2vecrestful.utils.Cfg;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Word2VecFactory {
	public static Logger LOG = LogManager.getLogger(Word2VecFactory.class);
	public static final String CFG_KEY_MODEL = Word2VecFactory.class.getName().concat(".model");
	public static String model = (Cfg.get(CFG_KEY_MODEL));
	public static final String CFG_KEY_BIN = Word2VecModelLoader.class.getName().concat(".bin");
	public static boolean binModel = Boolean.parseBoolean(Cfg.get(CFG_KEY_BIN));

	private static String nrmlBinMdlFilePath = (Cfg.get("org.aksw.word2vecrestful.word2vec.normalizedbinmodel.model"));
	private static boolean nrmlBinMdlBinFlg = Boolean
			.parseBoolean(Cfg.get("org.aksw.word2vecrestful.word2vec.normalizedbinmodel.bin"));
	
	private static String nrmlRsrchFldMdlFilePath = (Cfg.get("org.aksw.word2vecrestful.word2vec.nrmlrsrchfldbinmodel.model"));
	private static boolean nrmlRsrchFldMdlBinFlg = Boolean
			.parseBoolean(Cfg.get("org.aksw.word2vecrestful.word2vec.nrmlrsrchfldbinmodel.bin"));
	
	private static String nrmlRsrchMthdMdlFilePath = (Cfg.get("org.aksw.word2vecrestful.word2vec.nrmlrsrchmthdbinmodel.model"));
	private static String nrmlStstclMthdMdlFilePath = (Cfg.get("org.aksw.word2vecrestful.word2vec.nrmlststclmthdbinmodel.model"));
	private static boolean nrmlRsrchMthdMdlBinFlg = Boolean
			.parseBoolean(Cfg.get("org.aksw.word2vecrestful.word2vec.nrmlrsrchmthdbinmodel.bin"));

	public static Word2VecModel get() {
		return new Word2VecModelLoader().loadModel(new File(model), binModel);
	}
	
	public static Word2VecModel getNormalBinModel() {
		return new Word2VecModelLoader().loadModel(new File(nrmlBinMdlFilePath), nrmlBinMdlBinFlg);
	}
	
	public static Word2VecModel getNrmlRsrchFldModel() {
		return new Word2VecModelLoader().loadModel(new File(nrmlRsrchFldMdlFilePath), nrmlRsrchFldMdlBinFlg);
	}
	
	public static Word2VecModel getNrmlRsrchMthdModel() {
		return new Word2VecModelLoader().loadModel(new File(nrmlRsrchMthdMdlFilePath), nrmlRsrchMthdMdlBinFlg);
	}
	
	public static Word2VecModel getNrmlStstclMthdModel() {
		return new Word2VecModelLoader().loadModel(new File(nrmlStstclMthdMdlFilePath), nrmlRsrchMthdMdlBinFlg);
	}
}
