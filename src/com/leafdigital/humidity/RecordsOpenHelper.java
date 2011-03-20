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

import android.content.Context;
import android.database.sqlite.*;

public class RecordsOpenHelper extends SQLiteOpenHelper
{
	private static final int DATABASE_VERSION = 2;

  RecordsOpenHelper(Context context)
  {
      super(context, "records", null, DATABASE_VERSION);
  }

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE readings(bank INTEGER, javatime INTEGER, temperature REAL, "
			+ "temperature_error REAL, humidity REAL, humidity_error REAL)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		if(oldVersion < 2)
		{
			db.beginTransaction();
			db.execSQL("CREATE TEMP TABLE oldreadings AS SELECT bank, javatime, temperature, "
				+ "humidity, dewpoint, density FROM readings");
			db.execSQL("DROP TABLE readings");
			db.execSQL("CREATE TABLE readings(bank INTEGER, javatime INTEGER, temperature REAL, "
				+ "temperature_error REAL, humidity REAL, humidity_error REAL)");
			db.execSQL("INSERT INTO readings(bank, javatime, "
				+ "temperature, temperature_error, humidity, humidity_error) SELECT "
				+ "bank, javatime, temperature, 1.0, humidity, 4.0 FROM oldreadings");
			db.execSQL("DROP TABLE oldreadings");
			db.setTransactionSuccessful();
			db.endTransaction();
		}
	}
}
