package dslab.util.dmap;

import dslab.exceptions.HandshakeException;
import dslab.util.Keys;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Dictionary;

public class DMAPHandshakeHandler
{
    private String componentId;
    private AESHandler aesHandler;

    public DMAPHandshakeHandler(String componentId)
    {
        this.componentId = componentId;
    }

    public void handshakeServerSide(BufferedReader bufferedReader, PrintWriter printWriter) throws IOException, HandshakeException {

        printWriter.println("ok " + this.componentId);
        String encryptedMessage = bufferedReader.readLine();
        byte[] rsaEncrypted = Base64.getDecoder().decode(encryptedMessage);

        String decryptedMessage = this.rsaDecryption(rsaEncrypted);
        //check for secret key and iv
        if(!decryptedMessage.startsWith("ok")) {
            throw new HandshakeException("handshake failed on first response");
        }

        String[] parts = decryptedMessage.split(" ");

        byte[] challenge = Base64.getDecoder().decode(parts[1]);
        byte[] secretKey = Base64.getDecoder().decode(parts[2]);
        byte[] iv = Base64.getDecoder().decode(parts[3]);

        this.aesHandler = new AESHandler(secretKey, iv);


        printWriter.println(aesHandler.aesEncryption("ok " + parts[1]));

        encryptedMessage = bufferedReader.readLine();


        if(!aesHandler.aesDecryption(encryptedMessage).equals("ok")){
            throw new HandshakeException("handshake failed on aes decryption");
        }

    }

    public void handshakeClientSide(BufferedReader bufferedReader, PrintWriter printWriter) throws IOException, HandshakeException {
        SecureRandom secureRandom = new SecureRandom();
        printWriter.println("startsecure");
        String line = bufferedReader.readLine();
        String[] parts = line.split(" ");
        if(!parts[0].equals("ok")){
            throw new HandshakeException("handshake failed on client side");
        }

        String serverComponentId = parts[1];

        byte[] challenge = new byte[32];
        secureRandom.nextBytes(challenge);

        String challengeString = Base64.getEncoder().encodeToString(challenge);

        byte[] secretKey;
        byte[] iv = new byte[16];
        try
        {
            secretKey = KeyGenerator.getInstance("AES").generateKey().getEncoded();
            secureRandom = SecureRandom.getInstanceStrong();
            secureRandom.nextBytes(iv);
            this.aesHandler = new AESHandler(secretKey, iv);

        } catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e);
        }

        String secretKeyString = Base64.getEncoder().encodeToString(secretKey);
        String ivString = Base64.getEncoder().encodeToString(iv);

        printWriter.println(this.rsaEncryption(("ok " + challengeString + " " + secretKeyString + " " + ivString).getBytes(), serverComponentId));

        this.aesHandler = new AESHandler(secretKey, iv);


        String returnedChallenge = bufferedReader.readLine();
        returnedChallenge = aesHandler.aesDecryption(returnedChallenge);
        parts = returnedChallenge.split(" ");
        System.out.println(parts[1]);
        if(!parts[1].equals(challengeString)){
            throw new HandshakeException("error receiving challenge");
        }

        printWriter.println(aesHandler.aesEncryption("ok"));


    }

    public String rsaDecryption(byte[] encryptedMessage) throws IOException
    {
        String decryptedMessage;
        try
        {
            PrivateKey privateKey = Keys.readPrivateKey(new File("./keys/server/" + this.componentId + ".der"));
            Cipher decryptCipher  = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedMessageBytes = decryptCipher.doFinal(encryptedMessage);
            decryptedMessage = new String(decryptedMessageBytes, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException |
                 BadPaddingException e)
        {
            throw new RuntimeException(e);
        }

        return decryptedMessage;
    }

    public String rsaEncryption (byte[] decryptedMessage, String componentId){
        String encryptedMessage = "ok test";
        try
        {
            System.out.println("./keys/client/" + componentId + "_pub.der");
            PublicKey publicKey = Keys.readPublicKey(new File("./keys/client/" + componentId + "_pub.der"));

            Cipher encryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedMessageBytes = encryptCipher.doFinal(decryptedMessage);
            encryptedMessage = Base64.getEncoder().encodeToString(encryptedMessageBytes);
        } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException e)
        {
            System.out.println("CATCH");
            System.err.println(e.getMessage());
        }
        return encryptedMessage;
    }

    public String getComponentId()
    {
        return this.componentId;
    }

    public void setComponentId(String componentId)
    {
        this.componentId = componentId;
    }

    public AESHandler getAesHandler()
    {
        return aesHandler;
    }

    public void setAesHandler(AESHandler aesHandler)
    {
        this.aesHandler = aesHandler;
    }
}
