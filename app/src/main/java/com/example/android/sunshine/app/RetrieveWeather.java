package com.example.android.sunshine.app;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RetrieveWeather {

  static String get7DayForecast(Context context, String zip) {
    // These two need to be declared outside the try/catch
    // so that they can be closed in the finally block.
    HttpURLConnection urlConnection = null;
    BufferedReader reader = null;

    // Will contain the raw JSON response as a string.
    String forecastJsonStr = null;

    try {
      // Construct the URL for the OpenWeatherMap query
      // Possible parameters are avaiable at OWM's forecast API page, at
      // http://openweathermap.org/API#forecast
      URL url = new URL(generateUrl(context.getString(R.string.open_weather_url),
          "q" + zip,
          "mode=json",
          "units=metric",
          "cnt=7",
          "APPID=" + context.getString(R.string.open_weather_map_key)));

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
      Log.e("ForecastFragment", "Error ", e);
      // If the code didn't successfully get the weather data, there's no point in attemping
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
          Log.e("ForecastFragment", "Error closing stream", e);
        }
      }
    }
    return forecastJsonStr;
  }

  static String generateUrl (String url, String... params){
    StringBuilder sb = new StringBuilder();
    sb.append(url);
    if (params.length > 0) {
      for (String param : params) {
        if (sb.toString().contains("?")){
          sb.append("?");
        }
        else {
          sb.append("&");
        }
        sb.append(param);
      }
    }
    return sb.toString();
  }
}
