
package br.com.masfe.rnbluetoothdatecsprinter;

import java.util.Collections;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import java.util.List;
import java.util.Arrays;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.ReactPackage;

public class RNBluetoothDatecsPrinterPackage implements ReactPackage 
{
    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
    
      BluetoothService  bluetoothService = new BluetoothService(reactContext);
      return Arrays.<NativeModule>asList(new RNBluetoothDatecsPrinter(reactContext, bluetoothService));
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }
} 