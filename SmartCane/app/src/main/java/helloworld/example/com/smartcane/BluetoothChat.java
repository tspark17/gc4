package helloworld.example.com.smartcane;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main Activity of the application
 * Enable bluetooth automatically
 * Ask permission for GPS activation
 * Call SMS send and call intents
 * Keep SharedPreference for emergency contacts and device information
 * Use SoundPool for alarming
 *
 * Last modified on 2016-05-04
 */
public class BluetoothChat extends Activity implements SensorEventListener {

    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_DIAL_INFO = 0;
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Layout Views
    private TextView mTitle;
    private ImageButton btButton;
    private Button aButton,dButton;
    private TextView ifConnected;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    // Sound
    private SoundPool sound;
    private int detect;
    private int bt_success, bt_fail, r, emer;
    // Vibrator
    private Vibrator vide;

    // SharedPreferences
    SharedPreferences mac;
    SharedPreferences.Editor d_editor;
    SharedPreferences pnum;
    SharedPreferences.Editor editor;
    SharedPreferences contact;
    SharedPreferences.Editor c_editor;

    // Contact
    private String name;
    private String number;
    public static int pnext = -1;
    private String PhoneNumber[] = new String[5];
    // Accelerometer
    private long lastTime;
    private float speed;
    private float lastX;
    private float lastY;
    private float lastZ;
    private float x, y, z;
    private static final int SHAKE_THRESHOLD = 4000;
    private static final int DATA_X = SensorManager.DATA_X;
    private static final int DATA_Y = SensorManager.DATA_Y;
    private static final int DATA_Z = SensorManager.DATA_Z;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    // GPS
    boolean isGPSEnabled, isNetworkEnabled;
    double lat, lng;
    LocationManager locationManager;
    LocationListener locationListener;
    String locationProvider;

    // BackPress event
    private final long FINISH_INTERVAL_TIME = 2000;
    private long backPressedTime = 0;


    /**
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ActivityStacks.getInstance().addActivity(this);
        if (D) Log.e(TAG, "+++ ON CREATE +++");

        // Initialize SoundPools
        sound = new SoundPool(1, AudioManager.STREAM_ALARM, 0);
        detect = sound.load(this, R.raw.object, 1);
        bt_success = sound.load(this, R.raw.success, 1);
        bt_fail = sound.load(this, R.raw.fail, 1);
        r = sound.load(this, R.raw.record, 1);
        emer = sound.load(this, R.raw.emergency, 1);
        // Initialize Vibrator
        vide = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Initialize Accelerometer sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // Initialize SharedPreferences
        sharedPreferences();
        for (int i = 0; i < 5; i++) {
            if (PhoneNumber[i].equals("")) {
                pnext = i;
                break;
            }
        }

        // GPS
        chkGpsService();
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        // Listener
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                lat = location.getLatitude();
                lng = location.getLongitude();
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };
        if (locationManager.isProviderEnabled(locationManager.NETWORK_PROVIDER) == true) {
            locationProvider = LocationManager.NETWORK_PROVIDER;
        } else
            locationProvider = LocationManager.GPS_PROVIDER;
        // Register the listener with the Location Manager to receive location
        // Updates
        locationManager.requestLocationUpdates(locationProvider, 2000, 0, locationListener);
        // Bluetooth
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            ActivityStacks.getInstance().finishAllActivity();
            return;
        }

        // Set up the window layout
        ifConnected = (TextView) findViewById(R.id.ifConnected);
        btButton = (ImageButton) findViewById(R.id.btButton);
        btButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent serverIntent = new Intent(BluetoothChat.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            }
        });
        aButton = (Button) findViewById(R.id.aButton);
        aButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setData(ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(intent, REQUEST_DIAL_INFO);

            }
        });
        dButton = (Button) findViewById(R.id.dButton);
        dButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(BluetoothChat.this, ContactActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Initialize SharePreferences
     */
    public void sharedPreferences() {
        if (mac == null)
            mac = getSharedPreferences("device", Context.MODE_MULTI_PROCESS);
        if (pnum == null)
            pnum = getSharedPreferences("pnum", Context.MODE_MULTI_PROCESS);
        for (int i = 0; i < 5; i++) {
            String idx = "p" + i;
            PhoneNumber[i] = pnum.getString(idx, "");
        }
        if (contact == null)
            contact = getSharedPreferences("contact", Context.MODE_MULTI_PROCESS);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (D) Log.e(TAG, "++ ON START ++");

        // Resume Accelerometer Sensor
        if (accelerometerSensor != null)
            sensorManager.registerListener(this, accelerometerSensor,
                    SensorManager.SENSOR_DELAY_GAME);

        // If Bluetooth is not on, enable it
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
        // Connect to the device previously connected if exists
        if(!(mac.getString("device", "").equals(""))) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mac.getString("device", ""));
            mChatService.connect(device);
            Toast.makeText(BluetoothChat.this, mac.getString("device", ""), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if (D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (D) Log.e(TAG, "-- ON STOP --");

        // Stop Accelerometer sensor
        if (sensorManager != null)
            sensorManager.unregisterListener(this);
        // Remove locationListener
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if (D) Log.e(TAG, "--- ON DESTROY ---");
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if (D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            ifConnected.setText("Connected to ");
                            ifConnected.append((mConnectedDeviceName));
                            sound.play(bt_success, 1, 1, 0, 0, 1);
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            ifConnected.setText("Connecting...");
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            ifConnected.setText("Connect a device");
                            //여기
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    while(true) {
                                        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mac.getString("device", ""));
                                        mChatService.connect(device);
                                        Toast.makeText(BluetoothChat.this, mac.getString("device", ""), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    // Event occur
                    if (readMessage.equals("1")) {
                    // Alarm
                        long pattern[] = {0, 300, 50, 300, 100};
                        vide.vibrate(pattern, -1);
                        sound.play(detect, 1, 1, 0, 0, 1);
                        Toast.makeText(getApplicationContext(), "Object detected", Toast.LENGTH_SHORT).show();
                    }
                    else if(readMessage.equals("2")) {
                        emergencyHandler();
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    if (msg.getData().getString(TOAST).equals("Unable to connect device"))
                        sound.play(bt_fail, 1, 1, 0, 0, 1);
                    break;
            }
        }
    };

    /**
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_DIAL_INFO:
                if (resultCode == RESULT_OK) {
                    Cursor cursor = getContentResolver().query(data.getData(),
                            new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                    ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null);
                    cursor.moveToFirst();
                    name = cursor.getString(0);        // Get name
                    number = cursor.getString(1);   // Get number
                    // Save in SharedPreferences
                    c_editor = contact.edit();
                    editor = pnum.edit();
                    String idx = "p" + pnext;
                    c_editor.putString(idx, name);
                    editor.putString(idx, "tel:" + number);
                    c_editor.commit();
                    editor.commit();
                    PhoneNumber[pnext++] = pnum.getString(idx, "");
                    cursor.close();
                    sound.play(r, 1, 1, 0, 0, 1);
                }
                break;
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Save in SharedPreference
                    d_editor = mac.edit();
                    d_editor.putString("device", address);
                    d_editor.commit();
                    // Get the BluetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mac.getString("device", ""));
                    // Attempt to connect to the device
                    mChatService.connect(device);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occured
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    ActivityStacks.getInstance().finishAllActivity();
                }
            default:
                break;
        }
    }

    /**
     *
     * @param sensor
     * @param accuracy
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void emergencyHandler() {
        vide.vibrate(1000);
        for(int i = 0; i < 5; i++) {
            //sound.play(emer, 1, 1, 0, 0, 1);
            Toast.makeText(getApplicationContext(), "Danger!", Toast.LENGTH_SHORT).show();
            String send = pnum.getString("p" + i, "");
            if(send.equals(""))
                break;
            String realSend = send.substring(4, 7) + send.substring(8, 12) + send.substring(13);
            sendSMS(realSend, "현재 위험해요! 도와주세요. http://maps.google.com/?q=" + lat + "," + lng);
            // Call
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse(send));
            startActivity(intent);
        }
    }
    /**
     *
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            long gabOfTime = (currentTime - lastTime);
            if (gabOfTime > 100) {
                lastTime = currentTime;
                x = event.values[SensorManager.DATA_X];
                y = event.values[SensorManager.DATA_Y];
                z = event.values[SensorManager.DATA_Z];
                speed = Math.abs(x + y + z - lastX - lastY - lastZ) / gabOfTime * 10000;

                if (speed > SHAKE_THRESHOLD) {
                    // Event occu
                    emergencyHandler();
                }
                lastX = event.values[DATA_X];
                lastY = event.values[DATA_Y];
                lastZ = event.values[DATA_Z];
                speed = 0;
            }
        }
    }

    /**
     *
     * @param smsNumber
     * @param smsText
     */
    public void sendSMS(String smsNumber, String smsText) {
        PendingIntent sentIntent = PendingIntent.getBroadcast(this, 0, new Intent("SMS_SENT_ACTION"), 0);
        PendingIntent deliveredIntent = PendingIntent.getBroadcast(this, 0, new Intent("SMS_DELIVERED_ACTION"), 0);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "전송 완료", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(getBaseContext(), "전송 실패", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "서비스 지역이 아닙니다", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "무선(Radio)가 꺼져있습니다", Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "PDU Null", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter("SMS_SENT_ACTION"));

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        // 도착 완료
                        Toast.makeText(getBaseContext(), "SMS 도착 완료", Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        // 도착 안됨
                        Toast.makeText(getBaseContext(), "SMS 도착 실패", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter("SMS_DELIVERED_ACTION"));

        SmsManager mSmsManager = SmsManager.getDefault();
        mSmsManager.sendTextMessage(smsNumber, null, smsText, sentIntent, deliveredIntent);
    }

    /**
     *
     * @return
     */
    private boolean chkGpsService() {
        String gps = android.provider.Settings.Secure.getString(getContentResolver(),
                android.provider.Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if (!(gps.matches(".*gps.*") && gps.matches(".*network.*"))) {
            // If GPS is off, alert dialog for permission
            AlertDialog.Builder gsDialog = new AlertDialog.Builder(this);
            gsDialog.setTitle("위치 서비스 설정");
            gsDialog.setMessage("무선 네트워크 사용, GPS 위성 사용을 모두 체크하셔야 정확한 위치 서비스가 가능합니다.\n위치 서비스 기능을 설정하시겠습니까?");
            gsDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    startActivity(intent);
                }
            }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    return;
                }
            }).create().show();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onBackPressed() {
        long tempTIme = System.currentTimeMillis();
        long intervalTime = tempTIme - backPressedTime;
        // Finish all activities if BackPress event occur two times in interval time
        if (0 <= intervalTime && FINISH_INTERVAL_TIME >= intervalTime) {
            ActivityStacks.getInstance().finishAllActivity();
            super.onBackPressed();
        }
        else {
            backPressedTime = tempTIme;
            Toast.makeText(BluetoothChat.this, "Press 'Back' button again to exit.", Toast.LENGTH_SHORT).show();
        }
    }
}
