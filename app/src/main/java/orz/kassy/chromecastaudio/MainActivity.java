package orz.kassy.chromecastaudio;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = "ChromeCastAudio";

    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private ArrayList<CastDevice> mDeviceList;
    private CastDeviceListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkGooglePlayServices();

        setContentView(R.layout.activity_main);

        mDeviceList = new ArrayList<CastDevice>();
        ListView listView = (ListView) findViewById(R.id.deviceListView);
        mAdapter = new CastDeviceListAdapter(this, 0, mDeviceList);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);

        mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(getString(R.string.app_id)))
                .build();
        mMediaRouter = MediaRouter.getInstance(getApplicationContext());
    }

    /**
     * Search 1. MediaRouterでsearch
     */
    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();
        mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMediaRouter.removeCallback(mMediaRouterCallback);
    }

    /**
     * Search 2. MediaRouterでSearchしたCallback. CastDeviceが見つかっている
     */
    private final MediaRouter.Callback mMediaRouterCallback = new MediaRouter.Callback() {
        @Override
        public void onRouteAdded(MediaRouter router, RouteInfo info) {
            Log.i(TAG, "onRouteAdded");
            CastDevice castDevice = CastDevice.getFromBundle(info.getExtras());

            // CastDeviceにAudio出力機能があるときのみ
            if (castDevice.hasCapability(CastDevice.CAPABILITY_AUDIO_OUT)) {
                mDeviceList.add(castDevice);
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onRouteRemoved(MediaRouter router, RouteInfo route) {
            Log.i(TAG, "onRouteRemoved");
        }

        @Override
        public void onRouteSelected(MediaRouter router, RouteInfo info) {
            Log.d(TAG, "onRouteSelected");
        }

        @Override
        public void onRouteUnselected(MediaRouter router, RouteInfo info) {
            Log.d(TAG, "onRouteUnselected");
        }
    };


    /**
     * ListViewをクリックした時
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent intent = new Intent(this, CastActivity.class);
        intent.putExtra(CastActivity.INTENT_EX_CAST_DEVICE, mDeviceList.get(position));
        startActivity(intent);
    }

    /**
     * ListViewへのアダプターのカスタム
     */
    public class CastDeviceListAdapter extends ArrayAdapter<CastDevice> {
        private LayoutInflater mInflater;

        public CastDeviceListAdapter(Context context, int textViewResourceId, List<CastDevice> objects) {
            super(context, textViewResourceId, objects);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final CastDevice castDevice = (CastDevice)getItem(position);
            if (null == convertView) {
                convertView = mInflater.inflate(R.layout.view_list_row, null);
            }

            // CastDeviceの名前をセット
            TextView txtName = (TextView) convertView.findViewById(R.id.txtListRowName);
            txtName.setText(castDevice.getFriendlyName());

            return convertView;
        }
    }

    /**
     * check device google play service
     * @return
     */
    private boolean checkGooglePlayServices() {
        int googlePlayServicesCheck = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (googlePlayServicesCheck == ConnectionResult.SUCCESS) {
            return true;
        }
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(googlePlayServicesCheck, this, 0);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });
        dialog.show();
        return false;
    }
}
