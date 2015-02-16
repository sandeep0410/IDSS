package com.mto.android.app.idss.structure;

import com.google.android.gms.maps.model.LatLng;



public class FixedSizeLinkedQueue{		
	//New item added at tail and removal takes place from head

	private Node head;
	private Node tail;		

	public FixedSizeLinkedQueue(Node node) {
		this.head = node;
		this.tail = node;
	}

	public FixedSizeLinkedQueue() {
		
	}

	public FixedSizeLinkedQueue(FixedSizeLinkedQueue f) {
		head = f.head;
		tail = f.tail;
	}

	public void addToList(Node node){
		if(null!=head && null != tail){
			tail.next = node;
			tail = node;
			
			while(getsize() > 10){
				head = head.next;
		}}
	}

	public LatLng getTenthlastvalue(){
		return head.value;
	}

	public LatLng getLatestValue(){
		return tail.value;
	}

	public int getsize() {
		int size=1;
		for(Node n=head; n.next!=null; n = n.next)
			size++;
		return size;
	}
	
	public void clear(){
		if(head!=null)
			head =null;
		if(tail != null)
			tail =null;
	}

}
