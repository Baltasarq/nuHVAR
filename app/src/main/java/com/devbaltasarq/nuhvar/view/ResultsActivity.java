// VARSE 2019/23 (c) Baltasar for MILEGroup MIT License <baltasarq@uvigo.es>


package com.devbaltasarq.nuhvar.view;


import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.devbaltasarq.nuhvar.R;
import com.devbaltasarq.nuhvar.core.Id;
import com.devbaltasarq.nuhvar.core.Ofm;
import com.devbaltasarq.nuhvar.core.Result;
import com.devbaltasarq.nuhvar.view.adapters.ListViewResultArrayAdapter;
import com.devbaltasarq.nuhvar.view.showresult.ResultViewerActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ResultsActivity extends AppCompatActivity {
    private final static String LOG_TAG = ResultsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_results );

        final ImageButton BT_IMPORT = this.findViewById( R.id.btImport );
        final ActionBar ACTION_BAR = this.getSupportActionBar();

        if ( ACTION_BAR != null ) {
            ACTION_BAR.setDisplayHomeAsUpEnabled( true );
            ACTION_BAR.setDisplayShowHomeEnabled( true );
            ACTION_BAR.setDisplayUseLogoEnabled( true );
            ACTION_BAR.setLogo( R.drawable.ic_graph );
            ACTION_BAR.setTitle( R.string.lblResults );
        }

        // Init
        this.dataStore = Ofm.get();
        this.results = new ArrayList<>( 10 );

        // Event handlers
        BT_IMPORT.setOnClickListener(
                v -> this.LAUNCH_FILE_PICKER.launch(
                                    new String[]{ "text/plain" } ) );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        this.results.clear();
        this.loadResults();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if ( item.getItemId() == android.R.id.home ) {
            this.finish();
            return true;
        }

        return false;
    }

    public void showGraph(Result result)
    {
        try {
            ResultViewerActivity.result = this.dataStore.retrieveResult( result.getId() );

            final Intent GRPH_VIEWER_INTENT = new Intent( this, ResultViewerActivity.class );
            this.startActivity( GRPH_VIEWER_INTENT );
        } catch(IOException exc) {
            final String ERR_MSG = this.getString( R.string.errIO );

            Log.e( LOG_TAG, "loading result: " + ERR_MSG + ": " + exc.getMessage() );
            Util.showMessage( this, ERR_MSG );
        }
    }

    public void shareResult(final Result PARTIAL_RESULT)
    {
        try {
            final Result RESULT = this.dataStore.retrieveResult( PARTIAL_RESULT.getId() );
            final File RR_TEMP_FILE = this.dataStore.createTempFile(
                                            RESULT.getRec()
                                                + "-"
                                                + RESULT.getId().toString(),
                                            "-rrs.txt" );

            // Write rr's to the tmp dir
            final Writer BEATS_STREAM = Ofm.openWriterFor( RR_TEMP_FILE );
            RESULT.exportToStdTextFormat( BEATS_STREAM );
            Ofm.close( BEATS_STREAM );

            // Prepare
            final Intent SHARING_INTENT = new Intent( Intent.ACTION_SEND );
            final Uri FILE_URI = FileProvider.getUriForFile(
                    ResultsActivity.this,
                    "com.devbaltasarq.nuhvar.fileprovider",
                    RR_TEMP_FILE );

            SHARING_INTENT.setType( "text/plain" );
            SHARING_INTENT.putExtra( Intent.EXTRA_STREAM, FILE_URI );

            // Grant permissions
            SHARING_INTENT.addFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION );

            final List<ResolveInfo> RESOLVED_INFO_ACTIVITIES =
                    this.getPackageManager().queryIntentActivities(
                            SHARING_INTENT,
                            PackageManager.MATCH_DEFAULT_ONLY );

            for (final ResolveInfo RI : RESOLVED_INFO_ACTIVITIES) {
                this.grantUriPermission(
                        RI.activityInfo.packageName,
                        FILE_URI,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION );
            }

            // Share the file
            this.startActivity( Intent.createChooser(
                                    SHARING_INTENT,
                                    this.getString( R.string.lblShare ) ) );
        } catch(IOException exc) {
            Log.e( LOG_TAG,
                    this.getString( R.string.errExport )
                            + ": " + exc.getMessage() );
        }

        return;
    }

    public void deleteResult(Result res)
    {
        AlertDialog.Builder dlg = new AlertDialog.Builder( this );

        dlg.setTitle( this.getString( R.string.lblDelete )
                        + " " + this.getString( R.string.lblResult ).toLowerCase() );
        dlg.setMessage( this.getString( R.string.msgAreYouSure ) );
        dlg.setPositiveButton( R.string.lblDelete, (dlgIntf, i) -> {
            try {
                this.dataStore.removeResult( res.getId() );
                this.results.clear();
                this.loadResults();
            } catch(IOException exc) {
                Log.e( LOG_TAG, this.getString( R.string.errDeleting ) + ": " + exc.getMessage() );
                Util.showMessage( this, this.getString( R.string.errDeleting ) );
            }
        });
        dlg.setNegativeButton( R.string.lblNo, null );
        dlg.create().show();
    }

    /** Reads a given file.
      * @param uri the file to read from.
      * @return a String with the contents of the file.
      * @throws IOException if reading goes wrong.
      */
    private String readTextFromUri(Uri uri) throws IOException
    {
        final StringBuilder TORET = new StringBuilder();
        final ContentResolver SOLVER = this.getContentResolver();
        final String UTF8_BOM = "\uFEFF";

        try (final BufferedReader INPUT =
                     new BufferedReader(
                            new InputStreamReader(
                                     SOLVER.openInputStream( uri ) )))
        {
            String line;

            while ( ( line = INPUT.readLine() ) != null ) {
                if ( line.startsWith( UTF8_BOM )) {
                    line = line.substring( UTF8_BOM.length() );
                }

                TORET.append( line );
                TORET.append( '\n' );
            }

            Ofm.close( INPUT );
        }

        return TORET.toString();
    }

    private void onImport(final String RAW_DATA)
    {
        final List<Integer> DATA = Stream.of( RAW_DATA.split( "\n" ) )
                                    .map( s -> Integer.valueOf( s.trim() ) )
                                    .collect( Collectors.toList() );

        final Calendar DATE_TIME = Calendar.getInstance();
        final String STR_ISO_TIME = String.format(
                Locale.getDefault(),
                "-%04d-%02d-%02d",
                DATE_TIME.get( Calendar.YEAR ),
                DATE_TIME.get( Calendar.MONTH + 1 ),
                DATE_TIME.get( Calendar.DAY_OF_MONTH ));
        final Ofm OFM = Ofm.get();
        int totalMilliSeconds = DATA.stream().mapToInt( Integer::intValue ).sum();

        // Create a suitable result
        final Result.Builder RES_BUILDER =
                new Result.Builder(
                        "import" + STR_ISO_TIME,
                        DATE_TIME.getTimeInMillis() );

        int millisOffset = 0;
        for(int rr: DATA) {
            RES_BUILDER.add( new Result.BeatEvent( millisOffset, rr ) );
            millisOffset += rr;
        }

        final Result RES = RES_BUILDER.build( totalMilliSeconds );

        try {
            OFM.store( RES );
            this.onResume();
        } catch(IOException exc) {
            Log.e( LOG_TAG, "" + exc.getMessage() );
            Util.showMessage( this, this.getString( R.string.errIO ) );
        }
    }

    /** Loads the results for the given experiment. */
    private void loadResults()
    {
        if ( this.results.isEmpty() ) {
            try {
                final TextView LBL_NO_ENTRIES = this.findViewById( R.id.lblNoEntries );
                final ListView LV_RESULTS = this.findViewById( R.id.lvResultItems );
                final List<File> RESULT_FILES = this.dataStore.getAllResults();

                // Sort by creation time, reversed (more recent before)
                RESULT_FILES.sort( Comparator.comparingLong( File::lastModified ) );

                final int NUM_RESULT_ENTRIES = RESULT_FILES.size();
                final Result[] RESULT_ENTRIES = new Result[ NUM_RESULT_ENTRIES ];

                for(int i = 0; i < NUM_RESULT_ENTRIES; ++i) {
                    final File RES_FILE = RESULT_FILES.get( i );

                    try {
                        final Map.Entry<Id, String> PAIR_ID_REC =
                                            Ofm.readIdAndRecFromResultFile( RES_FILE );
                        final Id ID = PAIR_ID_REC.getKey();
                        final String REC = PAIR_ID_REC.getValue();

                        RESULT_ENTRIES[ i ] =
                                new Result(
                                        ID,
                                        RESULT_FILES.get( i ).lastModified(),
                                        0,
                                        REC,
                                        new Result.BeatEvent[ 0 ] );
                    } catch(IOException e) {
                        Util.showMessage( this, "Error scanning Results" );
                    }
                }

                // Prepare the list view
                this.results.clear();
                this.results.addAll( Arrays.asList( RESULT_ENTRIES ) );
                LV_RESULTS.setAdapter( new ListViewResultArrayAdapter( this, RESULT_ENTRIES ) );

                // Show the experiments list (or maybe not).
                if ( RESULT_ENTRIES.length > 0 ) {
                    LBL_NO_ENTRIES.setVisibility( View.GONE );
                    LV_RESULTS.setVisibility( View.VISIBLE );
                } else {
                    LBL_NO_ENTRIES.setVisibility( View.VISIBLE );
                    LV_RESULTS.setVisibility( View.GONE );
                }
            } catch(IOException exc)
            {
                final String ERROR_IO = this.getString( R.string.errIO );

                Log.e( LOG_TAG, ERROR_IO );
                Util.showMessage( this, ERROR_IO );
            }
        }

        return;
    }

    private final ActivityResultLauncher<String[]> LAUNCH_FILE_PICKER =
            this.registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(), uri -> {
                        final ResultsActivity SELF = ResultsActivity.this;

                        if ( uri != null ) {
                            Log.d(
                                    LOG_TAG,
                                    this.getString( R.string.lblImport )
                                            + "..." );

                            try {
                                SELF.onImport( SELF.readTextFromUri( uri ) );
                            } catch(IOException exc) {
                                final String MSG_ERR_IO = this.getString( R.string.lblImport )
                                        + ": "
                                        + this.getString( R.string.errIO );

                                Log.e( LOG_TAG, MSG_ERR_IO);
                                Util.showMessage( this, MSG_ERR_IO );
                            }
                        }
                    });

    private Ofm dataStore;
    private List<Result> results = null;
}
