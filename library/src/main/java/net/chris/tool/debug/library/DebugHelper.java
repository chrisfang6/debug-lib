package net.chris.tool.debug.library;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.util.Log;

import com.squareup.leakcanary.AndroidExcludedRefs;
import com.squareup.leakcanary.DisplayLeakService;
import com.squareup.leakcanary.ExcludedRefs;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import java.util.ArrayList;
import java.util.List;

public class DebugHelper {

    private static final String TAG = DebugHelper.class.getSimpleName();

    private RefWatcher refWatcher;

    // [full name of Class, name of Field]
    private final List<Pair<String, String>> excludedInstanceFields = new ArrayList<>();

    // [full name of Class, name of Field]
    private final List<Pair<String, String>> excludedStaticFields = new ArrayList<>();

    // full name of Class
    private final List<String> excludedClasses = new ArrayList<>();

    private ActivityLifecycleCallbacks activityLifecycleCallbacks = new ActivityLifecycleCallbacks() {
        @Override
        public void onActivityDestroyed(Activity activity) {
            // for excluded Classes.
            for (String excludedClass : excludedClasses) {
                try {
                    if (Class.forName(excludedClass).isInstance(activity)) {
                        return;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "invalid class name", e);
                }
            }
            refWatcher.watch(activity);
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }
    };


    public void enableStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyDialog()
                .penaltyLog()
                .penaltyFlashScreen()
                .penaltyDeath()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyDeath()
                .build());
    }

    public RefWatcher installLeakCanary(@NonNull final Application application) {
        if (LeakCanary.isInAnalyzerProcess(application)) {
            refWatcher = RefWatcher.DISABLED;
        } else {
            LeakCanary.enableDisplayLeakActivity(application);
            refWatcher = LeakCanary.refWatcher(application)
                    .listenerServiceClass(DisplayLeakService.class)
                    .excludedRefs(getExcludedRefs().build())
                    .buildAndInstall();
            application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
        }
        return refWatcher;
    }

    public void uninstallLeakCanary(@NonNull final Application application) {
        application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);
        refWatcher = null;
    }

    public void addExcludedInstanceField(@NonNull final String classFullName, @NonNull final String fieldName) {
        if (classFullName == null || fieldName == null) {
            Log.e(TAG, String.format("invalid instance class name %s OR field name %s", classFullName, fieldName));
            return;
        }
        excludedInstanceFields.add(Pair.create(classFullName, fieldName));
    }

    public void addExcludedStaticField(@NonNull final String classFullName, @NonNull final String fieldName) {
        if (classFullName == null || fieldName == null) {
            Log.e(TAG, String.format("invalid static class name %s OR field name %s", classFullName, fieldName));
            return;
        }
        excludedStaticFields.add(Pair.create(classFullName, fieldName));
    }

    private ExcludedRefs.Builder getExcludedRefs() {
        ExcludedRefs.Builder builder = AndroidExcludedRefs.createAppDefaults();
        for (Pair<String, String> excluded : excludedInstanceFields) {
            builder = builder.instanceField(excluded.first, excluded.second);
        }
        for (Pair<String, String> excluded : excludedStaticFields) {
            builder = builder.instanceField(excluded.first, excluded.second);
        }
        return builder;
    }

}
