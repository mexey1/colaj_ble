package org.bluez;

import java.util.Map;

import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.ObjectManager;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.types.Variant;

public interface GattApplication1 extends ObjectManager {
	public Map<DBusPath, Map<String, Map<String, Variant<?>>>> GetManagedObjects();
	public String getObjectPath();
	public boolean isRemote();

}
