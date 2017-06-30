# cordova-mqtt-android


1.Enable mqtt in cordeva:
	cordova.plugins.backgroundMode.enable(serverid, topic1, topic2, topic3);

2.Change topic in src\android\BackgroundMode.java line 84:

	JSONArray is serverid, topic1, topic2, topic3
	
	change this code to change topic in line 110
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



3.Change mqttserver port:
	src\android\MQTTConnection.java line 32:
		private static int _Port=1883;
	
4.Change Service Name:
	src\android\ForegroundService.java line 172:
		String serName="ThisApp.ForegroundProcess";
		line 187:
		String serName="ThisApp.ForegroundProcess";
		
5.Change DefaultNoticeText:
	src\android\ForegroundService.java  line 217:
		Notification.Builder notification = new Notification.Builder(c)
            .setContentIntent(pendingIntent)
            .setContentTitle("NewNotice") ----notice title
            .setContentText(text)	----notice content
            .setTicker("NoticeDescription")----notice description
            .setOngoing(false)
            .setSmallIcon(resId);----notice icon get icon in function getIconResId()