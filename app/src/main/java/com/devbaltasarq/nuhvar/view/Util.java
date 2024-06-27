// (c) NuhVar Baltasar 2023/24 MIT License <baltasarq@gmail.com>


package com.devbaltasarq.nuhvar.view;


import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;


/** A placeholder class used to have an utility method for SnackBar's */
public final class Util {
    /** Shows a SnackBar for LENGTH_LONG in the current activity. */
    public static void showMessage(AppCompatActivity act, String msg)
    {
        final Snackbar MSG = Snackbar.make(
                                act.getWindow().getDecorView().getRootView(),
                                msg, Snackbar.LENGTH_LONG );
        MSG.show();
    }

    public static double getDensity(AppCompatActivity activity)
    {
        double toret = 0.0;

        if ( Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU ) {
            toret = activity.getWindow().getWindowManager().getCurrentWindowMetrics().getDensity();
        } else {
            toret = activity.getResources().getDisplayMetrics().scaledDensity;
        }

        return toret;
    }
}
