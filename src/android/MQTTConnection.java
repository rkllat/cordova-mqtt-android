package de.appplant.cordova.plugin.background;

import com.ibm.mqtt.*;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import android.util.Log;

public class MQTTConnection implements MqttSimpleCallback {
    IMqttClient mqttClient = null;

    // Let's not use the MQTT persistence.
    private static MqttPersistence MQTT_PERSISTENCE = null;
    // We don't need to remember any state between the connections, so we use a clean start.
    private static boolean MQTT_CLEAN_START = true;
    // Let's set the internal keep alive for MQTT to 15 mins. I haven't tested this value much. It could probably be increased.
    private static short MQTT_KEEP_ALIVE = 60*15;
    // MQTT client ID, which is given the broker. In this example, I also use this for the topic header.
    // You can use this to run push notifications for multiple apps with one MQTT broker.
    public static String MQTT_CLIENT_ID = "Android";
    // Set quality of services to 0 (at most once delivery), since we don't want push notifications
    // arrive more than once. However, this means that some messages might get lost (delivery is not guaranteed)
    private static int[] MQTT_QUALITIES_OF_SERVICE;
    private static int MQTT_QUALITY_OF_SERVICE = 0;
    // The broker should not retain any messages.
    private static boolean MQTT_RETAINED_PUBLISH = false;
	private static int NOTIFICATION_ID=0;
    private long mStartTime;
    private String token = "";
	
	private static String _Host="";
	private static int _Port=1883;
	private static String[] _topic;
	
    public MQTTConnection(String MQTT_HOST,int MQTT_BROKER_PORT_NUM,String tokens,String[] Topic) throws MqttException {
		_Host=MQTT_HOST;
		_Port=MQTT_BROKER_PORT_NUM;
        // Create connection spec
		token=tokens;
		_topic=Topic;
        String mqttConnSpec = "tcp://" + MQTT_HOST + "@" + MQTT_BROKER_PORT_NUM;
        // Create the client and connect
        mqttClient = MqttClient.createMqttClient(mqttConnSpec, MQTT_PERSISTENCE);
        String clientID = MQTT_CLIENT_ID + "/" + token;
        mqttClient.connect(clientID, MQTT_CLEAN_START, MQTT_KEEP_ALIVE);

        // register this client app has being able to receive messages
        mqttClient.registerSimpleHandler(this);
			
        // Subscribe to an initial topic, which is combination of client ID and device ID.
		subscribeToTopic(_topic);
        Log.i("MQTTConnection","Connected Success");

        // Save start time
        mStartTime = System.currentTimeMillis();
    }

    // Disconnect
    public void disconnect() {
        try {
            mqttClient.disconnect();
        } catch (MqttPersistenceException e) {
            Log.i("MqttException",e.getMessage());
        }
    }

    /*
          * Send a request to the message broker to be sent messages published with
          *  the specified topic name. Wildcards are allowed.
          */
    private void subscribeToTopic(String[] topicName) throws MqttException {

        if ((mqttClient == null) || (mqttClient.isConnected() == false)) {
            // quick sanity check - don't try and subscribe if we don't have
            //  a connection
            Log.i("Connection error","No connection");
        } else {
            MQTT_QUALITIES_OF_SERVICE=new int[topicName.length];
			for(int i=0;i<topicName.length;i++){
				MQTT_QUALITIES_OF_SERVICE[i]=0;
			}
            mqttClient.subscribe(topicName, MQTT_QUALITIES_OF_SERVICE);
        }
    }

    /*
          * Sends a message to the message broker, requesting that it be published
          *  to the specified topic.
          */
    private void publishToTopic(String topicName, String message) throws MqttException {
        if ((mqttClient == null) || (mqttClient.isConnected() == false)) {
            // quick sanity check - don't try and publish if we don't have
            //  a connection
            System.out.println("No connection to public to");
        } else {
            mqttClient.publish(topicName,
                    message.getBytes(),
                    MQTT_QUALITY_OF_SERVICE,
                    MQTT_RETAINED_PUBLISH);
        }
    }

    /*
          * Called if the application loses it's connection to the message broker.
          */
    public void connectionLost() throws Exception { 
		while(true){
			try{
				String mqttConnSpec = "tcp://" + _Host + "@" + _Port;
				String clientID = MQTT_CLIENT_ID + "/" + token;
				mqttClient = MqttClient.createMqttClient(mqttConnSpec, MQTT_PERSISTENCE);
				mqttClient.connect(clientID, MQTT_CLEAN_START, MQTT_KEEP_ALIVE);
				mqttClient.registerSimpleHandler(this);
				subscribeToTopic(_topic);
				Log.i("MQTTReConnection","Success");
				break;
			}
			catch(Exception ex){
				ex.printStackTrace();
			}
			Thread.sleep(30*1000);
		}
    }

    /*
          * Called when we receive a message from the message broker.
          */
    public void publishArrived(String topicName, byte[] payload, int qos, boolean retained) {
        // Show a notification
		Boolean hasThisTopic=false;
		for(int i=0;i<_topic.length;i++){
			if(_topic[i].equals(topicName)){
				hasThisTopic=true;
				break;
			}
		}
		if(hasThisTopic){
			String s = "";
			try {
				s = new String(payload, "UTF-8");
				ByteArrayOutputStream boas = null;
				BufferedOutputStream bos = null;
				boas = new ByteArrayOutputStream();
				bos = new BufferedOutputStream(boas);
				bos.write(payload, 0, payload.length);
				bos.flush();
			} catch (Exception ex) {
				ex.printStackTrace();
			}				
			NOTIFICATION_ID++;
			BackgroundMode.ShowNotice(NOTIFICATION_ID,s);
		}
    }

    public void sendKeepAlive() throws MqttException {
        // publish to a keep-alive topic
        publishToTopic(MQTT_CLIENT_ID + "/keepalive", token);
    }
}