package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/**
 * "Home Fire OS" shortcut, for testing: opens Amazon's original home and pauses HomeKeyService's bounce until this
 * home is opened again (its tile in Amazon's app row) or the stick restarts. Invisible; finishes at once.
 */
public class AmazonHomeActivity extends Activity {

    static final String AMAZON_HOME = "com.amazon.tv.launcher";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        HomeKeyService.amazonRequested();
        try {
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .setPackage(AMAZON_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
