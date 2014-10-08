package es.encloud.service;

import es.encloud.model.EncFile;
import javafx.concurrent.Task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by jesus on 12/09/2014.
 */
public class ZipService extends Task<Void> {

    public enum Operation {ZIP, UNZIP}


    @Override
    protected Void call() throws Exception {
        try {
            if (operation == Operation.ZIP) {
                zip();
            } else {
                unzip();
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private List<EncFile> files;
    private Operation operation;
    // we will save the target when zipping, and the folder when unzipping
    private File target;

    public ZipService(List<EncFile> files, File target, Operation operation) {
        this.files = files;
        this.operation = operation;
        this.target = target;
    }


    private void zip() throws IOException {
        FileOutputStream fos = new FileOutputStream(target);
        ZipOutputStream zos = new ZipOutputStream(fos);
        byte[] b = new byte[1024];

        // Calculate the total length, will be used to calculate the progress
        double totalLength = 0;
        double processedLength = 0;
        for (EncFile file : files) {
            totalLength += file.getLength();
        }

        for (EncFile file : files) {
            File input = new File(file.getFullPath());
            FileInputStream fis = new FileInputStream(input);
            zos.putNextEntry(new ZipEntry(input.getName()));
            int count;
            while ((count = fis.read(b)) != -1) {
                zos.write(b, 0, count);
                processedLength += count;
                updateProgress(processedLength / totalLength, 1);
            }
            zos.closeEntry();
            fis.close();
        }
        zos.close();
    }


    private void unzip() throws IOException {

        //Calculate total size
        double totalLength = 0;
        double processedLength = 0;
        for (EncFile file : files) {
            if (file.getName().endsWith(".zip")) {
                totalLength += file.getLength();
            }
        }


        for (EncFile file : files) {
            if (!file.getName().endsWith(".zip")) {
                //Only zip files can be unzipped
                continue;
            }
            FileInputStream fis = new FileInputStream(file.getFullPath());
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {

                if(entry.isDirectory()){
                    new File(target.getAbsolutePath() + File.separator + entry.getName()).mkdirs();
                    zis.closeEntry();
                    continue;
                }
                File newFile = new File(target.getAbsolutePath() + File.separator + entry.getName());
                FileOutputStream fos = new FileOutputStream(newFile);
                byte[] b = new byte[1024];
                int count;
                while ((count = zis.read(b)) != -1) {
                    fos.write(b, 0, count);
                    processedLength += count;
                    updateProgress(processedLength / totalLength, 1);
                }
                fos.close();
                zis.closeEntry();
            }
            zis.close();
            fis.close();
        }
    }

}
