package dslab.mailbox;


import dslab.util.Email;
import dslab.util.Globals;
import dslab.util.dmap.DMAPConnection;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MailboxDMAPServerThread extends Thread implements Globals {

    private ServerSocket dmapServer;
    private Map<String, Map<Integer, Email>> userInbox;
    private Map<String, String> userPassword;
    private ThreadPoolExecutor threadPoolExecutor;
    private Socket clientSocket;
    private String componentId;

    public MailboxDMAPServerThread(ServerSocket dmapServer, Map<String, Map<Integer, Email>> userInbox, Map<String, String> userPassword, String componentId) {
        this.dmapServer = dmapServer;
        this.userInbox = userInbox;
        this.userPassword = userPassword;
        this.threadPoolExecutor =
                (ThreadPoolExecutor) Executors.newFixedThreadPool(THREADPOOL_SIZE);
        this.clientSocket = null;
        this.componentId = componentId;
    }

    @Override
    public void run() {
        while (true) {
            try {
                this.clientSocket = this.dmapServer.accept();
                this.threadPoolExecutor.execute(new DMAPConnection(this.clientSocket, this.userInbox, this.userPassword, this.componentId));
            } catch (SocketException e) {
                this.shutdown();
                break;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private void shutdown() {
        try {
            this.threadPoolExecutor.shutdown();
            this.threadPoolExecutor.awaitTermination(2, TimeUnit.SECONDS);
            if (!this.threadPoolExecutor.isTerminated()) {
                this.threadPoolExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            // should never get here
        }
    }
}
