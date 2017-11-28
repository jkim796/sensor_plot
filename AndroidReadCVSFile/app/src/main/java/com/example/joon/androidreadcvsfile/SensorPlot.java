package com.example.joon.androidreadcvsfile;

import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.androidplot.Plot;
import com.androidplot.util.PixelUtils;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;
import com.google.gson.Gson;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.SubscriptionEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class SensorPlot extends AppCompatActivity {
    private static final String TAG = "SensorPlot";
    private static final String REMOTE_SERVER = "http://10.0.2.2:8000/";
    private String csvfile;
    public static final int SIZE = 1569;
    private Integer[] freq;
    private Float[] phase;

    //private Float[] minfreq;
    //private Long[] time;

    private XYPlot phase_dip_plot;
    private XYPlot time_plot;

    private Redrawer redrawer;
    public static int[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.LTGRAY, Color.YELLOW};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot);

        // SensorPlot setup
        phase_dip_plot = (XYPlot) findViewById(R.id.phase_dip_plot);
        phase_dip_plot.setDomainBoundaries(10000000, 199998750, BoundaryMode.FIXED );
        phase_dip_plot.setRangeBoundaries(148, 180, BoundaryMode.FIXED);

        time_plot = (XYPlot) findViewById(R.id.time_plot);
        //time_plot.setD
        //time_plot.setRangeBoundaries(10000000, 200000000, BoundaryMode.FIXED);

        // Set up pusher watcher - this will get the new csv filename generated in the server
        PusherOptions options = new PusherOptions();
        options.setCluster("us2");
        Pusher pusher = new Pusher("36fc8a3649c22f1c1723", options);
        Channel channel = pusher.subscribe("my-channel");
        channel.bind("my-event", new SubscriptionEventListener() {
            @Override
            public void onEvent(String channelName, String eventName, final String data) {
                Log.i(TAG, "Message from Pusher: " + data);
                Gson gson = new Gson();
                Filename filename = gson.fromJson(data, Filename.class);
                Log.i(TAG, "Message parsed; filename: " + filename.getMessage());
                csvfile = REMOTE_SERVER + filename.getMessage();
                //csvfile = "https://jkim796.github.io/test.csv";
                Log.i(TAG, "Latest CSV filename obtained: " + csvfile);
                DownloadFilesTask downloadFilesTask = new DownloadFilesTask();
                downloadFilesTask.execute();
            }
        });
        pusher.connect();
        //plot.redraw();
        //redrawer = new Redrawer(Arrays.asList(new Plot[]{phase_dip_plot, time_plot}),
        //        30, true);
    }

    private class DownloadFilesTask extends AsyncTask<URL, Void, List<String[]>> {
        protected List<String[]> doInBackground(URL... urls) {
            return downloadRemoteTextFileContent();
        }
        protected void onPostExecute(List<String[]> result) {
            if (result != null) {
                printCVSContent(result);
            }
        }
    }

    private void printCVSContent(List<String[]> result) {
        // Frequency Domain SensorPlot
        freq = new Integer[SIZE];
        phase = new Float[SIZE];
        //String cvsColumn = "";
        for (int i = 1; i < result.size(); i++){
            String [] row = result.get(i);
            freq[i] = Integer.parseInt(row[0]);
            phase[i] = Float.parseFloat(row[2]);
        }
        XYSeries series1 = new SimpleXYSeries(
                Arrays.asList(freq), Arrays.asList(phase), "PhaseDip"
        );
        Random rand = new Random();
        int color = colors[rand.nextInt(colors.length)];
        Log.i(TAG, "Using color " + color);
        Log.i(TAG, "freq[1] = " + freq[1] + ", phase[1] = " + phase[1]);
        LineAndPointFormatter series1Format = new LineAndPointFormatter(Color.BLACK, color, null, null);
        series1Format.getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(5));
        phase_dip_plot.addSeries(series1, series1Format);
        //phase_dip_plot.redraw();

        // Time Domain SensorPlot
        //min = new Float[1];
        //time = new Long[1];
        Float minphase = phase[1];
        int index = 1;
        long start = System.nanoTime();

        for (int i = 2; i < SIZE; i++) {
            if (phase[i] < minphase) {
                minphase = phase[i];
                index = i;
            }
        }

        Integer minfreq = freq[index];
        long elapsed = System.nanoTime() - start;
        Log.i(TAG, "Min Frequency: " + minfreq + " at index " + index + " with min phase: " + minphase + ", elapsed: " + elapsed);
        //minphase[0] = min;
        //time[0] = Calendar.getInstance().getTimeInMillis();
        Log.i(TAG, "Time: " + Calendar.getInstance().getTimeInMillis() + "; currentTimeMillis: " + System.currentTimeMillis());
        XYSeries series2 = new SimpleXYSeries(Arrays.asList(new Integer[]{minfreq}), Arrays.asList(new Long[]{System.currentTimeMillis()}),"");
        LineAndPointFormatter series2Format = new LineAndPointFormatter(null, color, null, null);
        series2Format.getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(10));
        time_plot.addSeries(series2, series2Format);


        phase_dip_plot.redraw();
        time_plot.redraw();
    }

    private List<String[]> downloadRemoteTextFileContent() {
        URL mUrl = null;
        List<String[]> csvLine = new ArrayList<>();
        String[] content = null;
        try {
            mUrl = new URL(csvfile);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            assert mUrl != null;
            URLConnection connection = mUrl.openConnection();
            BufferedReader br = new BufferedReader(new
                    InputStreamReader(connection.getInputStream()));
            String line = "";
            while((line = br.readLine()) != null){
                content = line.split(",");
                csvLine.add(content);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return csvLine;
    }

    private class Filename {
        private String message;

        public String getMessage() {
            return message;
        }
    }
}
