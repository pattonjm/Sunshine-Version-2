package com.example.android.sunshine.app;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import static android.net.Uri.Builder;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

  public ArrayAdapter<String> adapter;

  public ForecastFragment() {
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {

    View rootView = inflater.inflate(R.layout.fragment_main, container, false);
    String[] data = {
        "Mon 6/23 - Sunny - 31/17",
        "Tue 6/24 - Foggy - 21/8",
        "Wed 6/25 - Cloudy - 22/17",
        "Thurs 6/26 - Rainy - 18/11",
        "Fri 6/27 - Foggy - 21/10",
        "Sat 6/28 - TRAPPED IN WEATHERSTATION - 23/18",
        "Sun 6/29 - Sunny - 20/7"
    };
    List<String> weekForecast = new ArrayList<>(Arrays.asList(data));
    adapter = new ArrayAdapter<>(getActivity(),
        R.layout.list_item_forecast,
        R.id.list_item_forecast_textview,
        weekForecast);
    ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
    listView.setAdapter(adapter);
    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int position, long l){
        String forecast = adapter.getItem(position);
        Toast.makeText(getActivity(), forecast, Toast.LENGTH_SHORT).show();
      }
    });

    FetchWeatherTask weatherTask = new FetchWeatherTask();
    weatherTask.execute("84606", getString(R.string.open_weather_map_key));
    return rootView;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.forecast_fragment, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_refresh) {
      FetchWeatherTask weatherTask = new FetchWeatherTask();
      weatherTask.execute("84606", getString(R.string.open_weather_map_key));
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    @Override
    protected void onPostExecute(String[] strings) {
      adapter.clear();
      adapter.addAll(Arrays.asList(strings));
    }

    @Override
    protected String[] doInBackground(String... params) {
      // These two need to be declared outside the try/catch
      // so that they can be closed in the finally block.
      HttpURLConnection urlConnection = null;
      BufferedReader reader = null;

      // Will contain the raw JSON response as a string.
      String forecastJsonStr;

      String format = "json";
      String units = "metric";
      String numDays = "7";


      try {
        // Construct the URL for the OpenWeatherMap query
        // Possible parameters are available at OWM's forecast API page, at
        // http://openweathermap.org/API#forecast
        final String FORECAST_BASE_URL =
            "http://api.openweathermap.org/data/2.5/forecast/daily";
        final String QUERY_PARAM = "q";
        final String FORMAT_PARAM = "mode";
        final String UNITS_PARAM = "units";
        final String DAYS_PARAM = "cnt";
        final String APP_ID_PARAM = "APPID";

        Builder uriBuilder = new Builder();
        Uri uri = Uri.parse(FORECAST_BASE_URL).buildUpon()
            .appendQueryParameter(QUERY_PARAM, params[0])
            .appendQueryParameter(FORMAT_PARAM, format)
            .appendQueryParameter(UNITS_PARAM, units)
            .appendQueryParameter(DAYS_PARAM, numDays)
            .appendQueryParameter(APP_ID_PARAM, params[1])
            .build();
        URL url = new URL(uri.toString());
        // Create the request to OpenWeatherMap, and open the connection
        urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.connect();

        // Read the input stream into a String
        InputStream inputStream = urlConnection.getInputStream();
        StringBuilder builder = new StringBuilder();
        if (inputStream == null) {
          // Nothing to do.
          return null;
        }
        reader = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        while ((line = reader.readLine()) != null) {
          // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
          // But it does make debugging a *lot* easier if you print out the completed
          // builder for debugging.
          builder.append(line).append("\n");
        }

        if (builder.length() == 0) {
          // Stream was empty.  No point in parsing.
          return null;
        }
        forecastJsonStr = builder.toString();
      } catch (IOException e) {
        Log.e(LOG_TAG, "Error ", e);
        // If the code didn't successfully get the weather data, there's no point in attempting
        // to parse it.
        return null;
      } finally {
        if (urlConnection != null) {
          urlConnection.disconnect();
        }
        if (reader != null) {
          try {
            reader.close();
          } catch (final IOException e) {
            Log.e(LOG_TAG, "Error closing stream", e);
          }
        }
      }
      try {
        return getWeatherDataFromJson(forecastJsonStr, 7);
      } catch (JSONException e) {
        Log.e(LOG_TAG, e.getMessage(), e);
        e.printStackTrace();
      }
      return (String[]) Collections.singletonList("").toArray();
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
      // For presentation, assume the user doesn't care about tenths of a degree.
      long roundedHigh = Math.round(high);
      long roundedLow = Math.round(low);

      return roundedHigh + "/" + roundedLow;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     * <p/>
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
        throws JSONException {

      // These are the names of the JSON objects that need to be extracted.
      final String OWM_LIST = "list";
      final String OWM_WEATHER = "weather";
      final String OWM_TEMPERATURE = "temp";
      final String OWM_MAX = "max";
      final String OWM_MIN = "min";
      final String OWM_DESCRIPTION = "main";

      JSONObject forecastJson = new JSONObject(forecastJsonStr);
      JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

      String[] resultStrs = new String[numDays];
      GregorianCalendar gc = new GregorianCalendar();
      for (int i = 0; i < weatherArray.length(); i++) {
        // For now, using the format "Day, description, hi/low"
        String day;
        String description;
        String highAndLow;

        // Get the JSON object representing the day
        JSONObject dayForecast = weatherArray.getJSONObject(i);

        //create a Gregorian Calendar, which is in current date
        //add i dates to current date of calendar
        gc.add(GregorianCalendar.DATE, 1);
        //get that date, format it, and "save" it on variable day
        Date time = gc.getTime();
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd", Locale.US);
        day = shortenedDateFormat.format(time);

        // description is in a child array called "weather", which is 1 element long.
        JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
        description = weatherObject.getString(OWM_DESCRIPTION);

        // Temperatures are in a child object called "temp".  Try not to name variables
        // "temp" when working with temperature.  It confuses everybody.
        JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
        double high = temperatureObject.getDouble(OWM_MAX);
        double low = temperatureObject.getDouble(OWM_MIN);

        highAndLow = formatHighLows(high, low);
        resultStrs[i] = day + " - " + description + " - " + highAndLow;
      }
      return resultStrs;

    }

  }
}
