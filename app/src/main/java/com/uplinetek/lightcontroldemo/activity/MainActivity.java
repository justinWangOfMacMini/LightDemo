package com.uplinetek.lightcontroldemo.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.uplinetek.lightcontroldemo.R;
import com.uplinetek.lightcontroldemo.ui.MyImageButton;
import com.uplinetek.lightcontroldemo.utils.Config;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by Justin on 2017/6/3.
 */

public class MainActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    public static final String TAG = "LightConnectDemo";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;

    private int mState = UART_PROFILE_DISCONNECTED;

    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;

    private Button btnConnect;
    private MyImageButton btnSwitch;
    private SeekBar sbLight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "蓝牙不可用", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnConnect = (Button)findViewById(R.id.button_connect);

        btnSwitch = (MyImageButton)findViewById(R.id.switch_button);
        btnSwitch.setButtonStatus(Config.STATUS_NO);
        btnSwitch.setImageResource(R.drawable.btn_ejkg);
        btnSwitch.setEnabled(false);

        sbLight = (SeekBar)findViewById(R.id.sb_degree);
        sbLight.setProgressDrawable(getResources().getDrawable(R.drawable.seekbar_gray));
        sbLight.setThumb(getResources().getDrawable(R.drawable.seekbar_thumb_gray));
        sbLight.setEnabled(false);

        service_init();

        btnConnect.setOnClickListener(MainActivity.this);
        btnSwitch.setOnClickListener(MainActivity.this);

        seekBarManager();


    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_connect:{

                clickBtnConnect(v);

            }
            break;
            case R.id.switch_button:{

                clickBtnSwitch(v);

            }
            break;

        }
    }

    private void clickBtnConnect(View theBtnConnect) {

        Button btnConnect = (Button)theBtnConnect;

        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onClick - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        else {
            if (btnConnect.getText().equals("连接")){

                //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices

                Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
            } else {
                //Disconnect button pressed

                if (mDevice!=null)
                {
                    mService.disconnect();

                }




            }
        }

    }

    private void clickBtnSwitch(View theSwitchButton) {

        MyImageButton switchButton = (MyImageButton) theSwitchButton;

        if (switchButton.getButtonStatus() == Config.STATUS_ON) {

            switchButton.setButtonStatus(Config.STATUS_OFF);
            switchButton.setImageResource(R.drawable.btn_ejkg_off);
            mService.writeRXCharacteristic((Config.ej_UPaEoff + "\r\n").getBytes());

        } else if (switchButton.getButtonStatus() == Config.STATUS_OFF) {

            switchButton.setButtonStatus(Config.STATUS_ON);
            switchButton.setImageResource(R.drawable.btn_ejkg_on);
            mService.writeRXCharacteristic((Config.ej_UPaEon + "\r\n").getBytes());

        } else {

            switchButton.setButtonStatus(Config.STATUS_NO);
            switchButton.setImageResource(R.drawable.btn_ejkg);
            switchButton.setClickable(false);
            mService.writeRXCharacteristic((Config.ej_UPaEno + "\r\n").getBytes());

        }



    }

    private void seekBarManager() {

        final int minNum = 0;
        sbLight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                String lightStr;
                int light = seekBar.getProgress() + minNum;
                if (light < 10) {
                    lightStr = "0" + light;
                } else {
                    lightStr = "" + light;
                }

                mService.writeRXCharacteristic((Config.UPmT+lightStr).getBytes());
            }
        });

    }

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            //*********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");

                        btnConnect.setText("断开连接");

                        btnSwitch.setButtonStatus(Config.STATUS_OFF);
                        btnSwitch.setImageResource(R.drawable.btn_ejkg_off);
                        btnSwitch.setEnabled(true);

                        sbLight.setProgressDrawable(getResources().getDrawable(R.drawable.seekbar_red));
                        sbLight.setThumb(getResources().getDrawable(R.drawable.seekbar_thumb_red));
                        sbLight.setEnabled(true);

                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }

            //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");

                        btnConnect.setText("连接");

                        btnSwitch.setButtonStatus(Config.STATUS_NO);
                        btnSwitch.setImageResource(R.drawable.btn_ejkg);
                        btnSwitch.setEnabled(false);

                        sbLight.setProgressDrawable(getResources().getDrawable(R.drawable.seekbar_gray));
                        sbLight.setThumb(getResources().getDrawable(R.drawable.seekbar_thumb_gray));
                        sbLight.setEnabled(false);

                        mState = UART_PROFILE_DISCONNECTED;
//                        mService.close();
                        //setUiState();

                    }
                });
            }


            //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }
            //*********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            String text = new String(txValue, "UTF-8");
                            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());

                            Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
//                            listAdapter.add("["+currentDateTimeString+"] RX: "+text);
//                            messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);

                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }
            //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)){
//                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }


        }
    };

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

        }

        public void onServiceDisconnected(ComponentName classname) {
            ////     mService.disconnect(mDevice);
            mService = null;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
//                    ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");

                    if (!mService.connect(deviceAddress)) {

                        Toast.makeText(MainActivity.this, "连接失败，请重试",Toast.LENGTH_LONG).show();

                    }


                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
//            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        }
        else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.popup_title)
                    .setMessage(R.string.popup_message)
                    .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.popup_no, null)
                    .show();
        }
    }

}
