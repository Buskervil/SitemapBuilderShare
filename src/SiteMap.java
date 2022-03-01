import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SiteMap {
    private final ArrayDeque<LinkNode> queue = new ArrayDeque<>();
    private final Set<String> excludingUrls = new HashSet<>();

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
        excludingUrls.add(hostName);
        ExecutorService threadPool = Executors.newFixedThreadPool(8);
        queue.add(new CallableBuildTree(node, excludingUrls, hostName, 0).call());
//        for(LinkNode ln : queue){
//            for (LinkNode lln : ln.getChilds())
//                System.out.println(lln.title);
//        }
        int depth = 1;
        while(queue.size() > 0){
            int sise = queue.size();
            for (int i = 0; i < sise; i++){
                LinkNode ln = queue.pop();
                //excludingUrls.add(ln.title);
                //excludingUrls.add(ln.url);

                //Thread.sleep(100);
                Set<LinkNode> childs = threadPool.submit(new CallableBuildTree(ln, excludingUrls, hostName, depth)).get().getChilds();
                for (LinkNode lnode : childs){
                    //if(!excludingUrls.contains(lnode.url) && !excludingUrls.contains(lnode.title) && lnode.url.contains(hostName))
                        queue.add(lnode);
                }
//                for(LinkNode lln : queue){
//                    System.out.println(lln.title);
//                }
            }
            depth++;
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

class CallableBuildTree implements Callable<LinkNode> {

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
    public LinkNode call() {
        System.out.println(Thread.currentThread().getId());
        System.out.println(excludingUrls.stream().findFirst().get());
        System.out.println(depth);
        if (depth > 3)
            return node;
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
        return node;
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
        System.out.println("Я toString, собираю строку");
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
