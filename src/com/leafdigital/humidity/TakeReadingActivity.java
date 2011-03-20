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
import android.content.*;
import android.os.Bundle;
import android.text.*;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;

import static com.leafdigital.humidity.HumidityActivity.*;

public class TakeReadingActivity extends Activity
{
	private final static String DECIMAL_REGEX = "[0-9]+(\\.[0-9]+)?";

	private final static String PREFS_TEMP_ERROR = "tempError",
		PREFS_HUMIDITY_ERROR = "humidityError";

	private Button ok;
	private EditText tempEdit, humidityEdit, tempErrorEdit, humidityErrorEdit;

	/**
	 * Rounds a value for display. This rounds to two decimal places, but then
	 * discards the decimal places if not used, so that it can display results
	 * of the form 0.97, 0.9, and 0.
	 * @param value Value
	 * @return String
	 */
	private static String roundValue(float value)
	{
		String s = String.format("%.2f", value);
		while(s.endsWith("0"))
		{
			s = s.substring(0, s.length() - 1);
		}
		if(s.endsWith("."))
		{
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.takereading);

    ok = (Button)findViewById(R.id.ok);
    tempEdit = (EditText)findViewById(R.id.temp);
    tempErrorEdit = (EditText)findViewById(R.id.temperror);
    humidityEdit = (EditText)findViewById(R.id.humidity);
    humidityErrorEdit = (EditText)findViewById(R.id.humidityerror);

    // Set default error values
    SharedPreferences prefs = getSharedPreferences("com.leafdigital.humidity",MODE_PRIVATE);
    tempErrorEdit.setText(roundValue(prefs.getFloat(PREFS_TEMP_ERROR, 1.0f)));
    humidityErrorEdit.setText(roundValue(prefs.getFloat(PREFS_HUMIDITY_ERROR, 4.0f)));

    TextWatcher watcher = new TextWatcher()
    {
			@Override
			public void afterTextChanged(Editable arg0)
			{
				textChanged();
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3)
			{
			}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3)
			{
			}
    };
    tempEdit.addTextChangedListener(watcher);
    tempErrorEdit.addTextChangedListener(watcher);
    humidityEdit.addTextChangedListener(watcher);
    humidityErrorEdit.addTextChangedListener(watcher);

    ok.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
	  		// User has entered a reading
	  		double temp = Double.parseDouble(tempEdit.getText().toString());
	  		double humidity = Double.parseDouble(humidityEdit.getText().toString());

	  		float tempError = Float.parseFloat(tempErrorEdit.getText().toString());
	  		float humidityError = Float.parseFloat(humidityErrorEdit.getText().toString());

	  		// Update preferences to track the error
	  		SharedPreferences prefs = getSharedPreferences("com.leafdigital.humidity",MODE_PRIVATE);
	  		if(prefs.getFloat(PREFS_TEMP_ERROR, 1.0f) != tempError
	  			|| prefs.getFloat(PREFS_HUMIDITY_ERROR, 1.0f) != humidityError)
	  		{
	  			SharedPreferences.Editor editor = prefs.edit();
	  			editor.putFloat(PREFS_TEMP_ERROR, tempError);
	  			editor.putFloat(PREFS_HUMIDITY_ERROR, humidityError);
	  			editor.commit();
	  		}

				Intent intent = new Intent(TakeReadingActivity.this, ShowReadingActivity.class);
				intent.putExtra(EXTRA_TEMP, temp);
				intent.putExtra(EXTRA_TEMP_ERROR, tempError);
				intent.putExtra(EXTRA_HUMIDITY, humidity);
				intent.putExtra(EXTRA_HUMIDITY_ERROR, humidityError);

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

  private void textChanged()
  {
  	String tempString = tempEdit.getText().toString();
		String tempErrorString = tempErrorEdit.getText().toString();
		String humidityString = humidityEdit.getText().toString();
		String humidityErrorString = humidityErrorEdit.getText().toString();
		ok.setEnabled(
  		tempString.matches(DECIMAL_REGEX)
  		&& tempErrorString.matches(DECIMAL_REGEX)
  		&& humidityString.matches(DECIMAL_REGEX)
  		&& humidityErrorString.matches(DECIMAL_REGEX)
  		&& Double.parseDouble(tempString) <= 40.0
  		&& Double.parseDouble(tempErrorString) <= 10.0
  		&& Double.parseDouble(humidityString) <= 100.0
  		&& Double.parseDouble(humidityString) >= 1.0
  		&& Double.parseDouble(humidityErrorString) <= 10.0);
  }
}
