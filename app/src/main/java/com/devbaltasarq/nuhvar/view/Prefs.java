// nuhVAR (c) 2023/24 Baltasar MIT License <baltasarq@gmail.com>


package com.devbaltasarq.nuhvar.view;


import android.content.Context;
import android.content.SharedPreferences;

import com.devbaltasarq.nuhvar.view.showresult.LineChart;


/** Almacenamiento de preferencias. */
public final class Prefs {
    private static final String CFG_FILE_NAME = "settings.cfg";
    private static final String ETQ_DEFAULT_MAX_TIME = "default_max_time";
    private static final String ETQ_DEFAULT_CHART_MIN_BPM = "chart_min_bpm";
    private static final String ETQ_DEFAULT_CHART_MAX_BPM = "chart_max_bpm";
    private static final int DEFAULT_MAX_TIME = 0;
    private static final int DEFAULT_CHART_MAX_BPM = LineChart.MAX_HR;
    private static final int DEFAULT_CHART_MIN_BPM = LineChart.MIN_HR;

    public enum Option { MAX_TIME, MIN_BPM, MAX_BPM }

    private Prefs(Context ctx)
    {
        this.sharedPrefs = ctx.getSharedPreferences(
                                CFG_FILE_NAME,
                                Context.MODE_PRIVATE );
    }

    /** @return the default max time for experiments. */
    public int getDefaultMaxTime()
    {
        return this.sharedPrefs.getInt(
                        ETQ_DEFAULT_MAX_TIME,
                        DEFAULT_MAX_TIME );
    }

    /** @return the default max bpm for charts. */
    public int getChartMaxBpm()
    {
        return this.sharedPrefs.getInt(
                        ETQ_DEFAULT_CHART_MAX_BPM,
                        DEFAULT_CHART_MAX_BPM );
    }

    /** @return the default min bpm for charts. */
    public int getChartMinBpm()
    {
        return this.sharedPrefs.getInt(
                        ETQ_DEFAULT_CHART_MIN_BPM,
                        DEFAULT_CHART_MIN_BPM );
    }

    /** Sets the default max time for experiments.
      * @param val the new time (in minutes).
      */
    public void setDefaultMaxTime(int val)
    {
        final SharedPreferences.Editor EDITOR = this.sharedPrefs.edit();

        EDITOR.putInt( ETQ_DEFAULT_MAX_TIME, val );
        EDITOR.apply();
    }

    /** Sets the default min bpm for charts.
      * @param val the new bpm.
      */
    public void setDefaultChartMinBpm(int val)
    {
        final SharedPreferences.Editor EDITOR = this.sharedPrefs.edit();

        EDITOR.putInt( ETQ_DEFAULT_CHART_MIN_BPM, val );
        EDITOR.apply();
    }

    /** Sets the default max bpm for charts.
     * @param val the new bpm.
     */
    public void setDefaultChartMaxBpm(int val)
    {
        final SharedPreferences.Editor EDITOR = this.sharedPrefs.edit();

        EDITOR.putInt( ETQ_DEFAULT_CHART_MAX_BPM, val );
        EDITOR.apply();
    }

    /** @return the associated value.
      * @param opt the option to get.
      */
    public int get(Option opt)
    {
        int toret;

        switch( opt ) {
            case MAX_TIME -> {
                toret = this.getDefaultMaxTime();
            }
            case MIN_BPM -> {
                toret = this.getChartMinBpm();
            }
            case MAX_BPM -> {
                toret = this.getChartMaxBpm();
            }
            default -> {
                throw new RuntimeException( "Prefs.get(): option not found" );
            }
        }

        return toret;
    }

    /** Change the associated preference.
      * @param opt the option to change.
      * @param val the value to change the option to.
      */
    public void set(Option opt, int val)
    {
        switch( opt ) {
            case MAX_TIME -> {
                this.setDefaultMaxTime( val );
            }
            case MIN_BPM -> {
                this.setDefaultChartMinBpm( val );
            }
            case MAX_BPM -> {
                this.setDefaultChartMaxBpm( val );
            }
            default -> {
                throw new RuntimeException( "Prefs.get(): option not found" );
            }
        }

        return;
    }

    /** Init the prefs manager. */
    public static Prefs init(Context ctx)
    {
        if ( prefs == null ) {
            prefs = new Prefs( ctx );
        }

        return prefs;
    }

    public static Prefs get()
    {
        if ( prefs == null ) {
            throw new RuntimeException( "tried to get prefs without init" );
        }

        return prefs;
    }

    private final SharedPreferences sharedPrefs;
    private static Prefs prefs;
}
