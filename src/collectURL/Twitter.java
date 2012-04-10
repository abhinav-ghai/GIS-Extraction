package collectURL;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

public class Twitter extends TimerTask implements Runnable{
	private final BlockingQueue<URL> queue;
	public Twitter(BlockingQueue<URL> q){
		queue = q;
	}
	private void collect() throws Exception{
		System.out.println("twitter collector launched");
		queue.put(new URL("http://downloads.cloudmade.com/americas/northern_america/united_states/alabama#downloads_breadcrumbs"));
		HttpClient twitterClient = new DefaultHttpClient();
		List<NameValuePair> qparams = new ArrayList<NameValuePair>();
		qparams.add(new BasicNameValuePair("q", "#Microsoft"));
		qparams.add(new BasicNameValuePair("5", "5"));
		qparams.add(new BasicNameValuePair("include_entities", "true"));
		URI uri = URIUtils.createURI("http", "search.twitter.com", -1, "/search.json", 
		    URLEncodedUtils.format(qparams, "UTF-8"), null);
		HttpGet httpget = new HttpGet(uri);
		HttpResponse response = twitterClient.execute(httpget);
		HttpEntity entity = response.getEntity();
		JSONArray urls = null;
		if (entity!=null){
			InputStream stream = entity.getContent();
			String twitterOutput = IOUtils.toString(stream);
			JSONObject twitterJson = new JSONObject(twitterOutput);
			JSONArray resultArray = twitterJson.getJSONArray("results");
			for(int i=0;i<resultArray.length();i++){
				urls = resultArray.getJSONObject(i).getJSONObject("entities").getJSONArray("urls");
				// Add URL in queue,implement queue.put(URL)
				System.out.println(urls.toString());
			}
		}
	}
	@Override
	public void run() {
		try {
			collect();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
