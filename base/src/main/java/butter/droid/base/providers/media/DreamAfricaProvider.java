/*
 * This file is part of Butter.
 *
 * Butter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Butter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Butter. If not, see <http://www.gnu.org/licenses/>.
 */

package butter.droid.base.providers.media;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.LinkedTreeMap;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import butter.droid.base.ButterApplication;
import butter.droid.base.R;
import butter.droid.base.providers.media.models.Genre;
import butter.droid.base.providers.media.models.Media;
import butter.droid.base.providers.media.models.Movie;
import butter.droid.base.utils.LocaleUtils;
import butter.droid.base.utils.StringUtils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.json.*;
import android.text.Html;

public class DreamAfricaProvider extends MediaProvider {

    private static final DreamAfricaProvider sMediaProvider = new DreamAfricaProvider();
    private static Integer CURRENT_API = 0;
    private Integer page_count=10;
    private static final String[] API_URLS = {
            "http://www.wedreamafrica.com"
    };
    public static String CURRENT_URL = API_URLS[CURRENT_API];

    private static Filters sFilters = new Filters();
    private int totalPages = 0;
    private int page = 0;
    ArrayList<Media> currentList = new ArrayList<Media>();

    @Override
    protected Call enqueue(Request request, com.squareup.okhttp.Callback requestCallback) {
        Context context = ButterApplication.getAppContext();
        PackageInfo pInfo;
        String versionName = "0.0.0";
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionName = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        request = request.newBuilder().removeHeader("User-Agent").addHeader("User-Agent", String.format("Mozilla/5.0 (Linux; U; Android %s; %s; %s Build/%s) AppleWebkit/534.30 (KHTML, like Gecko) PT/%s", Build.VERSION.RELEASE, LocaleUtils.getCurrentAsString(), Build.MODEL, Build.DISPLAY, versionName)).build();
        return super.enqueue(request, requestCallback);
    }

    @Override
    public Call getList(final ArrayList<Media> existingList, Filters filters, final Callback callback) {
        sFilters = filters;
        ArrayList<NameValuePair> params = new ArrayList<>();
        params.add(new NameValuePair("json", "get_posts"));
        params.add(new NameValuePair("post_type", "torrent"));
        params.add(new NameValuePair("count", "1"));
        params.add(new NameValuePair("include", "title,content,date,custom_fields"));
        params.add(new NameValuePair("custom_fields", "sponsor_logo,sponsor_message,category,synopsis,cover_image"));
        params.add(new NameValuePair("page", Integer.toString(page)));

        if (filters == null) {
            filters = new Filters();
        }

        if (filters.keywords != null) {
            params.add(new NameValuePair("search", filters.keywords));
        }

        if (filters.genre != null) {
            params.add(new NameValuePair("meta_key", "category"));
            params.add(new NameValuePair("meta_value", filters.genre));
        }

        if (filters.order == Filters.Order.ASC) {
            params.add(new NameValuePair("order", "ASC"));
        } else {
            params.add(new NameValuePair("order", "DESC"));
        }

/*        if(filters.langCode != null) {
        params.add(new NameValuePair("lang", filters.langCode));
    }*/

        String sort;
        switch (filters.sort) {
            default:
            case POPULARITY:
                sort = "comment_count";
                break;
            case YEAR:
                sort = "date";
                break;
            case DATE:
                sort = "date";
                break;
            case RATING:
                sort = "comment_count";
                break;
            case ALPHABET:
                sort = "title";
                break;
            case TRENDING:
                sort = "comment_count";
                break;
        }

        params.add(new NameValuePair("order_by", sort));
        String query = "?" + buildQuery(params);
        Request.Builder requestBuilder = new Request.Builder()
                .url(CURRENT_URL + query);
        return enqueue(requestBuilder.build(), new com.squareup.okhttp.Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {

                        JSONObject obj = new JSONObject(response.body().string());
                        JSONArray arr = obj.getJSONArray("posts");
                        JSONObject post = arr.getJSONObject(0);

                        Movie movie = new Movie(sMediaProvider, null);
                        movie.imdbId = (String) post.getString("id");
                        movie.videoId = (String) post.getString("id");
                        movie.title = Html.fromHtml((String) post.getString("title")).toString();
                        try {

                            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            String yearStr = Integer.toString(formatter.parse((String) post.getString("date")).getYear());
                            Double year = Double.parseDouble(yearStr);
                            movie.year = Integer.toString(year.intValue());
                        } catch (ParseException e) {
                            System.out.println(e.getMessage());   // Rethrow the exception.
                        }
                        movie.rating = "";
                        JSONObject customFields = post.getJSONObject("custom_fields");
                        movie.genre = StringUtils.uppercaseFirst(customFields.getJSONArray("category").getString(0));
                        movie.image = (String) customFields.getJSONArray("cover_image").getString(0);
                        movie.headerImage = (String) customFields.getJSONArray("cover_image").getString(0);
                        movie.trailer = null;
                        String runtimeStr = "0";
                        Double runtime = 0d;
                        if (!runtimeStr.isEmpty())
                            runtime = Double.parseDouble(runtimeStr);
                        movie.runtime = Integer.toString(runtime.intValue());
                        movie.synopsis = Html.fromHtml((String) customFields.getJSONArray("synopsis").getString(0)).toString();
                        movie.certification = null;
                        movie.fullImage = movie.image;

                        Media.Torrent torrent = new Media.Torrent();
                        torrent.seeds = 0;
                        torrent.peers = 0;
                        torrent.hash = null;
                        torrent.url = post.getString("content");
                        movie.torrents.put("hd", torrent);
                        currentList.add(movie);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    totalPages = 5;
                    page++;
                    callback.onSuccess(sFilters, currentList, true);
                    return;
                }
                onFailure(response.request(), new IOException("Couldn't connect to Vodo"));
            }
        });
    }

    @Override
    public Call getDetail(ArrayList<Media> currentList, Integer index, Callback callback) {
        ArrayList<Media> returnList = new ArrayList<>();
        returnList.add(currentList.get(index));
        callback.onSuccess(null, returnList, true);
        return null;
    }

    @Override
    public int getLoadingMessage() {
        return R.string.loading_movies;
    }

    @Override
    public List<NavInfo> getNavigation() {
        List<NavInfo> tabs = new ArrayList<>();
        tabs.add(new NavInfo(R.id.yts_filter_a_to_z,Filters.Sort.ALPHABET, Filters.Order.ASC, ButterApplication.getAppContext().getString(R.string.a_to_z),R.drawable.yts_filter_a_to_z));
        return tabs;
    }

    @Override
    public List<Genre> getGenres() {
        return null;
    }

}
