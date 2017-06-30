
package de.appplant.cordova.plugin.background;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ActivityManager;  
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import com.ibm.mqtt.MqttException;
import android.app.Service;

import java.util.List;
import java.util.Iterator;
import java.io.IOException;
import android.util.Log;

public class BackgroundMode extends CordovaPlugin {

    // Event types for callbacks
    private enum Event {
        ACTIVATE, DEACTIVATE, FAILURE
    }
	private static int MQTT_BROKER_PORT_NUM = 1883;
	private static String MQTT_HOST = "";
	//Service is running?
	private static Boolean ISSERVICEBOOT=false;	
    // Plugin namespace
    private static final String JS_NAMESPACE = "cordova.plugins.backgroundMode";

    // Flag indicates if the app is in background or foreground
    private boolean inBackground = true;

    // Flag indicates if the plugin is enabled or disabled
    private boolean isDisabled = true;

    // Flag indicates if the service is bind
    private boolean isBind = false;

    // Default settings for the notification
    private static JSONObject defaultSettings = new JSONObject();

    // Tmp config settings for the notification
    private static JSONObject updateSettings;
	
	private static Context _con;
	
	private static String[] _Topic;
	private static String _userID;
    // Used to (un)bind the service to with the activity
    private final ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Nothing to do here
        }
    };

    /**
     * Executes the request.
     *
     * @param action   The action to execute.
     * @param args     The exec() arguments.
     * @param callback The callback context used when
     *                 calling back into JavaScript.
     *
     * @return
     *      Returning false results in a "MethodNotFound" error.
     *
     * @throws JSONException
     */
    @Override
    public boolean execute (String action, JSONArray args,
                            CallbackContext callback) throws JSONException {
        if (action.equalsIgnoreCase("configure")) {
            JSONObject settings = args.getJSONObject(0);
            boolean update = args.getBoolean(1);

            if (update) {
                setUpdateSettings(settings);
                updateNotifcation();
            } else {
                setDefaultSettings(settings);
            }

            return true;
        }

        if (action.equalsIgnoreCase("enable")) {
			String ip = args.getString(0);
			String orgId = args.getString(1);
			String CourseStr=args.getString(2);
			_userID=args.getString(3);
			if(CourseStr!="A"){
				String[] courseStrArray=CourseStr.split(",");
				int length=courseStrArray.length*2+3;
				_Topic=new String[length];
				for(int i=0;i<courseStrArray.length;i++){
					_Topic[i]=orgId+"-"+courseStrArray[i];			
				}
				for(int i=courseStrArray.length;i<courseStrArray.length*2;i++){
					_Topic[i]=courseStrArray[i-courseStrArray.length];			
				}
				_Topic[length-3]=_userID;
				_Topic[length-2]=orgId;
				_Topic[length-1]="topicAll";
			}
			else{
				_Topic=new String[2];
				_Topic[0]=_userID;
				_Topic[1]=orgId;
				_Topic[2]="topicAll";
			}
			MQTT_HOST=ip;
			Activity context = cordova.getActivity();
			_con=context.getApplicationContext();
			enableMode();
            return true;
        }

        if (action.equalsIgnoreCase("disable")) {
            disableMode();
            return true;
        }

        return false;
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     *
     * @param multitasking
     *      Flag indicating if multitasking is turned on for app
     */
    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        inBackground = true;
        startService();
    }

    /**
     * Called when the activity will start interacting with the user.
     *
     * @param multitasking
     *      Flag indicating if multitasking is turned on for app
     */
    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        inBackground = false;
        stopService();
    }

    /**
     * Called when the activity will be destroyed.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService();
    }

    /**
     * Enable the background mode.
     */
    private void enableMode() {
        isDisabled = false;
        if (inBackground) {
            startService();
        }
    }	

    /**
     * Disable the background mode.
     */
    private void disableMode() { 
		ForegroundService.dicConnect();
        stopService();
        isDisabled = true;
    }

    /**
     * Update the default settings for the notification.
     *
     * @param settings
     *      The new default settings
     */
    private void setDefaultSettings(JSONObject settings) {
        defaultSettings = settings;
    }

    /**
     * Update the config settings for the notification.
     *
     * @param settings
     *      The tmp config settings
     */
    private void setUpdateSettings(JSONObject settings) {
        updateSettings = settings;
    }

    /**
     * The settings for the new/updated notification.
     *
     * @return
     *      updateSettings if set or default settings
     */
    protected static JSONObject getSettings() {
        if (updateSettings != null)
            return updateSettings;

        return defaultSettings;
    }

    /**
     * Called by ForegroundService to delete the update settings.
     */
    protected static void deleteUpdateSettings() {
        updateSettings = null;
    }
	
	protected static void ShowNotice(int id,String text) {	
		ForegroundService fs=new ForegroundService();
		fs.ShowNotification(_con,id,text);
	}
    /**
     * Update the notification.
     */
    private void updateNotifcation() {
        if (isBind) {
            stopService();
            startService();
        }
    }
	

    /**
     * Bind the activity to a background service and put them into foreground
     * state.
     */
    private void startService() {
		Activity context = cordova.getActivity();
			Intent intent = new Intent(
					context, ForegroundService.class);
	
			if (isDisabled || isBind)
				return;
	
			try {
				context.bindService(
						intent, connection, Context.BIND_AUTO_CREATE);
	
				fireEvent(Event.ACTIVATE, null);
	
				context.startService(intent);
			} catch (Exception e) {
				fireEvent(Event.FAILURE, e.getMessage());
			}
			ForegroundService.connect(MQTT_HOST,MQTT_BROKER_PORT_NUM,_userID,_Topic);
			isBind = true;
    }


	
    /**
     * Bind the activity to a background service and put them into foreground
     * state.
     */
    private void stopService() {
        Activity context = cordova.getActivity();
        Intent intent = new Intent(
                context, ForegroundService.class);
        if (!isBind)
            return;

        fireEvent(Event.DEACTIVATE, null);
        context.unbindService(connection);
        context.stopService(intent);
        isBind = false;
    }

    /**
     * Fire vent with some parameters inside the web view.
     *
     * @param event
     *      The name of the event
     * @param params
     *      Optional arguments for the event
     */
    private void fireEvent (Event event, String params) {
        String eventName;

        if (updateSettings != null && event != Event.FAILURE)
            return;

        switch (event) {
            case ACTIVATE:
                eventName = "activate"; break;
            case DEACTIVATE:
                eventName = "deactivate"; break;
            default:
                eventName = "failure";
        }

        String active = event == Event.ACTIVATE ? "true" : "false";

        String flag = String.format("%s._isActive=%s;",
                JS_NAMESPACE, active);

        String fn = String.format("setTimeout('%s.on%s(%s)',0);",
                JS_NAMESPACE, eventName, params);

        final String js = flag + fn;

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl("javascript:" + js);
            }
        });
    }

}