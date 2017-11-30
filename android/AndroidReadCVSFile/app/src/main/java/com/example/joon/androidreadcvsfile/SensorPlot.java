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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
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
    private LineChart mChart;

    private static final String TAG = "SensorPlot";
    //private static final String REMOTE_SERVER = "http://10.0.2.2:8000/";
    private String remote_server_ip;
    private static final String PROTO = "http://";
    private static final String PORT = "8000";
    private String csvfile;
    public static final int SIZE = 1569;
    private Integer[] freq;
    private Float[] phase;
    private Pusher pusher;

    private Integer[] freqency_yaxis;
    private Long[] time;

    private XYPlot phase_dip_plot;
    //private XYPlot time_plot;

    private Redrawer redrawer;
    public static int[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.CYAN, Color.GRAY, Color.MAGENTA, Color.LTGRAY, Color.YELLOW};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot);

        mChart = (LineChart) findViewById(R.id.chart);
        setupChart();
        setupAxes();
        setupData();
        setLegend();

        // SensorPlot setup
        phase_dip_plot = (XYPlot) findViewById(R.id.phase_dip_plot);
        phase_dip_plot.setDomainBoundaries(10000000, 199998750, BoundaryMode.FIXED );
        phase_dip_plot.setRangeBoundaries(148, 180, BoundaryMode.FIXED);

//        time_plot = (XYPlot) findViewById(R.id.time_plot);
//        //time_plot.setD
//        time_plot.setRangeBoundaries(199998750 - 100, 199998750 + 100, BoundaryMode.FIXED);
//        time_plot.setDomainLowerBoundary(System.nanoTime(), BoundaryMode.AUTO);

        // Set up pusher watcher - this will get the new csv filename generated in the server
        PusherOptions options = new PusherOptions();
        options.setCluster("us2");
        pusher = new Pusher("36fc8a3649c22f1c1723", options);
        Channel channel = pusher.subscribe("my-channel");
        channel.bind("my-event", new SubscriptionEventListener() {
            @Override
            public void onEvent(String channelName, String eventName, final String data) {
                Log.i(TAG, "Message from Pusher: " + data);
                Gson gson = new Gson();
                Filename filename = gson.fromJson(data, Filename.class);
                remote_server_ip = filename.getIp();
                Log.i(TAG, "Message received from IP: " + remote_server_ip + "; Message parsed; filename: " + filename.getMessage());
                csvfile = PROTO + remote_server_ip + ":" + PORT + "/" + filename.getMessage();
                // test
                //csvfile = "http://10.0.2.2:8000/" + filename.getMessage();
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
        freqency_yaxis = new Integer[1];
        time = new Long[1];
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
//        long elapsed = System.nanoTime() - start;
//        Log.i(TAG, "Min Frequency: " + minfreq + " at index " + index + " with min phase: " + minphase + ", elapsed: " + elapsed);
//        freqency_yaxis[0] = minfreq;
//        time[0] = System.nanoTime();
//        //minphase[0] = min;
//        //time[0] = Calendar.getInstance().getTimeInMillis();
//        Log.i(TAG, "Time: " + time[0] + "; currentTimeMillis: " + System.currentTimeMillis());
//        XYSeries series2 = new SimpleXYSeries(Arrays.asList(freqency_yaxis), Arrays.asList(time),"");
//        LineAndPointFormatter series2Format = new LineAndPointFormatter(null, color, null, null);
//        series2Format.getVertexPaint().setStrokeWidth(PixelUtils.dpToPix(15));
//        time_plot.addSeries(series2, series2Format);

        phase_dip_plot.redraw();
//        time_plot.redraw();
        addEntry(minfreq);
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
        private String ip;

        public String getMessage() {
            return message;
        }

        public String getIp() {
            return ip;
        }
    }

    private void setupChart() {
        // disable description text
        mChart.getDescription().setEnabled(false);
        // enable touch gestures
        mChart.setTouchEnabled(true);
        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);
        // enable scaling
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        // set an alternative background color
        mChart.setBackgroundColor(Color.DKGRAY);
    }

    private void setupAxes() {
        XAxis xl = mChart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaximum(199998750 + 100);
        leftAxis.setAxisMinimum(199998750 - 100);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

        // Add a limit line
        LimitLine ll = new LimitLine(200000000, "Upper Limit");
        ll.setLineWidth(2f);
        ll.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        ll.setTextSize(10f);
        ll.setTextColor(Color.WHITE);
        // reset all limit lines to avoid overlapping lines
        leftAxis.removeAllLimitLines();
        leftAxis.addLimitLine(ll);
        // limit lines are drawn behind data (and not on top)
        leftAxis.setDrawLimitLinesBehindData(true);
    }

    private void setupData() {
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        // add empty data
        mChart.setData(data);
    }

    private void setLegend() {
        // get the legend (only possible after setting data)
        Legend l = mChart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.CIRCLE);
        l.setTextColor(Color.WHITE);
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Time Domain Data");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColors(ColorTemplate.VORDIPLOM_COLORS[0]);
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(10f);
        // To show values of each point
        set.setDrawValues(true);

        return set;
    }

    private void addEntry(int freq) {
        LineData data = mChart.getData();

        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), freq), 0);

            // let the chart know it's data has changed
            data.notifyDataChanged();
            mChart.notifyDataSetChanged();

            // limit the number of visible entries
            mChart.setVisibleXRangeMaximum(15);

            // move to the latest entry
            mChart.moveViewToX(data.getEntryCount());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pusher.disconnect();
    }
}
