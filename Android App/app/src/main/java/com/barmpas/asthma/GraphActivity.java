package com.barmpas.asthma;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.ValueDependentColor;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

/*
 * The graph activity. Uses the data that the database activity passed to it and displays the results
 * using graph view. Red for error and green for correct presses.
 */


public class GraphActivity extends AppCompatActivity {

    GraphView graph;
    String error_str, date_str, breathes_str;
    TextView txt_date, txt_number_breathes, txt_number_presses;

    //Passing data
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_layout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        graph = (GraphView) findViewById(R.id.graph);
        error_str = getIntent().getStringExtra("ERROR");
        date_str = getIntent().getStringExtra("DATE");
        breathes_str = getIntent().getStringExtra("BREATHES");
        txt_number_presses = findViewById(R.id.txt_number_presses);
        txt_number_breathes = findViewById(R.id.txt_number_breathes);
        txt_date = findViewById(R.id.txt_date);
        populateGraph();
    }


    //Populate the graph and styling them depending if an error occurs or not.
    public void populateGraph() {

        txt_date.setText("Date: " + date_str);

        String breathes[] = breathes_str.split("/");
        int total_breathes = 0;
        for (int i = 0; i < breathes.length; i++) {
            total_breathes = total_breathes + Integer.parseInt(breathes[i]);
        }
        txt_number_breathes.setText("Total number of breathes: " + total_breathes);

        final String error[] = error_str.split("/");
        txt_number_presses.setText("Total number of presses: " + error.length);


        final DataPoint[] points = new DataPoint[error.length + 1];
        points[0] = new DataPoint(0, 0);
        for (int i = 1; i < points.length; i++) {
            points[i] = new DataPoint(i, Integer.parseInt(breathes[i - 1]));
        }
        BarGraphSeries<DataPoint> series = new BarGraphSeries<>(points);

        graph.addSeries(series);

        // styling
        series.setValueDependentColor(new ValueDependentColor<DataPoint>() {
            @Override
            public int get(DataPoint data) {
                if ((int) data.getX() == 0) {
                    return Color.parseColor("#26C6DA");
                } else {
                    if (error[(int) data.getX() - 1].equals("1")) {
                        return Color.RED;
                    } else {
                        return Color.GREEN;
                    }
                }
            }
        });

        series.setSpacing(points.length * 10);
        series.setDrawValuesOnTop(true);
        series.setValuesOnTopColor(Color.GRAY);

        graph.getViewport().setMinX(1);
        graph.getViewport().setMaxX(10);

        graph.getViewport().setScrollable(true); // enables horizontal scrolling
        graph.getViewport().setScalable(true); // enables horizontal zooming and scrolling
        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);
        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);

        graph.getGridLabelRenderer().setHorizontalAxisTitle("Number of presses");
        graph.getGridLabelRenderer().setVerticalAxisTitle("Number of breathes");

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }


}


