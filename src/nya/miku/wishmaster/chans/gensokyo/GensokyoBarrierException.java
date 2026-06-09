package nya.miku.wishmaster.chans.gensokyo;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.message.BasicNameValuePair;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import nya.miku.wishmaster.R;
import nya.miku.wishmaster.api.ChanModule;
import nya.miku.wishmaster.api.interfaces.CancellableTask;
import nya.miku.wishmaster.common.Async;
import nya.miku.wishmaster.common.IOUtils;
import nya.miku.wishmaster.common.MainApplication;
import nya.miku.wishmaster.http.interactive.SimpleCaptchaException;
import nya.miku.wishmaster.http.streamer.HttpRequestModel;
import nya.miku.wishmaster.http.streamer.HttpResponseModel;
import nya.miku.wishmaster.http.streamer.HttpStreamer;

public class GensokyoBarrierException extends SimpleCaptchaException {
    private static final long serialVersionUID = 1L;

    private static final Pattern BASE64_PATTERN = Pattern.compile(
            "background-image:\\s*url\\(data:image/(?:png|jpeg|gif);base64,([A-Za-z0-9+/=]+)\\)");

    private final byte[] barrierBytes;

    public GensokyoBarrierException(byte[] barrierBytes) {
        this.barrierBytes = barrierBytes;
    }

    @Override
    protected Bitmap getNewCaptcha() {
        return decodeCaptcha(barrierBytes);
    }

    @Override
    protected void storeResponse(String answer) {
    }

    @Override
    public void handle(final Activity activity, final CancellableTask task, final Callback callback) {
        showBarrierDialog(activity, task, callback, decodeCaptcha(barrierBytes));
    }

    private void showBarrierDialog(final Activity activity, final CancellableTask task, final Callback callback, final Bitmap captcha) {
        try {
            if (task.isCancelled()) return;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LinearLayout layout = new LinearLayout(activity);
                    layout.setOrientation(LinearLayout.VERTICAL);
                    ImageView captchaView = new ImageView(activity);
                    int height = (int) (activity.getResources().getDisplayMetrics().density * 120 + 0.5f);
                    captchaView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, height));
                    captchaView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    if (captcha != null) captchaView.setImageBitmap(captcha);
                    layout.addView(captchaView);
                    final EditText responseField = new EditText(activity);
                    responseField.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
                    responseField.setSingleLine();
                    layout.addView(responseField);
                    final AlertDialog dialog = new AlertDialog.Builder(activity).setView(layout)
                            .setPositiveButton(R.string.dialog_captcha_check, null)
                            .setNeutralButton("No idea", null)
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    callback.onError(getCancelledMessage());
                                }
                            }).create();
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.setTitle("Hakurei Barrier");
                    dialog.show();
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final String answer = responseField.getText().toString().trim();
                            if (answer.isEmpty()) return;
                            dialog.dismiss();
                            if (task.isCancelled()) return;
                            Async.runAsync(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        handleCheck(activity, task, callback, answer);
                                    } catch (final Exception e) {
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                callback.onError(e.getMessage());
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    });
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                            if (task.isCancelled()) return;
                            Async.runAsync(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        handleRefresh(activity, task, callback);
                                    } catch (final Exception e) {
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                callback.onError(e.getMessage());
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    });
                    captchaView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                            if (task.isCancelled()) return;
                            Async.runAsync(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        handleRefresh(activity, task, callback);
                                    } catch (final Exception e) {
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                callback.onError(e.getMessage());
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    });
                }
            });
        } catch (final Exception e) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    callback.onError(getErrorMessage(e));
                }
            });
        }
    }

    private void handleCheck(Activity activity, CancellableTask task, Callback callback, String answer) throws Exception {
        ChanModule chan = MainApplication.getInstance().getChanModule(GensokyoModule.CHAN_NAME);
        if (!(chan instanceof GensokyoModule)) return;
        GensokyoModule module = (GensokyoModule) chan;
        HttpClient http = module.getHttpClient();

        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("captcha", answer));
        pairs.add(new BasicNameValuePair("ok", "Ok"));
        UrlEncodedFormEntity entity;
        try {
            entity = new UrlEncodedFormEntity(pairs, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e1) {
            throw new RuntimeException(e1);
        }

        HttpRequestModel request = HttpRequestModel.builder().setPOST(entity).setNoRedirect(true).build();
        HttpResponseModel response = null;
        try {
            String url = "https://" + GensokyoModule.CHAN_DOMAIN + "/border.php";
            response = HttpStreamer.getInstance().getFromUrl(url, request, http, null, null);
            module.persistCookies();
            if (module.hasShoujoCookie()) {
                callback.onSuccess();
                return;
            }
        } finally {
            if (response != null) response.release();
        }

        Bitmap newCaptcha = fetchBarrierPage(http, module);
        showBarrierDialog(activity, task, callback, newCaptcha);
    }

    private void handleRefresh(Activity activity, CancellableTask task, Callback callback) throws Exception {
        ChanModule chan = MainApplication.getInstance().getChanModule(GensokyoModule.CHAN_NAME);
        if (!(chan instanceof GensokyoModule)) return;
        GensokyoModule module = (GensokyoModule) chan;
        HttpClient http = module.getHttpClient();

        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("no", "No idea"));
        UrlEncodedFormEntity entity;
        try {
            entity = new UrlEncodedFormEntity(pairs, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e1) {
            throw new RuntimeException(e1);
        }

        HttpRequestModel request = HttpRequestModel.builder().setPOST(entity).setNoRedirect(true).build();
        HttpResponseModel response = null;
        try {
            String url = "https://" + GensokyoModule.CHAN_DOMAIN + "/border.php";
            response = HttpStreamer.getInstance().getFromUrl(url, request, http, null, null);
            module.persistCookies();
        } finally {
            if (response != null) response.release();
        }

        Bitmap newCaptcha = fetchBarrierPage(http, module);
        showBarrierDialog(activity, task, callback, newCaptcha);
    }

    private Bitmap fetchBarrierPage(HttpClient http, GensokyoModule module) throws Exception {
        HttpRequestModel request = HttpRequestModel.builder().setGET().build();
        HttpResponseModel response = null;
        try {
            String rootUrl = "https://" + GensokyoModule.CHAN_DOMAIN + "/";
            response = HttpStreamer.getInstance().getFromUrl(rootUrl, request, http, null, null);
            module.persistCookies();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            IOUtils.copyStream(response.stream, baos);
            return decodeCaptcha(baos.toByteArray());
        } finally {
            if (response != null) response.release();
        }
    }

    private Bitmap decodeCaptcha(byte[] bytes) {
        if (bytes == null) return null;
        String html = new String(bytes);
        Matcher matcher = BASE64_PATTERN.matcher(html);
        if (matcher.find()) {
            byte[] imageData = Base64.decode(matcher.group(1), Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
        }
        return null;
    }

}
