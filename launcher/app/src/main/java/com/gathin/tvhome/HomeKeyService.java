package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Sends the remote's Home button to this launcher. Three cases:
 * - Remotes that emit KEYCODE_ALL_APPS for Home (Xiaomi sticks and boxes): Android TV 9 routes that key only to the
 *   system-image launcher, so once the stock launcher is disabled the button does nothing.
 * - TVs where the stock launcher must stay enabled (TCL G08 on Google TV 14: disabling the setup wizard drops Wi-Fi,
 *   and without it disabled the wizard crash-loops Home once launcherx is off). There KEYCODE_HOME is taken here,
 *   and this launcher is also opened when the service starts, i.e. at boot.
 * - Fire OS, where Amazon's launcher is a protected package and KEYCODE_HOME never reaches accessibility. There the
 *   Amazon home screen is watched for and we bounce back through an alarm: Android holds a plain startActivity for
 *   ~5 s after a Home press, and system_server (which sends the alarm) is exempt. It fires at once only with the
 *   global alarm_manager_constants min_futurity=0. Amazon's other screens (its Settings tab, app library) stay.
 *   Bounce idea from Fireglass (github.com/bhaweshverma50/fireglass, HomeService), MIT, (c) 2026 Mihir Gupta.
 * Every other key passes through untouched. If this launcher can't be started, Home falls back to the system's.
 *
 * Enable over adb:
 *   settings put secure enabled_accessibility_services com.gathin.tvhome/.HomeKeyService
 *   settings put secure accessibility_enabled 1
 */
public class HomeKeyService extends AccessibilityService {

    private static final String AMAZON_HOME = "com.amazon.tv.launcher";
    // Only on the Settings tab of Amazon's home; a view id, so it doesn't depend on the TV's language.
    private static final String SETTINGS_TAB_MARKER = AMAZON_HOME + ":id/settings_card_view";
    private static volatile long settingsAskedAt;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable settingsWatch = this::checkSettings;
    private long lastBounce;
    private String front = "";          // package of the window in front
    private boolean settingsMode;       // the user is in Amazon's Settings tab on purpose

    // Set by the "Home Fire OS" shortcut (AmazonHomeActivity): Amazon's home stays until this home is opened again.
    private static volatile boolean amazonMode;

    /** Testing: show Amazon's own home and stop bouncing off it until the user comes back to this home. */
    static void amazonRequested() {
        amazonMode = true;
    }

    /** Our Settings pill was pressed: on Fire OS that opens the Settings tab of Amazon's home. */
    static void settingsRequested() {
        settingsAskedAt = SystemClock.uptimeMillis();
    }

    @Override
    protected void onServiceConnected() {
        goHome();
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        int key = event.getKeyCode();
        if (key != KeyEvent.KEYCODE_ALL_APPS && key != KeyEvent.KEYCODE_HOME) return false;
        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled() && !goHome()) {
            performGlobalAction(GLOBAL_ACTION_HOME);   // not routed through onKeyEvent, so no loop
        }
        return true;
    }

    private boolean goHome() {
        Intent home = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setPackage(getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(home);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || e.getPackageName() == null) return;
        String pkg = e.getPackageName().toString();
        if (pkg.equals("android") || pkg.equals("com.android.systemui")) return;   // popups don't count
        front = pkg;
        CharSequence cls = e.getClassName();
        if (pkg.equals(getPackageName())) {
            // Only the home itself ends a visit: the invisible Fire OS shortcut belongs to this package too.
            if (cls != null && cls.toString().endsWith(".MainActivity")) {
                settingsMode = false;
                amazonMode = false;
            }
            return;
        }
        // Something else opened first (Amazon's settings sub-screens included): drop a queued bounce.
        if (!pkg.equals(AMAZON_HOME)) {
            getSystemService(AlarmManager.class).cancel(bounceIntent(this));
            return;
        }
        if (cls == null || !cls.toString().contains("HomeActivity") || settingsMode || amazonMode) return;
        if (SystemClock.uptimeMillis() - settingsAskedAt < 3000) {
            // Opened from our Settings pill: stay until the Settings tab is left (Back or Home inside it).
            settingsMode = true;
            handler.removeCallbacks(settingsWatch);
            handler.postDelayed(settingsWatch, 1500);
            return;
        }
        bounce();
    }

    /** Navigating inside Amazon's home sends no window events, so poll for its Settings tab. */
    private void checkSettings() {
        if (!settingsMode) return;
        if (front.equals(AMAZON_HOME)) {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            boolean onTab = root != null && !root.findAccessibilityNodeInfosByViewId(SETTINGS_TAB_MARKER).isEmpty();
            if (root != null) root.recycle();
            if (!onTab) {
                settingsMode = false;
                bounce();
                return;
            }
        }
        handler.postDelayed(settingsWatch, 800);
    }

    private void bounce() {
        long now = SystemClock.uptimeMillis();
        if (now - lastBounce < 1000) return;
        lastBounce = now;
        getSystemService(AlarmManager.class).setExact(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime(), bounceIntent(this));
        // Amazon's launcher holds ~130 MB on a 1 GB stick; reclaim it once it's behind us (after the 5 s guard).
        ActivityManager am = getSystemService(ActivityManager.class);
        handler.postDelayed(() -> am.killBackgroundProcesses(AMAZON_HOME), 8000);
    }

    private static PendingIntent bounceIntent(Context c) {
        Intent i = new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    @Override
    public void onInterrupt() {
    }
}
