// Varse (c) 2019/20 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.nuhvar.view.showresult;


import android.content.Context;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.devbaltasarq.nuhvar.R;
import com.devbaltasarq.nuhvar.core.Result;
import com.devbaltasarq.nuhvar.core.ResultAnalyzer;
import com.devbaltasarq.nuhvar.view.Prefs;
import com.devbaltasarq.nuhvar.view.Util;

import java.util.ArrayList;


/** Represents the result data set as a graph on the screen.
  * @author Leandro (removed chart dependency and temporary file loading by baltasarq)
  */
public class ResultViewerActivity extends AppCompatActivity {
    private static final String LogTag = ResultViewerActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_result_viewer );

        // Back button
        final ActionBar ACTION_BAR = this.getSupportActionBar();

        if ( ACTION_BAR != null ) {
            ACTION_BAR.setTitle( R.string.lblResultViewer );
            ACTION_BAR.setDisplayHomeAsUpEnabled( true );
            ACTION_BAR.setLogo( R.drawable.ic_graph );
        }

        // Change data/graphic button
        final ImageButton BT_GRAPH = this.findViewById( R.id.btGraphic );
        final ImageButton BT_DATA = this.findViewById( R.id.btData );
        final ImageButton BT_ZOOM = this.findViewById( R.id.btZoom );

        BT_GRAPH.setOnClickListener( (v) -> this.changeToGraphView() );
        BT_DATA.setOnClickListener( (v) -> this.changeToDataView() );
        BT_ZOOM.setOnClickListener( (v) -> this.changeChartZoom() );

        // Chart image viewer
        final StandardGestures GESTURES = new StandardGestures( this );

        this.chartView = findViewById( R.id.ivChartViewer );
        this.chartView.setOnTouchListener( GESTURES );

        final TextView BOX_DATA = this.findViewById( R.id.lblTextData );
        BOX_DATA.setMovementMethod( new ScrollingMovementMethod() );

        this.resultAnalyzer = new ResultAnalyzer( result );
        this.resultAnalyzer.analyze();

        if ( this.resultAnalyzer.getRRnfCount() > 0  ) {
            // Plots interpolated HR signal and data analysis
            this.prefs = Prefs.get();
            this.plotChart();
            final String REPORT = this.resultAnalyzer.buildReport();

            BOX_DATA.setText( Html.fromHtml( REPORT, Html.FROM_HTML_MODE_LEGACY ) );
        } else {
            final String MSG = "Empty data";

            Log.e( LogTag, MSG );
            Util.showMessage( this, MSG );
        }

        this.zoomed = false;
        this.changeToGraphView();
    }

    /** Show the graphic and hide the data info. */
    private void changeToGraphView()
    {
        final ImageButton BT_GRAPH = this.findViewById( R.id.btGraphic );
        final ImageButton BT_DATA = this.findViewById( R.id.btData );
        final ImageButton BT_ZOOM = this.findViewById( R.id.btZoom );
        final ImageView IV_CHART = this.findViewById( R.id.ivChartViewer );
        final TextView TV_DATA = this.findViewById( R.id.lblTextData );

        BT_ZOOM.setVisibility( View.VISIBLE );
        BT_DATA.getDrawable().mutate().setAlpha( 255 );
        BT_DATA.setEnabled( true );
        BT_GRAPH.getDrawable().mutate().setAlpha( 100 );
        BT_GRAPH.setEnabled( false );

        TV_DATA.setVisibility( View.GONE );
        IV_CHART.setVisibility( View.VISIBLE );
    }

    /** Show the text view and hide the graphic. */
    private void changeToDataView()
    {
        final ImageButton BT_GRAPH = this.findViewById( R.id.btGraphic );
        final ImageButton BT_DATA = this.findViewById( R.id.btData );
        final ImageButton BT_ZOOM = this.findViewById( R.id.btZoom );
        final ImageView IV_CHART = this.findViewById( R.id.ivChartViewer );
        final TextView TV_DATA = this.findViewById( R.id.lblTextData );

        BT_ZOOM.setVisibility( View.GONE );
        BT_GRAPH.getDrawable().mutate().setAlpha( 255 );
        BT_GRAPH.setEnabled( true );
        BT_DATA.getDrawable().mutate().setAlpha( 100 );
        BT_DATA.setEnabled( false );

        IV_CHART.setVisibility( View.GONE );
        TV_DATA.setVisibility( View.VISIBLE );
    }

    /** Shows the chart with normalized data, or not. */
    private void changeChartZoom()
    {
        final ImageButton BT_ZOOM = this.findViewById( R.id.btZoom );
        Drawable icon = AppCompatResources.getDrawable( this, R.drawable.ic_zoom_in );

        // Apply to the chart
        this.zoomed = !this.zoomed;
        this.plotChart();

        // Change the button appearance accordingly
        if ( this.zoomed ) {
            icon = AppCompatResources.getDrawable( this, R.drawable.ic_zoom_out );
        }

        BT_ZOOM.setImageDrawable( icon );
    }

    /** Plots the chart in a drawable and shows it. */
    private void plotChart()
    {
        final LineChart CHART = buildLineChart();

        CHART.setLegendY( "Heart-rate (bpm)" );
        CHART.setLegendX( "Time (sec.)" );
        CHART.setShowLabels( false );
        CHART.setMaxHR( this.prefs.get( Prefs.Option.MAX_BPM ) );
        CHART.setMinHR( this.prefs.get( Prefs.Option.MIN_BPM ) );

        if ( this.zoomed ) {
            CHART.setNormalize( true );
        }

        this.chartView.setImageDrawable( CHART );
    }

    private @NonNull LineChart buildLineChart()
    {
        final Float[] DATA_HR_INTERP_X = this.resultAnalyzer.getDataHRInterpolatedForX();
        final Float[] DATA_HR_INTERP = this.resultAnalyzer.getDataHRInterpolated();
        final ArrayList<LineChart.Point> POINTS = new ArrayList<>( DATA_HR_INTERP_X.length );
        final double DENSITY = Util.getDensity( this );

        if ( DATA_HR_INTERP_X.length > 0 ) {
            for(int i = 0; i < DATA_HR_INTERP_X.length; ++i) {
                final double TIME = DATA_HR_INTERP_X[ i ];
                final double BPM = DATA_HR_INTERP[ i ];

                POINTS.add( new LineChart.Point( TIME, BPM ) );
            }
        }

        return new LineChart( DENSITY, POINTS, 0xffffa500 );
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        if ( item.getItemId() == android.R.id.home ) {
            this.finish();
            return true;
        }

        return super.onOptionsItemSelected( item );
    }

    private ImageView chartView;
    private ResultAnalyzer resultAnalyzer;
    private Prefs prefs;
    public static Result result;
    private boolean zoomed;

    /** Manages gestures. */
    public static class StandardGestures implements View.OnTouchListener,
            ScaleGestureDetector.OnScaleGestureListener
    {

        public StandardGestures(Context c)
        {
            this.gestureScale = new ScaleGestureDetector( c, this );
            this.position = new PointF( 0, 0);
        }

        @Override @SuppressWarnings("ClickableViewAccessibility")
        public boolean onTouch(View view, MotionEvent event)
        {
            float curX;
            float curY;

            this.view = view;
            this.gestureScale.onTouchEvent( event );

            if ( !this.gestureScale.isInProgress() ) {
                switch ( event.getAction() ) {
                    case MotionEvent.ACTION_DOWN:
                        this.position.x = event.getX();
                        this.position.y = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        curX = event.getX();
                        curY = event.getY();
                        this.view.scrollBy( (int) ( this.position.x - curX ), (int) ( this.position.y - curY ) );
                        this.position.x = curX;
                        this.position.y = curY;
                        break;
                    case MotionEvent.ACTION_UP:
                        curX = event.getX();
                        curY = event.getY();
                        this.view.scrollBy( (int) ( this.position.x - curX ), (int) ( this.position.y - curY ) );
                        break;
                }
            }

            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector)
        {
            this.scaleFactor *= detector.getScaleFactor();

            // Prevent view from becoming too small
            this.scaleFactor = ( this.scaleFactor < 1 ? 1 : this.scaleFactor );

            // Change precision to help with jitter when user just rests their fingers
            this.scaleFactor = ( (float) ( (int) ( this.scaleFactor * 100 ) ) ) / 100;
            this.view.setScaleX( this.scaleFactor );
            this.view.setScaleY( this.scaleFactor) ;

            return true;
        }

        @Override
        public boolean onScaleBegin(@NonNull  ScaleGestureDetector detector)
        {
            return true;
        }

        @Override
        public void onScaleEnd(@NonNull ScaleGestureDetector detector)
        {
        }

        private final PointF position;
        private View view;
        private final ScaleGestureDetector gestureScale;
        private float scaleFactor = 1;
    }
}
