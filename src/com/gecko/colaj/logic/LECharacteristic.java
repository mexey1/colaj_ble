package com.gecko.colaj.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.bluez.GattCharacteristic1;

import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.dbus.types.UInt16;

import com.github.hypfvieh.bluetooth.wrapper.BluetoothGattCharacteristic;
import java.io.FileDescriptor;
import org.bluez.datatypes.TwoTuple;

import com.gecko.colaj.util.Utils;
/***
 *This class represents a single peripheral property that can either be read, written to or notified.
 */

public class LECharacteristic implements GattCharacteristic1,Properties{
	private static final String GATT_IFACE_PROPERTY = "org.bluez.GattCharacteristic1";
	private static final String SERVICE_PROPERTY ="Service";
       	private static final String UUID="UUID";
	private static final String FLAGS="Flags";
	private static final String DESCRIPTORS="Descriptors";
	private static final String VALUES = "Value";
	public static final String READ="read";
	public static final String WRITE="write";
	public static final String NOTIFY="notify";

	private byte[] value = null;
	private LEService service;
	private String uuid;
	private List<String> flags = new ArrayList<>();
	private String path;
	private boolean isNotifying;
	private LECharacteristicListener listener;	
	private DBusPath dbusPath = null;
	private LEApplication application;

	public LECharacteristic(String path,String uuid,LEService service,LEApplication app){
		this.service = service;
		this.application = app;
		this.path = path;
		this.uuid = uuid;
		dbusPath = new DBusPath(path);
	}

	public DBusPath getDBusPath(){
		return dbusPath;
	}

	public void setFlag(String flag){
		flags.add(flag);
	}
	/**
	 *method called to retrieve the value of this characteristic
	 */
	public String getValue(){
		return new String(value);	
	}

	public void setValue(String value){
		this.value = value.getBytes();
		StopNotify();
		StartNotify();
	}
	public void export(DBusConnection connection)throws DBusException{
		System.out.println("Exporting characteristic "+this +" at path "+path);
		connection.exportObject(path,this);
	}

	public Map<String, Map<String, Variant<?>>> getProperties(){
		System.out.println("Gatt characteristic property");
		Map<String,Variant<?>> options = new HashMap<>();

		Variant<DBusPath> pathProperty = new Variant<>(service.getDBusPath());
		options.put(SERVICE_PROPERTY,pathProperty);

		Variant<String> uuidProperty = new Variant<>(uuid);
		options.put(UUID,uuidProperty);

		Variant<String[]> flagsProperty = new Variant<>(Utils.getStringListAsArray(flags));
		options.put(FLAGS,flagsProperty);

		//Variant<Boolean> valuesProperty = new Variant<>(new Boolean(true));
                //options.put("Notifying",valuesProperty);

		Map<String, Map<String, Variant<?>>> extOptions = new HashMap<>();
		extOptions.put(GATT_IFACE_PROPERTY,options);
		
		System.out.println("Characteristics values "+options);
		return extOptions;

	}

	/**
	 *method called when the central requests the value of this characteristic
	 */
	@Override 
	public byte[] ReadValue(Map<String,Variant<?>> opts){
		int offset =0;
		if(opts.get("offset")!=null)
		{
			Object temp =opts.get("offset").getValue();
			offset = (temp instanceof UInt16)?((UInt16)temp).intValue():0;
		}
		byte value[] = this.value;
		byte piece[] = Arrays.copyOfRange(value,offset,value.length);
		return piece;	
	}

	/**
	 *this method is called when the central wants to write to this value
	 */
	@Override
	public void WriteValue(byte[] value, Map<String, Variant<?>> option){
		System.out.println("Writing value "+ (new String(value)) +" with options "+option);
		//listener.setValue(vaalue);
		this.value = value;
		if(uuid.equalsIgnoreCase("12345679-02FB-47C2-8D9F-10B8EC891363"))//if this is the password characteristic, then we can attempt login
			application.wifiConnect();
	}

	@Override
	public void StartNotify() {
		try{	
			System.out.println("Attempting to notify "+path);
			if(isNotifying) {
                        System.out.println("Characteristic is  already notifying");
                        return;
                	}
                	this.isNotifying = true;
                	Map<String,Variant<?>> props = new HashMap<>();
                	Variant<?> valuesProperty = new Variant<>(value);
                	props.put(VALUES,valuesProperty);
                	PropertiesChanged propsChgd = new PropertiesChanged(path,GATT_IFACE_PROPERTY,props,new java.util.ArrayList<String>());
			DBusConnection.getConnection(DBusConnection.DBusBusType.SYSTEM).sendMessage(propsChgd);
			//new BluetoothGattCharacteristic(this,service,path,DBusConnection.getConnection(DBusConnection.DBusBusType.SYSTEM)).startNotify();
			System.out.println("Notification sent");
		 }
		catch(DBusException e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void StopNotify() {
		if(!isNotifying) {
			System.out.println("Characteristic is already not notifying");
			return;
		}
		this.isNotifying = false;
	}

	@Override
	public <A> A Get(String interface_name, String property_name) {
		// TODO Auto-generated method stub

		return null;
	}

	@Override
	public <A> void Set(String interface_name, String property_name, A value) {
		// TODO Auto-generated method stub
	}

	@Override
	public Map<String, Variant<?>> GetAll(String ifaceName) {
		if(GATT_IFACE_PROPERTY.equals(ifaceName)) {
			return this.getProperties().get(GATT_IFACE_PROPERTY);
		}
		throw new RuntimeException("unknown interface:" + ifaceName + "]");
	}

	@Override
	public void Confirm(){

	}

	@Override
	public TwoTuple<FileDescriptor,UInt16> AcquireNotify(Map<String,Variant<?>> _options){
		return null;
	}

	
	@Override
	public TwoTuple<FileDescriptor,UInt16> AcquireWrite(Map<String,Variant<?>> _options){
		return null;
	}

	@Override
	public String getObjectPath(){
		return path;
	}

	@Override
	public boolean isRemote(){
		return false;
	}
}

