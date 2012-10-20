package org.yarix.free.android.dtctrl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public class MyWidgetProvider extends AppWidgetProvider {
	
	private static final String TAG = "yarix-3G-Control";
	private static final int SDK = android.os.Build.VERSION.SDK_INT;
	private TelephonyManager telephonyManager;
	private Object ITelephonyStub;
	private Class<?> ITelephonyClass;
	private Method enable3GMethod;
	private Method disable3GMethod;
	private Object iConnectivityManager;
	private Method setMobileDataEnabled;
	private ConnectivityManager connMgr;

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {

		Log.d(TAG,	"android.os.Build.VERSION.SDK_INT =" + SDK);
		initTelephony(context);
		
		// Get all ids  
		ComponentName thisWidget = new ComponentName(context, MyWidgetProvider.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
		for (int widgetId : allWidgetIds) {
			Boolean enabled = toggel3g(context);
			RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
					R.layout.mylayout);

			if (enabled == null) {
				Log.e(TAG, "try new API");
				remoteViews.setTextViewText(R.id.widgetText, "fix?");
				Log.e(TAG, "error: enabled =null!!");
			} else if (enabled) {
				remoteViews.setTextViewText(R.id.widgetText, "is On");
				remoteViews.setImageViewResource(R.id.widgetIcon,
						R.drawable.data_on);
			} else {
				remoteViews.setTextViewText(R.id.widgetText, "is Off");
				remoteViews.setImageViewResource(R.id.widgetIcon,
						R.drawable.data_off); 
			}

			Log.d(TAG, String.valueOf("toggel returned value = " + enabled));

			// Register an onClickListener
			Intent intent = new Intent(context, MyWidgetProvider.class);

			intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.layout, pendingIntent);
			appWidgetManager.updateAppWidget(widgetId, remoteViews);
		}
	}

	private void initTelephony(Context context) {
		connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (SDK<=8)
			initTelephony_SDK8(context);
		else
			initTelephony_SDK9(context);		
	}
	
	private void initTelephony_SDK8(Context context) {
		try {
			Log.d(TAG, "initTelephony_SDK8 started");
			telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			Class<?> telephonyManagerClass = Class.forName(telephonyManager.getClass().getName());
			Method getITelephonyMethod = telephonyManagerClass.getDeclaredMethod("getITelephony");
			getITelephonyMethod.setAccessible(true);
			ITelephonyStub = getITelephonyMethod.invoke(telephonyManager);
			ITelephonyClass = Class.forName(ITelephonyStub.getClass().getName());
			
			disable3GMethod = ITelephonyClass.getDeclaredMethod("disableDataConnectivity");
			disable3GMethod.setAccessible(true);
			enable3GMethod = ITelephonyClass.getDeclaredMethod("enableDataConnectivity");
			enable3GMethod.setAccessible(true);
			Log.d(TAG, "initTelephony_SDK8 ended");
		} catch (SecurityException e) {
			Log.e(TAG, "error " + e.getMessage());
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "error " + e.getMessage());
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			Log.e(TAG, "error " + e.getMessage());
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			Log.e(TAG, "error " + e.getMessage());
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			Log.e(TAG, "error " + e.getMessage());
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			Log.e(TAG, "error " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void initTelephony_SDK9(Context context) {//, boolean enabled) {
	    try {
			Log.d(TAG, "initTelephony_SDK9 started");	    	
			final Class conmanClass = Class.forName(connMgr.getClass().getName());
			final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
			iConnectivityManagerField.setAccessible(true);
			iConnectivityManager = iConnectivityManagerField.get(connMgr);
			final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
			setMobileDataEnabled = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
			setMobileDataEnabled.setAccessible(true);
			Log.d(TAG, "initTelephony_SDK9 ended");
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	}
	
	private Boolean toggel3g(Context context) {
		try {
			switch (getCurrentdataStatus(context)) {
			case MOBILE_3G:
				Toast.makeText(context, "3G connected, disabling 3G", Toast.LENGTH_LONG).show();
				disable3G(context);
				return false;
			case WIFI:
				Toast.makeText(context, "Wifi connected, disabling 3G.", Toast.LENGTH_LONG).show();
				disable3G(context);
				return false;
			case NONE:
				Toast.makeText(context, "Turn on 3G.", Toast.LENGTH_LONG).show();
				enable3G(context);
				return true;
			default:
				disable3G(context);
				return false;
			} 

		} catch (SecurityException e) {
			Log.e(TAG, "error " + e.getMessage());
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "error " + e.getMessage());
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			Log.e(TAG, "error " + e.getMessage());
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			Log.e(TAG, "error " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	private void enable3G(Context context) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Log.d(TAG, "enable3G started");
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
				R.layout.mylayout);
			int dr = R.drawable.data_on;

		remoteViews.setImageViewResource(R.id.widgetIcon, dr); 	
		if (SDK<=8)
			enable3GMethod.invoke(ITelephonyStub);
		else
			setMobileDataEnabled.invoke(iConnectivityManager, true);
		Log.d(TAG, "enable3G ended");

	}

	private void disable3G(Context context) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Log.d(TAG, "disable3G started");
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
				R.layout.mylayout);
		int dr = R.drawable.data_off;
		remoteViews.setImageViewResource(R.id.widgetIcon, dr); 	
		Log.d(TAG, "disable3G invoking ITelephonyStub");
		if (SDK<=8)
			disable3GMethod.invoke(ITelephonyStub);
		else 
			setMobileDataEnabled.invoke(iConnectivityManager, false);
		
		Log.d(TAG, "disable3G ended");

	}

	private DataStatus getCurrentdataStatus(Context context) {

		final NetworkInfo wifi = connMgr
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		final NetworkInfo mobile = connMgr
				.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		
		if (wifi.isAvailable() && wifi.isConnected()) {
				return DataStatus.WIFI;
		} 
		else if (mobile.isAvailable() &&  mobile.isConnected()) {
				return DataStatus.MOBILE_3G;
		} else {
			return DataStatus.NONE;
		}
	}
}