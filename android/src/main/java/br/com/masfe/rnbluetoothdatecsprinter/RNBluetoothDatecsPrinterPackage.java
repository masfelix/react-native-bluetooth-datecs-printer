package br.com.masfe.rnbluetoothdatecsprinter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.bridge.JavaScriptModule;

import br.com.masfe.rnbluetoothdatecsprinter.escpos.RNBluetoothEscPosPrinterModule;
import br.com.masfe.rnbluetoothdatecsprinter.tsc.RNBluetoothTscPrinterModule;

public class RNBluetoothDatecsPrinterPackage implements ReactPackage {
    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
      BluetoothService  bluetoothService = new BluetoothService(reactContext);
      return Arrays.<NativeModule>asList(
            new RNBluetoothDatecsPrinterModule(reactContext, bluetoothService),
            new RNBluetoothEscPosPrinterModule(reactContext, bluetoothService),
            new RNBluetoothTscPrinterModule(reactContext, bluetoothService)
        );
    }

    // Deprecated from RN 0.47
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Collections.emptyList();
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }
} 