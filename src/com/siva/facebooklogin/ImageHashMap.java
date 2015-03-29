package com.siva.facebooklogin;

import java.util.HashMap;

import android.graphics.Bitmap;

public class ImageHashMap extends HashMap<String, Bitmap>  {
	
	MappingInterface mMappingInterface;
	
	public void setAddListener(MappingInterface mappingInterface)
	{
		mMappingInterface = mappingInterface;
	}
	
	@Override
	public Bitmap put(String key, Bitmap value) {
		// TODO Auto-generated method stub
		addMarker(key, value);
		return super.put(key, value);
	}

	
	public void addMarker(String key, Bitmap value) {
		// TODO Auto-generated method stub
		mMappingInterface.addMarker(key, value);
		
	}


}
