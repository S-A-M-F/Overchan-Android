package nya.miku.wishmaster.chans.gensokyo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import cz.msebera.android.httpclient.cookie.Cookie;
import cz.msebera.android.httpclient.impl.cookie.BasicClientCookie;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceGroup;
import android.support.v4.content.res.ResourcesCompat;

import nya.miku.wishmaster.R;
import nya.miku.wishmaster.api.AbstractChanModule;
import nya.miku.wishmaster.api.interfaces.CancellableTask;
import nya.miku.wishmaster.api.interfaces.ProgressListener;
import nya.miku.wishmaster.api.models.AttachmentModel;
import nya.miku.wishmaster.api.models.BoardModel;
import nya.miku.wishmaster.api.models.PostModel;
import nya.miku.wishmaster.api.models.SimpleBoardModel;
import nya.miku.wishmaster.api.models.ThreadModel;
import nya.miku.wishmaster.api.models.UrlPageModel;
import nya.miku.wishmaster.api.util.WakabaReader;
import nya.miku.wishmaster.api.util.WakabaUtils;
import nya.miku.wishmaster.common.IOUtils;
import nya.miku.wishmaster.http.streamer.HttpRequestModel;
import nya.miku.wishmaster.http.streamer.HttpResponseModel;
import nya.miku.wishmaster.http.streamer.HttpStreamer;
import nya.miku.wishmaster.http.streamer.HttpWrongStatusCodeException;

public class GensokyoModule extends AbstractChanModule {
    public static final String CHAN_NAME = "gensokyo.4otaku.org";
    public static final String CHAN_DOMAIN = "gensokyo.4otaku.org";

    private static final String PREF_KEY_COOKIES = "GENSOKYO_COOKIES";
    private static final DateFormat ARCHIVE_DATE_FORMAT = new SimpleDateFormat("EEE dd MMMM yyyy HH:mm:ss", new Locale("ru"));

    private boolean useHttps() {
        return useHttps(true);
    }

    private String getUsingUrl() {
        return (useHttps() ? "https://" : "http://") + CHAN_DOMAIN + "/";
    }

    private static String getBoardPath(String boardName) {
        if ("b".equals(boardName)) return "curr/b";
        if ("arch_b".equals(boardName)) return "arch";
        return boardName;
    }

    private static String getThreadPath(String boardName) {
        if ("arch_b".equals(boardName)) return "arch/b";
        return getBoardPath(boardName);
    }

    private static String getBoardForPath(String path) {
        if ("curr/b".equals(path) || "curr/b/".equals(path)) return "b";
        if ("arch/b".equals(path) || "arch/b/".equals(path) || "arch".equals(path) || "arch/".equals(path)) return "arch_b";
        return path;
    }

    public GensokyoModule(SharedPreferences preferences, Resources resources) {
        super(preferences, resources);
    }

    @Override
    public void addPreferencesOnScreen(PreferenceGroup preferenceGroup) {
        addHttpsPreference(preferenceGroup, true);
        super.addPreferencesOnScreen(preferenceGroup);
    }

    @Override
    public String getChanName() {
        return CHAN_NAME;
    }

    @Override
    public String getDisplayingName() {
        return "Gensokyo RPGs";
    }

    @Override
    public Drawable getChanFavicon() {
        return ResourcesCompat.getDrawable(resources, R.drawable.favicon_cirno, null);
    }

    @Override
    protected void initHttpClient() {
        Set<String> saved = preferences.getStringSet(getSharedKey(PREF_KEY_COOKIES), null);
        if (saved != null) {
            for (String entry : saved) {
                String[] parts = entry.split("\\|", 4);
                if (parts.length >= 2) {
                    BasicClientCookie cookie = new BasicClientCookie(parts[0], parts[1]);
                    if (parts.length >= 3 && !parts[2].isEmpty()) cookie.setDomain(parts[2]);
                    if (parts.length >= 4 && !parts[3].isEmpty()) cookie.setPath(parts[3]);
                    httpClient.getCookieStore().addCookie(cookie);
                }
            }
        }
    }

    void persistCookies() {
        List<Cookie> cookies = httpClient.getCookieStore().getCookies();
        Set<String> serialized = new HashSet<>();
        for (Cookie c : cookies) {
            serialized.add(c.getName() + "|" + c.getValue() + "|" +
                    (c.getDomain() != null ? c.getDomain() : "") + "|" +
                    (c.getPath() != null ? c.getPath() : ""));
        }
        preferences.edit().putStringSet(getSharedKey(PREF_KEY_COOKIES), serialized).apply();
    }

    boolean hasShoujoCookie() {
        List<Cookie> cookies = httpClient.getCookieStore().getCookies();
        for (Cookie c : cookies) {
            if ("shoujo".equals(c.getName())) return true;
        }
        return false;
    }

    @Override
    public void saveCookie(cz.msebera.android.httpclient.cookie.Cookie cookie) {
        super.saveCookie(cookie);
        persistCookies();
    }

    @Override
    public void clearCookies() {
        super.clearCookies();
        preferences.edit().remove(getSharedKey(PREF_KEY_COOKIES)).apply();
    }

    @Override
    public SimpleBoardModel[] getBoardsList(ProgressListener listener, CancellableTask task, SimpleBoardModel[] oldBoardsList) throws Exception {
        return GensokyoBoards.getBoardsList();
    }

    @Override
    public BoardModel getBoard(String shortName, ProgressListener listener, CancellableTask task) throws Exception {
        return GensokyoBoards.getBoard(shortName);
    }

    @Override
    public String buildUrl(UrlPageModel model) throws IllegalArgumentException {
        if (!model.chanName.equals(CHAN_NAME)) throw new IllegalArgumentException("wrong chan");
        String base = getUsingUrl();
        String boardPath = getBoardPath(model.boardName);
        switch (model.type) {
            case UrlPageModel.TYPE_INDEXPAGE:
                return base;
            case UrlPageModel.TYPE_CATALOGPAGE:
                return base + boardPath + ("arch_b".equals(model.boardName) ? "/catalogue.html" : "/catalog.html");
            case UrlPageModel.TYPE_BOARDPAGE:
                if (model.boardPage == UrlPageModel.DEFAULT_FIRST_PAGE) model.boardPage = 0;
                return base + boardPath + "/" + (model.boardPage > 0 ? model.boardPage + ".html" : "");
            case UrlPageModel.TYPE_THREADPAGE:
                if ("arch_b".equals(model.boardName) && model.threadNumber != null && !model.threadNumber.matches("\\d+")) {
                    if ("index".equals(model.threadNumber)) return base + "arch/";
                    return base + "arch/" + model.threadNumber + ".html";
                }
                return base + getThreadPath(model.boardName) + "/res/" + model.threadNumber + ".html";
            case UrlPageModel.TYPE_OTHERPAGE:
                return base + (model.otherPath.startsWith("/") ? model.otherPath.substring(1) : model.otherPath);
            default:
                throw new IllegalArgumentException("wrong page type");
        }
    }

    @Override
    public UrlPageModel parseUrl(String url) throws IllegalArgumentException {
        url = url.replace("gensokyo.4otaku.ru", "gensokyo.4otaku.org");
        UrlPageModel model = WakabaUtils.parseUrl(url, CHAN_NAME, CHAN_DOMAIN);
        if (model.type == UrlPageModel.TYPE_OTHERPAGE && model.otherPath != null) {
            String p = model.otherPath.toLowerCase(java.util.Locale.US);
            if (p.endsWith("/catalog.html") || p.endsWith("/catalogue.html")) {
                model.type = UrlPageModel.TYPE_CATALOGPAGE;
                String path = p.replace("/catalog.html", "").replace("/catalogue.html", "");
                model.boardName = getBoardForPath(path);
                model.otherPath = null;
            } else if (p.matches("arch/[^/]+\\.html")) {
                String name = p.substring(5, p.length() - 5);
                model.type = UrlPageModel.TYPE_THREADPAGE;
                model.boardName = "arch_b";
                model.threadNumber = name;
                model.otherPath = null;
            }
        } else if (model.boardName != null) {
            model.boardName = getBoardForPath(model.boardName);
        }
        return model;
    }

    private void checkBarrier(byte[] bytes) throws GensokyoBarrierException {
        if (bytes == null) return;
        String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        if (content.contains("<title>Hakurei Barrier</title>")) {
            throw new GensokyoBarrierException(bytes);
        }
    }

    private byte[] getBytes(String url, HttpRequestModel request, ProgressListener listener, CancellableTask task) throws Exception {
        if (request == null) request = HttpRequestModel.DEFAULT_GET;
        HttpResponseModel response = HttpStreamer.getInstance().getFromUrl(url, request, httpClient, listener, task);
        try {
            if (response.statusCode == 200) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(32768);
                IOUtils.copyStream(response.stream, out);
                byte[] bytes = out.toByteArray();
                persistCookies();
                checkBarrier(bytes);
                return bytes;
            } else {
                throw new HttpWrongStatusCodeException(response.statusCode, response.statusCode + " - " + response.statusReason);
            }
        } finally {
            if (response != null) response.release();
        }
    }

    @Override
    public ThreadModel[] getThreadsList(String boardName, int page, ProgressListener listener, CancellableTask task, ThreadModel[] oldList) throws Exception {
        if ("arch_b".equals(boardName)) {
            if (page > 0) return new ThreadModel[0];

            ThreadModel thread = new ThreadModel();
            thread.threadNumber = "index";
            thread.postsCount = 1;
            thread.attachmentsCount = 0;
            thread.isSticky = true;
            thread.posts = new PostModel[1];
            thread.posts[0] = new PostModel();
            thread.posts[0].number = "index";
            thread.posts[0].subject = "\u0410\u0440\u0445\u0438\u0432 \u0442\u0440\u0435\u0434\u043e\u0432";
            thread.posts[0].comment = "\u0421\u043f\u0438\u0441\u043e\u043a RPG \u0438\u0433\u0440 \u0430\u0440\u0445\u0438\u0432\u0430";
            thread.posts[0].name = "";
            thread.posts[0].op = true;
            thread.posts[0].email = "";
            thread.posts[0].trip = "";
            thread.posts[0].parentThread = "index";
            thread.posts[0].timestamp = 0;
            thread.posts[0].attachments = new AttachmentModel[0];

            return new ThreadModel[] { thread };
        }

        UrlPageModel urlModel = new UrlPageModel();
        urlModel.chanName = CHAN_NAME;
        urlModel.type = UrlPageModel.TYPE_BOARDPAGE;
        urlModel.boardName = boardName;
        urlModel.boardPage = page;
        String url = buildUrl(urlModel);

        byte[] bytes = getBytes(url, HttpRequestModel.builder().setGET().setCheckIfModified(oldList != null).build(), listener, task);
        if (bytes == null) return oldList;

        return new ThreadModel[0];
    }

    @Override
    public PostModel[] getPostsList(String boardName, String threadNumber, ProgressListener listener, CancellableTask task, PostModel[] oldList) throws Exception {
        if ("arch_b".equals(boardName)) {
            if (threadNumber != null && threadNumber.matches("\\d+")) {
                UrlPageModel urlModel = new UrlPageModel();
                urlModel.chanName = CHAN_NAME;
                urlModel.type = UrlPageModel.TYPE_THREADPAGE;
                urlModel.boardName = boardName;
                urlModel.threadNumber = threadNumber;
                String url = buildUrl(urlModel);

                byte[] bytes = getBytes(url, HttpRequestModel.builder().setGET().setCheckIfModified(oldList != null).build(), listener, task);
                if (bytes == null) return oldList;

                WakabaReader reader = new WakabaReader(new ByteArrayInputStream(bytes), ARCHIVE_DATE_FORMAT, false);
                try {
                    ThreadModel[] threads = reader.readWakabaPage();
                    if (threads.length > 0 && threads[0].posts != null) {
                        return threads[0].posts;
                    }
                    return new PostModel[0];
                } finally {
                    reader.close();
                }
            } else {
                UrlPageModel urlModel = new UrlPageModel();
                urlModel.chanName = CHAN_NAME;
                urlModel.type = UrlPageModel.TYPE_THREADPAGE;
                urlModel.boardName = boardName;
                urlModel.threadNumber = threadNumber;
                String url = buildUrl(urlModel);

                byte[] bytes = getBytes(url, HttpRequestModel.builder().setGET().setCheckIfModified(oldList != null).build(), listener, task);
                if (bytes == null) return oldList;

                GensokyoRpgPageReader reader = new GensokyoRpgPageReader(new ByteArrayInputStream(bytes));
                try {
                    return reader.readPage();
                } finally {
                    reader.close();
                }
            }
        }

        return new PostModel[0];
    }

    @Override
    public ThreadModel[] getCatalog(String boardName, int catalogType, ProgressListener listener, CancellableTask task, ThreadModel[] oldList) throws Exception {
        return new ThreadModel[0];
    }

    @Override
    public String fixRelativeUrl(String url) {
        if (url != null && url.startsWith("../../b/")) {
            url = "/arch/b/" + url.substring(8);
        }
        return super.fixRelativeUrl(url);
    }

    @Override
    public void downloadFile(String url, java.io.OutputStream out, ProgressListener listener, CancellableTask task) throws Exception {
        String fixedUrl = fixRelativeUrl(url);
        HttpStreamer.getInstance().downloadFileFromUrl(fixedUrl, out, HttpRequestModel.DEFAULT_GET, httpClient, listener, task, false);
    }
}
