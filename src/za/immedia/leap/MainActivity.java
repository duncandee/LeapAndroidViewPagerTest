package za.immedia.leap;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String TAG = "Main";

    private final WebSocketConnection mConnection = new WebSocketConnection();
    private final ObjectMapper mp = new ObjectMapper();

    private TextView mPayload;
    private ViewPager mPager;
    private boolean inGesture = false;

    private ScreenSlidePagerAdapter mPagerAdapter;

    private ArrayList<Drawable> mD = new ArrayList<Drawable>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPayload = (TextView) findViewById(R.id.payload);
        mPager = (ViewPager) findViewById(R.id.pager);

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {

                // Blah Blah I know you should do this... TEST app ... hello
                mD.add(getResources().getDrawable(R.drawable.img_1));
                mD.add(getResources().getDrawable(R.drawable.img_2));
                mD.add(getResources().getDrawable(R.drawable.img_3));
                mD.add(getResources().getDrawable(R.drawable.img_4));
                mD.add(getResources().getDrawable(R.drawable.img_5));

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mPagerAdapter = new ScreenSlidePagerAdapter();
                mPager.setAdapter(mPagerAdapter);
                start();
                super.onPostExecute(result);
            }

        }.execute();

    }

    private class ScreenSlidePagerAdapter extends PagerAdapter {

        public int getCount() {
            return mD.size();
        }

        public Object instantiateItem(View collection, int position) {
            ImageView t = new ImageView(collection.getContext());
            t.setImageDrawable(mD.get(position));
            t.setScaleType(ScaleType.FIT_XY);
            ((ViewPager) collection).addView(t, 0);
            return t;
        }

        @Override
        public void destroyItem(View arg0, int arg1, Object arg2) {
            ((ViewPager) arg0).removeView((View) arg2);
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == ((View) arg1);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void start() {
        final String wsuri = "ws://192.168.3.155:6437";
        // final String wsuri = "ws://192.168.3.120:6437"; //Steven
        try {
            mConnection.connect(wsuri, new WebSocketHandler() {
                @Override
                public void onOpen() {
                    Log.d(TAG, "Status: Connected to " + wsuri);
                    mConnection.sendTextMessage("Hello, world!");
                }

                @Override
                public void onTextMessage(String payload) {
                    if (payload == null || inGesture)
                        return;

                    // Jackson was giving me shit so I am just using basic JSONObj
                    try {
                        JSONObject leap = new JSONObject(payload);

                        if (leap.has("hands")) {
                            JSONArray pointables = leap.getJSONArray("pointables");

                            if (pointables.length() > 0) {
                                JSONObject pointer = (JSONObject) pointables.get(0);

                                if (pointer.has("tipVelocity") && pointer.has("tipPosition")) {

                                    JSONArray tipVelocity = (JSONArray) pointer.getJSONArray("tipVelocity");
                                    JSONArray tipPosition = (JSONArray) pointer.getJSONArray("tipPosition");

                                    // mPayload.setText("" + tipVelocity.toString());

                                    if (tipVelocity.getInt(0) < -2500 && tipPosition.getInt(0) < -50) { // left
                                        int prev = mPager.getCurrentItem() - 1;
                                        mPager.setCurrentItem(prev < 0 ? 0 : prev);
                                        inGesture = true;
                                    } else if (tipVelocity.getInt(0) > 2500 && tipPosition.getInt(0) > 50) {// Right
                                        int next = mPager.getCurrentItem() + 1;
                                        inGesture = true;
                                        mPager.setCurrentItem(next > mPagerAdapter.getCount() ? mPager.getCurrentItem() : next);
                                    }

                                    if (inGesture) {
                                        getActionBar().setTitle("Leap -> " + mPager.getCurrentItem());
                                        Handler h = new Handler();
                                        h.postDelayed(new Runnable() {

                                            @Override
                                            public void run() {
                                                inGesture = false;

                                            }
                                        }, 1000);

                                    }
                                } else {
                                    mPayload.setText("");
                                }

                            } else {
                            }

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int code, String reason) {
                    Log.d(TAG, "Connection lost.");
                }
            });
        } catch (WebSocketException e) {
            Log.d(TAG, e.toString());
        }
    }
}
