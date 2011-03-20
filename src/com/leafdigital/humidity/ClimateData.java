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

public class ClimateData
{
	public final static int FIELD_TEMPERATURE=0, FIELD_HUMIDITY=1,
		FIELD_DEWPOINT=2, FIELD_DENSITY=3;

	private double[] measured = new double[4], min = new double[4], max = new double[4];

	public static class FieldValue
	{
		private float measured, min, max;

		private FieldValue(int field, double[] measured, double[] min, double[] max)
		{
			this.measured = (float)measured[field];
			this.min = (float)min[field];
			this.max = (float)max[field];
		}

		public float getMeasured()
		{
			return measured;
		}

		public float getMin()
		{
			return min;
		}

		public float getMax()
		{
			return max;
		}
	}

	public ClimateData(double temp, float tempError, double humidity, float humidityError)
	{
		measured[FIELD_TEMPERATURE] = temp;
		min[FIELD_TEMPERATURE] = Math.max(0, temp - tempError);
		max[FIELD_TEMPERATURE] = Math.min(40, temp + tempError);

		measured[FIELD_HUMIDITY] = humidity;
		min[FIELD_HUMIDITY] = Math.max(1, humidity - humidityError);
		max[FIELD_HUMIDITY] = Math.min(100, humidity + humidityError);

		measured[FIELD_DEWPOINT] = getDewpoint(measured);
		min[FIELD_DEWPOINT] = getDewpoint(min);
		max[FIELD_DEWPOINT] = getDewpoint(max);

		measured[FIELD_DENSITY] = getDensity(measured);
		min[FIELD_DENSITY] = getDensity(min);
		max[FIELD_DENSITY] = getDensity(max);
	}

  private static double getDewpoint(double[] values)
  {
  	double temp = values[FIELD_TEMPERATURE];
  	double humidity = values[FIELD_HUMIDITY];

		// Dewpoint valid up to 60 degrees, from
		// http://en.wikipedia.org/wiki/Dew_point#Calculating_the_dew_point
		double a = 17.271, b = 237.2;
		double gamma = ((a * temp) / (b + temp)) + Math.log(humidity/100.0);
		return b * gamma / (a - gamma);
  }

  private static double getDensity(double[] values)
  {
  	double temp = values[FIELD_TEMPERATURE];
  	double humidity = values[FIELD_HUMIDITY];

  	// Approximate saturated vapour density valid up to 40 degrees, from
		// http://hyperphysics.phy-astr.gsu.edu/hbase/kinetic/relhum.html#c3
		double saturated = 5.018 + 0.32321 * temp + 0.0081847 * temp * temp
			+ 0.00031243 * temp * temp * temp;
		return saturated * (humidity / 100.0);
  }

  public FieldValue getField(int field)
  {
  	return new FieldValue(field, measured, min, max);
  }
}
