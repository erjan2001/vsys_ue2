package dslab.transfer;

import dslab.util.Config;
import dslab.util.Email;
import dslab.util.Globals;
import dslab.util.dmtp.DMTPSend;
import dslab.util.dmtp.DMTPReceiverTransfer;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class TransferServerListenerThread extends Thread implements Globals {

    private final ServerSocket transferServerSocket;
    private final Config domains;
    private final Config config;

    private ThreadPoolExecutor threadPoolExecutor;

    public TransferServerListenerThread(ServerSocket transferServerSocket, Config config, Config domains) {
        this.transferServerSocket = transferServerSocket;
        this.domains = domains;
        this.config = config;
        this.threadPoolExecutor =
                (ThreadPoolExecutor) Executors.newFixedThreadPool(THREADPOOL_SIZE);
    }

    @Override
    public void run(){
        while(true){
            try {
                Socket clientSocket = this.transferServerSocket.accept();

                BlockingQueue<Email> emailBlockingQueue = new LinkedBlockingQueue<>();

                DMTPSend dmtpSend =new DMTPSend(this.domains,
                        emailBlockingQueue, new ConcurrentHashMap<>(), this.config, this.transferServerSocket);
                this.threadPoolExecutor.execute(new DMTPReceiverTransfer(clientSocket,
                        emailBlockingQueue, dmtpSend));

            }catch (SocketException e){
                this.shutdown();
                break;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public void shutdown(){
        this.threadPoolExecutor.shutdown();
        try {
            // Optionally, wait for the ThreadPool to finish tasks
            this.threadPoolExecutor.awaitTermination(1, TimeUnit.SECONDS);
             if(!this.threadPoolExecutor.isTerminated()){
                 this.threadPoolExecutor.shutdownNow();
             }
        } catch (InterruptedException e) {
            // should never get here
        }
    }
}
