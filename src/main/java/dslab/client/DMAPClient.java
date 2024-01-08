package dslab.client;

import at.ac.tuwien.dsg.orvell.Shell;
import dslab.exceptions.HandshakeException;
import dslab.exceptions.LoginFailException;
import dslab.exceptions.ShowException;
import dslab.util.Config;
import dslab.util.Email;
import dslab.util.dmap.DMAPHandshakeHandler;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class DMAPClient implements Runnable{

    private final MessageClient messageClient;
    private final Config config;
    private final Shell shell;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final DMAPHandshakeHandler dmapHandshakeHandler;

    public DMAPClient(MessageClient messageClient, Config config, Shell shell) {
        this.messageClient = messageClient;
        this.config = config;
        this.shell = shell;
        this.dmapHandshakeHandler = new DMAPHandshakeHandler(null);
    }

    private void setupConnection() throws IOException {
        this.socket = new Socket(this.config.getString("mailbox.host"),this.config.getInt("mailbox.port"));
        this.writer = new PrintWriter(this.socket.getOutputStream());
        this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
    }

    @Override
    public void run() {
        try{
            this.setupConnection();

            if(!reader.readLine().equals("ok DMAP2.0")){
                throw new IOException();
            }

            this.dmapHandshakeHandler.handshakeClientSide(reader, writer);
            this.loginClient();
            while (true) {
                if (this.socket.isClosed()) break;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Error while connecting to mailbox", e);
        } catch (LoginFailException | HandshakeException e) {
            System.err.println(e.getMessage());
        } finally {
            this.messageClient.shutdown();
        }
    }

    public void inbox(){
        try{
            //TODO encrypt
            this.writer.println("list");
            //TODO decrypt
            String resp = reader.readLine();

            if(resp.startsWith("error")){
                writer.println(resp);
            }

            ArrayList<String> ids = new ArrayList<>();
            for (String mail:
                 resp.split("\n")) {
                if(mail.startsWith("ok")){
                    break;
                }
                // mail = id + " " + from + " " + subject
                ids.add(mail.split(" ")[0]);
            }

            if (ids.isEmpty()) {
                this.shell.out().println("no mails in mailbox");
            } else {
                for (String id:
                     ids) {
                    this.shell.out().println(this.show(id));
                }
            }

        } catch (IOException e) {
            throw new UncheckedIOException("Error during connection to mailbox", e);
        } catch (ShowException e) {
            System.err.println(e.getMessage());
        }
    }

    public void shutdown() throws IOException {
        if (this.socket != null) {
            this.socket.close();
        }
        if (this.reader != null) {
            this.reader.close();
        }
        if (this.writer != null) {
            this.writer.close();
        }
    }

    private void loginClient() throws IOException, LoginFailException {
        String toSend = "login " + this.config.getString("mailbox.user") + " " + this.config.getString("mailbox.password");
        //TODO encrypt toSend
        this.writer.println(toSend);
        //TODO decrypt readLine
        if(!this.reader.readLine().equals("ok")) {
            throw new LoginFailException("Login failed for user: " + this.config.getString("mailbox.user"));
        }
    }

    private String show(String id) throws IOException, ShowException {

        //TODO encrypt
        this.writer.println("show " + id);

        //TODO decrypt
        String resp = this.reader.readLine();

        if (resp.startsWith("error")) {
            throw new ShowException(resp);
        }

        Email e = new Email();
        for (String email:
             resp.split("\n\r")) {
            String[] values = email.split(" ");
            if (values.length == 2) {
                switch (values[0]) {
                    case "from":
                        e.setFrom(values[1]);
                        break;
                    case "to":
                        e.setTo(values[1]);
                        break;
                    case "data":
                        e.setData(values[1]);
                        break;
                    case "subject":
                        e.setSubject(values[1]);
                        break;
                    case "hash":
                        e.setHash(values[1]);
                        break;
                }
            }
        }
        return e.toString();
    }

}
