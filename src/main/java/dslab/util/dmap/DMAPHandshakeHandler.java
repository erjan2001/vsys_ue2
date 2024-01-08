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
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
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

        String decryptedMessage = this.rsaDecryption(rsaEncrypted,componentId);
        //check for secret key and iv
        if(!decryptedMessage.startsWith("ok")) {
            throw new HandshakeException("handshake failed on first response");
        }

        String[] parts = decryptedMessage.split(" ");

        byte[] challenge = Base64.getDecoder().decode(parts[1]);
        byte[] secretKey = Base64.getDecoder().decode(parts[2]);
        byte[] iv = Base64.getDecoder().decode(parts[3]);

        this.aesHandler = new AESHandler(secretKey, iv);

        try
        {
            printWriter.println(aesHandler.aesEncryption("ok " + parts[1]));
        } catch (IllegalBlockSizeException | BadPaddingException e)
        {
            throw new RuntimeException(e);
        }

        encryptedMessage = bufferedReader.readLine();

        try
        {
            if(!aesHandler.aesDecryption(encryptedMessage).equals("ok")){
                throw new HandshakeException("handshake failed on aes decryption");
            }
        } catch (IllegalBlockSizeException | BadPaddingException e)
        {
            throw new RuntimeException(e);
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

        byte[] challenge = new byte[32];
        secureRandom.nextBytes(challenge);

        String challengeString = Base64.getEncoder().encodeToString(challenge);

        byte[] secretKey;
        byte[] iv = new byte[16];
        try
        {
            secretKey = KeyGenerator.getInstance("AES").generateKey().getEncoded();
            secureRandom.nextBytes(iv);
            this.aesHandler = new AESHandler(secretKey, iv);

        } catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e);
        }

        String secretKeyString = Base64.getEncoder().encodeToString(secretKey);
        String ivString = Base64.getEncoder().encodeToString(iv);

        this.aesHandler = new AESHandler(secretKey, iv);


    }

    public String rsaDecryption(byte[] encryptedMessage, String componentId) throws IOException
    {
        String decryptedMessage;
        try
        {
            PrivateKey privateKey = Keys.readPrivateKey(new File("./keys/server/" + componentId + ".der"));
            Cipher decryptCipher  = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            decryptCipher .init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedMessageBytes = decryptCipher.doFinal(encryptedMessage);
            decryptedMessage = new String(decryptedMessageBytes, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException |
                 BadPaddingException e)
        {
            throw new RuntimeException(e);
        }
        return decryptedMessage;
    }

    public String rsaEncryption (byte[] decryptedMessage){
        String encryptedMessage;
        try
        {
            PrivateKey privateKey = Keys.readPrivateKey(new File("./keys/servers/" + componentId + ".der"));
            Cipher encryptCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            encryptCipher.init(Cipher.ENCRYPT_MODE, privateKey);
            byte[] encryptedMessageBytes = encryptCipher.doFinal(decryptedMessage);
            encryptedMessage = Base64.getEncoder().encodeToString(encryptedMessageBytes);
        } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 IllegalBlockSizeException | BadPaddingException e)
        {
            throw new RuntimeException(e);
        }
        return encryptedMessage;

    }

    public String getComponentId()
    {
        return componentId;
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
