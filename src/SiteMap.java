import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

public class SiteMap {
    public String BuildMdSiteMap(String urlString, int maxNestingLevel) throws IOException {

        URL url = new URL(urlString);
        String host = url.getHost();
        LinkNode root = new LinkNode(urlString, host);
        Set<String> excludingUrls = new HashSet<>();
        excludingUrls.add(urlString);
        buildTree(root, excludingUrls, host, 0, maxNestingLevel);
        return root.toString(0);
    }

    private void buildTree(LinkNode node, Set<String> excludingUrls, String hostName, int currentNestedLevel, int maxNestingLevel) {
        if (currentNestedLevel >= maxNestingLevel)
            return;
        Elements links = JsoupParser.getAllLinksFrom(node.url);
        for (Element link : links) {
            String linkUrl = link.attr("abs:href");
            String linkTitle = link.text();
            if (!excludingUrls.contains(linkUrl) && !excludingUrls.contains(linkTitle) && linkUrl.contains(hostName)) {
                node.addChild(new LinkNode(linkUrl, linkTitle));
                excludingUrls.add(linkUrl);
                excludingUrls.add(linkTitle);
            }
        }

        for (LinkNode link : node.getChilds()) {
            //System.out.println(String.format("Переход по ссылке - %s - %s", link.title, link.url));
            buildTree(link, excludingUrls, hostName, currentNestedLevel + 1, maxNestingLevel);
        }
    }
}

class LinkNode {
    public final String url;
    public final String title;
    private final Set<LinkNode> childs;


    LinkNode(String url, String title) {
        this.url = url;
        this.title = title;
        childs = new HashSet<>();
    }

    public boolean addChild(LinkNode child) {
        if (child == null)
            return false;
        childs.add(child);
        return true;
    }

    public Set<LinkNode> getChilds() {
        return new HashSet<>(childs);
    }

    public String toString() {
        return String.format("[%s](%s)", title, url);
    }

    public String toString(int nestedLevel){
        StringBuilder builder = new StringBuilder();
        builder
                .append("\t".repeat(nestedLevel))
                .append(this)
                .append("\n");

        for (LinkNode nested : childs) {
            builder.append(nested.toString(nestedLevel + 1));
        }

        return builder.toString();
    }
}
