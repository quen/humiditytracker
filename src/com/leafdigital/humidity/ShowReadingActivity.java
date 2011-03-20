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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.leafdigital.humidity.ClimateData.FieldValue;

import static com.leafdigital.humidity.ClimateData.*;
import static com.leafdigital.humidity.HumidityActivity.*;

public class ShowReadingActivity extends Activity
{

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
    super.onCreate(savedInstanceState);
    setContentView(R.layout.showreading);

    double temp = getIntent().getDoubleExtra(EXTRA_TEMP, 0.0);
    float tempError = getIntent().getFloatExtra(EXTRA_TEMP_ERROR, 0.0f);
    double humidity = getIntent().getDoubleExtra(EXTRA_HUMIDITY, 0.0);
    float humidityError = getIntent().getFloatExtra(EXTRA_HUMIDITY_ERROR, 0.0f);

    ClimateData data = new ClimateData(temp, tempError, humidity, humidityError);

		FieldValue value = data.getField(FIELD_TEMPERATURE);
    ((TextView)findViewById(R.id.showTempL)).setText(
    	String.format("%.1f", value.getMin()));
    ((TextView)findViewById(R.id.showTempH)).setText(
    	String.format("%.1f", value.getMax()));

		value = data.getField(FIELD_HUMIDITY);
    ((TextView)findViewById(R.id.showHumidityL)).setText(
    	String.format("%.1f", value.getMin()));
    ((TextView)findViewById(R.id.showHumidityH)).setText(
    	String.format("%.1f", value.getMax()));

		value = data.getField(FIELD_DEWPOINT);
    ((TextView)findViewById(R.id.showDewpointL)).setText(
    	String.format("%.1f", value.getMin()));
    ((TextView)findViewById(R.id.showDewpointH)).setText(
    	String.format("%.1f", value.getMax()));

		value = data.getField(FIELD_DENSITY);
    ((TextView)findViewById(R.id.showDensityL)).setText(
    	String.format("%.2f", value.getMin()));
    ((TextView)findViewById(R.id.showDensityH)).setText(
    	String.format("%.2f", value.getMax()));

    findViewById(R.id.ok).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
		  	Intent intent = new Intent();
		  	intent.putExtra(EXTRA_SAVEBANK, -1);
				setResult(RESULT_OK, intent);
				finish();
			}
		});

    findViewById(R.id.save).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
	  		// User has selected to save the reading
	  		Intent intent = new Intent(ShowReadingActivity.this, SaveReadingActivity.class);
	  		intent.putExtra(EXTRA_TEMP, getIntent().getDoubleExtra(EXTRA_TEMP, 0.0));
	  		intent.putExtra(EXTRA_TEMP_ERROR, getIntent().getFloatExtra(EXTRA_TEMP_ERROR, 0.0f));
	  		intent.putExtra(EXTRA_HUMIDITY, getIntent().getDoubleExtra(EXTRA_HUMIDITY, 0.0));
	  		intent.putExtra(EXTRA_HUMIDITY_ERROR, getIntent().getFloatExtra(EXTRA_HUMIDITY_ERROR, 0.0f));
	  		startActivityForResult(intent, 0);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
  	// Pass result data through, and finish
		if(resultCode == RESULT_OK)
		{
	  	setResult(resultCode, data);
	  	finish();
		}
	}
}
