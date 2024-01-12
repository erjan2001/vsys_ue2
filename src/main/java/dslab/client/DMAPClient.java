package dslab.client;

import at.ac.tuwien.dsg.orvell.Shell;
import dslab.exceptions.HandshakeException;
import dslab.exceptions.LoginFailException;
import dslab.exceptions.ShowException;
import dslab.util.Config;
import dslab.util.Email;
import dslab.util.dmap.DMAPHandshakeHandler;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class DMAPClient implements Runnable {

    private final MessageClient messageClient;
    private final Config config;
    private final Shell shell;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final DMAPHandshakeHandler dmapHandshakeHandler;

    public DMAPClient(MessageClient messageClient, Config config, Shell shell, String componentId) {
        this.messageClient = messageClient;
        this.config = config;
        this.shell = shell;
        this.dmapHandshakeHandler = new DMAPHandshakeHandler(componentId);
    }


    private void setupConnection() throws IOException {
        this.socket = new Socket(this.config.getString("mailbox.host"), this.config.getInt("mailbox.port"));
        this.writer = new PrintWriter(this.socket.getOutputStream(), true);
        this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
    }

    @Override
    public void run() {
        try {
            this.setupConnection();
            if (!reader.readLine().equals("ok DMAP2.0")) {
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

    public void inbox() {
        try {
            this.writer.println(this.dmapHandshakeHandler.getAesHandler().aesEncryption("list"));
            ArrayList<String> ids = new ArrayList<>();
            String resp;

            while (!(resp = this.dmapHandshakeHandler.getAesHandler().aesDecryption(reader.readLine())).equals("ok")) {
                if (resp.startsWith("error")) {
                    this.writer.println(resp);
                    break;
                }
                // resp = id + " " + from + " " + subject
                ids.add(resp.split(" ")[0]);
            }
            if (ids.isEmpty()) {
                this.shell.out().println("no mails in mailbox");
            } else {
                for (String id :
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

    public void delete(Shell shell, String id) {
        this.writer.println(this.dmapHandshakeHandler.getAesHandler().aesEncryption("delete " + id));
        try {
            String resp = this.dmapHandshakeHandler.getAesHandler().aesDecryption(this.reader.readLine());
            shell.out().println(resp);
        } catch (IOException e) {
            throw new UncheckedIOException("Error during connection to mailbox", e);
        }
    }

    public void verify(Shell shell, String id, SecretKey secretKey) {
        try {
            Email email = this.show(id);
            System.out.println("hash:" + email.getHash());
            if(Email.generateHash(secretKey,email.getSubject(),email.getData(),email.getFrom(),String.join(",",email.getTo())).equals(email.getHash())){
                shell.out().println("ok");
            } else {
                shell.out().println("error");
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

        this.writer.println(this.dmapHandshakeHandler.getAesHandler().aesEncryption(toSend));
        String resp = this.dmapHandshakeHandler.getAesHandler().aesDecryption(this.reader.readLine());
        if (!resp.equals("ok")) {
            throw new LoginFailException("Login failed for user: " + this.config.getString("mailbox.user"));
        }
    }

    private Email show(String id) throws IOException, ShowException {

        this.writer.println(this.dmapHandshakeHandler.getAesHandler().aesEncryption("show " + id));

        String resp = this.dmapHandshakeHandler.getAesHandler().aesDecryption(this.reader.readLine());
        if (resp.startsWith("error")) {
            throw new ShowException(resp);
        }

        Email e = new Email();
        for (String line :
                resp.split("\n\r")) {
            String[] values = line.split(" ");
            int indexOfSpace = line.indexOf(" ");
            switch (values[0]) {
                case "from":
                    e.setFrom(values[1]);
                    break;
                case "to":
                    e.setTo(values[1]);
                    break;
                case "data":
                    e.setData(line.substring(indexOfSpace + 1));;
                    break;
                case "subject":
                    e.setSubject(line.substring(indexOfSpace + 1));;
                    break;
                case "hash":
                    if(values.length == 2) {
                        e.setHash(values[1]);
                    }
                    break;
            }
        }
        return e;
    }

}
