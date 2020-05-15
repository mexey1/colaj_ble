package com.gecko.colaj.util;

import java.util.List;
import com.gecko.colaj.logic.LECharacteristic;
public class Utils{


	 public static  LECharacteristic[] getCharacteristicsListAsArray(List<LECharacteristic> characteristics){
                LECharacteristic array [] = new LECharacteristic[characteristics.size()];
                return characteristics.toArray(array);
        }

	public static  String[] getStringListAsArray(List<String> string){
                String array [] = new String[string.size()];
                return string.toArray(array);
        }

	public static <T> T[] convertListToArray(List<T> list){
		T array [] =(T[]) new Object[list.size()];
		return list.toArray(array);
	}

}
