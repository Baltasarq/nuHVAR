// nuhVAR (c) 2023/24 Baltasar MIT License <baltasarq@gmail.com>


package com.devbaltasarq.nuhvar.view;


import android.os.Bundle;
import android.view.MenuItem;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.devbaltasarq.nuhvar.R;


public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

        EdgeToEdge.enable( this );
        this.setContentView( R.layout.activity_settings );

        ViewCompat.setOnApplyWindowInsetsListener( findViewById( R.id.mainSettings), (v, insets) -> {
            Insets systemBars = insets.getInsets( WindowInsetsCompat.Type.systemBars() );
            v.setPadding( systemBars.left, systemBars.top, systemBars.right, systemBars.bottom );

            return insets;
        });

        final ActionBar ACTION_BAR = this.getSupportActionBar();
        final TextView LBL_DEFAULT_MAX_TIME = this.findViewById( R.id.lblDefaultMaxTime );
        final TextView LBL_CHART_MAX_BPM = this.findViewById( R.id.lblChartMaxBpm );
        final TextView LBL_CHART_MIN_BPM = this.findViewById( R.id.lblChartMinBpm );

        if ( ACTION_BAR != null ) {
            ACTION_BAR.setDisplayHomeAsUpEnabled( true );
            ACTION_BAR.setDisplayShowHomeEnabled( true );
            ACTION_BAR.setDisplayUseLogoEnabled( true );
            ACTION_BAR.setTitle( R.string.lblSettings );
            ACTION_BAR.setLogo( R.drawable.ic_settings );
        }

        this.prefs = Prefs.get();
        LBL_DEFAULT_MAX_TIME.setOnClickListener(
                (v) -> this.onEditValue( R.string.msgDefaultMaxTime, Prefs.Option.MAX_TIME ));
        LBL_CHART_MAX_BPM.setOnClickListener(
                (v) -> this.onEditValue( R.string.lblChartMaxHR, Prefs.Option.MAX_BPM ));
        LBL_CHART_MIN_BPM.setOnClickListener(
                (v) -> this.onEditValue( R.string.lblChartMinHR, Prefs.Option.MIN_BPM ));
    }

    @Override
    public void onResume()
    {
        super.onResume();
        this.update();
    }

    private void update()
    {
        final TextView ED_DEFAULT_MAX_TIME = this.findViewById( R.id.edDefaultMaxTime );
        final TextView ED_CHART_MAX_BPM = this.findViewById( R.id.edChartMaxBpm );
        final TextView ED_CHART_MIN_BPM = this.findViewById( R.id.edChartMinBpm );

        ED_DEFAULT_MAX_TIME.setText( "" + this.prefs.getDefaultMaxTime() );
        ED_CHART_MAX_BPM.setText( "" + this.prefs.getChartMaxBpm() );
        ED_CHART_MIN_BPM.setText( "" + this.prefs.getChartMinBpm() );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem opt)
    {
        if ( opt.getItemId() == android.R.id.home ) {
            this.finish();
        }

        return super.onOptionsItemSelected( opt );
    }

    private void onEditValue(final int TITLE_ID, final Prefs.Option OPT)
    {
        final AlertDialog.Builder DLG = new AlertDialog.Builder( this );
        final String TITLE = this.getString( TITLE_ID );
        final NumberPicker ED_VALUE = new NumberPicker( this );
        int val = this.prefs.get( OPT );

        // Configure picker
        ED_VALUE.setMinValue( 0 );
        ED_VALUE.setMaxValue( 1000 );
        ED_VALUE.setValue( val );

        // Configure dialog
        DLG.setTitle( TITLE );
        DLG.setView( ED_VALUE );
        DLG.setNegativeButton( R.string.lblCancel, null );
        DLG.setPositiveButton( R.string.lblSave, (dlg, i) -> {
            this.prefs.set( OPT, ED_VALUE.getValue() );
            this.update();
        });

        DLG.create().show();
    }

    private Prefs prefs;
}
