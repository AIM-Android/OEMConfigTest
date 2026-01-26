package com.advantech.oemconfigtest;

import android.content.ComponentName;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.advantech.oemconfigtest.utils.Utils;

public class DeviceAdminReceiver extends android.app.admin.DeviceAdminReceiver {
    private static final String TAG = "DeviceAdminReceiver";

    /**
     * Get the ComponentName that DevicePolicyManager expects when calling its APIs.
     *
     * <p>This is the actual ComponentName of the DeviceAdminReceiver if we are calling as the DPC,
     * and null for other cases (delegates, Role Holder etc)
     */
    public static ComponentName getComponentName(Context context) {
        if (Utils.isDeviceOwner(context) || Utils.isProfileOwner(context)) {
            return getReceiverComponentName(context);
        } else {
            return null;
        }
    }

    /**
     * @param context The context of the application.
     * @return The component name of this component in the given context.
     */
    public static ComponentName getReceiverComponentName(Context context) {
        return new ComponentName(context.getApplicationContext(), DeviceAdminReceiver.class);
    }

    private void showToast(Context context, int resId) {
        showToast(context, context.getString(resId));
    }

    private void showToast(Context context, String message) {
        Log.v(TAG, "showToast():" + message);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}
