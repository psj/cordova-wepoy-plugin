package wepoy;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// printer
import android.device.PrinterManager;

// scanner
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.device.ScanManager;
import android.device.scanner.configuration.Triggering;

// card reader
import android.device.MagManager;

/**
 * This class echoes a string called from JavaScript.
 */
// onDecodeComplete barcodeType = 28
// BARCODE_FLAT	28 in printer
public class Wepoy extends CordovaPlugin {

    private PrinterManager printer;

    private final static String SCAN_ACTION = ScanManager.ACTION_DECODE;
    private ScanManager mScanManager;
    private String barcodeStr;
    private CallbackContext scanSuccessCallback;
    private BroadcastReceiver mScanReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            byte[] barcode = intent.getByteArrayExtra("barcode");
            int barocodelen = intent.getIntExtra("length", 0);
            barcodeStr = new String(barcode, 0, barocodelen);

            byte temp = intent.getByteExtra("barcodeType", (byte) 0);
            android.util.Log.i("debug", "----codetype--" + temp);

            try {
              JSONObject json = new JSONObject();
              json.put("barcode_type", temp);
              json.put("barcode_str", barcodeStr);

              scanSuccessCallback.sendPluginResult(new PluginResult(PluginResult.Status.OK, json));
            } catch (JSONException e) {
                //some exception handler code.
            }

        }

    };


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        printer = new PrinterManager();
        if (action.equals("print")) {
            String message = args.getString(0);
            int printerOpened = printer.open();

            this.print(message + " " + printerOpened, callbackContext);
            return true;
        }

        if (action.equals("scanBarcode")) {
            mScanManager = new ScanManager();
            mScanManager.openScanner();
            mScanManager.switchOutputMode(0);
            if(mScanManager.getTriggerMode() != Triggering.CONTINUOUS)
                mScanManager.setTriggerMode(Triggering.CONTINUOUS);

            this.scanBarcode(callbackContext);
            return true;
        }

        if (action.equals("listenToScan")) {
            mScanManager = new ScanManager();
            mScanManager.openScanner();
            mScanManager.switchOutputMode( 0);
            if(mScanManager.getTriggerMode() != Triggering.CONTINUOUS)
                mScanManager.setTriggerMode(Triggering.CONTINUOUS);

            listenToScan(callbackContext);
            return true;
        }

        if (action.equals("stopBarcodeScanner")) {
            mScanManager = new ScanManager();
            return true;
        }


        if (action.equals("scanMagneticStripe")) {
            scanMagneticStripe(callbackContext);
            return true;
        }

        return false;
    }

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    private void print(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            int ret;
            printer.setupPage(384, -1);
            // printer.drawText("hello", 5, 0, "arial", 23, false, false, 0);

            ret = printer.drawTextEx(message, 5, 0,384,-1, "arial", 24, 0, 0, 0);
            ret = printer.printPage(0);

            int status  = printer.getStatus();

            callbackContext.success(message + " printPage: " + ret + " status: " + status);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private void scanBarcode(CallbackContext callbackContext) {
        listenToScan(callbackContext);
        mScanManager.startDecode();
    }

    private void stopBarcodeScanner(CallbackContext callbackContext) {
        mScanManager.stopDecode();
        mScanManager.closeScanner();
        callbackContext.success("OK");
    }


    private void listenToScan(CallbackContext callbackContext) {
      IntentFilter filter = new IntentFilter();
      filter.addAction(SCAN_ACTION);
      webView.getContext().registerReceiver(mScanReceiver, filter);
      scanSuccessCallback = callbackContext;
    }

    private void scanMagneticStripe(CallbackContext callbackContext) {
      MagReadService mReadService = new MagReadService(webView.getContext(), callbackContext);
      mReadService.start();
    }

}
