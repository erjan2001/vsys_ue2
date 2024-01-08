package dslab.util.dmap;

import dslab.util.Email;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;

public class DMAPConnection extends Thread {

    private Socket clientSocket;
    private Map<String, Map<Integer, Email>> userInbox;
    private Map<String, String> userPassword;

    public DMAPConnection(Socket clientSocket, Map<String, Map<Integer, Email>> userInbox, Map<String, String> userPassword) {
        this.clientSocket = clientSocket;
        this.userInbox = userInbox;
        this.userPassword = userPassword;
    }

    @Override
    public void run() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            PrintWriter printWriter = new PrintWriter(this.clientSocket.getOutputStream(),true);

            printWriter.println("ok DMAP");

            String line = "";
            Map<Integer, Email> inbox = null;
            boolean flogin = false;

            while (!Thread.currentThread().isInterrupted() && (line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(" ");

                if (!flogin) {
                    switch (parts[0]) {
                        case "login":
                            if (parts.length == 3) {
                                if (parts[2].equals(userPassword.get(parts[1]))) {
                                    inbox = userInbox.get(parts[1]);
                                    printWriter.println("ok");
                                    flogin = true;
                                } else {
                                    if (userPassword.containsKey(parts[2])) {
                                        printWriter.println("error wrong password");
                                    } else {
                                        printWriter.println("error unknown user");
                                    }
                                }
                            } else {
                                printWriter.println("error no username or password");
                            }
                            break;
                        case "logout":
                        case "list":
                        case "delete":
                        case "show":
                            printWriter.println("error user not logged-in");
                            break;
                        case "quit":
                            printWriter.println("ok bye");
                            this.shutdown();
                            return;
                        default:
                            printWriter.println("error protocol error");
                            this.shutdown();
                            return;
                    }
                } else {
                    switch (parts[0]) {
                        case "login":
                            printWriter.println("error logout before");
                            break;
                        case "list":
                            for (Map.Entry<Integer, Email> e :
                                    inbox.entrySet()) {
                                printWriter.println(e.getKey() + " " + e.getValue().getFrom() + " " + e.getValue().getSubject());
                            }
                            printWriter.println("ok"); // client now knows when list is finished
                            break;
                        case "show":
                            if (parts.length == 2) {
                                int key = Integer.parseInt(parts[1]);
                                if (inbox.containsKey(key)) {
                                    printWriter.println(inbox.get(key).toString());
                                } else {
                                    printWriter.println("error unknown message id");
                                }
                            } else {
                                printWriter.println("error missing message id");
                            }
                            break;
                        case "delete":
                            if (parts.length == 2) {
                                int key = Integer.parseInt(parts[1]);
                                if (inbox.containsKey(key)) {
                                    inbox.remove(key);
                                    printWriter.println("ok");
                                } else {
                                    printWriter.println("error unknown message id");
                                }
                            } else {
                                printWriter.println("error missing message id");
                            }
                            break;
                        case "logout":
                            flogin = false;
                            printWriter.println("ok");
                            break;
                        case "quit":
                            printWriter.println("ok bye");
                            this.shutdown();
                            return;
                        default:
                            printWriter.println("error protocol error");
                            this.shutdown();
                            return;
                    }
                }
            }
        }catch (SocketException e){
            //
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            this.shutdown();
        }
    }

    private void shutdown(){
        if (this.clientSocket != null){
            try {
                this.clientSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
