package org.woheller69.freeDroidWarn;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.Button;

import nya.miku.wishmaster.R;

public class FreeDroidWarn {

    @SuppressWarnings("deprecation")
    public static void showWarningOnUpgrade(final Context context, final int buildVersion){
        final SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);
        int versionCode = prefManager.getInt("versionCodeWarn",0);
        if (buildVersion > versionCode){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
            alertDialogBuilder.setMessage(R.string.dialog_Warning);
            alertDialogBuilder.setNegativeButton(context.getString(R.string.dialog_more_info), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://keepandroidopen.org")));
                }
            });
            alertDialogBuilder.setPositiveButton(context.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences.Editor editor = prefManager.edit();
                    editor.putInt("versionCodeWarn", buildVersion);
                    editor.apply();
                }
            });
            alertDialogBuilder.setNeutralButton(context.getString(R.string.solution), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/woheller69/FreeDroidWarn?tab=readme-ov-file#solutions")));
                }
            });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
            if (neutralButton != null) {
                neutralButton.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
            }
        }

    }

}
