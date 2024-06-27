// nuhVAR (c) 2023/24 Baltasar MIT License <baltasarq@gmail.com>


package com.devbaltasarq.nuhvar.view;


import com.devbaltasarq.nuhvar.R;
import com.devbaltasarq.nuhvar.core.AppInfo;
import com.devbaltasarq.nuhvar.core.Ofm;
import com.devbaltasarq.nuhvar.core.PlainStringEncoder;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {
    public enum Opt { RECORD, RESULTS, SETTINGS };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_main );

        final ActionBar ACTION_BAR = this.getSupportActionBar();

        final ImageView BT_RECORD = this.findViewById( R.id.btRecord );
        final TextView LBL_RECORD = this.findViewById( R.id.lblRecord );
        final ImageView BT_RESULTS = this.findViewById( R.id.btResults );
        final TextView LBL_RESULTS = this.findViewById( R.id.lblResults );

        if ( ACTION_BAR != null ) {
            ACTION_BAR.setDisplayHomeAsUpEnabled( true );
            ACTION_BAR.setDisplayShowHomeEnabled( true );
            ACTION_BAR.setDisplayUseLogoEnabled( true );
            ACTION_BAR.setTitle( AppInfo.NAME );
            ACTION_BAR.setLogo( R.drawable.ic_app_icon );
        }

        BT_RECORD.setOnClickListener( (v) -> this.goTo( Opt.RECORD ) );
        LBL_RECORD.setOnClickListener( (v) -> this.goTo( Opt.RECORD ) );
        BT_RESULTS.setOnClickListener( (v) -> this.goTo( Opt.RESULTS ) );
        LBL_RESULTS.setOnClickListener( (v) -> this.goTo( Opt.RESULTS ) );
    }

    @Override
    public void onStart()
    {
        super.onStart();

        Ofm.init( this, PlainStringEncoder.get() );
        Prefs.init( this.getApplicationContext() );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        this.getMenuInflater().inflate( R.menu.menu_main, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if ( item.getItemId() == android.R.id.home ) {
            this.finish();
            return true;
        }
        else
        if ( id == R.id.action_record ) {
            this.goTo( Opt.RECORD );
            return true;
        }
        else
        if ( id == R.id.action_results ) {
            this.goTo( Opt.RESULTS );
            return true;
        }
        else
        if ( id == R.id.action_settings ) {
            this.goTo( Opt.SETTINGS );
            return true;
        }

        return super.onOptionsItemSelected( item );
    }

    private void goTo(Opt opt)
    {
        switch( opt ) {
            case RECORD -> {
                final Intent INTENT = new Intent( this, PerformExperimentActivity.class );
                this.startActivity( INTENT );
            }
            case RESULTS -> {
                final Intent INTENT = new Intent( this, ResultsActivity.class );
                this.startActivity( INTENT );
            }
            case SETTINGS -> {
                final Intent INTENT = new Intent( this, SettingsActivity.class );
                this.startActivity( INTENT );
            }
        }

        return;
    }
}
