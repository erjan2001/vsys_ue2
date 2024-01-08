package dslab.util.dmap;

import dslab.util.Keys;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
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
import java.util.Base64;

public class DMAPHandshakeHandler
{

    private Socket clientSocket;
    private String componentId;

    public DMAPHandshakeHandler(Socket clientSocket, String componentId)
    {
        this.clientSocket = clientSocket;
        this.componentId = componentId;
    }

    public void handshakeServerSide(BufferedReader bufferedReader, PrintWriter printWriter) throws IOException
    {

        printWriter.println("ok " + this.componentId);
        String encryptedMessage = bufferedReader.readLine();

        byte[] rsaEncrypted = Base64.getDecoder().decode(encryptedMessage);

        String decryptedMessage = this.rsaDecryption(rsaEncrypted,componentId);
        //check for secret key and iv
        if(!decryptedMessage.startsWith("ok")) {
            //TODO ist das wirlklich gut so?
            throw new RuntimeException();
        }

        String[] parts = decryptedMessage.split(" ");

        byte[] challenge = Base64.getDecoder().decode(parts[1]);
        byte[] secretKey = Base64.getDecoder().decode(parts[2]);
        byte[] iv = Base64.getDecoder().decode(parts[3]);

        //AES decryption --- should be separate method
        Cipher decryptCipher;
        Cipher encryptCipher;
        try
        {
            decryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
            encryptCipher = Cipher.getInstance("AES/CTR/NoPadding");

            decryptCipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(secretKey, "AES"),new IvParameterSpec(iv));
            encryptCipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(secretKey, "AES"),new IvParameterSpec(iv));


        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException e)
        {
            throw new RuntimeException(e);
        }
        // ------
        try
        {
            printWriter.println(aesEncryption("ok " + parts[1], encryptCipher));
        } catch (IllegalBlockSizeException | BadPaddingException e)
        {
            throw new RuntimeException(e);
        }

        encryptedMessage = bufferedReader.readLine();

        try
        {
            if(!aesDecryption(encryptedMessage, decryptCipher).equals("ok")){
                //TODO ??
                throw new RuntimeException();
            }
        } catch (IllegalBlockSizeException | BadPaddingException e)
        {
            throw new RuntimeException(e);
        }
    }

    public String rsaDecryption(byte[] encryptedMessage, String componentId) throws IOException
    {
        PrivateKey privateKey = Keys.readPrivateKey(new File("./keys/servers/" + componentId + ".der"));
        String decryptedMessage;
        try
        {
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

    public String aesEncryption(String decryptedMessage, Cipher encryptCipher) throws IllegalBlockSizeException, BadPaddingException
    {
            return Base64.getEncoder().encodeToString(encryptCipher.doFinal(decryptedMessage.getBytes()));
    }

    public String aesDecryption(String encryptedMessage, Cipher decryptCipher) throws IllegalBlockSizeException, BadPaddingException
    {
        byte[] decryptedBytes = decryptCipher.doFinal(Base64.getDecoder().decode(encryptedMessage));
        return new String(decryptedBytes);
    }

}
