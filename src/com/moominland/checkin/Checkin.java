package com.moominland.checkin;

import java.io.UnsupportedEncodingException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Vibrator;
import android.util.Log;
import android.widget.ImageView;

import android.widget.Toast;

public class Checkin extends Activity {
	private static final String TAG = "Checkin";
	private static final String FRIDGE = "Fridge";
	private static final String BOARDROOM = "Boardroom";
	private static final String TRACEYS_OFFCE = "Traceys Office";
	private NfcAdapter nfcAdapter;
	private PendingIntent pendingIntent;
	private IntentFilter[] intentFilters;
	
	private boolean fridgeCheckedIn = false;
	private boolean boardRoomCheckedIn = false;
	private boolean tracysOfficeCheckedIn = false;;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
		try {
			ndef.addDataType("text/chariot");
		} catch (MalformedMimeTypeException e) {
		}
		intentFilters = new IntentFilter[] { ndef };
		
	}

	@Override
	public void onNewIntent(Intent intent) {
		Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(100);

		Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		Log.i(TAG, Integer.toString(rawMsgs.length));
		String readMessage = readRawMsgs(rawMsgs);
		
		if (readMessage.equalsIgnoreCase(FRIDGE)) {
			checkinAtFridge();
		} else if (readMessage.equalsIgnoreCase(TRACEYS_OFFCE)) {
			checkinAtTracysOffice();
		} else if (readMessage.equalsIgnoreCase(BOARDROOM)) {
			checkinAtBoardroom();
		} else {
			toast(readMessage + " is an unknown location");
		}
		
		checkForAllCheckIns();
	}

	private void checkinAtFridge() {
		fridgeCheckedIn = true;
		toast("Checked in at the fridge");
		updateUIForId(R.id.fridgeStar);
	}
	
	private void checkinAtBoardroom() {
		boardRoomCheckedIn = true;
		toast("Checked in at the boardroom");
		updateUIForId(R.id.boardroomStar);
	}
	private void checkinAtTracysOffice() {
		tracysOfficeCheckedIn = true;
		toast("Checked in at Tracey's office");
		updateUIForId(R.id.traceyStar);
	}

	private void updateUIForId(int id) {
		ImageView imageView = (ImageView) findViewById(id);
		imageView.setImageResource(android.R.drawable.btn_star_big_on);
	}
	
	private void resetUIForId(int id) {
		ImageView imageView = (ImageView) findViewById(id);
		imageView.setImageResource(android.R.drawable.btn_star_big_off);
	}

	private void toast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	private void checkForAllCheckIns() {
		if (fridgeCheckedIn && tracysOfficeCheckedIn && boardRoomCheckedIn) {
			Toast.makeText(this, "Woohoo!\nYou won a soda!", Toast.LENGTH_LONG).show();
			resetCheckIns();
		}
	}

	private void resetCheckIns() {
		fridgeCheckedIn = false;
		tracysOfficeCheckedIn = false;
		boardRoomCheckedIn = false;
		
		resetUIForId(R.id.traceyStar);
		resetUIForId(R.id.boardroomStar);
		resetUIForId(R.id.fridgeStar);
	}

	private String readRawMsgs(Parcelable[] rawMsgs) {
		NdefMessage[] msgs = null;
		//build Ndef Messages
		if (rawMsgs != null) {
			msgs = new NdefMessage[rawMsgs.length];
			for (int i = 0; i < rawMsgs.length; i++) {
				msgs[i] = (NdefMessage) rawMsgs[i];
			}
		}
		StringBuffer sb = new StringBuffer();
		if (msgs != null) {
			for (NdefMessage ndefMessage : msgs) {
				for (NdefRecord record : ndefMessage.getRecords()) {
					Log.i(TAG, parse(record));
					sb.append(parse(record));
				}
			}
		}
		
		return sb.toString();
	}

	private String parse(NdefRecord record) {
		try {
			byte[] payload = record.getPayload();
			/*
			 * payload[0] contains the "Status Byte Encodings" field, per the
			 * NFC Forum "Text Record Type Definition" section 3.2.1.
			 * 
			 * bit7 is the Text Encoding Field.
			 * 
			 * if (Bit_7 == 0): The text is encoded in UTF-8 if (Bit_7 == 1):
			 * The text is encoded in UTF16
			 * 
			 * Bit_6 is reserved for future use and must be set to zero.
			 * 
			 * Bits 5 to 0 are the length of the IANA language code.
			 */
			String textEncoding = ((payload[0] & 0200) == 0) ? "UTF-8"
					: "UTF-16";
			//offset?
			int languageCodeLength = payload[0] & 0077;

			//String(byte[] data, int high, int offset, int byteCount) is deprecated...
			//String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
			return new String(payload, textEncoding);
		} catch (UnsupportedEncodingException e) {
			// should never happen unless we get a malformed tag.
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters,
				null);
	}

	@Override
	public void onPause() {
		super.onPause();
		nfcAdapter.disableForegroundDispatch(this);
	}
}