package com.gecko.colaj.logic;

/**
 * Interface that describe the source of the data source of one Characteristic.
 * 
 *
 */
public interface LECharacteristicListener {
	public byte[] getValue();
	public void setValue(byte[] value);
}
