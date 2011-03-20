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
import android.widget.Button;

import static com.leafdigital.humidity.HumidityActivity.*;

public class SaveReadingActivity extends Activity
{
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
    setContentView(R.layout.savereading);

    int[] ids = { R.id.stored1, R.id.stored2, R.id.stored3,
    	R.id.stored4, R.id.stored5, R.id.stored6 };
    for(int i=0; i<6; i++)
    {
    	final int storeNum = i;
    	Button storedButton = (Button)findViewById(ids[i]);
    	storedButton.setText(HumidityActivity.getBankName(this, i));
			storedButton.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
		  		Intent intent = new Intent();
		  		intent.putExtra(EXTRA_TEMP, getIntent().getDoubleExtra(EXTRA_TEMP, 0.0));
		  		intent.putExtra(EXTRA_TEMP_ERROR, getIntent().getFloatExtra(EXTRA_TEMP_ERROR, 0.0f));
		  		intent.putExtra(EXTRA_HUMIDITY, getIntent().getDoubleExtra(EXTRA_HUMIDITY, 0.0));
		  		intent.putExtra(EXTRA_HUMIDITY_ERROR, getIntent().getFloatExtra(EXTRA_HUMIDITY_ERROR, 0.0f));
		  		intent.putExtra(EXTRA_SAVEBANK, storeNum);
					setResult(RESULT_OK, intent);
					finish();
				}
			});
    }
	}

}
