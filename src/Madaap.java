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
		
		/*Set to your Gate installation home*/
		Gate.setGateHome(new File("G:\\gate-6.1-build3913-ALL"));
		
		Gate.init();
		
		Gate.getCreoleRegister().registerDirectories(new File(Gate.getPluginsHome(), ANNIEConstants.PLUGIN_DIR).toURI().toURL());
		
		/*Use your own \path-to\Gate\Plugins\Tools directory*/
		Gate.getCreoleRegister().registerDirectories(new File("G:\\gate-6.1-build3913-ALL\\plugins\\Tools").toURI().toURL()); 
		
		BlockingQueue<URL> queue = new LinkedBlockingQueue<URL>();
		TimerTask twitterTask = new Twitter(queue);
		TimerTask fbTask = new Facebook(queue);
		TimerTask deliciousTask = new Delicious(queue);
		TimerTask gplusTask = new Gplus(queue);
		@SuppressWarnings("unused")
		Extractor e = new Extractor(queue);
		Timer timer = new Timer();
		long ONCE_A_DAY = 1000*60*60*24;
		long ONE_SECOND = 1000*60;
		timer.scheduleAtFixedRate(twitterTask, 0, ONCE_A_DAY);
		timer.scheduleAtFixedRate(fbTask, ONE_SECOND, ONCE_A_DAY);
		timer.scheduleAtFixedRate(deliciousTask, 2*ONE_SECOND, ONCE_A_DAY);
		timer.scheduleAtFixedRate(gplusTask, 3*ONE_SECOND, ONCE_A_DAY);
	}
}
