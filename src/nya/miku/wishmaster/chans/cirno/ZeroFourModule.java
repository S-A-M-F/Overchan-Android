package nya.miku.wishmaster.chans.cirno;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.message.BasicNameValuePair;

import nya.miku.wishmaster.R;
import nya.miku.wishmaster.api.AbstractKusabaModule;
import nya.miku.wishmaster.api.interfaces.CancellableTask;
import nya.miku.wishmaster.api.interfaces.ProgressListener;
import nya.miku.wishmaster.api.models.BoardModel;
import nya.miku.wishmaster.api.models.CaptchaModel;
import nya.miku.wishmaster.api.models.DeletePostModel;
import nya.miku.wishmaster.api.models.PostModel;
import nya.miku.wishmaster.api.models.SendPostModel;
import nya.miku.wishmaster.api.models.SimpleBoardModel;
import nya.miku.wishmaster.api.models.ThreadModel;
import nya.miku.wishmaster.api.models.UrlPageModel;
import nya.miku.wishmaster.api.util.WakabaReader;
import nya.miku.wishmaster.api.util.WakabaUtils;
import nya.miku.wishmaster.common.IOUtils;
import nya.miku.wishmaster.http.ExtendedMultipartBuilder;
import nya.miku.wishmaster.http.streamer.HttpRequestModel;
import nya.miku.wishmaster.http.streamer.HttpResponseModel;
import nya.miku.wishmaster.http.streamer.HttpStreamer;
import nya.miku.wishmaster.http.streamer.HttpWrongStatusCodeException;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceGroup;
import android.support.v4.content.res.ResourcesCompat;

public class ZeroFourModule extends AbstractKusabaModule {
    static final String ZEROFOUR_NAME = "014chan.org";
    static final String ZEROFOUR_DOMAIN = "014chan.org";

    public ZeroFourModule(SharedPreferences preferences, Resources resources) {
        super(preferences, resources);
    }

    @Override
    public String getChanName() {
        return ZEROFOUR_NAME;
    }

    @Override
    public String getDisplayingName() {
        return "014chan";
    }

    @Override
    public Drawable getChanFavicon() {
        return ResourcesCompat.getDrawable(resources, R.drawable.favicon_014chan, null);
    }

    @Override
    protected String getUsingDomain() {
        return ZEROFOUR_DOMAIN;
    }

    @Override
    protected boolean canHttps() {
        return true;
    }

    @Override
    protected boolean useHttpsDefaultValue() {
        return false;
    }

    @Override
    protected DateFormat getDateFormat() {
        return DateFormats.CHAN_410_DATE_FORMAT;
    }

    @Override
    protected int getKusabaFlags() {
        return KusabaReader.FLAG_HANDLE_ADMIN_TAG;
    }

    @Override
    protected WakabaReader getKusabaReader(InputStream stream, UrlPageModel urlModel) {
        return new Chan014Reader(stream);
    }

    @Override
    public void addPreferencesOnScreen(PreferenceGroup preferenceGroup) {
        addHttpsPreference(preferenceGroup, false);
        super.addPreferencesOnScreen(preferenceGroup);
    }

    @Override
    public SimpleBoardModel[] getBoardsList(ProgressListener listener, CancellableTask task, SimpleBoardModel[] oldBoardsList) throws Exception {
        return ZeroFourBoards.getBoardsList();
    }

    @Override
    public BoardModel getBoard(String shortName, ProgressListener listener, CancellableTask task) throws Exception {
        return ZeroFourBoards.getBoard(shortName);
    }

    @Override
    public CaptchaModel getNewCaptcha(String boardName, String threadNumber, ProgressListener listener, CancellableTask task) throws Exception {
        String captchaUrl = getUsingUrl() + "captcha.php";
        return downloadCaptcha(captchaUrl, listener, task);
    }

    @Override
    public String sendPost(SendPostModel model, ProgressListener listener, CancellableTask task) throws Exception {
        String url = getUsingUrl() + "board.php";
        ExtendedMultipartBuilder postEntityBuilder = ExtendedMultipartBuilder.create().setDelegates(listener, task).
                addString("board", model.boardName).
                addString("replythread", model.threadNumber != null ? model.threadNumber : "0").
                addString("name", model.name).
                addString("captcha", model.captchaAnswer).
                addString("subject", model.subject).
                addString("message", model.comment).
                addString("postpassword", model.password);
        if (model.sage) postEntityBuilder.addString("sage", "on");
        postEntityBuilder.addString("noko", "on");
        if (model.attachments != null && model.attachments.length > 0)
            postEntityBuilder.addFile("imagefile", model.attachments[0], model.randomHash);

        HttpRequestModel request = HttpRequestModel.builder().setPOST(postEntityBuilder.build()).setNoRedirect(true).build();
        HttpResponseModel response = null;
        try {
            response = HttpStreamer.getInstance().getFromUrl(url, request, httpClient, null, task);
            if (response.statusCode == 302) {
                String redirectUrl = response.locationHeader;
                if (redirectUrl.length() == 0) throw new Exception();
                if (redirectUrl.contains("banned.php")) throw new Exception("Вы забанены");
                return fixRelativeUrl(redirectUrl);
            } else if (response.statusCode == 200) {
                ByteArrayOutputStream output = new ByteArrayOutputStream(1024);
                IOUtils.copyStream(response.stream, output);
                String htmlResponse = output.toString("UTF-8");
                if (!htmlResponse.contains("<blockquote")) {
                    int start = htmlResponse.indexOf("<h2 style=\"font-size: 2em;font-weight: bold;text-align: center;\">");
                    if (start != -1) {
                        int end = htmlResponse.indexOf("</h2>", start + 65);
                        if (end != -1) {
                            throw new Exception(htmlResponse.substring(start + 65, end).trim());
                        }
                    }
                }
            } else throw new Exception(response.statusCode + " - " + response.statusReason);
        } finally {
            if (response != null) response.release();
        }
        return null;
    }

    @Override
    protected List<? extends NameValuePair> getDeleteFormAllValues(DeletePostModel model) throws Exception {
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add(new BasicNameValuePair("board", model.boardName));
        pairs.add(new BasicNameValuePair("delete[]", model.postNumber));
        if (model.onlyFiles) pairs.add(new BasicNameValuePair("fileonly", "on"));
        pairs.add(new BasicNameValuePair("postpassword", model.password));
        return pairs;
    }

    @Override
    protected List<? extends NameValuePair> getReportFormAllValues(DeletePostModel model) throws Exception {
        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add(new BasicNameValuePair("board", model.boardName));
        pairs.add(new BasicNameValuePair("delete[]", model.postNumber));
        pairs.add(new BasicNameValuePair("reportpost", "Пожаловаться"));
        return pairs;
    }

    @Override
    public String buildUrl(UrlPageModel model) throws IllegalArgumentException {
        if (!model.chanName.equals(ZEROFOUR_NAME)) throw new IllegalArgumentException("wrong chan");
        if (model.type == UrlPageModel.TYPE_CATALOGPAGE) return getUsingUrl() + model.boardName + "/catalog.html";
        return WakabaUtils.buildUrl(model, getUsingUrl());
    }

    @Override
    public UrlPageModel parseUrl(String url) throws IllegalArgumentException {
        UrlPageModel model = WakabaUtils.parseUrl(url, ZEROFOUR_NAME, ZEROFOUR_DOMAIN);
        if (model.type == UrlPageModel.TYPE_OTHERPAGE && model.otherPath != null && model.otherPath.endsWith("/catalog.html")) {
            model.type = UrlPageModel.TYPE_CATALOGPAGE;
            model.boardName = model.otherPath.substring(0, model.otherPath.length() - 13);
            model.otherPath = null;
        }
        return model;
    }

    @Override
    public ThreadModel[] getCatalog(String boardName, int catalogType, ProgressListener listener, CancellableTask task, ThreadModel[] oldList)
            throws Exception {
        UrlPageModel urlModel = new UrlPageModel();
        urlModel.chanName = ZEROFOUR_NAME;
        urlModel.type = UrlPageModel.TYPE_CATALOGPAGE;
        urlModel.boardName = boardName;
        String url = buildUrl(urlModel);

        HttpResponseModel responseModel = null;
        Chan410CatalogReader in = null;
        HttpRequestModel rqModel = HttpRequestModel.builder().setGET().setCheckIfModified(oldList != null).build();
        try {
            responseModel = HttpStreamer.getInstance().getFromUrl(url, rqModel, httpClient, listener, task);
            if (responseModel.statusCode == 200) {
                in = new Chan410CatalogReader(responseModel.stream);
                if (task != null && task.isCancelled()) throw new Exception("interrupted");
                return in.readPage();
            } else {
                if (responseModel.notModified()) return oldList;
                throw new HttpWrongStatusCodeException(responseModel.statusCode, responseModel.statusCode + " - " + responseModel.statusReason);
            }
        } catch (Exception e) {
            if (responseModel != null) HttpStreamer.getInstance().removeFromModifiedMap(url);
            throw e;
        } finally {
            IOUtils.closeQuietly(in);
            if (responseModel != null) responseModel.release();
        }
    }

    private static class Chan014Reader extends KusabaReader {
        private static final char[] END_THREAD_FILTER = "<div id=\"thread".toCharArray();
        private static final char[] BADGE_FILTER = "class=\"post-badge".toCharArray();
        private int curPos = 0;
        private int badgePos = 0;

        public Chan014Reader(InputStream in) {
            super(in, DateFormats.CHAN_410_DATE_FORMAT, false, FLAG_HANDLE_ADMIN_TAG);
        }

        @Override
        protected void customFilters(int ch) throws java.io.IOException {
            if (ch == END_THREAD_FILTER[curPos]) {
                ++curPos;
                if (curPos == END_THREAD_FILTER.length) {
                    skipUntilSequence(">".toCharArray());
                    finalizeThread();
                    curPos = 0;
                }
            } else {
                if (curPos != 0) curPos = ch == END_THREAD_FILTER[0] ? 1 : 0;
            }

            if (ch == BADGE_FILTER[badgePos]) {
                ++badgePos;
                if (badgePos == BADGE_FILTER.length) {
                    String threadBadge = readUntilSequence("\"".toCharArray());
                    if (threadBadge.contains("locked")) currentThread.isClosed = true;
                    if (threadBadge.contains("sticky")) currentThread.isSticky = true;
                    badgePos = 0;
                }
            } else {
                if (badgePos != 0) badgePos = ch == BADGE_FILTER[0] ? 1 : 0;
            }
        }

        @Override
        protected void postprocessPost(PostModel post) {
            if (post.subject.contains("\u21E9")) post.sage = true;
        }
    }
}
