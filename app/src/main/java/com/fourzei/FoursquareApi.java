package com.fourzei;

import java.util.List;

import retrofit.RestAdapter;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

public class FoursquareApi {

    private static final String CREDS = "client_id=" + Config.FS_CLIENT_ID
            + "&client_secret=" + Config.FS_CLIENT_SECRET
            + "&v=20250402";

    private RestAdapter mRestAdapter;
    private FoursquareService mService;

    public FoursquareApi() {
        mRestAdapter = new RestAdapter.Builder()
                .setEndpoint("https://api.foursquare.com/v2")
                .build();

        mService = mRestAdapter.create(FoursquareService.class);
    }

    public FoursquareService getService() {
        return mService;
    }

    public interface FoursquareService {
        @GET("/venues/search?limit=20&radius=2000&intent=browse&" + CREDS)
        VenuesMetaResponse search(@Query("near") String near, @Query("ll") String latLong);

        @GET("/venues/{v_id}/photos?limit=20&" + CREDS)
        VenuesMetaResponse getPhotos(@Path("v_id") String venueId);
    }

    public static class VenuesMetaResponse {
        private VenuesResponse response;

        public VenuesResponse getResponse() {
            return response;
        }
    }

    public static class VenuesResponse {
        private List<Venue> venues;
        private PhotosResponse photos;

        public List<Venue> getVenues() {
            return venues;
        }

        public PhotosResponse getPhotos() {
            return photos;
        }
    }

    public static class PhotosResponse {
        private Integer count;
        private List<Photo> items;

        public Integer getCount() {
            return count;
        }

        public List<Photo> getItems() {
            return items;
        }
    }

    public static class Photo {
        private String prefix;
        private String suffix;
        private String id;

        public String getId() {
            return id;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getSuffix() {
            return suffix;
        }

        public String getOriginalUrl() {
            return prefix + "original" + suffix;
        }
    }

    public static class Venue {
        private String id;
        private String name;
        private Location location;

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public Location getLocation() {
            return location;
        }
    }

    public static class Location {
        private String address;

        public String getAddress() {
            return address;
        }
    }
}
