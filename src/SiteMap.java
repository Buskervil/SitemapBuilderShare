import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class SiteMap {
    private final ArrayDeque<LinkNode> queue = new ArrayDeque<>();
    private final Set<String> excludingUrls = new ConcurrentSkipListSet<>();

    public String BuildMdSiteMap(String urlString, int maxNestingLevel) throws IOException, ExecutionException, InterruptedException {

        URL url = new URL(urlString);
        String host = url.getHost();
        LinkNode root = new LinkNode(urlString, host);
        Set<String> excludingUrls = new HashSet<>();
        excludingUrls.add(urlString);
        //buildTree(root, excludingUrls, host, 0, maxNestingLevel);
        buildTreeAsync(root, host, 0, maxNestingLevel);

        return root.toString(0);
    }

    private void buildTreeAsync(LinkNode node, String hostName, int currentNestedLevel, int maxNestingLevel) throws InterruptedException, ExecutionException {
        int nestingLevel = 0;
        queue.add(node);

        while (queue.size() != 0){

            int size = queue.size();
            ArrayList<LinkNode> stepNodes = new ArrayList<>();
            Thread lastThread = new Thread();

            for (int i = 0; i < size; i++){
                LinkNode current = queue.pop();
                stepNodes.add(current);
                Thread thread = new Thread(new CallableBuildTree(current, excludingUrls, hostName, nestingLevel));
                lastThread = thread;
                thread.start();
            }
            lastThread.join();

            for (LinkNode linkNode : stepNodes){
                queue.addAll(linkNode.getChilds());
            }
            nestingLevel++;
        }
        System.out.println("Выходим");
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

class CallableBuildTree implements Runnable {

    LinkNode node;
    Set<String> excludingUrls;
    String hostName;
    int depth;

    public CallableBuildTree(LinkNode node, Set<String> excludingUrls, String hostName, int depth){
        this.node = node;
        this.excludingUrls = excludingUrls;
        this.hostName = hostName;
        this.depth = depth;
    }

    @Override
    public void run() {
        System.out.printf("Зашел поток с id %d\n",Thread.currentThread().getId());
        System.out.printf("Количество уже встреченных ссылок - %d\n",excludingUrls.size());
        System.out.printf("Уровень вложенности %d\n\n",depth);
        if (depth >= 3)
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
        return;
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
        //System.out.println("Я toString, собираю строку");
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
