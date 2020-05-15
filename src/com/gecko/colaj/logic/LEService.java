package com.gecko.colaj.logic;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import org.bluez.GattService1;

import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.dbus.exceptions.DBusException;

import com.gecko.colaj.util.Utils;

/**
 *This class represents a service that would be broadcast by the BLE adapter
 */

public class LEService implements GattService1,Properties{
	
	private static final String GATT_SERVICE_IFACE = "org.bluez.GattService1";
	private static final String UUID_PROPERTY = "UUID";
	private static final String PRIMARY_INDICATOR = "Primary";
	private static final String CHARACTERISTIC_PROPERTY="Characteristics";

	private String uuid;
	private boolean primaryInd = true;
	private List<LECharacteristic> serviceCharacteristics = new ArrayList<>();
	private List<DBusPath> characteristPaths = new ArrayList<>();
        private String path;

	public LEService(String path, String uuid, boolean isPrimary){
		this.uuid = uuid;
		this.primaryInd = isPrimary;
		this.path = path;
	}	

	public String getUUID(){
		return uuid;
	}

	public void addCharacteristic(LECharacteristic characteristic){
		serviceCharacteristics.add(characteristic);
		characteristPaths.add(characteristic.getDBusPath());
	}

	public void removeCharacteristic(LECharacteristic characteristic){
		serviceCharacteristics.remove(characteristic);
	        characteristPaths.add(characteristic.getDBusPath());

	}

	public List<LECharacteristic> getCharacteristics(){
		return serviceCharacteristics;
	}

	public void export(DBusConnection connection)throws DBusException {
		System.out.println("Exporting service "+this +" at path "+path);
		for(LECharacteristic chars: serviceCharacteristics)
			chars.export(connection);
		//connection.exportObject(path,this);
	}

	public DBusPath getDBusPath(){
		return new DBusPath(path);
	}

	public String getPath(){
		return path;
	}

	public Map<String,Map<String,Variant<?>>> getProperties(){
		System.out.println("Retrieving properties for Service");
		Map<String,Variant<?>> options = new HashMap<>();
		
		DBusPath characteristicPathArray[] = characteristPaths.toArray(new DBusPath[1]);

		Variant<String> uuid = new Variant<String>(this.uuid);
		options.put(UUID_PROPERTY,uuid);

		Variant<Boolean> primary = new Variant(new Boolean(true));
                options.put(PRIMARY_INDICATOR,primary);

		System.out.println("converting list to array "+ Utils.convertListToArray(characteristPaths));
		System.out.println("converting list to array "+ (characteristicPathArray instanceof DBusPath[]));
		Variant<DBusPath[]> pathArray = new Variant<>(characteristicPathArray);
		options.put(CHARACTERISTIC_PROPERTY,pathArray);
		System.out.println("converting list to array "+ (Utils.convertListToArray(characteristPaths) instanceof DBusPath[]));

		Map<String,Map<String,Variant<?>>> propsMap = new HashMap<>();
		propsMap.put(GATT_SERVICE_IFACE,options);
		System.out.println("Service Properties retrieved " +options);
		return propsMap;
	}

	private LECharacteristic[] getCharacteristicAsArray(){
		LECharacteristic array [] = new LECharacteristic[serviceCharacteristics.size()];
		return serviceCharacteristics.toArray(array);
	}

	@Override
	public <A> A Get(String interface_name, String property_name){
	 	// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <A> void Set(String interface_name, String property_name, A value) {
		// TODO Auto-generated method stub
	}

	@Override 
	public Map<String,Variant<?>> GetAll(String iface){
		if(iface.equals(GATT_SERVICE_IFACE))
			return getProperties().get(iface);
		throw new RuntimeException("Couldn't find interface "+iface);
	}

	@Override
	public boolean isRemote(){
		return false;
	}

	@Override
	public String getObjectPath(){
		return path;
	}



}
