import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

public class MainWatch implements Runnable {

    private String folder;
    private List<String> ips;
    private int seq;
    private int port;
    private RecentlyUpdated recentlyUpdated;
    private boolean subFolder;

    public MainWatch(String folder, List<String> ips, int seq, int port, RecentlyUpdated ru, boolean subfolder) {

        this.folder = folder;
        this.ips = ips;
        this.seq = seq;
        this.port = port;
        this.recentlyUpdated = ru;
        this.subFolder = subfolder;

    }

    public void watchDirectoryPath(Path path) {

        // Check if path is a folder
        try {
            Boolean isFolder = (Boolean) Files.getAttribute(path,
                    "basic:isDirectory", NOFOLLOW_LINKS);
            if (!isFolder) {
                throw new IllegalArgumentException("Path: " + path
                        + " is not a folder");
            }
        } catch (IOException e) {
            // Folder does not exists
            e.printStackTrace();
        }

        System.out.println("Watching path: " + path);

        // We obtain the file system of the Path
        FileSystem fs = path.getFileSystem();

        // We create the new WatchService using the new try() block
        try (WatchService service = fs.newWatchService()) {

            // We register the path to the service
            // We watch for creation events
            path.register(service, ENTRY_CREATE, ENTRY_MODIFY);

            // Start the infinite polling loop
            WatchKey key = null;
            while (true) {
                key = service.take();

                // Dequeueing events
                Kind<?> kind = null;
                Path newPath = null;
                for (WatchEvent<?> watchEvent : key.pollEvents()) {
                    // Get the type of the event
                    kind = watchEvent.kind();
                    if (OVERFLOW == kind) {
                        continue;
                    } else if (ENTRY_CREATE == kind) {

                        newPath = ((WatchEvent<Path>) watchEvent)
                                .context();

                        String f = path.toString() + "/" + newPath.toString();
                        File file = new File(f.trim());

                        // verify if the file was recently updated to avoid cicles

                        if (this.recentlyUpdated.containsFile(file.getName())) {
                            this.recentlyUpdated.removeFile(file.getName());

                        } else {

                            if (!file.isDirectory()) {
                                for (int i = 0; i < this.ips.size(); i++) {
                                    SendHandler sh = new SendHandler(3, this.ips.get(i), this.port, seq, file,
                                            this.subFolder);
                                    Thread send = new Thread(sh);
                                    send.start();
                                    this.seq++;
                                }
                            }

                        }

                    } else if (ENTRY_MODIFY == kind) {

                        newPath = ((WatchEvent<Path>) watchEvent)
                                .context();

                    }

                }

                if (!key.reset()) {
                    break; // loop
                }
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

    }

    public void run() {

        File dir = new File(this.folder);
        watchDirectoryPath(dir.toPath());
    }
}