package fi.bitrite.android.ws.model;

import android.content.Context;
import android.content.res.Resources;
import android.os.Parcel;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.yelp.parcelgen.JsonParser.DualCreator;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import fi.bitrite.android.ws.R;

public class Host extends _Host implements ClusterItem {

    private long mUpdated;

    public Host() {
        super();
    }

    public Host(int id, String name, String fullname, String street, String additional, String city,
                String province, String postalCode, String country, String mobilePhone,
                String homePhone, String workPhone, String comments, String preferredNotice,
                String maxCyclists, String notCurrentlyAvailable, String bed, String bikeshop,
                String campground, String food, String kitchenUse, String laundry, String lawnspace,
                String motel, String sag, String shower, String storage, String latitude,
                String longitude, String login, String created, String languagesSpoken,
                String picture, String profilePictureSmall, String profilePictureLarge) {
        super(id, name, fullname, street, additional, city, province, postalCode, country,
                mobilePhone, homePhone, workPhone, comments, preferredNotice, maxCyclists,
                notCurrentlyAvailable, bed, bikeshop, campground, food, kitchenUse, laundry,
                lawnspace, motel, sag, shower, storage, latitude, longitude, login, created,
                languagesSpoken, picture, profilePictureSmall, profilePictureLarge);
    }

    public static final DualCreator<Host> CREATOR = new DualCreator<Host>() {

        public Host[] newArray(int size) {
            return new Host[size];
        }

        public Host createFromParcel(Parcel source) {
            Host object = new Host();
            object.readFromParcel(source);
            return object;
        }

        @Override
        public Host parse(JSONObject obj) throws JSONException {
            Host newInstance = new Host();
            newInstance.readFromJson(obj);
            return newInstance;
        }
    };

    public String getLocation() {

        String location = "";
        if (!getStreet().isEmpty()) {
            location += getStreet() + "\n";
        }

        if (!getAdditional().isEmpty()) {
            location += getAdditional() + "\n";
        }
        location += getCity() + ", " + getProvince().toUpperCase();
        if (!getPostalCode().isEmpty()) {
            location += " " + getPostalCode();
        }

        if (!getCountry().isEmpty()) {
            location += ", " + getCountry().toUpperCase();
        }

        return location;
    }

    public String getNearbyServices(Context context) {
        Resources r = context.getResources();

        String nearbyServices = "";
        if (!getMotel().isEmpty()) {
            nearbyServices += r.getString(R.string.nearby_service_accommodation) + ": " + getMotel() + ", ";
        }
        if (!getBikeshop().isEmpty()) {
            nearbyServices += r.getString(R.string.nearby_service_bikeshop) + ": " +getBikeshop() + ", ";
        }
        if (!getCampground().isEmpty()) {
            nearbyServices += r.getString(R.string.nearby_service_campground) + ": " +getCampground() + ", ";
        }

        return nearbyServices;
    }
    public String getHostServices(Context context) {
        StringBuilder sb = new StringBuilder();
        Resources r = context.getResources();

        if (hasService(getShower()))
            sb.append(r.getString(R.string.host_service_shower) + ", ");
        if (hasService(getFood()))
            sb.append(r.getString(R.string.host_services_food) + ", ");
        if (hasService(getBed()))
            sb.append(r.getString(R.string.host_services_bed) + ", ");
        if (hasService(getLaundry()))
            sb.append(r.getString(R.string.host_service_laundry) + ", ");
        if (hasService(getStorage()))
            sb.append(r.getString(R.string.host_service_storage) + ", ");
        if (hasService(getKitchenUse()))
            sb.append(r.getString(R.string.host_service_kitchen) + ", ");
        if (hasService(getLawnspace()))
            sb.append(r.getString(R.string.host_service_tentspace) + ", ");
        if (hasService(getSag()))
            sb.append(context.getString(R.string.host_service_sag));

        return sb.toString();
    }

    private boolean hasService(String service) {
        return !service.isEmpty() && service.equals("1");
    }

    public boolean isNotCurrentlyAvailable() {
        return getNotCurrentlyAvailable().equals("1");
    }

    public long getUpdated() {
        return mUpdated;
    }

    public void setUpdated(long updated) {
        mUpdated = updated;
    }

    public String getMemberSince() {
        return formatDate(getCreated());
    }

    public String getLastLogin() {
        return getLogin();
    }

    @Override
    public LatLng getPosition() {
        return new LatLng(Double.parseDouble(mLatitude), Double.parseDouble(mLongitude));
    }

    public String getStreetCityAddress() {
        String result = "";
        if (mStreet != null && mStreet.length() > 0) {
            result = mStreet + ", ";
        }
        result += mCity + ", " + mProvince.toUpperCase();
        return result;
    }

    public Date getCreatedAsDate() {
        return stringToDate(mCreated);
    }
    public Date getLastLoginAsDate() {
        return stringToDate(mLogin);
    }
    private Date stringToDate(String s) {
        int intDate = 0;
        try {
            intDate = Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }

        return new Date(intDate * 1000L);
    }


    private String formatDate(String timestamp) {
        if (timestamp.isEmpty()) {
            return "";
        }

        Date date = new Date(Long.parseLong(timestamp) * 1000);
        DateFormat dateFormat = SimpleDateFormat.getDateInstance();
        return dateFormat.format(date);
    }

    public LatLng getLatLng() {
        return new LatLng(Double.parseDouble(mLatitude), Double.parseDouble(mLongitude));
    }
}
