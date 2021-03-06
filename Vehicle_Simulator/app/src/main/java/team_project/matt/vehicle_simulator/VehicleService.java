package team_project.matt.vehicle_simulator;

import android.widget.Toast;

import java.util.ArrayList;

public class VehicleService implements BluetoothServerStatus
{
    private final String UUID          = "dee0e505-9680-430e-a4c4-a225905ce33d";
    private final String CCCDescriptor = "00002902-0000-1000-8000-00805f9b34fb";

    private VehicleDashboard vehicleDashboard;
    private BluetoothLE      bluetoothDevice;

    private boolean serviceAdded = false;

    enum Property
    {
        // battery data
        BATTERY_LVL, RANGE, BATTERY_TEMP,
        
        // car data
        SPEED, DISTANCE, TURN_SIGNAL, LIGHTS, PARKING_BRAKE,
        
        // warnings
        SEATBELT, WIPER_LOW, TYRE_PRESSURE_LOW, AIRBAG, BRAKE_ERR, ABS_ERR, EV_ERR
    }
    
    class Characteristic
    {
        // supported types
        static final int FORMAT_NUMBER = 0;  // regular numerical value
        static final int FORMAT_STATE  = 1;  // finite number of states (on, off etc.)
        
        private String             UUID;
        private Property           property;
        private int                format;
        private ArrayList<Integer> supportedValues;
        private int                data;
        
        Characteristic(String UUID, Property property, int format)
        {
            this.UUID            = UUID;
            this.property        = property;
            this.format          = format;
            this.supportedValues = new ArrayList<>();
        }

        // add supported value if format has finite number of states
        void addSupportedValue(int supportedValue)
        {
            if (format == FORMAT_STATE) supportedValues.add(supportedValue);
        }

        // set data and update GATT service with value
        void setData(int data)
        {
            // regular number
            if (format == FORMAT_NUMBER) this.data = data;

            // update value if supported
            else if (format == FORMAT_STATE)
            {
                for (int value : supportedValues)
                    if (data == value)
                    {
                        this.data = data;
                        break;
                    }
            }
            
            if (serviceAdded) bluetoothDevice.updateCharacteristic(this.UUID, Integer.toString(this.data));
        }
    }

    private ArrayList<Characteristic> characteristics = new ArrayList<>();

    // property states
    static final int         STATE_OFF = 0;
    static final int          STATE_ON = 1;
    static final int STATE_SIGNAL_LEFT = 1, STATE_SIGNAL_RIGHT = 2;
    static final int  STATE_LIGHTS_LOW = 1,  STATE_LIGHTS_HIGH = 2, STATE_LIGHTS_ERR = 3;
    static final int STATE_WARNING_LOW = 1, STATE_WARNING_HIGH = 2;

    // set properties with UUIDs, supported values and ID'ing enum value
    private void buildCharacteristics()
    {
        // battery characteristics
        //------------------------------------------------------------
        Characteristic newCharacteristic = new Characteristic
                ("76a247fb-a76f-42da-91ce-d6a5bdebd0e2",
                        Property.BATTERY_LVL,
                        Characteristic.FORMAT_NUMBER);

        characteristics.add(newCharacteristic);

        newCharacteristic = new Characteristic
                ("bf252fd6-c1e3-4835-b4be-b5e353e62d7b",
                        Property.RANGE,
                        Characteristic.FORMAT_NUMBER);

        characteristics.add(newCharacteristic);

        newCharacteristic = new Characteristic
                ("4751b324-3935-4b1e-a4e7-9c0888d03325",
                        Property.BATTERY_TEMP,
                        Characteristic.FORMAT_NUMBER);

        characteristics.add(newCharacteristic);
        //------------------------------------------------------------

        // car characteristics
        //------------------------------------------------------------
        newCharacteristic = new Characteristic
                ("7b9b53ff-5421-4bdf-beb0-ca8c949542c1",
                        Property.SPEED,
                        Characteristic.FORMAT_NUMBER);

        characteristics.add(newCharacteristic);

        newCharacteristic = new Characteristic
                ("5bebe839-c2e2-4fad-bb18-65f792ddb16f",
                        Property.DISTANCE,
                        Characteristic.FORMAT_NUMBER);

        characteristics.add(newCharacteristic);

        newCharacteristic = new Characteristic
                ("74df0c8f-f3e1-4cf5-b875-56d7ca609a2e",
                        Property.TURN_SIGNAL,
                        Characteristic.FORMAT_STATE);

        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_SIGNAL_LEFT);
        newCharacteristic.addSupportedValue(STATE_SIGNAL_RIGHT);

        characteristics.add(newCharacteristic);

        newCharacteristic = new Characteristic
                ("131223c4-1e5f-486a-9ab5-d85c41984f6f",
                        Property.LIGHTS,
                        Characteristic.FORMAT_STATE);

        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_LIGHTS_LOW);
        newCharacteristic.addSupportedValue(STATE_LIGHTS_HIGH);
        newCharacteristic.addSupportedValue(STATE_LIGHTS_ERR);

        characteristics.add(newCharacteristic);

        newCharacteristic = new Characteristic
                ("f05976f6-aa9e-4d19-a255-aeda7dbb624f",
                        Property.PARKING_BRAKE,
                        Characteristic.FORMAT_STATE);

        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_ON);

        characteristics.add(newCharacteristic);
        //------------------------------------------------------------

        // warnings
        //------------------------------------------------------------
        newCharacteristic = new Characteristic
                ("da2d9231-ae69-4b5b-b4dc-d7c940e72815",
                        Property.SEATBELT,
                        Characteristic.FORMAT_STATE);

        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_ON);

        characteristics.add(newCharacteristic);

        newCharacteristic = new Characteristic
                ("0b458d6d-4e0c-442d-9c18-febd81281d78",
                        Property.WIPER_LOW,
                        Characteristic.FORMAT_STATE);

        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_ON);

        characteristics.add(newCharacteristic);

        newCharacteristic = new Characteristic
                ("ce99220c-75ed-4d38-9b25-5a0f7e766016",
                        Property.TYRE_PRESSURE_LOW,
                        Characteristic.FORMAT_STATE);

        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_ON);

        characteristics.add(newCharacteristic);

        newCharacteristic = new Characteristic
                ("8be2a5a1-5e2e-4344-9304-fb642b55746e",
                        Property.AIRBAG,
                        Characteristic.FORMAT_STATE);

        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_ON);

        characteristics.add(newCharacteristic);

        newCharacteristic = new Characteristic
                ("b45ea8a5-7d70-41bc-82a3-51cd35d594cc",
                        Property.BRAKE_ERR,
                        Characteristic.FORMAT_STATE);

        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_WARNING_LOW);
        newCharacteristic.addSupportedValue(STATE_WARNING_HIGH);

        characteristics.add(newCharacteristic);

        newCharacteristic = new Characteristic
                ("ce3843db-ef55-4b8d-a02a-e0eb4963a1e0",
                        Property.ABS_ERR,
                        Characteristic.FORMAT_STATE);

        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_WARNING_LOW);
        newCharacteristic.addSupportedValue(STATE_WARNING_HIGH);

        characteristics.add(newCharacteristic);

        newCharacteristic = new Characteristic
                ("d93f1c6c-6e14-44b3-95ce-d0a7f71efbb5",
                        Property.EV_ERR,
                        Characteristic.FORMAT_STATE);

        newCharacteristic.addSupportedValue(STATE_OFF);
        newCharacteristic.addSupportedValue(STATE_WARNING_LOW);
        newCharacteristic.addSupportedValue(STATE_WARNING_HIGH);

        characteristics.add(newCharacteristic);
        //------------------------------------------------------------
    }

    // prepare data to send to GATT and then start server
    private void startGATT()
    {
        ArrayList<String>  UUIDs         = new ArrayList<>();
        ArrayList<Integer> defaultValues = new ArrayList<>();

        // get UUID of each property and set start value as 0
        for (Characteristic characteristic : characteristics)
        {
            UUIDs.add(characteristic.UUID);
            defaultValues.add(STATE_OFF);
        }

        bluetoothDevice.startGATT(UUID, UUIDs, CCCDescriptor, defaultValues);
    }

    // start Bluetooth hardware
    void beginSetup(BluetoothLE bluetoothDevice, VehicleDashboard dashboardInterface)
    {
        if (bluetoothDevice != null && dashboardInterface != null)
        {
            this.bluetoothDevice  = bluetoothDevice;
            this.vehicleDashboard = dashboardInterface;

            // attempt to enable Bluetooth hardware, result returns to permission interface
            bluetoothDevice.beginSetup();
        }
    }

    // initialise characteristics and begin GAP
    void start()
    {
        if (bluetoothDevice.isEnabled())
        {
            buildCharacteristics();                     // prepare properties to load
            bluetoothDevice.startAdvertising(UUID);     // calls advertiseResult() on finish
        }

        else vehicleDashboard.showToast("Bluetooth device is not enabled.", Toast.LENGTH_LONG);
    }

    // called on advertise setup finish
    @Override
    public void advertiseResult(boolean started)
    {
        // GAP complete, start GATT next
        if (started) startGATT();

        // GAP failed, dont continue
        else vehicleDashboard.showToast("Could not broadcast device.", Toast.LENGTH_LONG);
    }

    // called when attempted to add service
    @Override
    public void serviceAddedResult(boolean added)
    {
        // failed
        if (!added) vehicleDashboard.showToast("Could not start vehicle.", Toast.LENGTH_LONG);

        // safe to update characteristics
        else serviceAdded = true;
    }

    // called after GATT setup finished
    @Override
    public void GATTResult(boolean started)
    {
        // failed
        if (!started) vehicleDashboard.showToast("Could not start vehicle.", Toast.LENGTH_LONG);

        // safe to start up vehicle
        else vehicleDashboard.vehicleReady();
    }

    void stop() { bluetoothDevice.shutDown(); }

    // fetch characteristic using its enum ID
    Characteristic getCharacteristic(Property property)
    {
        for (Characteristic characteristic : characteristics)
            if (property == characteristic.property)
                return characteristic;
        
        return null;
    }
}