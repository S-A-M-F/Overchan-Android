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
import nya.miku.wishmaster.api.models.ThreadModel;

public class GensokyoRpgIndexReader implements Closeable {
    private static final Pattern SECTION_PATTERN = Pattern.compile(
            "<blockquote>\\s*<p>\\s*<span class=\"replytitle\">(.*?)</span></p>\\s*<p>(.*?)</p>\\s*</blockquote>",
            Pattern.DOTALL);

    private static final Pattern GAME_LINK_PATTERN = Pattern.compile(
            "<a href='([^']+)'>([^<]+)</a>");

    private final Reader _in;

    public GensokyoRpgIndexReader(Reader reader) {
        _in = reader;
    }

    public GensokyoRpgIndexReader(InputStream in) {
        this(new BufferedReader(new InputStreamReader(in)));
    }

    public ThreadModel[] readPage() throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = _in.read(buf)) != -1) sb.append(buf, 0, n);

        String html = sb.toString();
        List<ThreadModel> threads = new ArrayList<>();

        Matcher sectionMatcher = SECTION_PATTERN.matcher(html);
        while (sectionMatcher.find()) {
            String sectionName = sectionMatcher.group(1).trim();
            String body = sectionMatcher.group(2);

            Matcher gameMatcher = GAME_LINK_PATTERN.matcher(body);
            while (gameMatcher.find()) {
                String href = gameMatcher.group(1);
                if (href.startsWith("/")) continue;
                if (!href.endsWith(".html")) continue;

                String name = StringEscapeUtils.unescapeHtml4(gameMatcher.group(2));
                int afterLink = gameMatcher.end();
                int brIdx = body.indexOf("<br", afterLink);
                int nextAIdx = body.indexOf("<a", afterLink);
                int endIdx;
                if (brIdx >= 0 && (nextAIdx < 0 || brIdx < nextAIdx)) {
                    endIdx = brIdx;
                } else if (nextAIdx >= 0) {
                    endIdx = nextAIdx;
                } else {
                    endIdx = body.length();
                }

                String desc = body.substring(afterLink, endIdx).trim();
                if (desc.startsWith("\u2014")) desc = desc.substring(1).trim();
                else if (desc.startsWith("-")) desc = desc.substring(1).trim();
                desc = StringEscapeUtils.unescapeHtml4(desc);
                desc = desc.replaceAll("\\s+", " ").trim();

                String threadNumber = href;
                if (threadNumber.endsWith(".html")) {
                    threadNumber = threadNumber.substring(0, threadNumber.length() - 5);
                }

                ThreadModel thread = new ThreadModel();
                thread.threadNumber = threadNumber;
                thread.postsCount = 1;
                thread.attachmentsCount = 0;
                thread.posts = new PostModel[1];
                thread.posts[0] = new PostModel();
                thread.posts[0].number = threadNumber;
                thread.posts[0].subject = name;
                thread.posts[0].comment = desc;
                thread.posts[0].name = sectionName;
                thread.posts[0].op = true;
                thread.posts[0].email = "";
                thread.posts[0].trip = "";
                thread.posts[0].parentThread = threadNumber;
                thread.posts[0].timestamp = 0;
                thread.posts[0].attachments = new AttachmentModel[0];

                threads.add(thread);
            }
        }

        return threads.toArray(new ThreadModel[threads.size()]);
    }

    @Override
    public void close() throws IOException {
        _in.close();
    }
}
