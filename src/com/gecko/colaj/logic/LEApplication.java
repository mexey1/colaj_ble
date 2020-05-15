//package org.github.hypfvieh.sandbox.bluez;

package com.gecko.colaj.logic;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.File;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.github.hypfvieh.bluetooth.DeviceManager;
import com.github.hypfvieh.bluetooth.wrapper.BluetoothAdapter;
import org.bluez.Adapter1;
import org.bluez.Device1;
import org.bluez.GattCharacteristic1;
import org.bluez.GattManager1;
import org.bluez.GattProfile1;
import org.bluez.exceptions.BluezFailedException;
import org.bluez.exceptions.BluezNotAuthorizedException;
import org.bluez.exceptions.BluezNotReadyException;
import org.bluez.GattApplication1;
import org.bluez.LEAdvertisingManager1;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.handlers.AbstractInterfacesAddedHandler;
import org.freedesktop.dbus.handlers.AbstractInterfacesRemovedHandler;
import org.freedesktop.dbus.handlers.AbstractPropertiesChangedHandler;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.ObjectManager;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
import org.freedesktop.dbus.types.Variant;


public class LEApplication implements DBusInterface,ObjectManager {

   private final String SERVICE_PATH="/com/gecko/colaj/logic/LEService";//"/com/github/hypfvieh/bluez/Paulmannprofile"
   private static final String BLUEZ_DBUS="org.bluez";
   // private final String SERVICE_PATH="/com/gecko/colaj/LEService";
    private final String CHAR_PATH="/com/gecko/colaj/logic/LECharacteristic";
    private final String SERVICE_UUID="2D266186-01FB-47C2-8D9F-10B8EC891363";
    private final String NETWORK_UUID="12345678-02FB-47C2-8D9F-10B8EC891363";
    private final String PASSWORD_UUID="12345679-02FB-47C2-8D9F-10B8EC891363";
    private final String CONNECTION_STATUS_UUID="12345670-02FB-47C2-8D9F-10B8EC891363";
    private final String APP_PATH="/com/gecko/colaj/logic/LEApplication";
    private final String ADV_PATH="/com/gecko/colaj/logic/LEAdvertisement";
    private final File outputFile = new File("/var/colaj/out.col");
	
    private LEService     service;
    private LECharacteristic ssidCharas,passwordCharas,netStatCharas;
    private DBusConnection       connection;
    private String serviceUUID = "2D266186-01FB-47C2-8D9F-10B8EC891363";
    private Map<String, Device1> btDevices = new HashMap<>();

    public LEApplication() throws DBusException {
	    try{
 			// open connection to bluez on SYSTEM Bus
        		connection = DBusConnection.getConnection(DBusConnection.DBusBusType.SYSTEM);
        		// create profile to export
        		service = new LEService(SERVICE_PATH,serviceUUID,true);
			outputFile.mkdirs();
	    }
	    catch(Exception e){
		e.printStackTrace();
	    }

    }

    public void register() throws DBusException {
	DeviceManager devManager = DeviceManager.createInstance(false);
        BluetoothAdapter btAdapter = devManager.getAdapter();
	String btAdapterPath = btAdapter.getDbusPath();//"/org/bluez/hci0"

        connection.exportObject(getObjectPath(), this);

        addPropertiesChangedListener();

        addInterfacesAddedListener();

        addInterfacesRemovedListener();

        // get the GattManager to register new profile
        GattManager1 gattmanager = connection.getRemoteObject(BLUEZ_DBUS,btAdapterPath , GattManager1.class);
	//get LEAdvertisingManager to advertise our service
	LEAdvertisingManager1 leAdvManager =(LEAdvertisingManager1)connection.getRemoteObject(BLUEZ_DBUS,btAdapterPath,LEAdvertisingManager1.class);
        System.out.println("Registering: " + this.getObjectPath());

	//create advertising object
	LEAdvertisement advertisement = new LEAdvertisement(LEAdvertisement.ADVERTISEMENT_TYPE_PERIPHERAL,ADV_PATH);
		
	//create network SSID characteristics
	ssidCharas = new LECharacteristic(CHAR_PATH+"0",NETWORK_UUID,service,this);
        ssidCharas.setFlag(LECharacteristic.READ);
        ssidCharas.setFlag(LECharacteristic.WRITE);

	//create password characteristic
        passwordCharas = new LECharacteristic(CHAR_PATH+"1",PASSWORD_UUID,service,this);
        passwordCharas.setFlag(LECharacteristic.READ);
        passwordCharas.setFlag(LECharacteristic.WRITE);

	//create network status characteristic
        netStatCharas = new LECharacteristic(CHAR_PATH+"2",CONNECTION_STATUS_UUID,service,this);
        netStatCharas.setFlag(LECharacteristic.READ);
        netStatCharas.setFlag(LECharacteristic.NOTIFY);

	//add characteristics to service
        service.addCharacteristic(ssidCharas);
	service.addCharacteristic(passwordCharas);
	service.addCharacteristic(netStatCharas);
		
	//add service to advertisement object and export it
        advertisement.addService(service);
        System.out.println("exporting advertisement "+advertisement);
        advertisement.export(connection);
        //export the service, this would in-turn export the characteristics
        service.export(connection);
		
	//register the advertisement
	leAdvManager.RegisterAdvertisement(new DBusPath(ADV_PATH), new HashMap<>());//ADV_PATH
	System.out.println("Registering service");	
        // register service
        gattmanager.RegisterApplication(new DBusPath(this.getObjectPath()), new HashMap<>());

    }

    public void wifiConnect(){
	    try{
		String ssid = ssidCharas.getValue();
        	String pass = passwordCharas.getValue();
        	System.out.println("Attempting to connect to "+ssid+" using "+pass);
        	//ssid = "'"+ssid.replace("'","'\\''")+"'";//escape single quotes
        	//pass = "'"+pass.replace("'","'\\''")+"'";
        	System.out.println("Encrypting the details ssid "+ssid+" pass "+pass );
        	String commands[] = new String[]{"wpa_passphrase",ssid,pass};
        	//Runtime rt = Runtime.getRuntime();
		ProcessBuilder builder = new ProcessBuilder(commands);
		createOutputFile();
		builder.redirectOutput(outputFile);
        	Process process = builder.start();//rt.exec(commands);
		boolean exitGracefully = process.waitFor(10,TimeUnit.SECONDS);//WE'D WAIT FOR 10 SECONDS BEFORE FORCING THE PROCESS TO TERMINATE
		boolean result = isSuccessful("/etc/wpa_supplicant_test.conf");
		//int exitVal = process.waitFor();
		process.destroy();//let's also destroy the process
		System.out.println("wifi config exited with "+result);
		if(result)//we successfully encrypted and saved the wifi config
		{
			//attempt to connect to the wifi networka
			createOutputFile();
			commands = new String[]{"wpa_supplicant","-c", "/etc/wpa_supplicant_test.conf","-i","wlan0"};
			System.out.println("associating with wifi");
			process = builder.command(commands).start();
			exitGracefully =  process.waitFor(10,TimeUnit.SECONDS);
			result = isSuccessful(null);
			process.destroy();//let's also destroy the process
			//exitVal = process.waitFor();
			System.out.println("associating with wifi exited with "+result);
			if(result)//we successfully associated with the wifi network
			{
				//we'd start the wpa_supplicant with -B option to run in the background
				commands = new String[]{"wpa_supplicant","-c", "/etc/wpa_supplicant_test.conf","-i","wlan0","-B"};
				builder = new ProcessBuilder(commands);
				builder.start();
				//attempt to get an ip address
				commands  = new String[]{"dhclient","wlan0"};
				builder = new ProcessBuilder(commands);
				createOutputFile();
				builder.redirectOutput(outputFile);
				System.out.println("obtaining IP");
				process = builder.start();
				exitGracefully = process.waitFor(10,TimeUnit.SECONDS);
				isSuccessful(null);
				if(!exitGracefully)//we couldn't get an IP address
					netStatCharas.setValue("201");
				else
					netStatCharas.setValue("200");

				//result = isSuccessful(null);
				//exitVal = process.waitFor();
                        	System.out.println("obtaining IP  exited with "+result);
				//netStatCharas.setValue("200");
			}
			else
				netStatCharas.setValue(Integer.toString(201));

		}
		else
			netStatCharas.setValue(Integer.toString(202));

        	//System.out.println("process exit code is "+process.waitFor());
	    }
	    catch(IOException e){
		e.printStackTrace();
	    }
	    catch(InterruptedException e){
	    	e.printStackTrace();
	    }
	    catch(Exception e){
		e.printStackTrace();
	    }
    }

    private void createOutputFile() throws IOException,SecurityException{
	outputFile.delete();
	outputFile.createNewFile();
    }

    private boolean isSuccessful(String file){
	try{
		StringBuilder builder = new StringBuilder();
		Scanner scanner = null;
		scanner = new Scanner(outputFile);
		while(scanner.hasNextLine())
			builder.append(scanner.nextLine());
		
		if(file != null)
		{
			File dest = new File(file);
			dest.createNewFile();
			Files.copy(outputFile.toPath(),dest.toPath(),StandardCopyOption.REPLACE_EXISTING);
		}
		System.out.println(builder);
		String output = builder.toString();
		return !(output.contains("Fail") || output.contains("REJECT") || output.contains("Error"));
			
	}
	catch(IOException e){
		e.printStackTrace();
		return false;
	}
	catch(Exception e)
	{
		e.printStackTrace();
		return false;
	}

    }
    private void addInterfacesRemovedListener() throws DBusException {
        connection.addSigHandler(InterfacesRemoved.class,
                new AbstractInterfacesRemovedHandler() {
                    @Override
                    public void handle(InterfacesRemoved _s) {
                        if (_s != null) {
                            if (_s.getInterfaces().contains(Device1.class.getName())) {
                                System.out.println("Bluetooth device removed: " + _s.getSignalSource());
                                btDevices.remove(_s.getPath());
                            }

                            System.out.println("InterfaceRemoved ----> " + _s.getInterfaces());
                        }

                    }

                });
    }

    private void addInterfacesAddedListener() throws DBusException {
        connection.addSigHandler(InterfacesAdded.class,
                new AbstractInterfacesAddedHandler() {

                    @Override
                    public void handle(InterfacesAdded _s) {
                        if (_s != null) {
                            Map<String, Map<String, Variant<?>>> interfaces = _s.getInterfaces();
                            interfaces.entrySet().stream().filter(e -> e.getKey().equals(Device1.class.getName()))
                                    .forEach(e -> {
                                        Variant<?> address = e.getValue().get("Address");
                                        if (address != null && address.getValue() != null) {
                                            System.out.println("Bluetooth device added: " + address.getValue());
                                            String p = _s.getSignalSource().getPath();
                                            try {
                                                Device1 device1 =
                                                        connection.getRemoteObject("org.bluez", p, Device1.class);
                                                btDevices.put(p, device1);
                                            } catch (DBusException _ex) {
                                                // TODO Auto-generated catch block
                                                _ex.printStackTrace();
                                            }
                                        }
                                    });

                            interfaces.entrySet().stream()
                                    .filter(e -> e.getKey().equals(GattCharacteristic1.class.getName())).forEach(e -> {
                                        System.out.println("New characteristics: " + e.getValue());
                                    });
                            // System.out.println("InterfaceAdded ----> " + _s.getInterfaces());
                        }

                    }

                });
    }

    private void addPropertiesChangedListener() throws DBusException {
        connection.addSigHandler(PropertiesChanged.class,
                new AbstractPropertiesChangedHandler() {

                    @Override
                    public void handle(PropertiesChanged _s) {
                        if (_s != null) {

                            if (!_s.getPath().contains("/org/bluez")
                                    && !_s.getPath().contains(getClass().getPackage().getName())) { // filter all events
                                                                                                    // not belonging to
                                                                                                    // bluez
                                return;
                            }

                            // if (_s.get)
                            System.err.println("PropertiesChanged:----> " + _s.getPropertiesChanged());
                            if (!_s.getPropertiesRemoved().isEmpty())
                                System.err.println("PropertiesRemoved:----> " + _s.getPropertiesRemoved());
                        }
                    }

                });
    }

    @Override
    public boolean isRemote() {
        return false;
    }

    @Override
    public String getObjectPath() {
        return "/" + getClass().getName().replace(".", "/");
    }

    @Override
    public Map<DBusPath, Map<String, Map<String, Variant<?>>>> GetManagedObjects() {
        System.out.println("GetManagedObjects for the application Called");
        Map<DBusPath, Map<String, Map<String, Variant<?>>>> outerMap = new HashMap<>();

        outerMap.put(new DBusPath(service.getObjectPath()), service.getProperties());
	
	//let's share the characteristics as well
	List<LECharacteristic> list = service.getCharacteristics();
	for(LECharacteristic chars: list)
		outerMap.put(chars.getDBusPath(),chars.getProperties());
        return outerMap;
    }

    protected void scan(int _i) {
        System.out.println("Scanning for " + _i + " seconds");
        Adapter1 adapter = null;
        try {
            adapter = connection.getRemoteObject("org.bluez", "/org/bluez/hci0", Adapter1.class);
            adapter.StartDiscovery();
            Thread.sleep(_i * 1000);

        } catch (DBusException | InterruptedException _ex) {
            // TODO Auto-generated catch block
            _ex.printStackTrace();
        } finally {
            if (adapter != null) {
                try {
                    adapter.StopDiscovery();
                } catch (BluezNotReadyException | BluezFailedException | BluezNotAuthorizedException _ex) {
                    // TODO Auto-generated catch block
                    _ex.printStackTrace();
                }
            }
        }
        System.out.println("Scanning for finished");
    }

    /*
     * =================================================================
     *
     * STATIC STUFF
     *
     * =================================================================
     */

    public static void main(String[] args) {
        Thread thread = new Thread("MyThread") {
            private boolean running = true;

            @Override
            public void run() {
                System.out.println("Init");
                LEApplication LEApplication = null;
                try {
                    LEApplication = new LEApplication();
                    System.out.println("Registering");
                    LEApplication.register();
                    System.out.println("Waiting");
                    while (running) {
                        try {
                            Thread.sleep(500L);
                        } catch (InterruptedException _ex) {
                            running = false;
                        }
                    }
                } catch (Exception _ex) {
                    // TODO Auto-generated catch block
                    _ex.printStackTrace();
                } finally {
                    running = false;
                    System.out.println("Terminating");
                    if (LEApplication != null) {
                        LEApplication.connection.disconnect();
                    }
                }

            }

        };

        thread.start();
    }


    static class GattProfile1Impl implements GattProfile1, Properties {
        private boolean                              released;
        private String                               path;

        private Map<String, Map<String, Variant<?>>> properties = new HashMap<>();

        public GattProfile1Impl(String _path) {
            released = false;
            path = _path;

            Map<String, Variant<?>> map = new HashMap<>();
            map.put("UUIDs", new Variant<>(new String[] {
                    "0000ffb0-0000-1000-8000-00805f9b34fb"
            }));

            properties.put(GattProfile1.class.getName(), map);
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        public Map<String, Map<String, Variant<?>>> getProperties() {
            return properties;
        }

        @Override
        public String getObjectPath() {
            return path;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void Release() {
            released = true;
        }

        public boolean isReleased() {
            System.out.println("released called");
            return released;
        }

        @Override
        public <A> A Get(String _interface_name, String _property_name) {
            System.out.println("Get called");
            // Variant<?> variant = properties.get(_interface_name).get(_property_name);
            return null; //
        }

        @Override
        public <A> void Set(String _interface_name, String _property_name, A _value) {
            System.out.println("Set called");

        }

        @Override
        public Map<String, Variant<?>> GetAll(String _interface_name) {
            System.out.println("queried for: " + _interface_name);
            return properties.get(_interface_name);
        }

    }

}

