package dslab.util.dmap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class AESHandler
{

    private byte[] secretKey;
    private byte[] iv;

    private Cipher decryptCipher;
    private Cipher encryptCipher;

    public AESHandler(byte[] secretKey, byte[] iv)
    {
        this.secretKey = secretKey;
        this.iv = iv;
        try
        {
            this.decryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
            this.encryptCipher = Cipher.getInstance("AES/CTR/NoPadding");

            this.decryptCipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(secretKey, "AES"),new IvParameterSpec(iv));
            this.encryptCipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(secretKey, "AES"),new IvParameterSpec(iv));


        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                 InvalidAlgorithmParameterException e)
        {
            System.out.println("here catched");
            throw new RuntimeException(e);
        }
    }

    public String aesEncryption(String decryptedMessage)
    {
        try
        {
            return Base64.getEncoder().encodeToString(this.encryptCipher.doFinal(decryptedMessage.getBytes()));
        } catch (IllegalBlockSizeException e)
        {
            throw new RuntimeException(e);
        } catch (BadPaddingException e)
        {
            throw new RuntimeException(e);
        }
    }

    public String aesDecryption(String encryptedMessage)
    {
        byte[] decryptedBytes = new byte[0];
        try
        {
            decryptedBytes = this.decryptCipher.doFinal(Base64.getDecoder().decode(encryptedMessage));
        } catch (IllegalBlockSizeException e)
        {
            throw new RuntimeException(e);
        } catch (BadPaddingException e)
        {
            throw new RuntimeException(e);
        }
        return new String(decryptedBytes);
    }
}
