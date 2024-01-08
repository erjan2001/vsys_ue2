package dslab.util.dmap;

import dslab.util.Email;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;

public class DMAPConnection extends Thread {

    private Socket clientSocket;
    private Map<String, Map<Integer, Email>> userInbox;
    private Map<String, String> userPassword;
    private String componentId;

    public DMAPConnection(Socket clientSocket, Map<String, Map<Integer, Email>> userInbox, Map<String, String> userPassword, String componentId) {
        this.clientSocket = clientSocket;
        this.userInbox = userInbox;
        this.userPassword = userPassword;
        this.componentId = componentId;
    }

    @Override
    public void run() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            PrintWriter printWriter = new PrintWriter(this.clientSocket.getOutputStream(),true);

            printWriter.println("ok DMAP2.0");

            String line = "";
            Map<Integer, Email> inbox = null;
            boolean flogin = false;
            boolean secureConnection = false;
            AESHandler aesHandler = null;

            while (!Thread.currentThread().isInterrupted() && (line = bufferedReader.readLine()) != null) {

                if(secureConnection){
                    line = aesHandler.aesDecryption(line);
                }

                String[] parts = line.split(" ");

                if(parts[0].equals("startsecure")){


                        DMAPHandshakeHandler dmapHandshakeHandler = new DMAPHandshakeHandler(this.clientSocket, componentId);
                        dmapHandshakeHandler.handshakeServerSide(bufferedReader, printWriter);

                        aesHandler = dmapHandshakeHandler.getAesHandler();
                        secureConnection = true;
                        continue;
                }

                if (!flogin) {
                    switch (parts[0]) {
                        case "login":
                            if (parts.length == 3) {
                                if (parts[2].equals(userPassword.get(parts[1]))) {
                                    inbox = userInbox.get(parts[1]);
                                    printWithPossibleEncryption("ok", printWriter, aesHandler, secureConnection);
                                    flogin = true;
                                } else {
                                    if (userPassword.containsKey(parts[2])) {
                                        printWithPossibleEncryption("error wrong password", printWriter, aesHandler, secureConnection);
                                    } else {
                                        printWithPossibleEncryption("error unknown user", printWriter, aesHandler, secureConnection);
                                    }
                                }
                            } else {
                                printWithPossibleEncryption("error no username or password", printWriter, aesHandler, secureConnection);
                            }
                            break;
                        case "logout":
                        case "list":
                        case "delete":
                        case "show":
                            printWithPossibleEncryption("error user not logged-in", printWriter, aesHandler, secureConnection);
                            break;
                        case "quit":
                            printWithPossibleEncryption("ok bye", printWriter, aesHandler, secureConnection);
                            this.shutdown();
                            return;
                        default:
                            printWithPossibleEncryption("error protocol error", printWriter, aesHandler, secureConnection);
                            this.shutdown();
                            return;
                    }
                } else {
                    switch (parts[0]) {
                        case "login":
                            printWithPossibleEncryption("error logout before", printWriter, aesHandler, secureConnection);
                            break;
                        case "list":
                            for (Map.Entry<Integer, Email> e :
                                    inbox.entrySet()) {
                                printWriter.println(e.getKey() + " " + e.getValue().getFrom() + " " + e.getValue().getSubject());
                            }

                            break;
                        case "show":
                            if (parts.length == 2) {
                                int key = Integer.parseInt(parts[1]);
                                if (inbox.containsKey(key)) {
                                    printWriter.println(inbox.get(key).toString());
                                } else {
                                    printWithPossibleEncryption("error unknown message id", printWriter, aesHandler, secureConnection);
                                }
                            } else {
                                printWithPossibleEncryption("error missing message id", printWriter, aesHandler, secureConnection);
                            }
                            break;
                        case "delete":
                            if (parts.length == 2) {
                                int key = Integer.parseInt(parts[1]);
                                if (inbox.containsKey(key)) {
                                    inbox.remove(key);
                                    printWithPossibleEncryption("ok", printWriter, aesHandler, secureConnection);
                                } else {
                                    printWithPossibleEncryption("error unknown message id", printWriter, aesHandler, secureConnection);
                                }
                            } else {
                                printWithPossibleEncryption("error missing message id", printWriter, aesHandler, secureConnection);
                            }
                            break;
                        case "logout":
                            flogin = false;
                            printWithPossibleEncryption("ok", printWriter, aesHandler, secureConnection);
                            break;
                        case "quit":
                            printWithPossibleEncryption("ok bye", printWriter, aesHandler, secureConnection);
                            this.shutdown();
                            return;
                        default:
                            printWithPossibleEncryption("error protocol error", printWriter, aesHandler, secureConnection);
                            this.shutdown();
                            return;
                    }
                }
            }
        }catch (SocketException e){
            //
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (IllegalBlockSizeException e)
        {
            throw new RuntimeException(e);
        } catch (BadPaddingException e)
        {
            throw new RuntimeException(e);
        } finally {
            this.shutdown();
        }
    }

    private void printWithPossibleEncryption(String response, PrintWriter printWriter, AESHandler aesHandler, boolean secureConnection) throws IllegalBlockSizeException, BadPaddingException
    {
        if(secureConnection){
            printWriter.println(aesHandler.aesEncryption(response));
        }else{
            printWriter.println(response);
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
