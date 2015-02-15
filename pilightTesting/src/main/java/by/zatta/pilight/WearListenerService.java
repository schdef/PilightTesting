package by.zatta.pilight;

import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Stefan on 24.01.2015.
 */
public class WearListenerService extends WearableListenerService {

    private static final String TAG = "Moto360";

    public WearListenerService() {
        Log.v(TAG, "WearListenerService is in created");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "WearListenerService is in onCreate");
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.v(TAG, "received message from wear");
        if ("/MESSAGE".equals(messageEvent.getPath())) {
            String data = new String(messageEvent.getData());
            Log.v(TAG, "message from wear is: " + data);
            callTentacleBackend(data);
        }
    }

    private void callTentacleBackend(String data) {
        HttpResponse response = null;
        data = data.toLowerCase();
        try {
            // Create http client object to send request to server
            HttpClient client = new DefaultHttpClient();
            // Create URL string
            String state = "";
            String location = "Wohnzimmer";
            String device = "";
            if (data.contains("aus")) {
                state = "off";
            } else if (data.contains("ein")) {
                state = "on";
            }

            if (data.contains("regal")) {
                device = "Regal";
            } else if (data.contains("lampe")) {
                device = "Stehlampe";
            } else if (data.contains("enter")) {
                device = "Entertainment";
            }

            //String URL = "http://192.168.178.33:5001/send?%7B%22message%22%3A%22send%22%2C%22code%22%3A%7B%22location%22%3A%22" + location + "%22%2C%22device%22%3A%22" + device + "%22%2C%22state%22%3A%22" + state + "%22%7D%7D";
            String URL = "http://192.168.178.33:5001/send?%7B%22message%22%3A%22send%22%2C%22code%22%3A%7B%22location%22%3A%22" + location + "%22%2C%22device%22%3A%22" + device + "%22%2C%22state%22%3A%22" + state + "%22%7D%7D";
            // Create Request to server and get response
            Log.v(TAG, "calling URL: " + URL);
            HttpGet httpget = new HttpGet();
            httpget.setURI(new URI(URL));
            response = client.execute(httpget);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}