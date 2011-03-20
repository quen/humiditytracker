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

import java.text.SimpleDateFormat;
import java.util.Date;

import com.leafdigital.humidity.ClimateData.FieldValue;

import android.app.*;
import android.app.AlertDialog.Builder;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.*;
import android.os.Bundle;
import android.text.*;
import android.util.*;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.LinearLayout.LayoutParams;

import static com.leafdigital.humidity.ClimateData.*;
import static com.leafdigital.humidity.HumidityActivity.*;

public class ShowRecordsActivity extends Activity
{
	private int bank;
	private DataPoint[] data;
	private RecordsGraph graph;
	private int graphField;

	private int nearestPointIndex;

	private final static String PREFS_CURRENT_GRAPH = "currentGraph";

	public final static float[] MAX_RANGE = { 40.0f, 100.0f, 40.0f, 50.0f };
	public final static float[] RANGE_STEP = { 5.0f, 5.0f, 5.0f, 5.0f };
	public final static int[] FIELD_LABEL = { R.string.temp, R.string.humidity,
		R.string.dewpoint, R.string.density };
	public final static int[] FIELD_UNIT = { R.string.degreesc, R.string.percent,
		R.string.degreesc, R.string.gm3 };

	private final static float HIT_RADIUS = 48;
	private final static float GRAPH_PADDING = 10;

	private final static long ONE_DAY = 24L * 3600L * 1000L;

	private final static int DIALOG_POINT=1, DIALOG_NAME=2, DIALOG_DELETE=3;

	private static class DataPoint
	{
		long javaTime;
		ClimateData data;

		private DataPoint(long javaTime, ClimateData data)
		{
			this.javaTime = javaTime;
			this.data = data;
		}
	}

	private class RecordsGraph extends View
	{
		private float xScale, yScale, left, bottom, density;
		private long startTime;

		public RecordsGraph()
		{
			super(ShowRecordsActivity.this);
			setLayoutParams(new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1.0f));
		}

		@Override
		public boolean onTouchEvent(MotionEvent event)
		{
			switch(event.getAction())
			{
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_MOVE:
				updateNearestPoint(event.getX(), event.getY());
				return true;
			case MotionEvent.ACTION_UP:
				updateNearestPoint(event.getX(), event.getY());
				if(nearestPointIndex != -1)
				{
					showDialog(DIALOG_POINT);
				}
				nearestPointIndex = -1;
				invalidate();
				return true;
			}
			return false;
		}

		private void updateNearestPoint(float x, float y)
		{
			// Crap algorithm, but let's just try all the points within any kind of
			// range.
			float radius = HIT_RADIUS * density;
			float bestDiff = radius * radius;
			int bestIndex = -1;
			for(int i=0; i<data.length; i++)
			{
				// Get X point and check if within range
				float pointX = (float)(data[i].javaTime - startTime) * xScale + left;
				if (pointX > x + radius)
				{
					break;
				}
				if (pointX < x - radius)
				{
					continue;
				}

				// Find Y point
				FieldValue field = data[i].data.getField(graphField);
				float pointY = bottom - field.getMeasured() * yScale;

				// Work out difference (= distance squared)
				float diff = (pointY - y) * (pointY - y) + (pointX - x) * (pointX - x);
				if (diff < bestDiff)
				{
					bestDiff = diff;
					bestIndex = i;
				}
			}

			if(bestIndex != nearestPointIndex)
			{
				nearestPointIndex = bestIndex;
				invalidate();
			}
		}

		@Override
		protected void onDraw(Canvas canvas)
		{
			DisplayMetrics metrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(metrics);
			density = metrics.density;
			float blobRadius = 2f * density;
			float padding = GRAPH_PADDING * density;

			// Clear to off-black
			canvas.drawColor(Color.argb(255, 30, 30, 30));

			// Get available width and height for graph
			float w = getWidth() - padding*2, h = getHeight() - padding*2;
			left = padding;
			bottom = getHeight() - padding;

			// Draw axes in grey
			Paint grey = new Paint(Paint.ANTI_ALIAS_FLAG);
			grey.setColor(Color.argb(255, 128, 128, 128));
			canvas.drawLine(left, bottom, left+w, bottom, grey);
			canvas.drawLine(left, bottom, left, bottom-h, grey);
			canvas.drawLine(left, bottom-h, left+ 1.5f*blobRadius, bottom-h, grey);

			// Work out Y scaling
			float actualMax = 0;
			for(int i=0; i<data.length; i++)
			{
				float value = data[i].data.getField(graphField).getMax();
				if(value > actualMax)
				{
					actualMax = value;
				}
			}
			float range = MAX_RANGE[graphField];
			if(data.length == 0)
			{
				actualMax = range;
			}
			while(range - RANGE_STEP[graphField] > actualMax)
			{
				range -= RANGE_STEP[graphField];
			}

			yScale = h / range;

			// Draw scale text
			String max = String.format("%.0f", range);
			grey.setTextSize(12f * density);
			Rect bounds = new Rect();
			grey.getTextBounds(max, 0, max.length(), bounds);
			canvas.drawText(max, left + blobRadius * 2 * density,
				bottom - h - bounds.top, grey);

			// If there is no data, stop here
			if(data.length == 0)
			{
				return;
			}

			// Work out X scaling
			startTime = data[0].javaTime;
			long timeRange = data[data.length - 1].javaTime - startTime;
			if(timeRange == 0)
			{
				timeRange = 1;
			}
			xScale = w / (float)timeRange;

			// Draw each point
			Paint white = new Paint(Paint.ANTI_ALIAS_FLAG);
			white.setColor(Color.argb(255, 255, 255, 255));
			Paint faintLine = new Paint(Paint.ANTI_ALIAS_FLAG);
			faintLine.setColor(Color.argb(160, 0, 255, 0));
			for(int i=0; i<data.length; i++)
			{
				float x = (float)(data[i].javaTime - startTime) * xScale;
				FieldValue field = data[i].data.getField(graphField);
				float yMin = field.getMin() * yScale, yMax = field.getMax() * yScale;
				float y = field.getMeasured() * yScale;
				if (i==nearestPointIndex)
				{
					Paint faint = new Paint(Paint.ANTI_ALIAS_FLAG);
					faint.setColor(Color.argb(64,255,255,255));
					canvas.drawCircle(x + left, bottom - y, blobRadius*20, faint);
				}

				canvas.drawLine(x + left, bottom - yMin, x + left, bottom - yMax, faintLine);
				canvas.drawCircle(x + left, bottom - y, blobRadius, white);
			}
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
		{
			int w;
			if(MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED)
			{
				w = 100;
			}
			else
			{
				w = MeasureSpec.getSize(widthMeasureSpec);
			}

			int h;
			if(MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED)
			{
				h = 100;
			}
			else
			{
				h = MeasureSpec.getSize(heightMeasureSpec);
			}
			setMeasuredDimension(w, h);
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Get current bank and set title
		bank = getIntent().getIntExtra(HumidityActivity.EXTRA_SAVEBANK, -1);

		loadData();

    setContentView(R.layout.showrecords);
    LinearLayout layout = (LinearLayout)findViewById(R.id.layout);

    graph = new RecordsGraph();
    layout.addView(graph, 0);

    // Get default field
    setField(getSharedPreferences("com.leafdigital.humidity", MODE_PRIVATE).getInt(
    	PREFS_CURRENT_GRAPH, FIELD_HUMIDITY));
	}

	private void loadData()
	{
		// Load records. (Note: This uses a writable database because it might need
		// to do a db version update, so it has to be able to write)
		SQLiteDatabase db = new RecordsOpenHelper(this).getWritableDatabase();
		Cursor cursor = db.query(
			"readings", new String[] {  "javatime", "temperature", "temperature_error",
				"humidity", "humidity_error" },
			"bank = ?",  new String[] { bank + "" },  null, null, "javatime");
		int count = cursor.getCount();
		data = new DataPoint[count];
		for(int i=0; i<count; i++)
		{
			cursor.moveToNext();
			data[i] = new DataPoint(cursor.getLong(0), new ClimateData(cursor.getFloat(1),
				cursor.getFloat(2),cursor.getFloat(3),cursor.getFloat(4)));
		}
		cursor.close();
		db.close();

		// Clear other fields that depend on data
		nearestPointIndex = -1;
	}

	private void setField(int field)
	{
		this.graphField = field;
		setTitle(HumidityActivity.getBankName(this, bank) + " - " + getString(FIELD_LABEL[field])
			+ " (" + getString(FIELD_UNIT[field]) + ")");
		graph.invalidate();

		SharedPreferences prefs = getSharedPreferences("com.leafdigital.humidity", MODE_PRIVATE);
		if(prefs.getInt(PREFS_CURRENT_GRAPH, FIELD_HUMIDITY) != field)
		{
			if(field == FIELD_HUMIDITY)
			{
				prefs.edit().remove(PREFS_CURRENT_GRAPH).commit();
			}
			else
			{
				prefs.edit().putInt(PREFS_CURRENT_GRAPH, field).commit();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.showrecords, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		int id = -1;
		switch(graphField)
		{
		case FIELD_TEMPERATURE :
			id = R.id.temp;
			break;
		case FIELD_HUMIDITY :
			id = R.id.humidity;
			break;
		case FIELD_DEWPOINT :
			id = R.id.dewpoint;
			break;
		case FIELD_DENSITY :
			id = R.id.density;
			break;
		}
		menu.findItem(id).setChecked(true);

		menu.findItem(R.id.delete).setEnabled(data.length != 0);
		long now = System.currentTimeMillis();
		menu.findItem(R.id.deletebefore1d).setEnabled(data.length>0
			&& now - data[0].javaTime > 1 * ONE_DAY);
		menu.findItem(R.id.deletebefore7d).setEnabled(data.length>0
			&& now - data[0].javaTime > 7 * ONE_DAY);
		menu.findItem(R.id.deletebefore30d).setEnabled(data.length>0
			&& now - data[0].javaTime > 30 * ONE_DAY);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
		case R.id.temp :
			setField(FIELD_TEMPERATURE);
			return true;
		case R.id.humidity :
			setField(FIELD_HUMIDITY);
			return true;
		case R.id.dewpoint :
			setField(FIELD_DEWPOINT);
			return true;
		case R.id.density :
			setField(FIELD_DENSITY);
			return true;
		case R.id.name :
			showDialog(DIALOG_NAME);
			return true;
		case R.id.deletelast :
			deleteData(data[data.length-1].javaTime-1, getString(R.string.deletelast), true);
			return true;
		case R.id.deletebefore1d :
			deleteData(System.currentTimeMillis() - ONE_DAY, getString(R.string.deletebefore1d), false);
			return true;
		case R.id.deletebefore7d :
			deleteData(System.currentTimeMillis() - 7 * ONE_DAY, getString(R.string.deletebefore7d), false);
			return true;
		case R.id.deletebefore30d :
			deleteData(System.currentTimeMillis() - 30 * ONE_DAY, getString(R.string.deletebefore30d), false);
			return true;
		case R.id.deleteall :
			deleteData(System.currentTimeMillis() + 1, getString(R.string.deleteall), false);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id)
	{
		// Create dialog
		final Dialog dialog = new Dialog(this);
		switch(id)
		{
		case DIALOG_POINT :
			dialog.setContentView(R.layout.pointdialog);
			View ok = dialog.findViewById(R.id.ok);
			ok.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					dialog.cancel();
				}
			});
			break;

		case DIALOG_NAME :
			dialog.setContentView(R.layout.namedialog);
			dialog.setTitle(R.string.name);
			final EditText edit = (EditText)dialog.findViewById(R.id.name);
			final View okName = dialog.findViewById(R.id.ok);
			okName.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					String name = edit.getText().toString();
					SharedPreferences.Editor editor = getSharedPreferences("com.leafdigital.humidity",MODE_PRIVATE).edit();
					editor.putString(PREFS_BANK_NAME + bank, name);
					editor.commit();
					dialog.dismiss();
					setField(graphField);
				}
			});
			edit.addTextChangedListener(new TextWatcher()
			{
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count)
				{
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after)
				{
				}

				@Override
				public void afterTextChanged(Editable s)
				{
					int length = edit.getText().length();
					okName.setEnabled(length <= 6 && length >= 1);
				}
			});
			break;

		case DIALOG_DELETE:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(getString(R.string.confirmdelete));
			builder.setPositiveButton(getString(R.string.delete),
				new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						confirmDelete();
						dialog.dismiss();
					}
				});
			builder.setNegativeButton(getString(R.string.cancel),
				new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					dialog.cancel();
				}
			});
			builder.setTitle("Placeholder");
			return builder.create();
		}
		return dialog;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog)
	{
		switch(id)
		{
		case DIALOG_POINT :
			preparePointDialog(dialog);
			break;
		case DIALOG_NAME :
			prepareNameDialog(dialog);
			break;
		case DIALOG_DELETE :
			prepareDeleteDialog(dialog);
			break;
		}
	}

	private void preparePointDialog(Dialog dialog)
	{
		// Get data
		DataPoint point = data[nearestPointIndex];
		FieldValue field = point.data.getField(graphField);

		// Set title to date
		SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		dialog.setTitle(date.format(new Date(point.javaTime)));

		// Set icon
		ImageView image = (ImageView)dialog.findViewById(R.id.icon);
		int icon = -1;
		switch(graphField)
		{
		case FIELD_TEMPERATURE:
			icon = R.drawable.menu_temp;
			break;
		case FIELD_HUMIDITY:
			icon = R.drawable.menu_humidity;
			break;
		case FIELD_DEWPOINT:
			icon = R.drawable.menu_dewpoint;
			break;
		case FIELD_DENSITY:
			icon = R.drawable.menu_density;
			break;
		}
		image.setImageResource(icon);

		// Set units
		for(int unitId : new int[] { R.id.unit1, R.id.unit2, R.id.unit3 })
		{
			((TextView)dialog.findViewById(unitId)).setText(getString(FIELD_UNIT[graphField]));
		}

		// Set actual values
		String format = graphField == FIELD_DENSITY ? "%.2f" : "%.1f";
		((TextView)dialog.findViewById(R.id.minValue)).setText(
			String.format(format, field.getMin()));
		((TextView)dialog.findViewById(R.id.estValue)).setText(
			String.format(format, field.getMeasured()));
		((TextView)dialog.findViewById(R.id.maxValue)).setText(
			String.format(format, field.getMax()));
	}

	private void prepareNameDialog(Dialog dialog)
	{
		((EditText)dialog.findViewById(R.id.name)).setText(
			HumidityActivity.getBankName(this, bank));
		((Button)dialog.findViewById(R.id.ok)).setEnabled(true);
	}

	private void prepareDeleteDialog(Dialog dialog)
	{
		dialog.setTitle(deleteMessage);
	}

	private long deleteTime;
	private boolean deleteAfter;
	private String deleteMessage;

	private void deleteData(long time, String message, boolean after)
	{
		// This is crappy, but I have to call showDislog and then use this stuff
		// later.
		this.deleteTime = time;
		this.deleteMessage = message;
		this.deleteAfter = after;
		showDialog(DIALOG_DELETE);
	}

	private void confirmDelete()
	{
		SQLiteDatabase db = new RecordsOpenHelper(this).getWritableDatabase();

		db.delete("readings", "javatime " + (deleteAfter ? ">" : "<")
			+ " ? AND bank = ?", new String[] { deleteTime + "", bank + ""});

		db.close();

		loadData();

	 	graph.invalidate();
	}
}
