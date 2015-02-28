package by.zatta.pilight;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleListActivity extends Activity
        implements WearableListView.ClickListener, DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "Moto360";
    List<String> deviceList = new ArrayList<>();
    private GoogleApiClient mGoogleApiClient;
    private WearableListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listview);

        listView =
                (WearableListView) findViewById(R.id.wearable_list);

        deviceList.add("Empty");
        listView.setAdapter(new Adapter(this, deviceList.toArray(new String[]{})));

        listView.setClickListener(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        requestDeviceList();
    }

    @Override
    public void onClick(WearableListView.ViewHolder v) {
        Integer tag = (Integer) v.itemView.getTag();
        callBackend("/MESSAGE", deviceList.get(tag));
    }

    private void requestDeviceList() {
        Log.v(TAG, "requestDeviceList start");
        callBackend("/GETDEVICELIST", "");
        Log.v(TAG, "requestDeviceList end");
    }

    private void callBackend(final String messageType, final CharSequence message) {
        if (mGoogleApiClient == null)
            return;

        final PendingResult<NodeApi.GetConnectedNodesResult> nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        nodes.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                final List<Node> nodes = result.getNodes();
                if (nodes != null) {
                    for (int i = 0; i < nodes.size(); i++) {
                        final Node node = nodes.get(i);
                        Log.v(TAG, "Node: " + node.getDisplayName());
                        // You can just send a message
                        Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), messageType, message.toString().getBytes());
                        Log.v(TAG, "send message to backend");
                        // or you may want to also check check for a result:
                        // final PendingResult<SendMessageResult> pendingSendMessageResult = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), "/MESSAGE", null);
                        // pendingSendMessageResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        //      public void onResult(SendMessageResult sendMessageResult) {
                        //          if (sendMessageResult.getStatus().getStatusCode()==WearableStatusCodes.SUCCESS) {
                        //              // do something is successed
                        //          }
                        //      }
                        // });
                    }
                }
            }
        });
    }

    @Override
    public void onTopEmptyRegionClick() {
    }

    @Override
    protected void onResume() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.v(TAG, "received onDataChanged");
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/pilightdevicelist") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    final ArrayList<String> deviceList = dataMap.getStringArrayList("device-list");
                    Log.v(TAG, "Received deviceList with count: " + deviceList.size());

                    // cleaned up device list
                    final ArrayList<String> cleanedDeviceList = new ArrayList<>();
                    for (int i = 0; i < deviceList.size(); i++) {
                        String deviceName = deviceList.get(i);
                        if (!deviceName.contains("time-in-ms")) {
                            cleanedDeviceList.add(deviceName);
                        }
                    }
                    Collections.sort(cleanedDeviceList);
                    this.deviceList = cleanedDeviceList;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listView.setAdapter(new Adapter(SimpleListActivity.this, cleanedDeviceList.toArray(new String[]{})));
                            listView.invalidate();
                        }
                    });
                }
            }
        }
    }

    private static final class Adapter extends WearableListView.Adapter {
        private final Context mContext;
        private final LayoutInflater mInflater;
        private String[] mDataset;

        public Adapter(Context context, String[] dataset) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
            mDataset = dataset;
        }

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
            return new ItemViewHolder(mInflater.inflate(R.layout.list_item, null));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder,
                                     int position) {
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            TextView view = itemHolder.textView;
            view.setText(mDataset[position]);
            holder.itemView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return mDataset.length;
        }

        public static class ItemViewHolder extends WearableListView.ViewHolder {
            private TextView textView;

            public ItemViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView.findViewById(R.id.name);
            }
        }
    }
}