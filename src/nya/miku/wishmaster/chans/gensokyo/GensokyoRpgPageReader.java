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

public class GensokyoRpgPageReader implements Closeable {
    private static final Pattern SECTION_PATTERN = Pattern.compile(
            "<blockquote>\\s*<p>\\s*<span class=\"replytitle\">(.*?)</span></p>\\s*<p>(.*?)</p>\\s*</blockquote>",
            Pattern.DOTALL);

    private static final Pattern LINK_PATTERN = Pattern.compile(
            "<a\\s+href=['\"]([^'\"]+)['\"]",
            Pattern.CASE_INSENSITIVE);

    private static final String DOMAIN = "https://" + GensokyoModule.CHAN_DOMAIN;
    private static final String ARCH_PATH = "arch";

    private final Reader _in;

    public GensokyoRpgPageReader(Reader reader) {
        _in = reader;
    }

    public GensokyoRpgPageReader(InputStream in) {
        this(new BufferedReader(new InputStreamReader(in)));
    }

    public PostModel[] readPage() throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = _in.read(buf)) != -1) sb.append(buf, 0, n);

        String html = sb.toString();
        List<PostModel> posts = new ArrayList<>();

        Matcher matcher = SECTION_PATTERN.matcher(html);
        while (matcher.find()) {
            String title = matcher.group(1).trim();
            title = title.replaceAll("</?em>", "").replaceAll("</?strong>", "").replaceAll("</?b>", "").replaceAll("</?i>", "").replaceAll("</?span[^>]*>", "");
            title = StringEscapeUtils.unescapeHtml4(title).trim();

            String body = matcher.group(2);
            body = body.replaceAll("(?i)(<br\\s*/?>)(\\s*\\d+\\.html:)", "$1$1$2");
            body = resolveLinks(body);

            PostModel post = new PostModel();
            post.number = String.valueOf(posts.size() + 1);
            post.subject = title;
            post.comment = body;
            post.name = "";
            post.op = posts.isEmpty();
            post.email = "";
            post.trip = "";
            post.parentThread = "index";
            post.timestamp = 0;
            post.attachments = new AttachmentModel[0];

            posts.add(post);
        }

        return posts.toArray(new PostModel[posts.size()]);
    }

    private String resolveLinks(String html) {
        StringBuffer sb = new StringBuffer();
        Matcher m = LINK_PATTERN.matcher(html);
        while (m.find()) {
            String href = m.group(1);
            String resolved;
            if (href.startsWith("http://") || href.startsWith("https://")) {
                resolved = href.replace("gensokyo.4otaku.ru", "gensokyo.4otaku.org");
            } else if (href.startsWith("/")) {
                resolved = DOMAIN + href;
            } else {
                resolved = DOMAIN + "/" + ARCH_PATH + "/" + href;
            }
            m.appendReplacement(sb, "<a href='" + Matcher.quoteReplacement(resolved) + "'");
        }
        m.appendTail(sb);
        return sb.toString();
    }

    @Override
    public void close() throws IOException {
        _in.close();
    }
}
