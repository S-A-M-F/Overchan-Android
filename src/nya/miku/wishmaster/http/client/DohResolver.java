package nya.miku.wishmaster.http.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import nya.miku.wishmaster.R;
import nya.miku.wishmaster.common.Logger;
import nya.miku.wishmaster.common.MainApplication;
import nya.miku.wishmaster.lib.org_json.JSONArray;
import nya.miku.wishmaster.lib.org_json.JSONObject;

import cz.msebera.android.httpclient.conn.DnsResolver;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Dns;
import android.os.Build;
import android.widget.Toast;

public class DohResolver implements DnsResolver {
    private static final String TAG = "DohResolver";
    private static final long CACHE_TTL_MS = 60000;

    private static DohResolver instance;

    private final OkHttpClient bootstrapClient;
    private final Map<String, CacheEntry> cache;

    private DohResolver() {
        this.bootstrapClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .dns(new BootstrapDns())
                .build();
        this.cache = new HashMap<String, CacheEntry>();
    }

    public static synchronized void init() {
        if (instance == null && Build.VERSION.SDK_INT >= 9) {
            instance = new DohResolver();
        }
    }

    public static DohResolver getInstance() {
        return instance;
    }

    public void clearCache() {
        synchronized (cache) {
            cache.clear();
        }
    }

    private String getPrefDnsMode() {
        return MainApplication.getInstance().settings.getDnsMode();
    }

    private String getPrefDohUrl() {
        return MainApplication.getInstance().settings.getDohUrl();
    }

    private DnsMode parseMode(String modeStr) {
        String systemOnly = MainApplication.getInstance().resources.getString(R.string.pref_dns_mode_value_system_only);
        String dohFallback = MainApplication.getInstance().resources.getString(R.string.pref_dns_mode_value_doh_fallback);
        String dohCompare = MainApplication.getInstance().resources.getString(R.string.pref_dns_mode_value_doh_compare);
        String dohStrict = MainApplication.getInstance().resources.getString(R.string.pref_dns_mode_value_doh_strict);

        if (dohFallback.equals(modeStr)) return DnsMode.DOH_WITH_FALLBACK;
        if (dohCompare.equals(modeStr)) return DnsMode.DOH_AND_COMPARE;
        if (dohStrict.equals(modeStr)) return DnsMode.DOH_STRICT;
        return DnsMode.SYSTEM_ONLY;
    }

    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        if (instance == null) {
            return InetAddress.getAllByName(host);
        }

        DnsMode mode = parseMode(getPrefDnsMode());

        if (mode == DnsMode.SYSTEM_ONLY) {
            return InetAddress.getAllByName(host);
        }

        InetAddress[] dohResult = null;
        IOException dohError = null;

        try {
            dohResult = resolveViaDoh(host);
        } catch (IOException e) {
            dohError = e;
        }

        if (dohResult != null && mode == DnsMode.DOH_AND_COMPARE) {
            InetAddress[] systemResult = null;
            try {
                systemResult = InetAddress.getAllByName(host);
            } catch (UnknownHostException e) {
            }
            if (systemResult != null && !addressSetsEqual(dohResult, systemResult)) {
                reportMismatch(host, dohResult, systemResult);
            }
            return dohResult;
        }

        if (dohResult != null) {
            return dohResult;
        }

        if (mode == DnsMode.DOH_STRICT) {
            throw new UnknownHostException(host + " (DoH strict mode, no fallback)");
        }

        if (dohError != null) {
            Logger.e(TAG, "DoH resolution failed for " + host + ", falling back to system DNS", dohError);
        }
        return InetAddress.getAllByName(host);
    }

    private InetAddress[] resolveViaDoh(String host) throws IOException {
        synchronized (cache) {
            CacheEntry entry = cache.get(host);
            if (entry != null && (System.currentTimeMillis() - entry.timestamp) < CACHE_TTL_MS) {
                if (entry.error != null) throw entry.error;
                return entry.addresses;
            }
        }

        Response response = null;
        try {
            String dohUrl = getPrefDohUrl();
            HttpUrl url = HttpUrl.parse(dohUrl);
            if (url == null) {
                throw new IOException("Invalid DoH URL: " + dohUrl);
            }

            HttpUrl queryUrl = url.newBuilder()
                    .addQueryParameter("name", host)
                    .addQueryParameter("type", "A")
                    .addQueryParameter("ct", "application/dns-json")
                    .build();

            Request request = new Request.Builder()
                    .url(queryUrl)
                    .header("Accept", "application/dns-json")
                    .build();

            response = bootstrapClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                if (response.code() == 404) {
                    throw new UnknownHostException(host);
                }
                throw new IOException("DoH server returned " + response.code());
            }

            if (response.body() == null) {
                throw new IOException("DoH server returned empty body");
            }

            String body = response.body().string();
            JSONObject json = new JSONObject(body);

            int status = json.optInt("Status", -1);
            if (status != 0) {
                throw new IOException("DoH query returned status " + status);
            }

            JSONArray answers = json.optJSONArray("Answer");
            List<InetAddress> addresses = new ArrayList<InetAddress>();

            if (answers != null) {
                for (int i = 0; i < answers.length(); i++) {
                    JSONObject answer = answers.getJSONObject(i);
                    int type = answer.optInt("type", -1);
                    if (type == 1 || type == 28) {
                        String data = answer.optString("data", null);
                        if (data != null) {
                            try {
                                addresses.add(InetAddress.getByName(data));
                            } catch (UnknownHostException e) {
                            }
                        }
                    }
                }
            }

            if (addresses.isEmpty()) {
                throw new UnknownHostException(host);
            }

            InetAddress[] result = addresses.toArray(new InetAddress[addresses.size()]);

            synchronized (cache) {
                cache.put(host, new CacheEntry(result, null));
            }
            return result;
        } catch (Exception e) {
            IOException ioEx;
            if (e instanceof IOException) {
                ioEx = (IOException) e;
            } else {
                ioEx = new IOException("DoH resolution failed", e);
            }
            synchronized (cache) {
                cache.put(host, new CacheEntry(null, ioEx));
            }
            throw ioEx;
        } finally {
            if (response != null) {
                try { response.close(); } catch (Exception e) {}
            }
        }
    }

    private static boolean addressSetsEqual(InetAddress[] a, InetAddress[] b) {
        Set<String> setA = new HashSet<String>();
        for (InetAddress addr : a) {
            if (addr != null) setA.add(addr.getHostAddress());
        }
        Set<String> setB = new HashSet<String>();
        for (InetAddress addr : b) {
            if (addr != null) setB.add(addr.getHostAddress());
        }
        return setA.equals(setB);
    }

    private void reportMismatch(String host, InetAddress[] dohResult, InetAddress[] systemResult) {
        try {
            String prefKey = MainApplication.getInstance().resources.getString(R.string.pref_key_dns_mismatch_domains);
            String existing = MainApplication.getInstance().preferences.getString(prefKey, "");
            Set<String> domains = new HashSet<String>();
            if (existing.length() > 0) {
                String[] parts = existing.split(",");
                for (String part : parts) {
                    if (part.length() > 0) domains.add(part);
                }
            }
            domains.add(host);
            StringBuilder sb = new StringBuilder();
            for (String d : domains) {
                if (sb.length() > 0) sb.append(",");
                sb.append(d);
            }
            MainApplication.getInstance().preferences.edit()
                    .putString(prefKey, sb.toString())
                    .apply();

            final String msg = MainApplication.getInstance().resources.getString(
                    R.string.dns_mismatch_toast_format, host);
            Logger.w(TAG, msg + " DoH: " + Arrays.toString(dohResult) + " System: " + Arrays.toString(systemResult));

            android.os.Handler handler = new android.os.Handler(MainApplication.getInstance().getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainApplication.getInstance(), msg, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Logger.e(TAG, "Failed to report DNS mismatch", e);
        }
    }

    private static InetAddress toAddr(String hostname, int b1, int b2, int b3, int b4) {
        try {
            return InetAddress.getByAddress(hostname, new byte[]{(byte) b1, (byte) b2, (byte) b3, (byte) b4});
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static class BootstrapDns implements Dns {
        @Override
        public List<InetAddress> lookup(String hostname) {
            InetAddress addr;
            if ("cloudflare-dns.com".equals(hostname)) {
                addr = toAddr(hostname, 1, 1, 1, 1);
                if (addr != null) return Collections.singletonList(addr);
            }
            if ("dns.google".equals(hostname)) {
                addr = toAddr(hostname, 8, 8, 8, 8);
                if (addr != null) return Collections.singletonList(addr);
            }
            if ("dns.quad9.net".equals(hostname)) {
                addr = toAddr(hostname, 9, 9, 9, 9);
                if (addr != null) return Collections.singletonList(addr);
            }
            try {
                return Dns.SYSTEM.lookup(hostname);
            } catch (UnknownHostException e) {
                return Collections.emptyList();
            }
        }
    }

    private static class CacheEntry {
        final InetAddress[] addresses;
        final IOException error;
        final long timestamp;

        CacheEntry(InetAddress[] addresses, IOException error) {
            this.addresses = addresses;
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
