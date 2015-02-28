package by.zatta.pilight;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import by.zatta.pilight.model.Config;
import by.zatta.pilight.model.DeviceEntry;

/**
 * Created by Stefan on 24.01.2015.
 */
public class WearListenerService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "Moto360";
    private static final String HOST = "192.168.178.33";
    private static final String PORT = "5001";
    private GoogleApiClient mGoogleApiClient;
    private List<DeviceEntry> pilightDevices = new ArrayList<>();
    public WearListenerService() {
        Log.v(TAG, "WearListenerService is in created");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "WearListenerService is in onCreate");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.v(TAG, "received message from wear");
        if ("/MESSAGE".equals(messageEvent.getPath())) {
            String deviceNameId = new String(messageEvent.getData());
            Log.v(TAG, "MESSAGE message from wear is: " + deviceNameId);
            if (deviceNameId.toLowerCase().contains("empty")) {
                publishList();
            } else {
                switchDevice(deviceNameId);
            }
        } else if ("/GETDEVICELIST".equals(messageEvent.getPath())) {
            String data = new String(messageEvent.getData());
            Log.v(TAG, "GETDEVICELIST message from wear is: " + data);
            publishList();
        }
    }

    private void publishList() {
        Log.d(TAG, "publishList start");
        ArrayList<String> deviceList = new ArrayList<String>();
        getDevices();
        if (pilightDevices != null) {
            for (DeviceEntry dentry : pilightDevices) {
                deviceList.add(dentry.getNameID());
                Log.d(TAG, "added device: " + dentry.getNameID());
            }
        } else {
            deviceList.add("Empty");
        }
        deviceList.add("time-in-ms-" + System.currentTimeMillis());

        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/pilightdevicelist");
        putDataMapReq.getDataMap().putStringArrayList("device-list", deviceList);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);


        Log.d(TAG, "publishList end");
    }

    private void getDevices() {
        String serverString = getDeviceList();
        Log.d(TAG, "Server-Response: " + serverString);
        if (serverString.contains("{\"config\":")) {
            try {
                JSONObject json = new JSONObject(serverString);
                pilightDevices = Config.getDevices(json.getJSONObject("config"));
                Collections.sort(pilightDevices);
                Log.d(TAG, "pilightDevices count: " + pilightDevices.size());
            } catch (JSONException e) {
                Log.d(TAG, "problems in JSONning");
            }
        } else {
            Log.d(TAG, "no server config returned");
        }
    }

    private String getDeviceList() {
        String URL = "http://" + HOST + ":" + PORT + "/config";
        HttpResponse response = null;
        String content = "";
        try {
            HttpClient client = new DefaultHttpClient();
            Log.v(TAG, "calling URL: " + URL);
            HttpGet httpget = new HttpGet();
            httpget.setURI(new URI(URL));
            response = client.execute(httpget);
            content = EntityUtils.toString(response.getEntity());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return content;
    }

    private void switchDevice(String deviceNameIdToSwitch) {
        deviceNameIdToSwitch = deviceNameIdToSwitch.toLowerCase();
        getDevices();
        if (pilightDevices != null && pilightDevices.size() > 0) {
            try {
                HttpClient client = new DefaultHttpClient();
                String URL = buildSwitchUrl(pilightDevices, deviceNameIdToSwitch);
                Log.v(TAG, "calling URL: " + URL);
                HttpGet httpget = new HttpGet();
                httpget.setURI(new URI(URL));
                client.execute(httpget);
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

    private String buildSwitchUrl(List<DeviceEntry> pilightDevices, String deviceNameIdToSwitch) {
        String state = "";
        String location = "";
        String device = "";
        for (DeviceEntry dentry : pilightDevices) {
            if (dentry.getNameID().equalsIgnoreCase(deviceNameIdToSwitch)) {
                state = dentry.getState();
                location = dentry.getLocationID();
                device = dentry.getNameID();
            }
        }
        state = inverseState(state);
        return "http://" + HOST + ":" + PORT + "/send?%7B%22message%22%3A%22send%22%2C%22code%22%3A%7B%22location%22%3A%22" + location + "%22%2C%22device%22%3A%22" + device + "%22%2C%22state%22%3A%22" + state + "%22%7D%7D";
    }

    private String inverseState(String state) {
        if (state.contains("on")) {
            state = "off";
        } else {
            state = "on";
        }
        return state;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }
}