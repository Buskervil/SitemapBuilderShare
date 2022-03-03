import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class SiteMap {

    public String BuildMdSiteMap(String urlString, int maxNestingLevel) throws IOException, InterruptedException {

        URL url = new URL(urlString);
        String host = url.getHost();
        LinkNode root = new LinkNode(urlString, host);
        LinkNode tree = buildTreeAsync(root, host, maxNestingLevel);
        return tree.toString(0);
    }

    private LinkNode buildTreeAsync(LinkNode node, String hostName, int maxNestingLevel) throws InterruptedException {

        ArrayDeque<LinkNode> queue = new ArrayDeque<>();
        Set<String> excludingUrls = new ConcurrentSkipListSet<>();

        queue.add(node);
        excludingUrls.add(node.url);
        int nestingLevel = 0;

        while (queue.size() != 0){
            Thread lastThread = null;
            for (LinkNode linkNode : queue){
                Thread thread = new Thread(new CallableBuildTree(linkNode, excludingUrls, hostName, nestingLevel, maxNestingLevel));
                lastThread = thread;
                thread.start();
            }
            lastThread.join();

            ArrayDeque<LinkNode> nextLevelQueue = new ArrayDeque<>();
            for (LinkNode linkNode : queue){
                nextLevelQueue.addAll(linkNode.getNested());
            }
            queue = nextLevelQueue;
            nestingLevel++;
        }
        return node;
    }
}

class CallableBuildTree implements Runnable {

    LinkNode node;
    Set<String> excludingUrls;
    String hostName;
    int depth;
    int maxDepth;

    public CallableBuildTree(LinkNode node, Set<String> excludingUrls, String hostName, int depth, int maxDepth){
        this.node = node;
        this.excludingUrls = excludingUrls;
        this.hostName = hostName;
        this.depth = depth;
        this.maxDepth = maxDepth;
    }

    @Override
    public void run() {
        if (depth >= maxDepth)
            return;
        Elements links = JsoupParser.getAllLinksFrom(node.url);
        for (Element link : links) {
            String linkUrl = link.attr("abs:href");
            String linkTitle = link.text();

            if (!excludingUrls.contains(linkUrl) && linkUrl.contains(hostName)) {
                node.addNested(new LinkNode(linkUrl, linkTitle));
                excludingUrls.add(linkUrl);
                excludingUrls.add(linkTitle);
            }
        }
    }
}

class LinkNode {
    public final String url;
    public final String title;
    private final Set<LinkNode> nested;


    LinkNode(String url, String title) {
        this.url = url;
        this.title = title;
        nested = new HashSet<>();
    }

    public void addNested(LinkNode child) {
        if (child != null)
            nested.add(child);
    }

    public Set<LinkNode> getNested() {
        return new HashSet<>(nested);
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

        for (LinkNode nested : nested
                .stream()
                .sorted(Comparator.comparing(l -> l.title))
                .collect(Collectors.toList())) {
            builder.append(nested.toString(nestedLevel + 1));
        }

        return builder.toString();
    }
}
