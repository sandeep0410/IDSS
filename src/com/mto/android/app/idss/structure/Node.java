package com.mto.android.app.idss.structure;

import com.google.android.gms.maps.model.LatLng;

public class Node{
	LatLng value;
	Node next;	

	public Node(LatLng value){
		this.value = value;
	}
}
