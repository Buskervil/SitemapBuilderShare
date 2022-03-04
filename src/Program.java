import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Program {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

        long start = System.nanoTime();
        if (args.length < 2){
            throw new IllegalArgumentException("принимаемые аргументы: [URL] [destinationFilePath] [глубина обхода {опционально, по умолчанию равна 1}]");
        }

        String url = args[0];
        String destinationPath = args[1];
        int maxNestedLevel = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        JsoupParser.getAllLinksFrom(url);

        SiteMap map = new SiteMap();
        String siteMap = map.BuildMdSiteMap(url, maxNestedLevel);
        System.out.println(siteMap);
        try(FileWriter writer = new FileWriter(destinationPath, false))
        {
            writer.write(siteMap);
            writer.flush();
        }
        catch(IOException ex){
            System.out.println(ex.getMessage());
        }
        System.out.println(String.format("Executed by %d s",
                (System.nanoTime() - start) / (1000_000_000)));
    }
}
