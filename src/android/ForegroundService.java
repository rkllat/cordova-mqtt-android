/*
    Copyright 2013-2014 appPlant UG

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */

package de.appplant.cordova.plugin.background;

import java.util.Timer;
import java.util.TimerTask;
import org.json.JSONObject;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.app.ActivityManager;
import java.util.List;
import java.util.Iterator;
import com.ibm.mqtt.MqttException;

/**
 * Puts the service in a foreground state, where the system considers it to be
 * something the user is actively aware of and thus not a candidate for killing
 * when low on memory.
 */
public class ForegroundService extends Service {

    // Fixed ID for the 'foreground' notification
    private static final int NOTIFICATION_ID = -574543954;

    // Scheduler to exec periodic tasks
    final Timer scheduler = new Timer();

    // Used to keep the app alive
    TimerTask keepAliveTask;
	
	// This is the instance of an MQTT connection.
	public static MQTTConnection mConnection;
	public static Context servContext;
    /**
     * Allow clients to call on to the service.
     */
    @Override
    public IBinder onBind (Intent intent) {
        return null;
    }

    /**
     * Put the service in a foreground state to prevent app from being killed
     * by the OS.
     */
    @Override
    public void onCreate () {
        super.onCreate();
		servContext = getApplicationContext();
        keepAwake();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sleepWell();
    }

    /**
     * Put the service in a foreground state to prevent app from being killed
     * by the OS.
     */
    public void keepAwake() {
        final Handler handler = new Handler();

        if (!this.inSilentMode()) {
            startForeground(NOTIFICATION_ID, makeNotification());
        } else {
            Log.w("BackgroundMode", "In silent mode app may be paused by OS!");
        }

        BackgroundMode.deleteUpdateSettings();

        keepAliveTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Nothing to do here
                        // Log.d("BackgroundMode", "" + new Date().getTime());
                    }
                });
            }
        };

        scheduler.schedule(keepAliveTask, 0, 1000);
    }

    /**
     * Stop background mode.
     */
    private void sleepWell() {
        stopForeground(true);
        keepAliveTask.cancel();
    }

    /**
     * Create a notification as the visible part to be able to put the service
     * in a foreground state.
     *
     * @return
     *      A local ongoing notification which pending intent is bound to the
     *      main activity.
     */
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private Notification makeNotification() {
        JSONObject settings = BackgroundMode.getSettings();
        Context context     = getApplicationContext();
        String pkgName      = context.getPackageName();
        Intent intent       = context.getPackageManager()
                .getLaunchIntentForPackage(pkgName);

        Notification.Builder notification = new Notification.Builder(context)
            .setContentTitle(settings.optString("title", ""))
            .setContentText(settings.optString("text", ""))
            .setTicker(settings.optString("ticker", ""))
            .setOngoing(true)
            .setSmallIcon(getIconResId());

        if (intent != null && settings.optBoolean("resume")) {

            PendingIntent contentIntent = PendingIntent.getActivity(
                    context, NOTIFICATION_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            notification.setContentIntent(contentIntent);
        }


        if (Build.VERSION.SDK_INT < 16) {
            // Build notification for HoneyComb to ICS
            return notification.getNotification();
        } else {
            // Notification for Jellybean and above
            return notification.build();
        }
    }
	
	
	public static Boolean getRunningServicesInfo(Context c) {
		Boolean hasService=false;
		String serName="USTCORIXuanke.ForegroundProcess";
		/*
        ActivityManager activityManager = (ActivityManager)c.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(100);

        Iterator<ActivityManager.RunningServiceInfo> l = services.iterator();
        while (l.hasNext()) {
                ActivityManager.RunningServiceInfo si = (ActivityManager.RunningServiceInfo) l.next();
				Log.i("所有服务","Service:"+si.service.toString());
                if(si.service.toString().indexOf(serName)>-1){
                  hasService=true;
                  break;
                }
        }*/
		ActivityManager activityManager = (ActivityManager)c.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
		for(ActivityManager.RunningAppProcessInfo process:processes){		
			if(process.processName.toString().indexOf(serName)>-1)
			{
				hasService=true;
				break;
			}
		}        
        return hasService;
    }
	
	public static Boolean StopRunningServicesInfo(Context c) {	
		Boolean stop=false;
		String serName="USTCORIXuanke.ForegroundProcess";
		ActivityManager activityManager = (ActivityManager)c.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
		for(ActivityManager.RunningAppProcessInfo process:processes){		
			if(process.processName.toString().indexOf(serName)>-1)
			{
				activityManager.killBackgroundProcesses(process.processName);
				System.out.println("disbale结束进程！！！！！");
				stop=true;
				break;
			}
		}  
		return stop;
	}
	
	public static void connect(String ip,int port,String _userid,String[] _Topic) {
		try {
			System.out.println("开始连接");
			mConnection = new MQTTConnection(ip,port,_userid,_Topic);
		} catch (MqttException e) {
			// Schedule a reconnect, if we failed to connect
			System.out.println("MqttException: " + (e.getMessage() != null ? e.getMessage() : "NULL"));
		}
	}
	
	public static void dicConnect(){
		System.out.println("关闭连接");
		mConnection.disconnect();
	}
	
	public void ShowNotification(Context c,int id,String text) {
              String pkgName = c.getPackageName();
               Intent intent = c.getPackageManager().getLaunchIntentForPackage(pkgName);
                Resources res = c.getResources();
               int resId = res.getIdentifier("icon", "drawable", pkgName);

               PendingIntent pendingIntent =PendingIntent.getActivity(c, id, intent, PendingIntent.FLAG_CANCEL_CURRENT);

               NotificationManager nManager = (NotificationManager)c.getSystemService(NOTIFICATION_SERVICE);

            Notification.Builder notification = new Notification.Builder(c)
            .setContentIntent(pendingIntent)
            .setContentTitle("选课通知")
            .setContentText(text)
            .setTicker("您收到一条选课通知")
            .setOngoing(false)
            .setSmallIcon(resId);
             if (Build.VERSION.SDK_INT < 16) {
				 Notification n=notification.getNotification();				 
				 n.defaults|=Notification.DEFAULT_SOUND;
				 n.flags = Notification.FLAG_AUTO_CANCEL;
               nManager.notify(id, n);
                    } else {
						 Notification n=notification.build();						 
				 n.defaults|=Notification.DEFAULT_SOUND;
				 n.flags = Notification.FLAG_AUTO_CANCEL;
               nManager.notify(id,  n);
                    }
           }

    /**
     * Retrieves the resource ID of the app icon.
     *
     * @return
     *      The resource ID of the app icon
     */
    private int getIconResId() {
        Context context = getApplicationContext();
        Resources res   = context.getResources();
        String pkgName  = context.getPackageName();

        int resId;
        resId = res.getIdentifier("icon", "drawable", pkgName);

        return resId;
    }

    /**
     * In silent mode no notification has to be added.
     *
     * @return
     *      True if silent: was set to true
     */
    private boolean inSilentMode() {
        JSONObject settings = BackgroundMode.getSettings();

        return settings.optBoolean("silent", true);
    }
	
}
