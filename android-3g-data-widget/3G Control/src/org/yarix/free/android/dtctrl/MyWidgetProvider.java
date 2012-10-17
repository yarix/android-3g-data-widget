package org.yarix.free.android.dtctrl;

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
	
	private static final String TAG = "yarix";
	private TelephonyManager telephonyManager;
	private Object ITelephonyStub;
	private Class<?> ITelephonyClass;
	private Method enable3GMethod;
	private Method disable3GMethod;

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {

		initTelephony(context);
		
		// Get all ids  
		ComponentName thisWidget = new ComponentName(context, MyWidgetProvider.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
		for (int widgetId : allWidgetIds) {
			Boolean enabled = toggel3g(context);
			RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
					R.layout.mylayout);

			if (enabled == null) {
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

			Log.i(TAG, String.valueOf("toggel returned value = " + enabled));

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
		try {
			Log.i(TAG, "initTelephony started");
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
			Log.i(TAG, "initTelephony ended");
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

	private Boolean toggel3g(Context context) {
		try {
			switch (getCurrentdataStatus(context)) {
			case MOBILE_3G:
			case WIFI:
				disable3G(context);
				return false;
			case NONE:
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
		Log.i(TAG, "enable3G started");
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
				R.layout.mylayout);
			String txt = "on 3G";
			int dr = R.drawable.data_on;

			Log.i(TAG, " with " + txt + " [" + dr + "]");

		remoteViews.setTextViewText(R.id.widgetText, txt);
		remoteViews.setImageViewResource(R.id.widgetIcon, dr); 	
		enable3GMethod.invoke(ITelephonyStub);
		Log.i(TAG, "enable3G ended");

	}

	private void disable3G(Context context) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Log.i(TAG, "disable3G started");
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
				R.layout.mylayout);
		String txt = "ddd";
		int dr = R.drawable.data_on;
			dr = R.drawable.data_off;
		Log.i(TAG, " with " + txt + " [" + dr + "]");

		remoteViews.setTextViewText(R.id.widgetText, txt);
		remoteViews.setImageViewResource(R.id.widgetIcon, dr); 	
		disable3GMethod.invoke(ITelephonyStub);
		Log.i(TAG, "disable3G ended");

	}

	private DataStatus getCurrentdataStatus(Context context) {
		final ConnectivityManager connMgr = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		final NetworkInfo wifi = connMgr
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		final NetworkInfo mobile = connMgr
				.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		
		if (wifi.isAvailable() && wifi.isConnected()) {
				Toast.makeText(context, "Wifi connected, disabling 3G.", Toast.LENGTH_LONG).show();
				return DataStatus.WIFI;
		} 
		else if (mobile.isAvailable() &&  mobile.isConnected()) {
				Toast.makeText(context, "3G connected, disabling...", Toast.LENGTH_LONG).show();
				return DataStatus.MOBILE_3G;
		} else {
			Toast.makeText(context, "Turn on 3G.", Toast.LENGTH_LONG).show();
			return DataStatus.NONE;
		}
	}
}