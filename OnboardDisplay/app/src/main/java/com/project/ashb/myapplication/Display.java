package com.project.ashb.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Display extends AppCompatActivity {
    private static final String TAG = "Display";

    // connection attributes
    final int DEVICE_CONNECTED = 1;
    final int DEVICE_DISCONNECTED = 0;

    // attributes to update the GUI
    ImageView   iv_indicator_left;
    ImageView   iv_indicator_right;
    ImageView   iv_indicator_left_grey;
    ImageView   iv_indicator_right_grey;
    ImageView   iv_needle_speed;
    ImageView   iv_gauge_speed;
    ImageView   iv_gauge_battery;
    ImageView   iv_needle_battery;

    ImageView iv_battery_temp;
    ImageView iv_parking_brake;
    ImageView iv_lights;
    TextView txt_range;
    TextView txt_distance;
    ImageView iv_charging;

    ImageView   iv_master_warning;
    ImageView   iv_seatbelt;
    ImageView   iv_lights_fault;
    ImageView   iv_low_wiper_fluid;
    ImageView   iv_low_tire_pressure;
    ImageView   iv_airbags;
    ImageView   iv_brake_system;
    ImageView   iv_abs;
    ImageView   iv_motor;

    TextView    tv_connected;
    TextView    tv_speed;
    TextView    tv_battery;
    Button      btn_retry;

    // bluetooth attributes
    BluetoothGatt   gatt;
    BluetoothGatt   gatt_warnings;
    BluetoothDevice device;
    boolean services_discovered = false;
    boolean services_discovered_warnings = false;

    // data structures for bluetooth
    List<BluetoothGattCharacteristic> device_characteristics = new ArrayList<>();
    List<BluetoothGattCharacteristic> device_characteristics_warnings = new ArrayList<>();

    // stores the characteristic values
    DashboardService dashboard = new DashboardService();

    // animations
    Animation       animation_right = new AlphaAnimation(1, 0);
    Animation       animation_left = new AlphaAnimation(1, 0);
    RotateAnimation rotateAnimation_speed;
    RotateAnimation rotateAnimation_battery;

    // needle attributes for speed and battery charge
    int             right_iv_battery;
    int             right_iv_speed;
    int             bottom_iv_speed;
    int             bottom_iv_battery;

    final float     STARTING_POSITION_SPEED = 105;
    final float     STARTING_POSITION_BATTERY = 125;
    float           current_position_speed;
    float           current_position_battery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);

        // gets extra variables passed from MainActivity.java
        Intent intent = getIntent();
        device = intent.getExtras().getParcelable("device");

        // starts the GATT service
        gatt = device.connectGatt(getApplicationContext(), false, gatt_callback, 2);
        gatt_warnings = device.connectGatt(getApplicationContext(), false, gatt_callback_warnings, 2);

        // initialises all of the GUI attributes
        iv_indicator_left =         (ImageView) findViewById(R.id.img_indicator_left);
        iv_indicator_right =        (ImageView) findViewById(R.id.img_indicator_right);
        iv_indicator_left_grey =    (ImageView) findViewById(R.id.img_indicator_left_grey);
        iv_indicator_right_grey =   (ImageView) findViewById(R.id.img_indicator_right_grey);
        iv_gauge_speed =            (ImageView) findViewById(R.id.iv_gauge_speed);
        iv_needle_speed =           (ImageView) findViewById(R.id.iv_needle_speed);
        iv_gauge_battery =          (ImageView) findViewById(R.id.iv_gauge_battery);
        iv_needle_battery =         (ImageView) findViewById(R.id.iv_needle_battery);
        iv_master_warning =         (ImageView) findViewById(R.id.iv_master_warning);
        iv_seatbelt  =              (ImageView) findViewById(R.id.iv_seatbelt);
        iv_lights_fault  =              (ImageView) findViewById(R.id.iv_lights_fault);
        iv_low_tire_pressure  =              (ImageView) findViewById(R.id.iv_low_tire_pressure);
        iv_low_wiper_fluid  =              (ImageView) findViewById(R.id.iv_low_wiper_fluid);
        iv_airbags  =              (ImageView) findViewById(R.id.iv_air_bag_fault);
        iv_brake_system  =              (ImageView) findViewById(R.id.iv_brakes);
        iv_abs  =              (ImageView) findViewById(R.id.iv_abs);
        iv_motor  =              (ImageView) findViewById(R.id.iv_motor);
        tv_connected =              (TextView) findViewById(R.id.text_connected);
        tv_speed =                  (TextView) findViewById(R.id.tv_speed);
        tv_battery =                (TextView) findViewById(R.id.tv_battery);
        btn_retry =                 (Button) findViewById(R.id.btn_retry_connection);
        iv_battery_temp = (ImageView) findViewById(R.id.iv_battery_temp);
        iv_parking_brake = (ImageView) findViewById(R.id.iv_handbrake);
        iv_lights = (ImageView) findViewById(R.id.iv_light_intensity);
        txt_range = (TextView) findViewById(R.id.txt_battery_range);
        txt_distance = (TextView) findViewById(R.id.txt_distance);
        iv_charging = (ImageView) findViewById(R.id.iv_charging);

        btn_retry.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        // create the animations for the speed and battery charge (moves to correct positions)
        createNeedleAnimations();
        // creates the animations for the turn signals (initially hidden)
        createTurnSignalAnimations();

    }

    public BluetoothGattCallback gatt_callback = new BluetoothGattCallback() {

        // when the connection state is changed, this will be called
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            // checks if the bluetooth device has disconnected
            if (status == BluetoothGatt.GATT_FAILURE || status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnects the service
                gatt.disconnect();
                runGUIThread(DEVICE_DISCONNECTED);
            }

            // checks if the device is connected
            else if (newState == BluetoothProfile.STATE_CONNECTED) {
                // waits for any running GATT services to finish
                try { Thread.sleep(600); }
                catch (InterruptedException e) { e.printStackTrace(); }
                // update the GUI to state the device is connected
                runGUIThread(DEVICE_CONNECTED);
                // attempts to discover services the device is advertising
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d("BluetoothLeService", "onServicesDiscovered()");
            if (status == BluetoothGatt.GATT_SUCCESS) {

                // gets the services and the services characteristics and stores them as attributes
                BluetoothGattService service = gatt.getService(UUID.fromString(dashboard.SERVICE_UUID));

                device_characteristics.add(service.getCharacteristic(UUID.fromString(dashboard.characteristics.get(dashboard.BATTERY_CHARGE))));
                device_characteristics.add(service.getCharacteristic(UUID.fromString(dashboard.characteristics.get(dashboard.BATTERY_RANGE))));
                device_characteristics.add(service.getCharacteristic(UUID.fromString(dashboard.characteristics.get(dashboard.BATTERY_TEMP))));
                device_characteristics.add(service.getCharacteristic(UUID.fromString(dashboard.characteristics.get(dashboard.BATTERY_CHARGE_STATUS))));

                device_characteristics.add(service.getCharacteristic(UUID.fromString(dashboard.characteristics.get(dashboard.SPEED))));
                device_characteristics.add(service.getCharacteristic(UUID.fromString(dashboard.characteristics.get(dashboard.DISTANCE_TRAVELED))));
                device_characteristics.add(service.getCharacteristic(UUID.fromString(dashboard.characteristics.get(dashboard.TURN_SIGNAL))));
                device_characteristics.add(service.getCharacteristic(UUID.fromString(dashboard.characteristics.get(dashboard.LIGHTS))));
                device_characteristics.add(service.getCharacteristic(UUID.fromString(dashboard.characteristics.get(dashboard.PARKING_BREAK))));

                // refresh the GUI
                runGUIThread(DEVICE_CONNECTED);

                // runs through each characteristic and sets a notification for each
                for(int characteristic = 0; characteristic < device_characteristics.size(); characteristic++){
                    gatt.setCharacteristicNotification(device_characteristics.get(characteristic), true);
                }
                services_discovered = true;
                readCharacteristics();
            }
        }

        // reads each characteristic  to to update the GUI with the initial device characteristic values
        //      (characteristics removed recursively from onCharacteristicRead())
        public void readCharacteristics() {
            gatt.readCharacteristic(device_characteristics.get(device_characteristics.size()-1));
        }

        // when a characteristic is successfully initially read, this method will be called
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            String current_char = characteristic.getUuid().toString();
            String received_value = new String(characteristic.getValue(), StandardCharsets.UTF_8);

            if (current_char.equals             (dashboard.characteristics.get(dashboard.BATTERY_CHARGE)))
                dashboard.battery_charge        = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.BATTERY_RANGE)))
                dashboard.battery_range         = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.BATTERY_TEMP)))
                dashboard.battery_temp          = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.BATTERY_CHARGE_STATUS)))
                dashboard.battery_charge_status = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.SPEED)))
                dashboard.speed                 = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.DISTANCE_TRAVELED)))
                dashboard.distance_traveled     = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.TURN_SIGNAL)))
                dashboard.turn_signal           = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.LIGHTS)))
                dashboard.lights                = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.PARKING_BREAK)))
                dashboard.parking_brake         = Integer.parseInt(received_value);

            Log.d(current_char + "     Changed Value", received_value);

            // updates the GUI on the UI thread
            try { runGUIThread(DEVICE_CONNECTED); } catch (Exception e) { Log.d(TAG, "Error (read)" + e); }

            // recursively removes each characteristic until all have been read
            device_characteristics.remove(device_characteristics.get(device_characteristics.size() - 1));
            if (device_characteristics.size() > 0) readCharacteristics();
        }

        // when a characteristic value has changed on the device, this method will be called
        //      (called via a notification on each characteristic)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            String current_char = characteristic.getUuid().toString();
            String received_value = new String(characteristic.getValue(), StandardCharsets.UTF_8);

            // checks the characteristic passed in against each of the available characteristics then updates the relevant values
            if (current_char.equals             (dashboard.characteristics.get(dashboard.BATTERY_CHARGE)))
                dashboard.battery_charge        = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.BATTERY_RANGE)))
                dashboard.battery_range         = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.BATTERY_TEMP)))
                dashboard.battery_temp          = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.BATTERY_CHARGE_STATUS)))
                dashboard.battery_charge_status = Integer.parseInt(received_value);
            if (current_char.equals             (dashboard.characteristics.get(dashboard.SPEED)))
                dashboard.speed                 = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.DISTANCE_TRAVELED)))
                dashboard.distance_traveled     = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.TURN_SIGNAL)))
                dashboard.turn_signal           = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.LIGHTS)))
                dashboard.lights                = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.PARKING_BREAK)))
                dashboard.parking_brake         = Integer.parseInt(received_value);

            Log.d(current_char + "     Changed Value", received_value);

            // updates the GUI on the UI thread
            try {
                runGUIThread(DEVICE_CONNECTED);
            } catch (Exception e) {
                Log.d(TAG, "Error (read)" + e);
            }
        }
    };

    public BluetoothGattCallback gatt_callback_warnings = new BluetoothGattCallback() {
        // when the connection state is changed, this will be called
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            // checks if the bluetooth device has disconnected
            if (status == BluetoothGatt.GATT_FAILURE || status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnects the service
                gatt.disconnect();
                runGUIThread(DEVICE_DISCONNECTED);
            }

            // checks if the device is connected
            else if (newState == BluetoothProfile.STATE_CONNECTED) {
                // waits for any running GATT services to finish
                try { Thread.sleep(600); }
                catch (InterruptedException e) { e.printStackTrace(); }
                // update the GUI to state the device is connected
                runGUIThread(DEVICE_CONNECTED);
                // attempts to discover services the device is advertising
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d("BluetoothLeService", "onServicesDiscovered()");
            if (status == BluetoothGatt.GATT_SUCCESS) {

                // gets the services and the services characteristics and stores them as attributes
                BluetoothGattService service = gatt.getService(UUID.fromString(dashboard.SERVICE_UUID));

                device_characteristics_warnings.add(service.getCharacteristic(UUID.fromString(dashboard.characteristics.get(dashboard.ABS))));
                device_characteristics_warnings.add(service.getCharacteristic(UUID.fromString(dashboard.characteristics.get(dashboard.ELECTRIC_DRIVE_SYSTEM))));
                device_characteristics_warnings.add(service.getCharacteristic(UUID.fromString(dashboard.characteristics.get(dashboard.MASTER_WARNING))));
                device_characteristics_warnings.add(service.getCharacteristic(UUID.fromString(dashboard.characteristics.get(dashboard.SEAT_BELT))));
                device_characteristics_warnings.add(service.getCharacteristic(UUID.fromString(dashboard.characteristics.get(dashboard.LIGHTS_FAULT))));
                device_characteristics_warnings.add(service.getCharacteristic(UUID.fromString(dashboard.characteristics.get(dashboard.LOW_WIPER_FLUID))));
                device_characteristics_warnings.add(service.getCharacteristic(UUID.fromString(dashboard.characteristics.get(dashboard.LOW_TIRE_PRESSURE))));
                device_characteristics_warnings.add(service.getCharacteristic(UUID.fromString(dashboard.characteristics.get(dashboard.AIR_BAGS))));
                device_characteristics_warnings.add(service.getCharacteristic(UUID.fromString(dashboard.characteristics.get(dashboard.BRAKE_SYSTEM))));

                // refresh the GUI
                runGUIThread(DEVICE_CONNECTED);

                // runs through each characteristic and sets a notification for each
                for(int characteristic = 0; characteristic < device_characteristics_warnings.size(); characteristic++){
                    gatt.setCharacteristicNotification(device_characteristics_warnings.get(characteristic), true);
                }
                services_discovered_warnings = true;
                readCharacteristics();
            }
        }

        // reads each characteristic  to to update the GUI with the initial device characteristic values
        //      (characteristics removed recursively from onCharacteristicRead())
        public void readCharacteristics() {
            gatt.readCharacteristic(device_characteristics_warnings.get(device_characteristics_warnings.size()-1));
        }

        // when a characteristic is successfully initially read, this method will be called
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            String current_char = characteristic.getUuid().toString();
            String received_value = new String(characteristic.getValue(), StandardCharsets.UTF_8);

            if (current_char.equals             (dashboard.characteristics.get(dashboard.MASTER_WARNING)))
                dashboard.master_warning        = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.SEAT_BELT)))
                dashboard.seat_belt             = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.LIGHTS_FAULT)))
                dashboard.lights_fault          = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.LOW_WIPER_FLUID)))
                dashboard.low_wiper_fluid       = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.LOW_TIRE_PRESSURE)))
                dashboard.low_tire_pressure     = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.AIR_BAGS)))
                dashboard.air_bags              = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.BRAKE_SYSTEM)))
                dashboard.brake_system          = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.ABS)))
                dashboard.abs                   = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.ELECTRIC_DRIVE_SYSTEM)))
                dashboard.electric_drive_system = Integer.parseInt(received_value);

            Log.d(current_char + "     Read Value", received_value);

            // updates the GUI on the UI thread
            try { runGUIThread(DEVICE_CONNECTED); } catch (Exception e) { Log.d(TAG, "Error (read)" + e); }

            // recursively removes each characteristic until all have been read
            device_characteristics.remove(device_characteristics.get(device_characteristics.size() - 1));
            if (device_characteristics.size() > 0) readCharacteristics();
        }

        // when a characteristic value has changed on the device, this method will be called
        //      (called via a notification on each characteristic)
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            String current_char = characteristic.getUuid().toString();
            String received_value = new String(characteristic.getValue(), StandardCharsets.UTF_8);

            if (current_char.equals             (dashboard.characteristics.get(dashboard.MASTER_WARNING)))
                dashboard.master_warning        = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.SEAT_BELT)))
                dashboard.seat_belt             = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.LIGHTS_FAULT)))
                dashboard.lights_fault          = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.LOW_WIPER_FLUID)))
                dashboard.low_wiper_fluid       = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.LOW_TIRE_PRESSURE)))
                dashboard.low_tire_pressure     = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.AIR_BAGS)))
                dashboard.air_bags              = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.BRAKE_SYSTEM)))
                dashboard.brake_system          = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.ABS)))
                dashboard.abs                   = Integer.parseInt(received_value);
            else if (current_char.equals        (dashboard.characteristics.get(dashboard.ELECTRIC_DRIVE_SYSTEM)))
                dashboard.electric_drive_system = Integer.parseInt(received_value);

            Log.d(current_char + "     Changed Value", received_value);

            // updates the GUI on the UI thread
            try {
                runGUIThread(DEVICE_CONNECTED);
            } catch (Exception e) {
                Log.d(TAG, "Error (read)" + e);
            }
        }
    };

    // this initialises the animations for the indicators on the GUI
    private void createTurnSignalAnimations() {
        // sets the duration
        animation_right.setDuration(1000);
        animation_left.setDuration(1000);

        animation_right.setInterpolator(new LinearInterpolator());
        animation_left.setInterpolator(new LinearInterpolator());

        // sets to repeat infinitely
        animation_right.setRepeatCount(Animation.INFINITE);
        animation_left.setRepeatCount(Animation.INFINITE);

        animation_right.setRepeatMode(Animation.REVERSE);
        animation_left.setRepeatMode(Animation.REVERSE);
    }

    public void createNeedleAnimations() {
        current_position_speed =    STARTING_POSITION_SPEED;
        current_position_battery =  STARTING_POSITION_BATTERY;

        Rect needle_bounds =    new Rect();
        iv_needle_speed.getLocalVisibleRect(needle_bounds);
        bottom_iv_speed =       needle_bounds.bottom;
        right_iv_speed =        needle_bounds.right;

        rotateAnimation_speed = new RotateAnimation(0, current_position_speed, bottom_iv_speed, right_iv_speed);
        rotateAnimation_speed.setFillAfter(true);
        rotateAnimation_speed.setDuration(500);
        iv_needle_speed.startAnimation(rotateAnimation_speed);

        iv_needle_battery.getLocalVisibleRect(needle_bounds);
        bottom_iv_battery =     needle_bounds.bottom;
        right_iv_battery =      needle_bounds.right;

        rotateAnimation_battery = new RotateAnimation(0, current_position_battery, bottom_iv_speed, right_iv_speed);
        rotateAnimation_battery.setFillAfter(true);
        rotateAnimation_battery.setDuration(500);
        iv_needle_battery.startAnimation(rotateAnimation_battery);
    }

    // runs a separate thread from the GattCallback to update the GUI on the UI Thread (used for updating the values throughout)
    private void runGUIThread(final int connection_status) {
        new Thread() {
            public void run() {
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // checks if the device has disconnected and gives the option to try again
                            if (connection_status == DEVICE_DISCONNECTED) {
                                tv_connected.setText("Disconencted");
                                btn_retry.setVisibility(View.VISIBLE);
                            }
                            else if (connection_status == DEVICE_CONNECTED) {
                                tv_connected.setText("Connected");
                                btn_retry.setVisibility(View.GONE);
                            }

                            // checks the indicator and starts/stops the relevant animations
                            if (dashboard.turn_signal == 0) {
                                animation_right.cancel();
                                animation_left.cancel();

                                iv_indicator_left.setVisibility(View.GONE);
                                iv_indicator_right.setVisibility(View.GONE);
                                iv_indicator_left_grey.setVisibility(View.VISIBLE);
                                iv_indicator_right_grey.setVisibility(View.VISIBLE);
                            }
                            else if (dashboard.turn_signal == 1) {
                                animation_right.cancel();
                                iv_indicator_left.startAnimation(animation_left);

                                iv_indicator_left.setVisibility(View.VISIBLE);
                                iv_indicator_left_grey.setVisibility(View.GONE);
                                iv_indicator_right.setVisibility(View.GONE);
                                iv_indicator_right_grey.setVisibility(View.VISIBLE);
                            }
                            else if (dashboard.turn_signal == 2) {
                                animation_left.cancel();
                                iv_indicator_right.startAnimation(animation_right);

                                iv_indicator_right.setVisibility(View.VISIBLE);
                                iv_indicator_right_grey.setVisibility(View.GONE);
                                iv_indicator_left.setVisibility(View.GONE);
                                iv_indicator_left_grey.setVisibility(View.VISIBLE);
                            }


                            if (dashboard.master_warning == 0) iv_master_warning.setImageResource(R.drawable.master_warning_grey);
                            else iv_master_warning.setImageResource(R.drawable.master_warning_red);
                            if (dashboard.seat_belt == 0) iv_seatbelt.setImageResource(R.drawable.seatbelt_warning_grey);
                            else iv_seatbelt.setImageResource(R.drawable.seatbelt_warning_red);
                            if (dashboard.lights_fault == 0) iv_lights_fault.setImageResource(R.drawable.lights_fault_grey);
                            else iv_lights_fault.setImageResource(R.drawable.lights_fault_red);
                            if (dashboard.low_wiper_fluid == 0) iv_low_wiper_fluid.setImageResource(R.drawable.low_wiper_fluid_grey);
                            else iv_low_wiper_fluid.setImageResource(R.drawable.low_wiper_fluid_red);
                            if (dashboard.low_tire_pressure == 0) iv_low_tire_pressure.setImageResource(R.drawable.low_tire_pressure_grey);
                            else iv_low_tire_pressure.setImageResource(R.drawable.low_tire_pressure_red);
                            if (dashboard.air_bags == 0) iv_airbags.setImageResource(R.drawable.airbag_fault_grey);
                            else iv_airbags.setImageResource(R.drawable.airbag_fault_red);
                            if (dashboard.brake_system == 0) iv_brake_system.setImageResource(R.drawable.brake_warning_grey);
                            else if (dashboard.brake_system == 1) iv_brake_system.setImageResource(R.drawable.brake_warning_orange);
                            else iv_brake_system.setImageResource(R.drawable.brake_warning_red);
                            if (dashboard.abs == 0) iv_abs.setImageResource(R.drawable.abs_fault_grey);
                            else if (dashboard.abs == 1) iv_abs.setImageResource(R.drawable.abs_fault_orange);
                            else iv_abs.setImageResource(R.drawable.abs_fault_red);
                            if (dashboard.electric_drive_system == 0) iv_motor.setImageResource(R.drawable.electric_drive_system_fault_grey);
                            else if (dashboard.electric_drive_system == 1) iv_motor.setImageResource(R.drawable.electric_drive_system_fault_orange);
                            else iv_motor.setImageResource(R.drawable.electric_drive_system_fault_red);

                            if (dashboard.battery_temp > 20) iv_battery_temp.setImageResource(R.drawable.temp_red);
                            else if (dashboard.battery_temp == 20) iv_battery_temp.setImageResource(R.drawable.temp_grey);
                            else if (dashboard.battery_temp < 20) iv_battery_temp.setImageResource(R.drawable.temp_blue);
                            if (dashboard.parking_brake == 0) iv_parking_brake.setImageResource(R.drawable.parking_brake_grey);
                            else iv_parking_brake.setImageResource(R.drawable.parking_brake_red);
                            if (dashboard.lights == 0) iv_lights.setImageResource(R.drawable.no_lightbeam_grey);
                            else if (dashboard.lights == 1) iv_lights.setImageResource(R.drawable.med_lightbeam_green);
                            else if (dashboard.lights == 2) iv_lights.setImageResource(R.drawable.high_lightbeam_blue);
                            if (dashboard.battery_charge_status == 0) iv_charging.setImageResource(R.drawable.charging_icon_grey);
                            else iv_charging.setImageResource(R.drawable.charging_icon_green);

                            txt_range.setText(Integer.toString(dashboard.battery_range));
                            txt_distance.setText(Integer.toString(dashboard.distance_traveled));


                            if (current_position_speed != STARTING_POSITION_SPEED + dashboard.speed * 2.0f) {
                                tv_speed.setText(Integer.toString(dashboard.speed));

                                // creates an animation with the received speed
                                rotateAnimation_speed = new RotateAnimation(current_position_speed, STARTING_POSITION_SPEED + dashboard.speed * 2.0f, bottom_iv_speed, right_iv_speed);
                                // keeps the dial in position
                                rotateAnimation_speed.setFillAfter(true);
                                // 0.5 seconds
                                rotateAnimation_speed.setDuration(50);
                                // linear interpolator so there is no acceleration or deceleration in the dial
                                rotateAnimation_speed.setInterpolator(new LinearInterpolator());
                                // starts the animation
                                iv_needle_speed.startAnimation(rotateAnimation_speed);
                                rotateAnimation_speed.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    // saves the new position when the animation is started
                                    public void onAnimationStart(Animation animation) {
                                        current_position_speed = STARTING_POSITION_SPEED + dashboard.speed * 2.0f;
                                    }

                                    @Override
                                    public void onAnimationEnd(Animation arg0) { }
                                    @Override
                                    public void onAnimationRepeat(Animation animation) { }
                                });
                            }

                            if (current_position_battery != STARTING_POSITION_BATTERY + dashboard.battery_charge * 2.0f) {
                                tv_battery.setText(Integer.toString(dashboard.battery_charge));

                                rotateAnimation_battery = new RotateAnimation(current_position_battery, STARTING_POSITION_BATTERY + dashboard.battery_charge * 2.0f, bottom_iv_battery, right_iv_battery);
                                rotateAnimation_battery.setFillAfter(true);
                                rotateAnimation_battery.setDuration(50);
                                rotateAnimation_battery.setInterpolator(new LinearInterpolator());
                                iv_needle_battery.startAnimation(rotateAnimation_battery);
                                rotateAnimation_battery.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {
                                        current_position_battery = STARTING_POSITION_BATTERY + dashboard.battery_charge * 2.0f;
                                    }

                                    @Override
                                    public void onAnimationEnd(Animation arg0) { }
                                    @Override
                                    public void onAnimationRepeat(Animation animation) { }
                                });
                            }

                        }
                    });
                    Thread.sleep(300);
                } catch (Exception e) { Log.d(TAG, "Error " + e); }
            }
        }.start();
    }
}