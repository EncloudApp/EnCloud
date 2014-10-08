/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.jesus.encloud.service;


import es.jesus.encloud.model.EncFile;
import javafx.concurrent.Task;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.security.Security;

/**
 * Class in charge of encrypting an decrypting a file
 *
 * @author jesus
 */
public class EncryptionService extends Task<EncFile> {

    static {
        try {
            // Remove java restriction. Do not use in USA
            final Class<?> jceSecurity = Class.forName("javax.crypto.JceSecurity");
            final Field isRestrictedField = jceSecurity.getDeclaredField("isRestricted");
            isRestrictedField.setAccessible(true);
            isRestrictedField.set(null, false);
            // Use BC provider
            Security.addProvider(new BouncyCastleProvider());
        } catch (IllegalArgumentException | SecurityException | ClassNotFoundException | IllegalAccessException | NoSuchFieldException ex) { //|
            ex.printStackTrace();
        }
    }


    public enum Operation {ENCRYPT, DECRYPT}

    private Operation operation;
    private EncFile file;
    private String password;
    private File targetFolder;


    public EncryptionService(EncFile file, String password, Operation operation) {
        this.file = file;
        this.targetFolder = targetFolder;
        this.operation = operation;
        this.password = password;
    }

    @Override
    protected EncFile call() throws Exception {
        try {
            if (operation.equals(Operation.ENCRYPT)) {
                return encrypt();
            }
            return decrypt();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private EncFile encrypt() throws Exception {

        // Salt
        byte[] salt = {
                (byte) 0x7e, (byte) 0xc8, (byte) 0xee, (byte) 0x99,
                (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c
        };

        // Iteration count
        int count = 2555;

        // Cipher
        Cipher cipher = Cipher.getInstance("PBEWITHSHA256AND256BITAES-CBC-BC", "BC");

        // Key
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());
        SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBEWITHSHA256AND256BITAES-CBC-BC");
        SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);


        PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);
        cipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
        // open file
        File fileIn = new File(file.getFullPath());
        double totalLength = fileIn.length();
        double processedLength = 0;
        int readLength = 0;
        File fileOut = generateOutputFile(file.getFullPath(), Operation.ENCRYPT);

        FileInputStream fis = new FileInputStream(fileIn);
        FileOutputStream fos = new FileOutputStream(fileOut);
        CipherInputStream cis = new CipherInputStream(fis, cipher);
        byte[] c = new byte[1024];

        while ((readLength = cis.read(c)) != -1) {
            fos.write(c, 0, readLength);
            processedLength += readLength;
            updateProgress(processedLength / totalLength, 1);
        }
        cis.close();
        fis.close();
        fos.close();
        updateProgress(1, 1);
        return new EncFile(fileOut.getName(), fileOut.length(), fileOut.getAbsolutePath());
    }

    private EncFile decrypt() throws Exception {
        Cipher cipher = Cipher.getInstance("PBEWITHSHA256AND256BITAES-CBC-BC", "BC");
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());
        SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBEWITHSHA256AND256BITAES-CBC-BC");
        SecretKey pbeKey = keyFac.generateSecret(pbeKeySpec);

        // Salt
        byte[] salt = {
                (byte) 0x7e, (byte) 0xc8, (byte) 0xee, (byte) 0x99,
                (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c
        };

        // Iteration count
        int count = 2555;

        PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);
        cipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
        // open file
        File fileIn = new File(file.getFullPath());
        double totalLength = fileIn.length();
        double processedLength = 0;
        int readLength = 0;
        File fileOut = generateOutputFile(file.getFullPath(), Operation.DECRYPT);
        FileInputStream fis = new FileInputStream(fileIn);
        FileOutputStream fos = new FileOutputStream(fileOut);
        CipherInputStream cis = new CipherInputStream(fis, cipher);
        byte[] c = new byte[1024];

        while ((readLength = cis.read(c)) != -1) {
            fos.write(c, 0, readLength);
            processedLength += readLength;
            updateProgress(processedLength / totalLength, 1);
        }
        cis.close();
        fis.close();
        fos.close();
        updateProgress(1, 1);
        return new EncFile(fileOut.getName(), fileOut.length(), fileOut.getAbsolutePath());
    }

    private File generateOutputFile(String fullPath, Operation operation) {
        File out;
        if (operation == Operation.ENCRYPT) {
            // add ".enc" suffix
            out = new File(fullPath + ".enc");
        } else {
            // remove ".enc" suffix
            out = new File(fullPath.substring(0, fullPath.length() - 4));
        }

        for (int i = 1; out.exists(); i++) {
            // if the file exist, add a number to it
            if (operation == Operation.ENCRYPT) {
                // add ".enc" suffix
                out = new File(fullPath + "." + i + ".enc");
            } else {
                // remove ".enc" suffix
                String path = fullPath.substring(0, fullPath.length() - 4);
                //TODO use string builder
                // TODO what if the file doesn't have a '.'
                path = path.substring(0, path.lastIndexOf(".")) + "." + i + path.substring(path.lastIndexOf("."));
                out = new File(path);
            }

        }
        //Return the file
        return out;
    }

}
