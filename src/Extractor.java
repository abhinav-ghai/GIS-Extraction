import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.Mongo;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.DocumentContent;
import gate.Factory;
import gate.FeatureMap;
import gate.creole.ANNIETransducer;
import gate.creole.ExecutionException;
import gate.creole.POSTagger;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.creole.annotdelete.AnnotationDeletePR;
import gate.creole.gazetteer.DefaultGazetteer;
import gate.creole.morph.Morph;
import gate.creole.orthomatcher.OrthoMatcher;
import gate.creole.splitter.SentenceSplitter;
import gate.creole.tokeniser.DefaultTokeniser;
import gate.util.InvalidOffsetException;

/**
 * Contains methods to perform annotations and extract entities on a given URL
 * and store them in a MongoDB database.
 * It is Consumer of URLs from Queue.
 * @author abhinav
 *
 */
public class Extractor implements Runnable{
	private static AnnotationDeletePR deletor;
	private static DefaultTokeniser tokeniser;
	private static DefaultGazetteer gazetteer;
	private static SentenceSplitter splitter;
	private static POSTagger tagger;
	private static ANNIETransducer transducer;
	private static OrthoMatcher matcher;
	private static Morph morpher;
	private static SerialAnalyserController controller;
	private static FeatureMap map = Factory.newFeatureMap();
	private static FeatureMap pub = Factory.newFeatureMap();
	private static FeatureMap mapAbstract = Factory.newFeatureMap();
	private static Corpus corpus;
	static{
		try {
			corpus = Factory.newCorpus("corpus madaap");
		} catch (ResourceInstantiationException e) {
			System.out.println("Could not create corpus");
			e.printStackTrace();
		}
	}
	
	static{
		try{
			deletor = (AnnotationDeletePR)Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR");
			tokeniser = (DefaultTokeniser) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser");
			gazetteer = (DefaultGazetteer) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer");
			splitter = (SentenceSplitter) Factory.createResource("gate.creole.splitter.SentenceSplitter"); 
			tagger = (POSTagger) Factory.createResource("gate.creole.POSTagger");
			transducer = (ANNIETransducer) Factory.createResource("gate.creole.ANNIETransducer");
			matcher = (OrthoMatcher) Factory.createResource("gate.creole.orthomatcher.OrthoMatcher");
			morpher = (Morph) Factory.createResource("gate.creole.morph.Morph");
			controller = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");
		}
		catch(ResourceInstantiationException e){
			e.printStackTrace();
			System.out.println("Couldn't initialize extractor");
		}
	}
	private final BlockingQueue<URL> queue;
	/**
	 * 
	 * @param url
	 */
	public Extractor(BlockingQueue<URL> q){
		queue = q;
		new Thread(this).start();
	}
	

	public static int getEntities(URL url) throws Exception{
				System.out.println(url.toString());
				Document doc = null;
				try {
					doc = Factory.newDocument(url);
				} catch (ResourceInstantiationException e1) {
					System.out.println("Cannot connect to provided link.");
					e1.printStackTrace();
					System.exit(1);
				}
				AnnotationSet set = doAnnotation(doc);
				//System.out.println(set);
				AnnotationSet original = doc.getAnnotations("Original markups");
				HashSet<?> formatSet = (HashSet<?>)getFormats(set,doc);
				Set<?> title = getTitle(original,doc);
				Set<?> authorSet = getAuthor(set,original,doc);
				String abst = getAbstract(original,doc);
				Set<String> downloadLinks = getDownloadLinks(original, doc);
				System.out.println("Formats:" + formatSet);
				System.out.println("Title: " + title.toString());
				System.out.println("Author: " + authorSet);
				System.out.println("Abstract: " + abst.toString());
				System.out.println("Links: " + downloadLinks.toString());
				Mongo m = null;
				try{
					m = new Mongo("localhost",27017);
				}
				catch(Exception e){
					System.out.println("Could not connect to MongoDB.");
				}
				DB db = m.getDB("madaapDB");
				DBCollection coll = db.getCollection("docsCollection");
				coll.createIndex(new BasicDBObject("URL", 1)); 
				BasicDBObject webpage = new BasicDBObject();
				webpage.put("_id", url.toString());
				webpage.put("Formats",formatSet.toString());
				webpage.put("Title",title.toString());
				webpage.put("Author", authorSet.toString());
				webpage.put("Abstract", abst.toString());
				webpage.put("Links", downloadLinks.toString());
				coll.insert(webpage);
				DBCursor allDocs = coll.find();
				while (allDocs.hasNext()){
					System.out.println(allDocs.next());
				}
				Factory.deleteResource(doc);
				return 0;
	}

	/**
	 * 
	 * @param doc
	 * @return
	 * @throws Exception
	 */
	public static AnnotationSet doAnnotation(Document doc) throws Exception {
		/*DefaultTokeniser tokeniser = (DefaultTokeniser) Factory.createResource("gate.creole.tokeniser.DefaultTokeniser");
		DefaultGazetteer gazetteer = (DefaultGazetteer) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer");
		SentenceSplitter splitter = (SentenceSplitter) Factory.createResource("gate.creole.splitter.SentenceSplitter"); 
		POSTagger tagger = (POSTagger) Factory.createResource("gate.creole.POSTagger");
		ANNIETransducer transducer = (ANNIETransducer) Factory.createResource("gate.creole.ANNIETransducer");
		OrthoMatcher matcher = (OrthoMatcher) Factory.createResource("gate.creole.orthomatcher.OrthoMatcher");
		Morph morpher = (Morph) Factory.createResource("gate.creole.morph.Morph");
		SerialAnalyserController controller = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");
		*/
		System.out.println("annotating...");
		/*Corpus corpus = null;
		try {
			corpus = Factory.newCorpus("corpus madaap");
		} catch (ResourceInstantiationException e1) {
			e1.printStackTrace();
		}*/
		corpus.add(doc);
		controller.add(deletor);
		controller.add(tokeniser);
		controller.add(gazetteer);
		controller.add(splitter);
		controller.add(tagger);
		controller.add(transducer);
		controller.add(matcher);
		controller.add(morpher);
		controller.setCorpus(corpus);
		try {
			controller.execute();
		} catch (ExecutionException e) {
			e.printStackTrace();
			System.out.println("Failed to execute processing pipeline");
		}
		corpus.remove(doc);
		AnnotationSet set = doc.getAnnotations();
		corpus.unloadDocument(doc);
		return set;
	}
	
	/**
	 * 
	 * @param set
	 * @param doc
	 * @return
	 */
	public static Set<DocumentContent> getFormats(AnnotationSet set, Document doc){
		//FeatureMap map = Factory.newFeatureMap();
		map.put("majorType", "GISformat");
		AnnotationSet formats = set.get("Lookup",map);
		Set<DocumentContent> formatSet = new HashSet<DocumentContent>();
		Iterator<Annotation> it = formats.iterator();
		while(it.hasNext()){
			Annotation ann = it.next();
			try {
				formatSet.add(doc.getContent().getContent(ann.getStartNode().getOffset(), ann.getEndNode().getOffset()));
			} catch (InvalidOffsetException e) {
				e.printStackTrace();
			}
		}
		return formatSet;
	}
	/**
	 * 
	 * @param set
	 * @param doc
	 * @return
	 * @throws InvalidOffsetException
	 */
	public static Set<DocumentContent> getTitle(AnnotationSet set,Document doc) throws InvalidOffsetException{
		Set<String> titles = new HashSet<String>();
		titles.add("title");
		titles.add("h1");
		//titles.add("h2");
		AnnotationSet original = set.get(titles);
		Set<DocumentContent> titleSet = new HashSet<DocumentContent>();
		if(original.isEmpty()){
			titleSet.add(null);
		}
		else{
			Iterator<Annotation> titleIt = original.iterator();
				while (titleIt.hasNext()){
					Annotation ann = titleIt.next();
					titleSet.add(doc.getContent().getContent(ann.getStartNode().getOffset(), ann.getEndNode().getOffset()));
			}
		}
		return titleSet;
	}
	
	public static Set<?> getAuthor(AnnotationSet set,AnnotationSet original, Document doc) throws InvalidOffsetException{
		
		AnnotationSet org = set.get("Organization");
		Set<DocumentContent> orgSet1 = new HashSet<DocumentContent>();
		Set<DocumentContent> orgSet2 = new HashSet<DocumentContent>();
		Set<DocumentContent> orgSet3 = new HashSet<DocumentContent>();
		Set<DocumentContent> orgSet = new LinkedHashSet<DocumentContent>();
		
		/*Part 1*/
		Annotation ann = null;
		Iterator<Annotation> it = org.iterator();
		while(it.hasNext()){
			ann = (Annotation) it.next();
			orgSet1.add(doc.getContent().getContent(ann.getStartNode().getOffset(), ann.getEndNode().getOffset()));
		}
		
		/*Part-2*/
		AnnotationSet sentence = set.get("Sentence");
		//FeatureMap pub = Factory.newFeatureMap();
		//String list = new String("distribute issue produce publish release assemble supply provide create develop");
		pub.put("root", "publish");
		pub.put("root", "distribute");
		pub.put("root", "distribution");
		pub.put("root", "produce");
		pub.put("root", "provide");
		pub.put("root", "create");
		AnnotationSet allContained = null;
		AnnotationSet pubTokens = null;
		Iterator<Annotation> senit = sentence.iterator();
		while(senit.hasNext()){
			ann = (Annotation) senit.next();
			allContained = set.get(ann.getStartNode().getOffset(), ann.getEndNode().getOffset());
			pubTokens = allContained.get("Token", pub);
			if(!pubTokens.isEmpty()){
				allContained = set.get("Organization",ann.getStartNode().getOffset(), ann.getEndNode().getOffset());
				Iterator<Annotation> orgIt = allContained.iterator();
				while (orgIt.hasNext()){
					ann = (Annotation) orgIt.next();
					orgSet2.add(doc.getContent().getContent(ann.getStartNode().getOffset(), ann.getEndNode().getOffset()));
				}
			}
		}
		/*Part-3*/
		AnnotationSet title = original.get("title");
		Iterator<Annotation> titles = title.iterator();
		while (titles.hasNext()){
			ann = (Annotation) titles.next();
			allContained = set.get("Organization",ann.getStartNode().getOffset(), ann.getEndNode().getOffset());
		}
		Iterator<Annotation> orgInTitle = allContained.iterator();
		while (orgInTitle.hasNext()){
			ann = (Annotation) orgInTitle.next();
			orgSet3.add(doc.getContent().getContent(ann.getStartNode().getOffset(), ann.getEndNode().getOffset()));
		}
		/*Combine sets*/
		orgSet.addAll(orgSet3);
		orgSet.addAll(orgSet2);
		//orgSet.addAll(orgSet1);
		return orgSet;
	}
	
	public static String getAbstract(AnnotationSet set,Document doc) throws InvalidOffsetException{
		//FeatureMap mapAbstract = Factory.newFeatureMap();
		mapAbstract.put("class", "body");
		AnnotationSet para = set.get("p");
		if (para.isEmpty()){
			return "";
		}
		else{
			Iterator<Annotation> it = para.iterator();
			Annotation ann = null;
			int minVal = 1000;
			int current = 0;
			while(it.hasNext()){
				ann = (Annotation) it.next();
				current = ann.getId().intValue();
				if (current<minVal){
					minVal = current;
				}
			}
			ann = para.get(minVal);
			return doc.getContent().getContent(ann.getStartNode().getOffset(),ann.getEndNode().getOffset()).toString();
		}
		
	}
	public static Set<String> getDownloadLinks(AnnotationSet original,Document doc) throws InvalidOffsetException{
		Set<String> href = new HashSet<String>();
		href.add("href");
		AnnotationSet a = original.get("a",href);
		Annotation ann = null;
		Set<String> linkSet = new HashSet<String>();
		String linkVal = null;
		Iterator<Annotation> links = a.iterator();
		while (links.hasNext()){
			ann = (Annotation) links.next();
			linkVal = (String) ann.getFeatures().get("href");
			//linkVal = doc.getContent().getContent(ann.getStartNode().getOffset(), ann.getEndNode().getOffset());
			Set<String> linkEnd = new HashSet<String>();
			linkEnd.add(".zip");
			linkEnd.add(".tar");
			linkEnd.add(".tgz");
			linkEnd.add(".gz");
			linkEnd.add(".csv");
			linkEnd.add(".json");
			linkEnd.add(".geojson");
			linkEnd.add(".kml");
			linkEnd.add(".kmz");
			linkEnd.add(".gml");
			linkEnd.add(".tab");
			linkEnd.add(".tif");
			linkEnd.add(".tiff");
			linkEnd.add(".jp2");
			linkEnd.add(".adf");
			linkEnd.add(".ecw");
			linkEnd.add(".e00");
			linkEnd.add(".hdr");
			linkEnd.add(".nc");
			linkEnd.add(".img");
			linkEnd.add(".dem");
			Iterator<String> linkIt = linkEnd.iterator();
			while (linkIt.hasNext()){
				if (linkVal.endsWith((String) linkIt.next())){
					linkSet.add(linkVal);
					break;
				}
			}
		}
		return linkSet;
		
	}

		public void run() {
		try {
			while(true){
				System.out.println("extractor launched");
				if(getEntities(queue.take())==0){
					report();
				}
			}
		}
		catch (InterruptedException e) {
			System.out.println("Extractor interrupted!");
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("Extractor is unable to get content.");
			e.printStackTrace();
		}
	}


		private void report() {
			// Report to user and Admin
			System.out.println("extractor successfully stored entities.");
			//System.exit(0);
		}
}
