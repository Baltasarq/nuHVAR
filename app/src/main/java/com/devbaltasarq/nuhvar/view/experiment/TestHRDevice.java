// VARSE 2019/23 (c) Baltasar for MILEGroup MIT License <baltasarq@uvigo.es>


package com.devbaltasarq.nuhvar.view.experiment;


import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.devbaltasarq.nuhvar.R;
import com.devbaltasarq.nuhvar.core.Duration;
import com.devbaltasarq.nuhvar.core.bluetooth.BleService;
import com.devbaltasarq.nuhvar.core.bluetooth.BluetoothDeviceWrapper;
import com.devbaltasarq.nuhvar.core.bluetooth.BluetoothUtils;
import com.devbaltasarq.nuhvar.core.bluetooth.HRListenerActivity;
import com.devbaltasarq.nuhvar.core.bluetooth.ServiceConnectionWithStatus;
import com.devbaltasarq.nuhvar.view.Util;
import com.devbaltasarq.nuhvar.view.PerformExperimentActivity;


/** Shows an activity in which the user can see the obtained bpm data.
  * The device is taken from the static attribute PerformExperimentActivity. */
public class TestHRDevice extends AppCompatActivity implements HRListenerActivity {
    private final static String LOG_TAG = TestHRDevice.class.getSimpleName();
    private enum Status { INACTIVE, RECORDING }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView( R.layout.activity_test_hrdevice );
        this.setTitle( "" );

        final TextView LBL_DEVICE_NAME = this.findViewById( R.id.lblDeviceName );
        final ActionBar ACTION_BAR = this.getSupportActionBar();

        if ( ACTION_BAR != null ) {
            ACTION_BAR.setDisplayHomeAsUpEnabled( true );
            ACTION_BAR.setDisplayShowHomeEnabled( true );
            ACTION_BAR.setDisplayUseLogoEnabled( true );
            ACTION_BAR.setLogo( R.drawable.ic_ecg );
            ACTION_BAR.setTitle( R.string.lblTestDevice );
        }

        // Set device
        this.btDevice = PerformExperimentActivity.chosenBtDevice;
        LBL_DEVICE_NAME.setText( this.btDevice.getName() );
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        if ( item.getItemId() == android.R.id.home ) {
            this.finish();
            return true;
        }

        return super.onOptionsItemSelected( item );
    }

    public void showStatus(String msg)
    {
        Log.d( LOG_TAG, msg );
        Util.showMessage( this, msg );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        BluetoothUtils.openBluetoothConnections( this,
                                                    this.getString( R.string.lblConnected ),
                                                    this.getString( R.string.lblDisconnected ) );
        this.status = Status.INACTIVE;
        this.showInactive();

        // Bluetooth permissions
        final String[] BT_PERMISSIONS_NEEDED =
                BluetoothUtils.fixBluetoothNeededPermissions( this );

        if ( BT_PERMISSIONS_NEEDED.length > 0 ) {
            Toast.makeText( this, R.string.errNoBluetooth, Toast.LENGTH_LONG ).show();
        } else {
            this.startRecording();
        }

        Log.d( LOG_TAG, "UI started, service tried to bound." );
    }

    @Override
    public void onPause()
    {
        super.onPause();

        BluetoothUtils.closeBluetoothConnections( this );
        this.showInactive();

        if ( this.status == Status.RECORDING ) {
            this.stopRecording();
        }

        Log.d( LOG_TAG, "test UI finished, closed connections." );
    }

    @Override
    public void finish()
    {
        this.showStatus( this.getString( R.string.msgExperimentFinished ) );
        super.finish();
    }

    /** Extracts the info received from the HR service.
     * @param intent The key-value extra collection has at least
     *                BleService.HEART_RATE_TAG for heart rate information (as int),
     *                and it can also have BleService.RR_TAG for the rr info (as int).
     */
    public void onReceiveBpm(Intent intent)
    {
        final int HR = intent.getIntExtra( BleService.HEART_RATE_TAG, -1 );
        final int MEAN_RR = intent.getIntExtra( BleService.MEAN_RR_TAG, -1 );
        final BluetoothDeviceWrapper.BeatInfo BEAT_INFO = new BluetoothDeviceWrapper.BeatInfo();
        int[] rrs = intent.getIntArrayExtra( BleService.RR_TAG );

        if ( this.status == Status.INACTIVE ) {
            this.startRecording();
        }

        BEAT_INFO.set( BluetoothDeviceWrapper.BeatInfo.Info.TIME, this.chrono.getMillis() );
        BEAT_INFO.set( BluetoothDeviceWrapper.BeatInfo.Info.HR, HR );
        BEAT_INFO.set( BluetoothDeviceWrapper.BeatInfo.Info.MEAN_RR, MEAN_RR );
        BEAT_INFO.setRRs( rrs );

        String strHr = "";
        String strRr = "";

        if ( HR >= 0 ) {
            strHr = Integer.toString( HR );
        }

        if ( MEAN_RR >= 0 ) {
            strRr = Integer.toString( MEAN_RR );
        }

        if ( rrs == null ) {
            this.setRRNotSupported();
        }

        this.showBpm( strHr, strRr );
    }

    private void startRecording()
    {
        this.chrono = new Chronometer( this::onChronoUpdate );
        this.chrono.reset();
        this.chrono.start();
        this.status = Status.RECORDING;
    }

    private void stopRecording()
    {
        this.chrono.stop();
        this.status = Status.INACTIVE;
    }

    /** Shows the info in the appropriate labels. */
    private void showBpm(String bpm, String rr)
    {
        final TextView LBL_BPM = this.findViewById( R.id.lblBpm );
        final TextView LBL_RR = this.findViewById( R.id.lblRR );

        if ( bpm != null
          && !bpm.isEmpty()  )
        {
            TestHRDevice.this.runOnUiThread( () -> LBL_BPM.setText( bpm ) );

            if ( rr != null
              && !rr.isEmpty()  )
            {
                TestHRDevice.this.runOnUiThread( () -> LBL_RR.setText( rr ) );
            } else {
                this.showRRInactive();
            }
        } else {
            this.showInactive();
        }

        return;
    }

    /** Puts a -- in the label, so the user knows the BPM are not being measured. */
    private void showHRInactive()
    {
        TestHRDevice.this.runOnUiThread( () -> {
            final TextView LBL_BPM = this.findViewById( R.id.lblBpm );

            LBL_BPM.setText( "--" );
        });
    }

    /** Puts a -- in the label, so the user knows the RR are not being measured. */
    private void showRRInactive()
    {
        TestHRDevice.this.runOnUiThread( () -> {
            final TextView LBL_RR = this.findViewById( R.id.lblRR );

            LBL_RR.setText( "--" );
        });
    }

    /** Puts a -- in all labels, so the user knows both the RRR the BPM are not being measured. */
    private void showInactive()
    {
        this.showHRInactive();
        this.showRRInactive();
    }

    /** Updates the time.
      * @param wc An instance of the Chronometer.
      */
    private void onChronoUpdate(Chronometer wc)
    {
        final TextView LBL_TIME = this.findViewById( R.id.lblTime );
        final int SECONDS = (int) ( (double) wc.getMillis() / 1000 );

        LBL_TIME.setText( new Duration( SECONDS ).toChronoString() );
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
    public ServiceConnection getServiceConnection()
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

    private ServiceConnectionWithStatus serviceConnection;
    private BroadcastReceiver broadcastReceiver;
    private BleService bleService;
    private BluetoothDeviceWrapper btDevice;

    private Status status;
    private Chronometer chrono;
}
