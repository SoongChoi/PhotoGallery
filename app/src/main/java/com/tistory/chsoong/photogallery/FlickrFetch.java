package com.tistory.chsoong.photogallery;

import android.net.Uri;
import android.util.Log;
import android.util.LruCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chsoong on 2017. 3. 28..
 */

public class FlickrFetch {
    private static final String TAG = "FlickrFetch";
    private static final LruCache mCache = new LruCache(100);

    private static final String FETCH_RECENTS_PHOTOS="flickr.photos.getRecent";
    private static final String SEARCH_PHOTOS="flickr.photos.search";
    private static final String FLICKER_API_BASE="https://api.flickr.com/services/rest/";
    private static final String API_KEY = "0e1e6d8d5e417d7f70bc9bd5d3b51217";
    private static final String AUTH_TOKEN = "72157682296715225-a2b179222c8fb377";
    private static final Uri ENDPOINT = Uri.parse(FLICKER_API_BASE)
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
//            .appendQueryParameter("auth_token", AUTH_TOKEN)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build();

    List<GalleryItem> items = new ArrayList<>();

    private boolean isValidItems(JSONObject jsonBody) throws JSONException {

        if(jsonBody.getString("stat").equals("ok")) {
            return true;
        }
        Log.e(TAG, "Invalid Frame Receive : + "+ jsonBody.getString("message"));
        return false;
    }

    public List<GalleryItem> downloadGalleryItems(String url) {
        try {

            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);

            JSONObject jsonBody = new JSONObject(jsonString);
            if(isValidItems(jsonBody)) {
                parseItmes(items, jsonBody);
                return items;
            }
            return null;

        } catch (JSONException je) {
            Log.e(TAG, "Failed to parse JSON", je);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items" ,ioe);

        }

        return null;
    }

    public List<GalleryItem> fetchRecentPhoto() {
        String url = builUrl(FETCH_RECENTS_PHOTOS, null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query) {
        String url = builUrl(SEARCH_PHOTOS, query);
        return downloadGalleryItems(url);
    }

    private String builUrl(String method, String query){
        Uri.Builder uriBuilder = ENDPOINT.buildUpon()
                .appendQueryParameter("method", method);

        if(method.equals(SEARCH_PHOTOS)) {
            uriBuilder.appendQueryParameter("text", query);
        }

        return uriBuilder.build().toString();
    }

    private void parseItmes(List<GalleryItem> items, JSONObject jsonBody)
        throws  IOException, JSONException {

        JSONObject phtosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = phtosJsonObject.getJSONArray("photo");

        for (int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoItems = photoJsonArray.getJSONObject(i);
            GalleryItem item = new GalleryItem();
            item.setId(photoItems.getString("id"));
            item.setCaption(photoItems.getString("title"));

            if(!photoItems.has("url_s")) {
                continue;
            }

            item.setUrl(photoItems.getString("url_s"));
            item.setOwner(photoItems.getString("owner"));
            items.add(item);
        }


    }

    public byte[] getUrlBytes(String urlSpec) throws IOException {

        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if(connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() +
                ": with " + urlSpec);
            }

            int byteRead = 0;
            byte[] buffer = new byte[1024];
            while ((byteRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, byteRead);
            }
            out.close();
            return out.toByteArray();

        }
        finally {
            connection.disconnect();
        }

    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }
}
