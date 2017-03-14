package com.example.edins.weatherinrewind;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private BarChart barChart;
    private LinearLayout layoutMaps, layoutBarChart;
    private ProgressDialog pDialog;

    ArrayList<BarEntry> barEntries;
    ArrayList<String> theDates;
    BarDataSet barDataSet;
    Button returnMap;
    String url = "https://api.darksky.net/forecast/e34efefedc301a5370ca23f015d97d0c/";
    String sendURL, latitudeString, longitudeString, changeDate, timeZoneJson;
    float tempMIN, tempMAX;
    long unixMinusOneDay, numberChange;
    int myNumber, firstNumber;
    TextView timeZone, messageDarkSky, titleText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get notified when the map is ready to be used
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // view
        barChart = (BarChart) findViewById(R.id.bargraph);
        layoutMaps = (LinearLayout) findViewById(R.id.layout_maps);
        layoutBarChart = (LinearLayout) findViewById(R.id.layout_barChart);
        layoutBarChart.setVisibility(LinearLayout.GONE);
        returnMap = (Button) findViewById(R.id.returnMap);
        returnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutMaps.setVisibility(LinearLayout.VISIBLE);
            }
        });

        // required to display for Dark Sky API
        messageDarkSky = (TextView)findViewById(R.id.messageDarkSky);
        messageDarkSky.setText(Html.fromHtml("<a href=https://darksky.net/poweredby/> Powered by Dark Sky"));
        messageDarkSky.setMovementMethod(LinkMovementMethod.getInstance());

        // network
         if (!isNetworkAvailable()){
             titleText = (TextView)findViewById(R.id.titleLayout);
             String title = "Sorry, you need internet connection";
             titleText.setText(title);
         }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // on map click
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // get latitude and longitude
                latitudeString = Double.toString(latLng.latitude);
                longitudeString = Double.toString(latLng.longitude);

                // set focus map on touch
                LatLng changeFocusMap = new LatLng(latLng.latitude, latLng.longitude);
                mMap.addMarker(new MarkerOptions().position(changeFocusMap));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(changeFocusMap));
                // latitude and longitude
                System.out.println(latitudeString);
                System.out.println(longitudeString);

                layoutMaps.setVisibility(LinearLayout.GONE);
                layoutBarChart.setVisibility(LinearLayout.VISIBLE);

                // for time
                Calendar ca = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                ca.set(Calendar.HOUR_OF_DAY, 0);
                ca.set(Calendar.MINUTE, 0);
                ca.set(Calendar.SECOND, 0);
                ca.set(Calendar.MILLISECOND, 0);
                long unixTimeStamp = ca.getTimeInMillis() / 1000;

                // for graphic
                barEntries = new ArrayList<>();
                theDates = new ArrayList<>();
                barDataSet = new BarDataSet(barEntries, "Ciao bello");

                // for time
                unixMinusOneDay = unixTimeStamp - (86400000 / 1000);
                numberChange = System.currentTimeMillis() - (1000 * 60 * 60 * 24);

                // start async class
                GetData getData = new GetData();
                getData.execute();

            }
        });
    }

    // async class
    private class GetData extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // show progress dialog
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Please wait...");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            HttpHandler sh = new HttpHandler();

            // making a request to url
            myNumber = 0;
            for (int i = 0; i < 7; i++) {
                sendURL = url + latitudeString + "," + longitudeString + "," + unixMinusOneDay + "?exclude=currently,flags,hourly";
                String jsonStr = sh.makeServiceCall(sendURL);
                System.out.println(sendURL);
                if (jsonStr != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(jsonStr);
                        // one time get timezone
                        if (i == 0) {
                            timeZoneJson = jsonObject.getString("timezone");
                        }
                        // get value
                        JSONObject mio = (JSONObject) jsonObject.get("daily");
                        JSONArray mojArray = mio.getJSONArray("data");
                        JSONObject c = mojArray.getJSONObject(0);
                        String temperatureMin = c.getString("temperatureMin");
                        String temperatureMax = c.getString("temperatureMax");

                        double temperatureMinCelsius = Math.round(((Double.parseDouble(temperatureMin) - 32) * 5 / 9) * 2) / 2.0;
                        double temperatureMaxCelsius = Math.round(((Double.parseDouble(temperatureMax) - 32) * 5 / 9) * 2) / 2.0;

                        tempMIN = (float) temperatureMinCelsius;
                        tempMAX = (float) temperatureMaxCelsius;

                        firstNumber = i + myNumber;
                        barEntries.add(new BarEntry(tempMIN, firstNumber));
                        barEntries.add(new BarEntry(tempMAX, firstNumber + 1));
                        Date d = new Date(numberChange);
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");
                        changeDate = sdf.format(d);
                        theDates.add(changeDate);
                        theDates.add("");
                        myNumber++;
                        unixMinusOneDay -= (86400000 / 1000);
                        numberChange -= (1000 * 60 * 60 * 24);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }


            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            // dismiss progress dialog
            if (pDialog.isShowing())pDialog.dismiss();

            // style
            barDataSet.setColors(new int[]{Color.BLUE, Color.RED, Color.BLUE, Color.RED, Color.BLUE, Color.RED, Color.BLUE, Color.RED, Color.BLUE, Color.RED, Color.BLUE, Color.RED});
            barDataSet.setValueTextColor(Color.WHITE);
            barChart.setDoubleTapToZoomEnabled(false);
            barChart.setTouchEnabled(false);
            barChart.setDescriptionColor(Color.WHITE);
            barChart.setAutoScaleMinMaxEnabled(true);
            barChart.setDescription("");
            barChart.animateY(3500, Easing.EasingOption.EaseOutBack);
            XAxis xAxis = barChart.getXAxis();
            YAxis xAxisRigt = barChart.getAxisRight();
            YAxis yAxisLeft = barChart.getAxisLeft();
            yAxisLeft.setEnabled(false);
            xAxisRigt.setTextColor(Color.WHITE);
            xAxis.setTextColor(Color.WHITE);
            xAxis.setTextSize(2.0f);
            // legend
            Legend legend = barChart.getLegend();
            legend.setTextColor(Color.WHITE);
            legend.setCustom(new int[]{Color.RED, Color.BLUE}, new String[]{"Max °C", "Min °C"});

            // insert data in graphic
            BarData theData = new BarData(theDates, barDataSet);
            barChart.setData(theData);

            // insert timezone in textview
            timeZone = (TextView) findViewById(R.id.timezone);
            String textZone = "Timezone: " + timeZoneJson;
            timeZone.setText(textZone);
        }
    }

    // for network
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
