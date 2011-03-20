/*
This file is part of leafdigital Humidity tracker.

Humidity tracker is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Humidity tracker is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Humidity tracker. If not, see <http://www.gnu.org/licenses/>.

Copyright 2010 Samuel Marshall.
*/
package com.leafdigital.humidity;

import android.app.*;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class HumidityActivity extends Activity {
	public static final String EXTRA_HUMIDITY = "com.leafdigital.humidity.Humidity";
	public static final String EXTRA_TEMP = "com.leafdigital.humidity.Temp";
	public static final String EXTRA_HUMIDITY_ERROR = "com.leafdigital.humidity.HumidityError";
	public static final String EXTRA_TEMP_ERROR = "com.leafdigital.humidity.TempError";
	public static final String EXTRA_SAVEBANK = "com.leafdigital.humidity.SaveBank";

	public final static String PREFS_BANK_NAME = "bankName";

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
   	findViewById(R.id.reading).setOnClickListener(new OnClickListener()
    {
			@Override
			public void onClick(View v)
			{
				startActivityForResult(
					new Intent(HumidityActivity.this, TakeReadingActivity.class), 0);
			}
    });

    int[] ids = { R.id.stored1, R.id.stored2, R.id.stored3,
    	R.id.stored4, R.id.stored5, R.id.stored6 };
    for(int i=0; i<6; i++)
    {
    	final int storeNum = i;
    	Button storedButton = (Button)findViewById(ids[i]);
			storedButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
		  		Intent intent = new Intent(HumidityActivity.this, ShowRecordsActivity.class);
		  		intent.putExtra(EXTRA_SAVEBANK, storeNum);
		  		startActivity(intent);
				}
			});
    }
  }

  @Override
  protected void onResume()
  {
    int[] ids = { R.id.stored1, R.id.stored2, R.id.stored3,
    	R.id.stored4, R.id.stored5, R.id.stored6 };
    for(int i=0; i<6; i++)
    {
    	Button storedButton = (Button)findViewById(ids[i]);
    	storedButton.setText(HumidityActivity.getBankName(this, i));
    }
  	super.onResume();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data)
  {
  	// Ignore anything but okay
		if(resultCode != RESULT_OK)
		{
			return;
		}

		if(data != null && data.getIntExtra(EXTRA_SAVEBANK, -1) != -1)
		{
			save(
				data.getIntExtra(EXTRA_SAVEBANK, -1),
				System.currentTimeMillis(),
				data.getDoubleExtra(EXTRA_TEMP, 0.0),
				data.getFloatExtra(EXTRA_TEMP_ERROR, 0.0f),
				data.getDoubleExtra(EXTRA_HUMIDITY, 0.0),
				data.getFloatExtra(EXTRA_HUMIDITY_ERROR, 0.0f));

			Intent intent = new Intent(HumidityActivity.this, ShowRecordsActivity.class);
  		intent.putExtra(EXTRA_SAVEBANK, data.getIntExtra(EXTRA_SAVEBANK, -1));
  		startActivity(intent);
		}
  }

  private void save(int bank, long time, double temp, float tempError,
  	double humidity, float humidityError)
  {
  	SQLiteDatabase db = new RecordsOpenHelper(this).getWritableDatabase();
  	ContentValues values = new ContentValues();
  	values.put("bank", bank);
  	values.put("javatime", time);
  	values.put("temperature", temp);
  	values.put("humidity", humidity);
  	values.put("temperature_error", tempError);
  	values.put("humidity_error", humidityError);
  	db.insert("readings", null, values);
  	db.close();
  }

  public static String getBankName(Activity activity, int bank)
  {
  	return activity.getSharedPreferences("com.leafdigital.humidity", MODE_PRIVATE).getString(
  		PREFS_BANK_NAME + bank, "" + (bank+1));
  }
}