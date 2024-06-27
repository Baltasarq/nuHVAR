// VARSE 2019/23 (c) Baltasar for MILEGroup MIT License <baltasarq@uvigo.es>


package com.devbaltasarq.nuhvar.view.experiment;


import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.devbaltasarq.nuhvar.R;
import com.devbaltasarq.nuhvar.core.Duration;
import com.devbaltasarq.nuhvar.core.Ofm;
import com.devbaltasarq.nuhvar.core.Result;
import com.devbaltasarq.nuhvar.core.bluetooth.BleService;
import com.devbaltasarq.nuhvar.core.bluetooth.BluetoothDeviceWrapper;
import com.devbaltasarq.nuhvar.core.bluetooth.BluetoothUtils;
import com.devbaltasarq.nuhvar.core.bluetooth.HRListenerActivity;
import com.devbaltasarq.nuhvar.core.bluetooth.ServiceConnectionWithStatus;
import com.devbaltasarq.nuhvar.view.Prefs;
import com.devbaltasarq.nuhvar.view.Util;
import com.devbaltasarq.nuhvar.view.PerformExperimentActivity;
import com.devbaltasarq.nuhvar.view.showresult.LineChart;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.ArrayList;


public class ExperimentDirector extends AppCompatActivity implements HRListenerActivity {
    public static final String LOG_TAG = ExperimentDirector.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_experiment_director );

        final ActionBar ACTION_BAR = this.getSupportActionBar();
        final FloatingActionButton FB_LAUNCH_NOW = this.findViewById( R.id.fbLaunchNow );
        final FloatingActionButton FB_STOP = this.findViewById( R.id.fbStop );
        final TextView LBL_DEVICE_NAME = this.findViewById( R.id.lblDeviceName );
        final ImageButton BT_ED_MAX_TIME = this.findViewById( R.id.btSetMaxTime );

        if ( ACTION_BAR != null ) {
            ACTION_BAR.setDisplayHomeAsUpEnabled( true );
            ACTION_BAR.setDisplayShowHomeEnabled( true );
            ACTION_BAR.setDisplayUseLogoEnabled( true );
            ACTION_BAR.setTitle( R.string.lblPerformEcg );
            ACTION_BAR.setLogo( R.drawable.ic_ecg );
        }

        // Assign values
        this.ofm = Ofm.get();
        this.prefs = Prefs.get();
        this.maxDuration = new Duration( this.prefs.get( Prefs.Option.MAX_TIME ) );
        this.activityIndex = 0;
        this.accumulatedTimeInSeconds = 0;
        this.onExperiment = false;
        this.btDevice = PerformExperimentActivity.chosenBtDevice;
        this.ivChartView = this.findViewById( R.id.ivChartViewer );

        // Prepare the UI for the start
        this.prepareUIForInitialDescription();

        // Events
        this.chrono = new Chronometer( this::onCronoUpdate );
        FB_LAUNCH_NOW.setOnClickListener( (v) -> this.launchExperiment() );
        FB_STOP.setOnClickListener( (v) -> this.onStopExperiment() );
        BT_ED_MAX_TIME.setOnClickListener( (v) -> this.onEditMaxTime() );
        LBL_DEVICE_NAME.setText( this.btDevice.getName() );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if ( item.getItemId() == android.R.id.home ) {
            this.stopExperiment();
            this.clearRecording();
            this.finish();
            return true;
        }

        return super.onOptionsItemSelected( item );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        this.setMaxTime( this.prefs.get( Prefs.Option.MAX_TIME ) );
        this.getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );

        if ( BluetoothUtils.fixBluetoothNeededPermissions( this ).length > 0 ) {
            Log.d( LOG_TAG, "Insufficient permissions" );
        }

        BluetoothUtils.openBluetoothConnections( this,
                this.getString( R.string.lblConnected ),
                this.getString( R.string.lblDisconnected ) );

        this.setAbleToLaunch( false );
    }

    @Override
    public void onPause()
    {
        super.onPause();

        this.setAbleToLaunch( false );

        if ( this.onExperiment ) {
            this.stopExperiment();
            this.clearRecording();

            this.getWindow().clearFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );

            BluetoothUtils.closeBluetoothConnections( this );
            Log.d( LOG_TAG, "Director finished, stopped chrono, closed connections." );
        }
    }

    private void setAbleToLaunch(boolean isAble)
    {
        final LinearLayout lyMaxTime = this.findViewById( R.id.lyMaxTime );
        final FloatingActionButton FB_LAUNCH_NOW = this.findViewById( R.id.fbLaunchNow );
        final TextView LBL_CONN_STATUS = this.findViewById( R.id.lblConnectionStatus );
        final TextView MSG_STATUS = this.findViewById( R.id.msgStatus );
        int visibility;

        if ( isAble ) {
            // "Connected" in "approval" color (e.g green).
            LBL_CONN_STATUS.setText( R.string.lblConnected );
            LBL_CONN_STATUS.setTextColor( Color.parseColor( "#228B22" ) );
            lyMaxTime.setVisibility( View.VISIBLE );
            MSG_STATUS.setText( R.string.msgReadyToLaunch );
        } else {
            // "Disconnected" in "denial" color (e.g red).
            LBL_CONN_STATUS.setText( R.string.lblDisconnected );
            LBL_CONN_STATUS.setTextColor( Color.parseColor( "#8B0000" ) );
            lyMaxTime.setVisibility( View.GONE );
            MSG_STATUS.setText( R.string.msgWaitingForConnection );
        }

        // Set the visibility of the launch button
        if ( isAble ) {
            visibility = View.VISIBLE;
        } else {
            visibility = View.GONE;
        }

        this.readyToLaunch = isAble;
        FB_LAUNCH_NOW.setVisibility( visibility );
    }

    @Override
    public void showStatus(String msg)
    {
        Log.d( LOG_TAG, msg );
        Util.showMessage( this, msg );
    }

    private void setMaxTime(int mins)
    {
        final TextView LBL_MAX_TIME = this.findViewById( R.id.lblMaxTimeMins );
        int secs = mins * 60;

        this.maxDuration.set( secs );
        LBL_MAX_TIME.setText( mins + "'" );
    }

    private void onEditMaxTime()
    {
        final AlertDialog.Builder DLG_MAX_TIME = new AlertDialog.Builder( this );
        final NumberPicker ED_TIME = new NumberPicker( this );

        // Configure picker
        ED_TIME.setMinValue( 0 );
        ED_TIME.setMaxValue( 1000 );
        ED_TIME.setValue( this.maxDuration.getMinutes() );

        // Configure dialog
        DLG_MAX_TIME.setTitle( R.string.msgMaxTime );
        DLG_MAX_TIME.setView( ED_TIME );
        DLG_MAX_TIME.setNegativeButton( R.string.lblCancel, null );
        DLG_MAX_TIME.setPositiveButton( R.string.lblSave, (dlg, i) ->
            this.setMaxTime( ED_TIME.getValue() ) );
        DLG_MAX_TIME.create().show();
    }

    /** Triggers when the crono changes. */
    @SuppressWarnings("unused")
    private void onCronoUpdate(Chronometer crono)
    {
        final TextView LBL_CRONO = this.findViewById( R.id.lblCrono );
        final int ELAPSED_TIME_SECONDS = this.getElapsedExperimentSeconds();
        Log.d( LOG_TAG, "Current activity index: " + this.activityIndex );
        Log.d( LOG_TAG, "Accumulated time: " + this.accumulatedTimeInSeconds );

        LBL_CRONO.setText( new Duration( ELAPSED_TIME_SECONDS ).toChronoString() );

        if ( ELAPSED_TIME_SECONDS % 2 == 0 ) {
            this.updateGraph();
        }

        // Stop if the service was disconnected
        if ( !this.serviceConnection.isConnected() ) {
            this.onExperiment = false;
        }

        // Check if max time has been reached
        int maxDurationInSecs = this.maxDuration.getTimeInSeconds();
        if ( maxDurationInSecs > 0
          && ELAPSED_TIME_SECONDS >= maxDurationInSecs )
        {
            this.onStopExperiment();
        }

        return;
    }

    private void updateGraph()
    {
        final Result.BeatEvent[] BEATS = this.resultBuilder.getEvents();
        final ArrayList<LineChart.Point> POINTS = new ArrayList<>( BEATS.length );
        final double DENSITY = Util.getDensity( this );

        // Build plot points
        for(final Result.BeatEvent BEAT: BEATS) {
            POINTS.add( new LineChart.Point(
                        (double) BEAT.getMillis() / 1000.0,
                        60000.0 / BEAT.getHeartBeatAt() ) );
        }

        // Show chart
        final LineChart CHART = new LineChart(
                                    DENSITY,
                                    POINTS,
                                    0xffffa500 );

        CHART.setShowLabels( false );
        CHART.setMaxHR( this.prefs.get( Prefs.Option.MAX_BPM ) );
        CHART.setMinHR( this.prefs.get( Prefs.Option.MIN_BPM ) );
        CHART.setLegendY( "Heart-rate (bpm)" );
        CHART.setLegendX( "Time (sec.)" );
        this.ivChartView.setImageDrawable( CHART );
    }

    /** @return the elapsed time, in millis, from the start of the experiment. */
    private long getElapsedExperimentMillis()
    {
        return this.chrono.getMillis();
    }

    /** @return the elapsed time, in seconds, from the start of the experiment. */
    private int getElapsedExperimentSeconds()
    {
        return (int) ( (double) this.getElapsedExperimentMillis() / 1000 );
    }

    /** Extracts the info received from the HR service.
      * @param intent The key-value extra collection has at least
      *                BleService.HEART_RATE_TAG for heart rate information (as int),
      *                and it can also have BleService.RR_TAG for the rr info (as int).
      */
    @Override
    public void onReceiveBpm(Intent intent)
    {
        final BluetoothDeviceWrapper.BeatInfo BEAT_INFO = new BluetoothDeviceWrapper.BeatInfo();
        final TextView LBL_INSTANT_BPM = this.findViewById( R.id.lblInstantBpm );
        final int HR = intent.getIntExtra( BleService.HEART_RATE_TAG, -1 );
        final int MEAN_RR = intent.getIntExtra( BleService.MEAN_RR_TAG, -1 );
        long time = this.getElapsedExperimentMillis();
        int[] rrs = intent.getIntArrayExtra( BleService.RR_TAG );

        LBL_INSTANT_BPM.setText( HR + this.getString( R.string.lblBpm ) );

        // Log it
        BEAT_INFO.set( BluetoothDeviceWrapper.BeatInfo.Info.TIME, time );
        BEAT_INFO.set( BluetoothDeviceWrapper.BeatInfo.Info.HR, HR );
        BEAT_INFO.set( BluetoothDeviceWrapper.BeatInfo.Info.MEAN_RR, MEAN_RR );
        BEAT_INFO.setRRs( rrs );
        Log.d( LOG_TAG, BEAT_INFO.toString() );

        if ( HR >= 0 ) {
            // Build RR's, if necessary
            if ( rrs == null ) {
                rrs = new int[] {
                        (int) ( 60.0 / ( (float) HR ) * 1000.0 )
                };

                this.setRRNotSupported();
            }

            // Start or store
            if ( !this.readyToLaunch ) {
                this.setAbleToLaunch( true );
            } else {
                for(int rr: rrs) {
                    this.addToResult( new Result.BeatEvent( time, rr ) );

                    time += rr;
                }
            }
        }

        return;
    }

    /** Shows the description and hides the frame. */
    private void prepareUIForInitialDescription()
    {
        this.prepareGlobalUI( false );
    }

    /** Shows the frame and hides the experiment's description. */
    private void prepareUIForExperiment()
    {
        this.prepareGlobalUI( true );
    }

    private void prepareGlobalUI(boolean experimentVisible)
    {
        final FloatingActionButton FB_LAUNCH = this.findViewById( R.id.fbLaunchNow );
        final FloatingActionButton FB_STOP = this.findViewById( R.id.fbStop );
        final ImageView IV_CHART = this.findViewById( R.id.ivChartViewer );
        final LinearLayout LY_INFO = this.findViewById( R.id.lyInfo );
        final LinearLayout LY_MAX_TIME = this.findViewById( R.id.lyMaxTime );
        int frameVisibility = View.VISIBLE;
        int infoVisibility = View.GONE;
        int launchVisibility = View.GONE;
        int stopVisibility = View.VISIBLE;
        int maxTimeVisibility = View.GONE;

        if ( !experimentVisible ) {
            frameVisibility = View.GONE;
            infoVisibility = View.VISIBLE;
            launchVisibility = View.VISIBLE;
            stopVisibility = View.GONE;

            if ( this.readyToLaunch ) {
                maxTimeVisibility = View.VISIBLE;
            }
        }

        IV_CHART.setVisibility( frameVisibility );
        LY_INFO.setVisibility( infoVisibility );
        LY_MAX_TIME.setVisibility( maxTimeVisibility );
        FB_STOP.setVisibility( stopVisibility );
        FB_LAUNCH.setVisibility( launchVisibility );
    }

    private void onStopExperiment()
    {
        // Stop experiment
        this.stopExperiment();

        // Ask to save, if not error
        final AlertDialog.Builder DLG = new AlertDialog.Builder( this );
        DLG.setCancelable( false );
        DLG.setTitle( R.string.msgExperimentFinished );

        if ( this.resultBuilder == null ) {
            DLG.setMessage( R.string.errRecording );
            DLG.setPositiveButton( R.string.lblBack, null );
        } else {
            final LayoutInflater INFLATER = this.getLayoutInflater();
            final View LY_INPUT = INFLATER.inflate( R.layout.content_experiment_finished, null );

            DLG.setView( LY_INPUT );

            // Ask whether to store it or not
            DLG.setNegativeButton( R.string.lblDismiss, (d, i) -> {
                this.clearRecording();
                d.dismiss();
                ExperimentDirector.this.finish();
            });
            DLG.setPositiveButton( R.string.lblSave, (d, i) -> {
                final EditText ED_RECORD = LY_INPUT.findViewById( R.id.edRecord );

                this.saveResult( ED_RECORD.getText().toString() );
                this.clearRecording();
                d.dismiss();
                ExperimentDirector.this.finish();
            });
        }

        DLG.create().show();
    }

    private void clearRecording()
    {
        if ( this.resultBuilder != null ) {
            this.resultBuilder.clear();
            this.resultBuilder = null;
        }

        return;
    }

    private void saveResult(String record)
    {
        if ( this.resultBuilder != null ) {
            final long ELAPSED_MILLIS = this.getElapsedExperimentMillis();

            try {
                if ( !record.isBlank() ) {
                    this.resultBuilder.setRecord( record.trim() );
                }

                this.ofm.store( this.resultBuilder.build( ELAPSED_MILLIS ) );
                Log.i( LOG_TAG, this.getString( R.string.msgExperimentFinished ) );
            } catch(IOException exc) {
                final String MSG = this.getString( R.string.msgUnableToSaveExperimentResult );

                Log.e( LOG_TAG, MSG );
                Util.showMessage( this, MSG );
            }
        } else {
            Log.e( LOG_TAG, "saveResult(): no recording data present" );
        }

        return;
    }

    /** Stops the experiment. */
    private synchronized void stopExperiment()
    {
        // Finish for good
        this.chrono.stop();
        this.onExperiment = false;
        this.setRequestedOrientation( this.scrOrientationOnExperiment );
    }

    /** Launches the experiment. */
    private void launchExperiment()
    {
        // Prevent screen rotation
        this.scrOrientationOnExperiment = this.getRequestedOrientation();
        this.setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_LOCKED );

        // Prepare the UI
        this.prepareUIForExperiment();

        // Create the result object
        this.onExperiment = true;
        this.activityIndex = -1;
        this.resultBuilder = new Result.Builder( "rec", System.currentTimeMillis() );

        // Start counting time
        this.chrono.reset();
        this.chrono.start();
        Log.i( LOG_TAG, "Starting..." );

        // Prepare first activity
        this.accumulatedTimeInSeconds = 0;
    }

    /** Adds a new event to the result.
      * Since the bpm information comes from one thread and the time from another,
      * this centralized consumer is synchronized.
      * @param evt the event to store.
      */
    private synchronized void addToResult(Result.BeatEvent evt)
    {
        if ( this.resultBuilder != null
          && this.onExperiment )
        {
            this.resultBuilder.add( evt );
        }

        return;
    }

    /** @return the BleService object used by this activity. */
    @Override
    public BleService getService()
    {
        return this.bleService;
    }

    @Override
    public void setService(BleService service)
    {
        this.bleService = service;
    }

    /** @return the BroadcastReceiver used by this activivty. */
    @Override
    public BroadcastReceiver getBroadcastReceiver()
    {
        return this.broadcastReceiver;
    }

    /** @return the device this activity will connect to. */
    @Override
    public BluetoothDeviceWrapper getBtDevice()
    {
        return this.btDevice;
    }

    /** @return the service connection for this activity. */
    @Override
    public ServiceConnectionWithStatus getServiceConnection()
    {
        return this.serviceConnection;
    }

    @Override
    public void setServiceConnection(ServiceConnectionWithStatus serviceConnection)
    {
        this.serviceConnection = serviceConnection;
    }

    @Override
    public void setBroadcastReceiver(BroadcastReceiver broadcastReceiver)
    {
        this.broadcastReceiver = broadcastReceiver;
    }

    @Override
    public void setHRNotSupported()
    {
        final LinearLayout LY_HR_NOT_SUPPORTED = this.findViewById( R.id.lyHRNotSupported );

        if ( LY_HR_NOT_SUPPORTED.getVisibility() != View.VISIBLE ) {
            LY_HR_NOT_SUPPORTED.setVisibility( View.VISIBLE );
        }
    }

    @Override
    public void setRRNotSupported()
    {
        final LinearLayout LY_RR_NOT_SUPPORTED = this.findViewById( R.id.lyRRNotSupported );

        if ( LY_RR_NOT_SUPPORTED.getVisibility() != View.VISIBLE ) {
            LY_RR_NOT_SUPPORTED.setVisibility( View.VISIBLE );
        }
    }

    private int scrOrientationOnExperiment;
    private int activityIndex;
    private int accumulatedTimeInSeconds;
    private boolean readyToLaunch;
    private boolean onExperiment;

    private ImageView ivChartView;
    private Chronometer chrono;
    private Result.Builder resultBuilder;
    private Ofm ofm;
    private Prefs prefs;
    private Duration maxDuration;

    private ServiceConnectionWithStatus serviceConnection;
    private BroadcastReceiver broadcastReceiver;
    private BleService bleService;
    private BluetoothDeviceWrapper btDevice;
}
