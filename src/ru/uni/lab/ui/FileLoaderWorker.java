package ru.uni.lab.ui;

import ru.uni.lab.service.DataService;

import javax.swing.*;
import java.io.File;
import java.util.concurrent.ExecutionException;

public class FileLoaderWorker extends SwingWorker<Void, Void> {

    private final File knpFile;
    private final File xmlFile;
    private final File dimFile;
    private final Runnable onSuccess;
    private final java.util.function.Consumer<Exception> onError;
    private long duration;

    public FileLoaderWorker(File knpFile, File xmlFile, File dimFile, Runnable onSuccess, java.util.function.Consumer<Exception> onError) {
        this.knpFile = knpFile;
        this.xmlFile = xmlFile;
        this.dimFile = dimFile;
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    @Override
    protected Void doInBackground() throws Exception {
        long start = System.currentTimeMillis();
        DataService.getInstance().logEvent("Worker thread started. Calling DataService.loadData...");
        DataService.getInstance().loadData(knpFile, xmlFile, dimFile);
        long end = System.currentTimeMillis();
        this.duration = end - start;
        DataService.getInstance().logEvent("Worker thread finished in " + duration + " ms.");
        return null;
    }
    
    public long getDuration() {
        return duration;
    }

    @Override
    protected void done() {
        try {
            get(); // Check for exceptions
            if (onSuccess != null) {
                onSuccess.run();
            }
        } catch (InterruptedException e) {
            DataService.getInstance().logEvent("Loading interrupted!");
        } catch (ExecutionException e) {
            if (onError != null) {
                onError.accept((Exception) e.getCause());
            } else {
                e.printStackTrace();
            }
        }
    }
}
