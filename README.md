# react-native-bluetooth-datecs-printer

## Getting started

`$npm react-native-bluetooth-datecs-printer --save`

### Mostly automatic installation

`$ react-native link react-native-datecs-connector`

### Manual installation

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`

- Add `import br.com.masfe.react-native-bluetooth-datecs-printer.RNReactNativeBluetoothDatecsPrinterPackage;` to the imports at the top of the file
- Add `new RNReactNativeBluetoothDatecsPrinterPackage()` to the list returned by the `getPackages()` method

2. Append the following lines to `android/settings.gradle`:
   ```
   in-native-react-native-bluetooth-datecs-printer'
   pr-native-react-native-bluetooth-datecs-printer').projectDir = new File(rootProject.projectDir, 	'../noact-native-react-native-bluetooth-datecs-printer/android')
   ```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
   ```
     co(':react-native-react-native-bluetooth-datecs-printer')
   ```

## Usage

```javascript
import { RNBluetoothDatecsPrinter} from 'react-native-bluetooth-datecs-printer';

// TODO: What to do with the module?
RNBluetoothDatecsPrinter;
```
