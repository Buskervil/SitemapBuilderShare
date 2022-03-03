import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class JsoupParser {
    public static Elements getAllLinksFrom(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/33.0.1750.152 Safari/537.36")
                    .timeout(1000)
                    .get();

            return doc.select("a[href]");
        } catch (IOException e) {
            System.err.printf("Произошла ошибка при запросе к серверу:\nЗапрос по URL: %s\nТекст ошибки: %s\n",url, e.getMessage());
            return new Elements();
        }
    }
}
