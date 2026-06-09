package nya.miku.wishmaster.chans.gensokyo;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nya.miku.wishmaster.api.models.AttachmentModel;
import nya.miku.wishmaster.api.models.PostModel;

public class GensokyoRpgGameReader implements Closeable {
    private static final Pattern THREAD_ENTRY_PATTERN = Pattern.compile(
            "(\\d+)\\.html:\\s*" +
            "\\[<a\\s+href='([^']+)'>local[^<]*</a>\\]" +
            "(?:\\s*\\[(?:(?:<a\\s+href='[^']+'>)iichan(?:</a>)|<s>iichan</s>)\\])?" +
            "(?:\\s*[-\\u2014-]\\s*)?" +
            "([^<]*?)" +
            "(?=<br\\s*/?|</p>|$)",
            Pattern.DOTALL);

    private static final String DOMAIN = "https://" + GensokyoModule.CHAN_DOMAIN;
    private static final String ARCH_PATH = "arch";

    private final Reader _in;

    public GensokyoRpgGameReader(Reader reader) {
        _in = reader;
    }

    public GensokyoRpgGameReader(InputStream in) {
        this(new BufferedReader(new InputStreamReader(in)));
    }

    public PostModel[] readPage() throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = _in.read(buf)) != -1) sb.append(buf, 0, n);

        String html = sb.toString();
        List<PostModel> posts = new ArrayList<>();

        Matcher matcher = THREAD_ENTRY_PATTERN.matcher(html);
        while (matcher.find()) {
            String threadId = matcher.group(1);
            String localHref = matcher.group(2);
            String description = matcher.group(3) != null ? matcher.group(3).trim() : "";

            String threadUrl;
            if (localHref.startsWith("/")) {
                threadUrl = DOMAIN + localHref;
            } else {
                threadUrl = DOMAIN + "/" + ARCH_PATH + "/" + localHref;
            }

            description = StringEscapeUtils.unescapeHtml4(description);
            String comment = "<a href=\"" + threadUrl + "\">\u2116" + threadId + "</a>: " + description;

            PostModel post = new PostModel();
            post.number = threadId;
            post.subject = description;
            post.comment = comment;
            post.name = "";
            post.op = false;
            post.email = "";
            post.trip = "";
            post.parentThread = threadId;
            post.timestamp = 0;
            post.attachments = new AttachmentModel[0];

            posts.add(post);
        }

        return posts.toArray(new PostModel[posts.size()]);
    }

    @Override
    public void close() throws IOException {
        _in.close();
    }
}
