// VARSE 2019/23 (c) Baltasar for MILEGroup MIT License <baltasarq@uvigo.es>


package com.devbaltasarq.nuhvar.view;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.companion.AssociationRequest;
import android.companion.BluetoothLeDeviceFilter;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.devbaltasarq.nuhvar.view.experiment.ExperimentDirector;
import com.devbaltasarq.nuhvar.view.experiment.TestHRDevice;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.devbaltasarq.nuhvar.R;
import com.devbaltasarq.nuhvar.core.bluetooth.BluetoothDeviceWrapper;
import com.devbaltasarq.nuhvar.core.bluetooth.BluetoothUtils;
import com.devbaltasarq.nuhvar.core.bluetooth.DemoBluetoothDevice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;


public class PerformExperimentActivity extends AppCompatActivity {
    private static final String LOG_TAG = PerformExperimentActivity.class.getSimpleName();
    private static final int RCQ_SELECT_DEVICE = 442;

    // Adapter for holding hrDevices found through deviceSearch.
    private static class BtDeviceListAdapter extends ArrayAdapter<BluetoothDeviceWrapper> {
        BtDeviceListAdapter(@NonNull Context cntxt, @NonNull List<BluetoothDeviceWrapper> entries)
        {
            super( cntxt, 0, entries );
        }

        @Override
        public @NonNull View getView(int i, View view, @NonNull ViewGroup viewGroup)
        {
            final LayoutInflater INFLATER = LayoutInflater.from( this.getContext() );

            if ( view == null ) {
                view = INFLATER.inflate( R.layout.listview_device_entry, null );
            }

            final BluetoothDeviceWrapper DEVICE = this.getItem( i );
            final TextView LBL_DEVICE_NAME = view.findViewById( R.id.lblDeviceName );
            final TextView LBL_DEVICE_ADDR = view.findViewById( R.id.lblDeviceAddress );
            String deviceName = this.getContext().getString( R.string.errUnknownDevice );
            String deviceAddress = "00:00:00:00:00:00";

            // Set device's name, if possible.
            if ( DEVICE != null ) {
                deviceName = getBTDeviceName( this.getContext(), DEVICE );
                deviceAddress = DEVICE.getAddress();
            }

            LBL_DEVICE_NAME.setText( deviceName );
            LBL_DEVICE_ADDR.setText( deviceAddress );
            return view;
        }
    }

    /** @return the context of this activity. */
    public Context getContext()
    {
        return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_perform_experiment );

        final ActionBar ACTION_BAR = this.getSupportActionBar();

        if ( ACTION_BAR != null ) {
            ACTION_BAR.setDisplayHomeAsUpEnabled( true );
            ACTION_BAR.setDisplayShowHomeEnabled( true );
            ACTION_BAR.setDisplayUseLogoEnabled( true );
            ACTION_BAR.setTitle( R.string.lblRecord );
            ACTION_BAR.setLogo( R.drawable.ic_ecg );
        }

        // Widgets
        final ImageButton BT_START_SCAN = this.findViewById( R.id.btStartScan );
        final ImageButton BT_STOP_SCAN = this.findViewById( R.id.btStopScan );
        final ImageButton BT_TEST_HR_DEVICE = this.findViewById( R.id.btTestHRDevice );
        final ListView LV_DEVICES = this.findViewById( R.id.lvDevices );
        final FloatingActionButton FB_LAUNCH_EXPR = this.findViewById( R.id.fbPerformExperiment );

        FB_LAUNCH_EXPR.setOnClickListener( (v) -> this.launchExperimentDirectorActivity() );

        BT_START_SCAN.setOnClickListener( (view) ->
            PerformExperimentActivity.this.startScanning()
        );

        BT_STOP_SCAN.setOnClickListener( (view) ->
            PerformExperimentActivity.this.stopScanning( true )
        );

        BT_TEST_HR_DEVICE.setOnClickListener( (view) ->
            PerformExperimentActivity.this.launchDeviceTester()
        );

        LV_DEVICES.setOnItemClickListener( (adptView, view, pos, l) -> {
            final PerformExperimentActivity CONTEXT = PerformExperimentActivity.this;
            final BluetoothDeviceWrapper BT_DEVICE = CONTEXT.devicesListAdapter.getItem( pos );

            if ( BT_DEVICE != null ) {
                CONTEXT.setChosenDevice( BT_DEVICE );
            } else {
                CONTEXT.showStatus( CONTEXT.getString( R.string.errUnknownDevice) );
            }
        });

        // Initalize UI
        this.configBtLaunched = false;
        this.btDefinitelyNotAvailable = false;
        this.deviceSearch = false;
        this.createList();
    }

    @Override
    public void onStart()
    {
        super.onStart();

        // Prepare bluetooth
        if ( !this.btDefinitelyNotAvailable ) {
            this.initBluetooth();

            // Ensures Bluetooth is enabled on the device.
            // If Bluetooth is not currently enabled,
            // fire an intent to display a dialog asking the user
            // to grant permission to enable it.
            if ( this.bluetoothAdapter == null ) {
                this.launchBtConfigPage();
            }
            else
            if ( !this.bluetoothAdapter.isEnabled() ) {
                this.launchBtConfigPage();
            } else {
                // Scans health devices automatically
                // if there is none yet.
                // Remember that the Demo Device is always there.
                if ( this.hrDevices.size() < 2 ) {
                    this.startScanning();
                }
            }
        }

        return;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        this.stopScanning( false );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        if ( menu != null ) {
            this.scanMenu = menu;

            if ( this.bluetoothAdapter != null ) {
                PerformExperimentActivity.this.runOnUiThread( () -> {
                    if ( menu.size() == 0 ) {
                        this.getMenuInflater().inflate( R.menu.menu_scan_devices, menu );
                    }

                    if ( this.isLookingForDevices() ) {
                        menu.findItem( R.id.menu_start_scan ).setVisible( false );
                        menu.findItem( R.id.menu_stop_scan ).setVisible( true );
                        menu.findItem( R.id.menu_refresh_scan ).setVisible( true );
                        menu.findItem( R.id.menu_refresh_scan ).setActionView(
                                R.layout.actionbar_indeterminate_progress );
                    } else {
                        menu.findItem( R.id.menu_start_scan ).setVisible( true );
                        menu.findItem( R.id.menu_stop_scan ).setVisible( false );
                        menu.findItem( R.id.menu_refresh_scan ).setVisible( false );
                    }
                });
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if ( item.getItemId() == android.R.id.home ) {
            this.finish();
            return true;
        }
        else
        if ( item.getItemId() == R.id.menu_start_scan ) {
            this.startScanning();
            return true;
        }
        else
        if ( item.getItemId() == R.id.menu_stop_scan ) {
            this.stopScanning( true );
            return true;
        }

        return super.onOptionsItemSelected( item );
    }

    @Override
    protected void onActivityResult(int req, int resultCode, @Nullable Intent data)
    {
        if ( req == RCQ_SELECT_DEVICE ) {
            if ( resultCode == RESULT_OK
              && data != null )
            {
                final ScanResult SCAN_RESULT =
                        data.getParcelableExtra( CompanionDeviceManager.EXTRA_DEVICE );
                if ( SCAN_RESULT != null) {
                    this.onDeviceFound( SCAN_RESULT.getDevice() );
                } else {
                    Log.d( LOG_TAG, "device: found but was null" );
                }
            } else {
                this.stopScanning( true );
            }
        } else {
            super.onActivityResult( req, resultCode, data );
        }

        return;
    }

    /** Creates the list of hrDevices. */
    private void createList()
    {
        final ListView LV_DEVICES = this.findViewById( R.id.lvDevices );

        // Create lists, if needed
        if ( this.hrDevices == null ) {
            this.hrDevices = new ArrayList<>( 8 );
        }

        if ( this.addrFound == null ) {
            this.addrFound = new HashSet<>( 16 );
        }

        // Set chosen device
        if ( demoDevice == null ) {
            demoDevice = new BluetoothDeviceWrapper( DemoBluetoothDevice.get() );
        }

        if ( chosenBtDevice == null ) {
            chosenBtDevice = demoDevice;
        }

        this.setChosenDevice( chosenBtDevice );

        // Clear devices found list
        this.devicesListAdapter = new BtDeviceListAdapter( this, this.hrDevices );
        LV_DEVICES.setAdapter( this.devicesListAdapter );
        this.clearDeviceListView();
    }

    /** Removes all hrDevices in the list. */
    private void clearDeviceListView()
    {
        this.addrFound.clear();
        this.devicesListAdapter.clear();
        this.devicesListAdapter.add( demoDevice );

        if ( chosenBtDevice != null
          && !chosenBtDevice.isDemo() )
        {
            this.addDeviceToListView( chosenBtDevice.getDevice() );
        }

        return;
    }

    /** Adds a given device to the list.
      * @param btDevice the bluetooth LE device.
      */
    public void onDeviceFound(BluetoothDevice btDevice)
    {
        if ( btDevice != null ) {
            final String ADDR = btDevice.getAddress();

            if ( ADDR != null
              && !this.addrFound.contains( ADDR ) )
            {
                this.addrFound.add( ADDR );
                this.addDeviceToListView( btDevice );
            }

            String name = getBTDeviceName( this, btDevice );
            this.deviceSearch = false;

            this.showStatus( name
                             + " " + this.getString( R.string.lblDeviceFound ).toLowerCase()
                             + "..." );
        } else {
            Log.e( LOG_TAG, "trying to add a null device (!!)" );
        }

        this.stopScanning( true );
    }

    /** Selects the device the user wants to employ.
      * @param newChosenDevice The device the user wants.
      * @see BluetoothDeviceWrapper
      */
    private void setChosenDevice(BluetoothDeviceWrapper newChosenDevice)
    {
        final TextView LBL_CHOSEN_DEVICE = this.findViewById( R.id.lblChosenDevice );

        assert newChosenDevice != null: "FATAL: newChosenDevice is null!!!";

        chosenBtDevice = newChosenDevice;
        LBL_CHOSEN_DEVICE.setText( getBTDeviceName( this, newChosenDevice ) );
    }

    /** Initializes Bluetooth. */
    private void initBluetooth()
    {
        // Getting the Bluetooth adapter
        this.bluetoothAdapter = BluetoothUtils.getBluetoothAdapter( this );

        if ( this.bluetoothAdapter == null ) {
            this.disableFurtherScan();
        }

        return;
    }

    /** Shows an info status on screen. */
    private void showStatus(String msg)
    {
        Util.showMessage( this, msg );
    }

    /** Hides the stop deviceSearch button and option menu, shows the opposite options. */
    private void disableScanUI()
    {
        this.activateScanUI( false );
    }

    /** Hides the start deviceSearch button and option menu, shows the opposite options. */
    private void enableScanUI()
    {
        this.activateScanUI( true );
    }

    /** Hides ot shows the stop deviceSearch button/start deviceSearch button.
     * @param activate true to show start scan options and hide the stop scan options,
     *                  false to do the opposite. */
    private void activateScanUI(boolean activate)
    {
        final ImageButton BT_START_SCAN = this.findViewById( R.id.btStartScan );
        final ImageButton BT_STOP_SCAN = this.findViewById( R.id.btStopScan );

        //PerformExperimentActivity.this.runOnUiThread( () -> {
            if ( this.bluetoothAdapter != null ) {
                if ( !activate ) {
                    BT_START_SCAN.setVisibility( View.VISIBLE );
                    BT_STOP_SCAN.setVisibility( View.GONE );
                } else {
                    BT_START_SCAN.setVisibility( View.GONE );
                    BT_STOP_SCAN.setVisibility( View.VISIBLE );
                }
            } else {
                BT_START_SCAN.setVisibility( View.GONE );
                BT_STOP_SCAN.setVisibility( View.GONE );
            }

            this.onCreateOptionsMenu( this.scanMenu );
        //});

        return;
    }

    /** Launches the bluetooth configuration page */
    private void launchBtConfigPage()
    {
        if ( !this.configBtLaunched
          && !this.btDefinitelyNotAvailable )
        {
            this.configBtLaunched = true;

            PerformExperimentActivity.this.runOnUiThread( () -> {
                this.showStatus( this.getString( R.string.lblActivateBluetooth ) );

                this.LAUNCH_ENABLE_BT.launch(
                        new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE ) );
            });
        }

        return;
    }

    /** Launches a tester activity to check the device. */
    private void launchDeviceTester()
    {
        this.showStatus( "Test "
                            + this.getString(  R.string.lblDevice )
                            + ": " + BluetoothUtils.getBTDeviceName( chosenBtDevice ) );

        LAUNCH_DEVICE_TESTER.launch(
                new Intent( this, TestHRDevice.class )
        );
    }

    /** @return whether the device is looking for (scanning and filtering), hrDevices or not. */
    public boolean isLookingForDevices()
    {
        return this.deviceSearch;
    }

    /** Asks for permissions before start scanning. */
    public void startScanning()
    {
        if ( !this.btDefinitelyNotAvailable ) {
            final String[] BT_PERMISSIONS_NEEDED =
                    BluetoothUtils.fixBluetoothNeededPermissions( PerformExperimentActivity.this );

            // Launch scanning or ask for permissions
            if ( BT_PERMISSIONS_NEEDED.length > 0 ) {
                this.reportUserAboutPermissions( BT_PERMISSIONS_NEEDED );
            } else {
                doStartScanning();
            }
        }

        return;
    }

    /** Reports the user about the need to give permissions. */
    private void reportUserAboutPermissions(final String[] BT_PERMISSIONS_NEEDED)
    {
        final AlertDialog.Builder DLG = new AlertDialog.Builder( this );

        DLG.setTitle( R.string.lblPermissionsNeeded );
        DLG.setMessage( R.string.msgReportPermissionsNeeded );

        DLG.setPositiveButton( "Ok", (dialogInterface, i) -> {
            this.LAUNCH_PERMISSIONS_REQ.launch( BT_PERMISSIONS_NEEDED );
        });

        DLG.setNegativeButton( R.string.lblCancel, (dialogInterface, i) ->
            PerformExperimentActivity.this.setBluetoothUnavailable()
        );

        DLG.create().show();
    }

    private void associateDcmAndroid33(final CompanionDeviceManager DEV_MANAGER,
                                              final AssociationRequest PAIRING_REQ)
    {
        if ( Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ) {
            Log.e( LOG_TAG, "associating DCM for Android version < 33" );
            return;
        }

        final Executor EXE = Runnable::run;

        DEV_MANAGER.associate( PAIRING_REQ, EXE, new CompanionDeviceManager.Callback() {
            // Called when a device is found. Launch the IntentSender so the user can
            // select the device they want to pair with.
            @Override
            public void onAssociationPending(@NonNull IntentSender chooserLauncher)
            {
                final PerformExperimentActivity SELF = PerformExperimentActivity.this;

                try {
                    SELF.startIntentSenderForResult( chooserLauncher, RCQ_SELECT_DEVICE,
                                            null, 0, 0, 0 );
                } catch (IntentSender.SendIntentException e) {
                    Log.e( LOG_TAG, "onAssociationPending: failed to send intent" );
                }
            }
            @Override
            public void onFailure(CharSequence errorMessage)
            {
                Log.e( LOG_TAG, "[FAIL] " + errorMessage );
            }
        });
    }

    private void associateDcmAndroid32(final CompanionDeviceManager DEV_MANAGER,
                                       final AssociationRequest PAIRING_REQ)
    {
        if ( Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2 ) {
            Log.e( LOG_TAG, "invoked associate DCM for Android version > 32" );
            return;
        }

        DEV_MANAGER.associate( PAIRING_REQ, new CompanionDeviceManager.Callback() {
            @Override
            public void onDeviceFound(@NonNull IntentSender chooserLauncher)
            {
                final PerformExperimentActivity SELF = PerformExperimentActivity.this;

                try {
                    SELF.startIntentSenderForResult( chooserLauncher, RCQ_SELECT_DEVICE,
                                            null, 0, 0, 0 );
                } catch (IntentSender.SendIntentException e) {
                    Log.e( LOG_TAG, "onAssociationPending: failed to send intent" );
                }
            }
            @Override
            public void onFailure(CharSequence errorMessage)
            {
                Log.e( LOG_TAG, "[FAIL] " + errorMessage );
            }
        }, null );

        return;
    }

    /** Launches deviceSearch for a given period of time */
    public void doStartScanning()
    {
        if ( !this.isLookingForDevices() ) {
            final BluetoothLeDeviceFilter DEVICE_FILTER =
                    new BluetoothLeDeviceFilter.Builder()
                        .setScanFilter( new ScanFilter.Builder()
                        /*    .setServiceUuid(
                                    new ParcelUuid( BluetoothUtils.UUID_HR_MEASUREMENT_SRV ) )
                        */
                            .build() ).build();

            final AssociationRequest PAIRING_REQ = new AssociationRequest.Builder()
                    .addDeviceFilter( DEVICE_FILTER )
                    .setSingleDevice( false )
                    .build();

            this.deviceSearch = true;

            final CompanionDeviceManager DEV_MANAGER =
                    (CompanionDeviceManager) getSystemService(
                                        Context.COMPANION_DEVICE_SERVICE );

            PerformExperimentActivity.this.runOnUiThread( () -> {
                this.enableScanUI();

                if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ) {
                    associateDcmAndroid33( DEV_MANAGER, PAIRING_REQ );
                } else {
                    associateDcmAndroid32( DEV_MANAGER, PAIRING_REQ );
                }

                this.showStatus( this.getString( R.string.lblStartScan ) );
            });
        }

        return;
    }

    public void addDeviceToListView(BluetoothDevice btDevice)
    {
        final BluetoothDeviceWrapper BTW_DEVICE = new BluetoothDeviceWrapper( btDevice );

        PerformExperimentActivity.this.runOnUiThread( () -> {
            if ( !this.hrDevices.contains( BTW_DEVICE ) ) {
                this.devicesListAdapter.add( BTW_DEVICE );
                this.showStatus( this.getString( R.string.lblDeviceFound )
                                    + ": " + getBTDeviceName( this, BTW_DEVICE ) );
            }
        });
    }

    public void stopScanning(final boolean WARN)
    {
        this.deviceSearch = false;

        this.runOnUiThread( () -> {
            if ( WARN ) {
                this.showStatus( this.getString(R.string.lblStopScan ) );
            }

            this.disableScanUI();
        });
    }

    private void setBluetoothUnavailable()
    {
        this.btDefinitelyNotAvailable = true;
        this.disableFurtherScan();
    }

    /** Enables the launch button or not. */
    private void disableFurtherScan()
    {
        final ImageButton BT_START_SCAN = this.findViewById( R.id.btStartScan );

        if ( BT_START_SCAN.getVisibility() != View.INVISIBLE ) {
            this.disableScanUI();

            PerformExperimentActivity.this.runOnUiThread( () -> {
                BT_START_SCAN.setEnabled( false );
                BT_START_SCAN.setVisibility( View.INVISIBLE );

                this.showStatus( this.getString( R.string.errNoBluetooth ) );
            });
        }

        return;
    }

    private void launchExperimentDirectorActivity()
    {
        final Intent CFG_LAUNCH_EXPR = new Intent( this, ExperimentDirector.class );

        this.startActivity( CFG_LAUNCH_EXPR );
    }

    public static String getBTDeviceName(Context ctx, BluetoothDeviceWrapper btwDevice)
    {
        String toret = BluetoothUtils.getBTDeviceName( btwDevice );

        if ( toret.equals( BluetoothUtils.STR_UNKNOWN_DEVICE ) ) {
            toret = ctx.getString( R.string.errUnknownDevice );
        }

        return toret;
    }

    public static String getBTDeviceName(Context ctx, BluetoothDevice btDevice)
    {
        String toret = BluetoothUtils.getBTDeviceName( btDevice );

        if ( toret.equals( BluetoothUtils.STR_UNKNOWN_DEVICE ) ) {
            toret = ctx.getString( R.string.errUnknownDevice );
        }

        return toret;
    }

    private Set<String> addrFound;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDeviceWrapper> hrDevices;
    private BtDeviceListAdapter devicesListAdapter;
    private Menu scanMenu;
    private boolean configBtLaunched;
    private boolean deviceSearch;
    private boolean btDefinitelyNotAvailable;

    private final ActivityResultLauncher<String[]> LAUNCH_PERMISSIONS_REQ =
            registerForActivityResult( new ActivityResultContracts.RequestMultiplePermissions(), mapGrants -> {
                int totalGrants = 0;

                for(Map.Entry<String, Boolean> entry: mapGrants.entrySet()) {
                    if ( entry.getValue() ) {
                        ++totalGrants;
                    } else {
                        Log.i( LOG_TAG, "permission not granted: " + entry.getKey() );
                    }
                }

                if ( totalGrants == mapGrants.size() ) {
                    this.doStartScanning();
                } else {
                    final AlertDialog.Builder DLG = new AlertDialog.Builder( this );

                    DLG.setMessage( R.string.errNoBluetoothPermissions );
                    DLG.setPositiveButton( R.string.lblBack, null );

                    DLG.create().show();
                    this.setBluetoothUnavailable();
                }
            });


    private final ActivityResultLauncher<Intent> LAUNCH_ENABLE_BT =
            this.registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        this.configBtLaunched = false;
                        this.initBluetooth();

                        if ( this.bluetoothAdapter == null ) {
                            this.setBluetoothUnavailable();
                        }
                    });

    private final ActivityResultLauncher<Intent> LAUNCH_DEVICE_TESTER =
            this.registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        this.configBtLaunched = false;
                        this.initBluetooth();

                        if ( this.bluetoothAdapter == null ) {
                            this.setBluetoothUnavailable();
                        }
                    });

    public static BluetoothDeviceWrapper chosenBtDevice;
    public static BluetoothDeviceWrapper demoDevice;
}
