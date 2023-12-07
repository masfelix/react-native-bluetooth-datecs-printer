package br.com.masfe.rnbluetoothdatecsprinter;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Base64;
import android.util.Log;
import com.datecs.api.printer.Printer;
import com.datecs.api.printer.ProtocolAdapter;
import com.facebook.react.bridge.*;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RNBluetoothDatecsPrinterModule extends ReactContextBaseJavaModule  implements BluetoothServiceStateObserver {

  
  private static final String TAG = "RNBluetoothDatecsPrinter";
  private final ReactApplicationContext reactContext;
  public static final String EVENT_DEVICE_ALREADY_PAIRED = "EVENT_DEVICE_ALREADY_PAIRED";
  public static final String EVENT_DEVICE_FOUND = "EVENT_DEVICE_FOUND";
  public static final String EVENT_DEVICE_DISCOVER_DONE = "EVENT_DEVICE_DISCOVER_DONE";
  public static final String EVENT_CONNECTION_LOST = "EVENT_CONNECTION_LOST";
  public static final String EVENT_UNABLE_CONNECT = "EVENT_UNABLE_CONNECT";
  public static final String EVENT_CONNECTED = "EVENT_CONNECTED";
  public static final String EVENT_BLUETOOTH_NOT_SUPPORT = "EVENT_BLUETOOTH_NOT_SUPPORT";

  
  // Intent request codes
  private static final int REQUEST_CONNECT_DEVICE = 1;
  private static final int REQUEST_ENABLE_BT = 2;

  public static final int MESSAGE_STATE_CHANGE = BluetoothService.MESSAGE_STATE_CHANGE;
  public static final int MESSAGE_READ = BluetoothService.MESSAGE_READ;
  public static final int MESSAGE_WRITE = BluetoothService.MESSAGE_WRITE;
  public static final int MESSAGE_DEVICE_NAME = BluetoothService.MESSAGE_DEVICE_NAME;

  public static final int MESSAGE_CONNECTION_LOST = BluetoothService.MESSAGE_CONNECTION_LOST;
  public static final int MESSAGE_UNABLE_CONNECT = BluetoothService.MESSAGE_UNABLE_CONNECT;
  public static final String DEVICE_NAME = BluetoothService.DEVICE_NAME;
  public static final String TOAST = BluetoothService.TOAST;

  // Return Intent extra    // Return Intent extra
  public static String EXTRA_DEVICE_ADDRESS = "device_address";

  private static final Map<String, Promise> promiseMap = Collections.synchronizedMap(new HashMap<String, Promise>());
  private static final String PROMISE_ENABLE_BT = "ENABLE_BT";
  private static final String PROMISE_SCAN = "SCAN";
  private static final String PROMISE_CONNECT = "CONNECT";

  private JSONArray pairedDevice = new JSONArray();
  private JSONArray foundDevice = new JSONArray();
  // Name Connected Device
  private String mConnectedDeviceName = null;
  // Local Bluetooth Adapter
  private BluetoothAdapter mBluetoothAdapter = null;
  // Member object services
  private BluetoothService mService = null;
  private ProtocolAdapter mProtocolAdapter;
  private ProtocolAdapter.Channel mPrinterChannel;
  private Printer mPrinter; //Datecs printer
  BluetoothSocket mSocket = null;

  public RNBluetoothDatecsPrinterModule(ReactApplicationContext reactContext, BluetoothService bluetoothService){
      super(reactContext);
      this.reactContext = reactContext;
      this.mService = bluetoothService;
      this.mService.addStateObserver(this);
      // Register for broadcasts when a device is discovered
      IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
      filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
      this.reactContext.registerReceiver(discoverReceiver, filter);
      reactContext.addActivityEventListener(mActivityEventListener);
  }

  /**
    * Retrieves constants related to Bluetooth functionality.
    * 
    * @return A map containing the Bluetooth-related constants.
    */
  @Override
  public
  @Nullable
  Map<String, Object> getConstants() {
      Map<String, Object> constants = new HashMap<>();
      constants.put(EVENT_DEVICE_ALREADY_PAIRED, EVENT_DEVICE_ALREADY_PAIRED);
      constants.put(EVENT_DEVICE_DISCOVER_DONE, EVENT_DEVICE_DISCOVER_DONE);
      constants.put(EVENT_DEVICE_FOUND, EVENT_DEVICE_FOUND);
      constants.put(EVENT_CONNECTION_LOST, EVENT_CONNECTION_LOST);
      constants.put(EVENT_UNABLE_CONNECT, EVENT_UNABLE_CONNECT);
      constants.put(EVENT_CONNECTED, EVENT_CONNECTED);
      constants.put(EVENT_BLUETOOTH_NOT_SUPPORT, EVENT_BLUETOOTH_NOT_SUPPORT);
      constants.put(DEVICE_NAME, DEVICE_NAME);
      constants.put(EVENT_BLUETOOTH_NOT_SUPPORT, EVENT_BLUETOOTH_NOT_SUPPORT);
      return constants;
  }

  private BluetoothAdapter getBluetoothAdapter(){
    if(mBluetoothAdapter == null){
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    // If the adapter is null, then Bluetooth is not supported
    if (mBluetoothAdapter == null) {
        emitRNEvent(EVENT_BLUETOOTH_NOT_SUPPORT,  Arguments.createMap());
    }

    return mBluetoothAdapter;
  }

  /**
   * Initiates the printing of a template using a printer.
   *
   * @param template The template to be printed.
   * @param imageWidth    The width to be printed.
   * @param imageHeight   The height to be printed.
   * @param promise  A Promise object used to handle the result of the printing operation.
   *                Resolves the promise if the printing is successful, otherwise catches an IOException.
   */
  @ReactMethod
  public void printerTemplate(String template, int imageWidth, int imageHeight, final Promise promise) {
    try {
        initPrinter(mSocket.getInputStream(), mSocket.getOutputStream(), template, imageWidth, imageHeight);
        promise.resolve(null);
    } catch (IOException e) {
        return;
    }
  }

  /**
   * Initializes the printer to print a template using specified input and output streams.
   *
   * @param inputStream  The input stream for communication with the printer.
   * @param outputStream The output stream for communication with the printer.
   * @param template     The template to be printed.
   * @param imageWidth        The width to be printed.
   * @param imageHeight       The height to be printed.
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void initPrinter(InputStream inputStream, OutputStream outputStream, String template, int imageWidth, int imageHeight) throws IOException {

      mProtocolAdapter = new ProtocolAdapter(inputStream, outputStream);
      if (mProtocolAdapter.isProtocolEnabled()) {
          mPrinterChannel = mProtocolAdapter.getChannel(ProtocolAdapter.CHANNEL_PRINTER);
          mPrinter = new Printer(mPrinterChannel.getInputStream(), mPrinterChannel.getOutputStream());
      } else {
          mPrinter = new Printer(mProtocolAdapter.getRawInputStream(),
          mProtocolAdapter.getRawOutputStream());
      }

      try {

          mPrinter.beep();
          mPrinter.reset();

          printTemplate(template, imageWidth, imageHeight);

          mPrinter.feedPaper(150);
          mPrinter.flush();

      } catch (IOException e) {
          e.printStackTrace();
      }
  }

  /**
   * Parses and prints a template content that may contain text and images.
   *
   * @param template The template to be printed, containing text and image tags.
   * @param imageWidth    The width to be printed
   * @param imageHeight   The height to be printed
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private void printTemplate(String template, int imageWidth, int imageHeight) throws IOException {

    String imageRegex = "\\{image\\=([0-9a-zA-Z\\+/=]{20,})\\}";

    String[] templatesText = template.split(imageRegex);

    List<String> tagsImagens = getImagesTags(template, imageRegex);
    String[] imagesSource = getImagesSources(tagsImagens);

    int i=0;

    int imagesArraySize = imagesSource.length;

    for(String templateTexto: templatesText) {
            if(!templateTexto.isEmpty()) {
                mPrinter.printTaggedText(templateTexto,"ISO-8859-1");
            }
            if(i<imagesArraySize) {
                printImage(imagesSource[i], imageWidth, imageHeight);
            }
            i++;
        }
  }

  /**
   * Extracts image sources from a list of image tags.
   *
   * @param tagsImagens A list containing image tags.
   * @return An array of image sources extracted from the tags.
   */
  private String[] getImagesSources(List<String> tagsImagens) {

    List<String> images = new ArrayList<>();

    for (String tagImage : tagsImagens) {
        images.add(tagImage.substring(tagImage.indexOf("=")+1,tagImage.indexOf("}")));
    }

    String[] imagesArray = new String[images.size()];

    return images.toArray(imagesArray);
  }

  /**
   * Extracts image tags from the given template using the provided image tag regular expression.
   *
   * @param template    The template containing text and image tags.
   * @param imageRegex  The regular expression used to identify image tags within the template.
   * @return A list of image tags extracted from the template based on the provided regular expression.
   */
  private List<String> getImagesTags(String template, String imageRegex) {
    
    List<String> tagsImagens = new ArrayList<>();
    Matcher m = Pattern.compile(imageRegex).matcher(template);

    while (m.find()) {
        tagsImagens.add(m.group());
    }

    return tagsImagens;
  }

  /**
   * Prints an image represented by a Base64 encoded string.
   *
   * @param imagePath The Base64 encoded string representing the image.
   * @param imageWidth   The width of the image to be printed.
   * @param imageHeight  The height of the image to be printed.
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private void printImage(String imagePath, int imageWidth, int imageHeight) throws IOException {
    byte[] decodedString = Base64.decode(imagePath, Base64.DEFAULT);
    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

    Bitmap scaledBitmap = Bitmap.createScaledBitmap(decodedByte, imageWidth, imageHeight, true); 

    final int width = scaledBitmap.getWidth();
    final int height = scaledBitmap.getHeight();
    final int[] argb = new int[width * height];
    scaledBitmap.getPixels(argb, 0, width, 0, 0, width, height);
    scaledBitmap.recycle(); 

    mPrinter.printImage(argb, width, height, Printer.ALIGN_CENTER, true,false); 
  }

    /**
     * Initiates the process to enable Bluetooth and retrieves a list of paired Bluetooth devices.
     *
     * @param promise A Promise object used to handle the result of the enablement process.
     *                Resolves the promise with a list of paired devices if Bluetooth is already enabled,
     *                requests Bluetooth activation otherwise, and resolves with paired devices after activation.
     */
    @ReactMethod
    public void enableBluetooth(final Promise promise) {
        BluetoothAdapter adapter = this.getBluetoothAdapter();
        if(adapter == null){
            promise.reject(EVENT_BLUETOOTH_NOT_SUPPORT);
        }else if (!adapter.isEnabled()) {
            // If Bluetooth is not on, request that it be enabled.
            // setupChat() will then be called during onActivityResult
            Intent enableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            promiseMap.put(PROMISE_ENABLE_BT, promise);
            this.reactContext.startActivityForResult(enableIntent, REQUEST_ENABLE_BT, Bundle.EMPTY);
        } else {
            WritableArray pairedDevice =Arguments.createArray();
            Set<BluetoothDevice> boundDevices = adapter.getBondedDevices();
            for (BluetoothDevice d : boundDevices) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("name", d.getName());
                    obj.put("address", d.getAddress());
                    pairedDevice.pushString(obj.toString());
                } catch (Exception e) {
                    //ignore.
                }
            }Log.d(TAG,"ble Enabled");
            promise.resolve(pairedDevice);
        }
    }

    @ReactMethod
    public void disableBluetooth(final Promise promise) {
        BluetoothAdapter adapter = this.getBluetoothAdapter();
        if(adapter == null){
            promise.resolve(true);
        }else {
            if (mService != null && mService.getState() != BluetoothService.STATE_NONE) {
                mService.stop();
            }
            promise.resolve(!adapter.isEnabled() || adapter.disable());
        }
    }

    @ReactMethod
    public void isBluetoothEnabled(final Promise promise) {
        BluetoothAdapter adapter = this.getBluetoothAdapter();
        promise.resolve(adapter!=null && adapter.isEnabled());
    }

    @ReactMethod
    public void scanDevices(final Promise promise) {
        BluetoothAdapter adapter = this.getBluetoothAdapter();
        if(adapter == null){
            promise.reject(EVENT_BLUETOOTH_NOT_SUPPORT);
        }else {
            cancelDisCovery();
            int permissionChecked = ContextCompat.checkSelfPermission(reactContext, android.Manifest.permission.ACCESS_COARSE_LOCATION);
            if (permissionChecked == PackageManager.PERMISSION_DENIED) {
                // // TODO: 2018/9/21
                ActivityCompat.requestPermissions(getCurrentActivity(),
                        new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                        1);
            }


            pairedDevice = new JSONArray();
            foundDevice = new JSONArray();
            Set<BluetoothDevice> boundDevices = adapter.getBondedDevices();
            for (BluetoothDevice d : boundDevices) {
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("name", d.getName());
                    obj.put("address", d.getAddress());
                    pairedDevice.put(obj);
                } catch (Exception e) {
                    //ignore.
                }
            }

            WritableMap params = Arguments.createMap();
            params.putString("devices", pairedDevice.toString());
            emitRNEvent(EVENT_DEVICE_ALREADY_PAIRED, params);
            if (!adapter.startDiscovery()) {
                promise.reject("DISCOVER", "NOT_STARTED");
                cancelDisCovery();
            } else {
                promiseMap.put(PROMISE_SCAN, promise);
            }
        }
    }

    @ReactMethod
    public void connect(String address, final Promise promise) {
        BluetoothAdapter adapter = this.getBluetoothAdapter();
        if (adapter!=null && adapter.isEnabled()) {
            BluetoothDevice device = adapter.getRemoteDevice(address);
            promiseMap.put(PROMISE_CONNECT, promise);
            mService.connect(device);
        } else {
            promise.reject("BT NOT ENABLED");
        }

    }

    @ReactMethod
    public void unpaire(String address,final Promise promise){
        BluetoothAdapter adapter = this.getBluetoothAdapter();
        if (adapter!=null && adapter.isEnabled()) {
            BluetoothDevice device = adapter.getRemoteDevice(address);
            this.unpairDevice(device);
            promise.resolve(address);
        } else {
            promise.reject("BT NOT ENABLED");
        }

    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass()
                    .getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void cancelDisCovery() {
        try {
            BluetoothAdapter adapter = this.getBluetoothAdapter();
            if (adapter!=null && adapter.isDiscovering()) {
                adapter.cancelDiscovery();
            }
            Log.d(TAG, "Discover canceled");
        } catch (Exception e) {
            //ignore
        }
    }

    /**
     * Activity event listener handling Bluetooth-related activities and results.
     * Manages Bluetooth connectivity, device connection, and enabling Bluetooth.
     */
    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
            BluetoothAdapter adapter = getBluetoothAdapter();
            Log.d(TAG, "onActivityResult " + resultCode);
            switch (requestCode) {
                case REQUEST_CONNECT_DEVICE: {
                    // When DeviceListActivity returns with a device to connect
                    if (resultCode == Activity.RESULT_OK) {
                        // Get the device MAC address
                        String address = intent.getExtras().getString(
                                EXTRA_DEVICE_ADDRESS);
                        // Get the BLuetoothDevice object
                        if (adapter!=null && BluetoothAdapter.checkBluetoothAddress(address)) {
                            BluetoothDevice device = adapter
                                    .getRemoteDevice(address);
                            // Attempt to connect to the device
                            mService.connect(device);
                        }
                    }
                    break;
                }
                case REQUEST_ENABLE_BT: {
                    Promise promise = promiseMap.remove(PROMISE_ENABLE_BT);
                    // When the request to enable Bluetooth returns
                    if (resultCode == Activity.RESULT_OK && promise != null) {
                        // Bluetooth is now enabled, so set up a session
                        if(adapter!=null){
                            WritableArray pairedDevice =Arguments.createArray();
                            Set<BluetoothDevice> boundDevices = adapter.getBondedDevices();
                            for (BluetoothDevice d : boundDevices) {
                                try {
                                    JSONObject obj = new JSONObject();
                                    obj.put("name", d.getName());
                                    obj.put("address", d.getAddress());
                                    pairedDevice.pushString(obj.toString());
                                } catch (Exception e) {
                                    //ignore.
                                }
                            }
                            promise.resolve(pairedDevice);
                        }else{
                            promise.resolve(null);
                        }

                    } else {
                        // User did not enable Bluetooth or an error occured
                        Log.d(TAG, "BT not enabled");
                        if (promise != null) {
                            promise.reject("ERR", new Exception("BT NOT ENABLED"));
                        }
                    }
                    break;
                }
            }
        }
    };

    @Override
    public String getName() {
        return "RNBluetoothDatecsPrinter";
    }

    private boolean objectFound(JSONObject obj) {
        boolean found = false;
        if (foundDevice.length() > 0) {
            for (int i = 0; i < foundDevice.length(); i++) {
                try {
                    String objAddress = obj.optString("address", "objAddress");
                    String dsAddress = ((JSONObject) foundDevice.get(i)).optString("address", "dsAddress");
                    if (objAddress.equalsIgnoreCase(dsAddress)) {
                        found = true;
                        break;
                    }
                } catch (Exception e) {
                }
            }
        }
        return found;
    }

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver discoverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "on receive:" + action);
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    JSONObject deviceFound = new JSONObject();
                    try {
                        deviceFound.put("name", device.getName());
                        deviceFound.put("address", device.getAddress());
                    } catch (Exception e) {
                        //ignore
                    }
                    if (!objectFound(deviceFound)) {
                        foundDevice.put(deviceFound);
                        WritableMap params = Arguments.createMap();
                        params.putString("device", deviceFound.toString());
                        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                .emit(EVENT_DEVICE_FOUND, params);
                    }

                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Promise promise = promiseMap.remove(PROMISE_SCAN);
                if (promise != null) {

                    JSONObject result = null;
                    try {
                        result = new JSONObject();
                        result.put("paired", pairedDevice);
                        result.put("found", foundDevice);
                        promise.resolve(result.toString());
                    } catch (Exception e) {
                        //ignore
                    }
                    WritableMap params = Arguments.createMap();
                    params.putString("paired", pairedDevice.toString());
                    params.putString("found", foundDevice.toString());
                    emitRNEvent(EVENT_DEVICE_DISCOVER_DONE, params);
                }
            }
        }
    };

    private void emitRNEvent(String event, @Nullable WritableMap params) {
        getReactApplicationContext().getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(event, params);
    }

    @Override
    public void onBluetoothServiceStateChanged(int state, Map<String, Object> bundle) {
    
      Log.d(TAG,"on bluetoothServiceStatChange:"+state);
    
      switch (state) {
          case BluetoothService.STATE_CONNECTED:
          case MESSAGE_DEVICE_NAME: {
              //Save the connected device's name
              mConnectedDeviceName = (String) bundle.get(DEVICE_NAME);
              BluetoothSocket socket = (BluetoothSocket) bundle.get("MAX_BT");
              if(socket != null) {
                  mSocket = socket;
              }
              Promise p = promiseMap.remove(PROMISE_CONNECT);
              if (p == null) {   Log.d(TAG,"No Promise found.");
                  WritableMap params = Arguments.createMap();
                  params.putString(DEVICE_NAME, mConnectedDeviceName);
                  emitRNEvent(EVENT_CONNECTED, params);
              } else { Log.d(TAG,"Promise Resolve.");
                  p.resolve(mConnectedDeviceName);
              }
              break;
          }
          case MESSAGE_CONNECTION_LOST: {
                emitRNEvent(EVENT_CONNECTION_LOST, null);
              break;
          }
          case MESSAGE_UNABLE_CONNECT: {     
              Promise p = promiseMap.remove(PROMISE_CONNECT);
              if (p == null) {
                  emitRNEvent(EVENT_UNABLE_CONNECT, null);
              } else {
                  p.reject("Unable to connect device");
              }
              break;
          }
          default:
              break;
      }//end switch
  }
}