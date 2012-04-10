import gate.Gate;
import gate.creole.ANNIEConstants;

import java.io.File;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import collectURL.Delicious;
import collectURL.Facebook;
import collectURL.Gplus;
import collectURL.Twitter;


public class Madaap {

	/**
	 * Starting point of application
	 * @param args
	 * @throws Exception
	 * 
	 */
	public static void main(String[] args) throws Exception{
		Gate.setGateHome(new File("G:\\gate-6.1-build3913-ALL"));
		Gate.init();
		Gate.getCreoleRegister().registerDirectories(new File(Gate.getPluginsHome(), ANNIEConstants.PLUGIN_DIR).toURI().toURL());
		Gate.getCreoleRegister().registerDirectories(new File("G:\\gate-6.1-build3913-ALL\\plugins\\Tools").toURI().toURL());
		
		//URL url = new URL("http://downloads.cloudmade.com/americas/northern_america/united_states/alabama#downloads_breadcrumbs");
		/*
		 * 
		 * URL url = new URL("");
		 * URL url = new URL("http://pubs.usgs.gov/ds/2006/240/");
		 * URL url = new URL("http://geonames.usgs.gov/domestic/download_data.htm");
		 * URL url = new URL("http://ngmdb.usgs.gov/ngmdb/ngmdb_home.html");
		URL url = new URL("http://www.indiana.edu/~gisdata/statewide/2010naip.html");
		URL url = new URL("http://www.ornl.gov/sci/landscan/");
		URL url = new URL("http://www.mrlc.gov/nlcd2006.php");
		URL url = new URL("https://www.nhgis.org/");
		URL url = new URL("http://www.geonames.org/");
		URL url = new URL("http://www4.agr.gc.ca/AAFC-AAC/display-afficher.do?id=1227014964079&lang=eng");
		URL url = new URL("http://opendata.paris.fr/opendata/jsp/site/Portal.jsp");
		File page = new File("page.html");
		FileUtils.copyURLToFile(url, page);
		*/
		
		BlockingQueue<URL> queue = new LinkedBlockingQueue<URL>();
		TimerTask twitterTask = new Twitter(queue);
		TimerTask fbTask = new Facebook(queue);
		TimerTask deliciousTask = new Delicious(queue);
		TimerTask gplusTask = new Gplus(queue);
		@SuppressWarnings("unused")
		Extractor e = new Extractor(queue);
		Timer timer = new Timer();
		long ONCE_A_DAY = 1000*60*60*24;
		long ONE_HOUR = 1000;
		timer.scheduleAtFixedRate(twitterTask, 0, ONCE_A_DAY);
		timer.scheduleAtFixedRate(fbTask, ONE_HOUR, ONCE_A_DAY);
		timer.scheduleAtFixedRate(deliciousTask, 2*ONE_HOUR, ONCE_A_DAY);
		timer.scheduleAtFixedRate(gplusTask, 3*ONE_HOUR, ONCE_A_DAY);
	}
}
