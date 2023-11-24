# react-native-bluetooth-datecs-printer

## Getting started

`$npm react-native-bluetooth-datecs-printer --save`

### Mostly automatic installation

`$ react-native link react-native-datecs-connector`

### Manual installation

#### Android

1. Open up `android/app/src/main/java/[...]/MainApplication.java`

- Add `import br.com.masfe.rnbluetoothdatecsprinter.RNBluetoothDatecsPrinterPackage` to the imports at the top of the file

2. Append the following lines to `android/settings.gradle`:

   ```
   include: ':react-native-bluetooth-datecs-printer'
   project(':react-native-bluetooth-datecs-printer').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-bluetooth-datecs-printer/android')
   ```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:

   ```
     implementation project(':react-native-bluetooth-datecs-printer')
   ```

## Usage

```javascript
import { RNBluetoothDatecsPrinter} from 'react-native-bluetooth-datecs-printer';
```
