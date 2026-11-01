package com.generated.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class WifiReceiver extends BroadcastReceiver {
    @Override
    public abstract void onReceive(Context context, Intent intent);
}
