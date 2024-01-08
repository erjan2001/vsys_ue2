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
    }

    public String aesEncryption(String decryptedMessage) throws IllegalBlockSizeException, BadPaddingException
    {
        return Base64.getEncoder().encodeToString(this.encryptCipher.doFinal(decryptedMessage.getBytes()));
    }

    public String aesDecryption(String encryptedMessage) throws IllegalBlockSizeException, BadPaddingException
    {
        byte[] decryptedBytes = this.decryptCipher.doFinal(Base64.getDecoder().decode(encryptedMessage));
        return new String(decryptedBytes);
    }
}
