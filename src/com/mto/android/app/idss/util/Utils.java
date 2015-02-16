package com.mto.android.app.idss.util;

import java.util.Calendar;

import android.util.Log;



public class Utils {	
	private volatile static boolean isTTsactive = true;
	private volatile static int look_ahead = 9;
	private volatile static int TTS_Frequency = 0;
	private volatile static String travelling_date = initialize_date();
	private static int month = -1;
	private static int year =-1;
	private static int day = -1;
	private static int hour = -1;
	private static int min = -1;


	public static void activateTTS(boolean activate){
		isTTsactive = activate;		
	}

	private static String initialize_date() {


		return travelling_date;

	}

	public static boolean isTTSactivated(){
		return isTTsactive;
	}

	public static void setLookAheadMiles(int dist){
		look_ahead=dist;
	}
	public static int getLookAheadMiles(){
		return look_ahead;
	}
	public static void setTTSfrequency(int frequency){
		TTS_Frequency = frequency;
	}
	public static int getTTSfrequency(){
		return TTS_Frequency;
	}


	public static int getDelaySpeechinSecs(){
		switch(TTS_Frequency){
		case 0:
			return 15000;
		case 1: 
			return 30000;
		case 2:
			return 45000;
		case 3:		
			return 60000;
		case 4:
			return 120000;
		case 5:
			return 180000;
		case 6:
			return 300000;

		}
		return 0;

	}

	public static int getLookAheadDistance(){
		return look_ahead+1;
	}

	public static int getCalendar(int field){
		Calendar cal = Calendar.getInstance();
		int lmonth = cal.get(Calendar.MONTH);
		int lyear = cal.get(Calendar.YEAR);
		int lday = cal.get(Calendar.DAY_OF_MONTH);
		int lhour = cal.get(Calendar.HOUR_OF_DAY);
		int lmin = cal.get(Calendar.MINUTE);
		Log.d("sandeep" , " " +lyear +" " +lmonth +" " +lday);
		if(field == Calendar.YEAR){
			if(year == -1){
				return lyear;
			}else 
				return year;
		}else if(field == Calendar.MONTH){
			if(month == -1){
				return lmonth;
			}else 
				return month;
		}else if(field == Calendar.DAY_OF_MONTH){
			if(day == -1){
				return lday;
			}else 
				return day;
		}else if(field == Calendar.HOUR_OF_DAY){
			if(hour == -1){
				return lhour;
			}else 
				return hour;
		}else if(field == Calendar.MINUTE){
			if(min == -1){
				return lmin;
			}else 
				return min;
		}

		return 0;

	}

	public static void setcalendar(int field, int value){
		if(field == Calendar.YEAR)
			year = value;
		else if(field == Calendar.MONTH)
			month = value;
		else if(field == Calendar.DAY_OF_MONTH)
			day = value;
		else if(field == Calendar.HOUR_OF_DAY)
			hour = value;
		else if(field == Calendar.MINUTE)
			min = value;

	}

	public static void resetSettings(){
		isTTsactive = true;
		look_ahead = 9;
		TTS_Frequency = 0;
		travelling_date = initialize_date();
		month = -1;
		year =-1;
		day = -1;
		hour = -1;
		min = -1;

	}
}
