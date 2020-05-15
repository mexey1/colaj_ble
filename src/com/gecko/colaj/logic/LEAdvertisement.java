package com.gecko.colaj.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bluez.LEAdvertisement1;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.types.Variant;
import org.freedesktop.dbus.exceptions.DBusException;

import com.gecko.colaj.util.Utils;

public class LEAdvertisement implements LEAdvertisement1, Properties{
	public static final String ADVERTISEMENT_TYPE_PERIPHERAL = "peripheral";
	public static final String ADVERTISEMENT_TYPE_BROADCAST = "broadcast";

	private static final String LE_ADVERTISEMENT_IFACE = "org.bluez.LEAdvertisement1";
	private static final String TYPE_KEY="Type";
	private static final String SERVICE_UUID_KEY ="ServiceUUIDs";
	private static final String SOLICIT_UUID_KEY = "SolicitUUIDs";
	private static final String MANUFACTURER_DATA_KEY="ManufacturerData";
	private static final String SERVICE_DATA_KEY = "ServiceData";
	private static final String TX_POWER_KEY="IncludeTxPower";

	private String type;
	private List<String> servicesUUIDs;
	private Map<Integer, Integer> manufacturerData;
	private List<String> solicitUUIDs;
	private Map<String, String> serviceData;
	private boolean includeTxPower = true;
	private String path;


	/**
	 *
	 * @param type
	 * @param path: absolute path of the advertisement
	 */
	public LEAdvertisement(String type, String path) {
		this.type = type;
		this.path = path;
		servicesUUIDs = new ArrayList<>();
		//serviceData = new HashMap<>();
	}

	public void addService(LEService service) {
		servicesUUIDs.add(service.getUUID());
	}

	protected void export(DBusConnection dbusConnection) throws DBusException {
		dbusConnection.exportObject(this.getPath().toString(), this);
	}

	/**
	 * Return the Path (dbus class)
	 * @return
	 */
	public DBusPath getPath() {
		return new DBusPath(path);
	}

	public Map<String, Map<String, Variant<?>>> getProperties() {
		System.out.println("Advertisement -> getAdvertisementProperties");

		Map<String, Variant<?>> advertisementMap = new HashMap<>();

		Variant<String> type = new Variant<String>(this.type);
		advertisementMap.put(TYPE_KEY, type);

		Variant<String[]> serviceUUIDs = new Variant<String[]>(Utils.getStringListAsArray(this.servicesUUIDs));
		advertisementMap.put(SERVICE_UUID_KEY, serviceUUIDs);
		
		/*Variant<Boolean> discoverable = new Variant<Boolean>(true);
                advertisementMap.put("Discoverable", discoverable);

		Variant<Integer> dTimeout = new Variant<Integer>(0);
                advertisementMap.put("DiscoverableTimeout", dTimeout);*/

		if(solicitUUIDs != null) {
			Variant<String[]> solicitUUIDs = new Variant<String[]>(Utils.getStringListAsArray(this.solicitUUIDs));
			advertisementMap.put(SOLICIT_UUID_KEY, solicitUUIDs);
		}
		if(manufacturerData != null) {
			Variant<Map<Integer, Integer>> manufacturerData = new Variant<Map<Integer, Integer>>(this.manufacturerData);
			advertisementMap.put(MANUFACTURER_DATA_KEY, manufacturerData);
		}
		if(serviceData != null) {
			//serviceData.put("2D266186-03FB-47C2-8D9F-10B8EC891363","he");
			Variant<Map<String, String>> serviceDataVariant = new Variant<>(this.serviceData);
			advertisementMap.put(SERVICE_DATA_KEY, serviceDataVariant);
		}


		Variant<Boolean> includeTxPower = new Variant<Boolean>(this.includeTxPower);
		advertisementMap.put(TX_POWER_KEY, includeTxPower);

		Map<String, Map<String, Variant<?>>> externalMap = new HashMap<>();
		externalMap.put(LE_ADVERTISEMENT_IFACE, advertisementMap);
		System.out.println("Retrieved advertisement props");
		return externalMap;
	}

	@Override
	public boolean isRemote() { return false; }

	@Override
	public void Release() {
		// TODO Auto-generated method stub
		System.out.println("LE Advertisement Release called !!");
	}

	@Override
	public <A> A Get(String arg0, String arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <A> void Set(String arg0, String arg1, A arg2) {
		// TODO Auto-generated method stub
	}

	@Override
	public Map<String, Variant<?>> GetAll(String interfaceName) {
		if(LE_ADVERTISEMENT_IFACE.equals(interfaceName)) {
			return this.getProperties().get(LE_ADVERTISEMENT_IFACE);
		}
		throw new RuntimeException("Unknown LE Advertisement Interface " + interfaceName + "]");
	}

	@Override
	public String getObjectPath(){
		return path; 
	}


}
