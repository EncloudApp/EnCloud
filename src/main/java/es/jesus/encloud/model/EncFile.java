/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.jesus.encloud.model;

import javafx.beans.property.*;

import java.io.File;

/**
 * @author jesus
 */
public class EncFile {

    private final StringProperty name;
    private final StringProperty type;
    private final String fullPath;
    private final LongProperty length;
    private final BooleanProperty selected;
    private final DoubleProperty progress;

    public EncFile(String name, long length, String fullPath) {
        this.name = new SimpleStringProperty(name);
        this.length = new SimpleLongProperty(length);
        this.selected = new SimpleBooleanProperty(Boolean.FALSE);
        this.type = new SimpleStringProperty(name.substring(
                name.lastIndexOf('.') + 1,
                name.length()).toUpperCase());
        this.fullPath = fullPath;
        this.progress = new SimpleDoubleProperty();
    }
    public EncFile(File file){
        this.name  = new SimpleStringProperty(file.getName());
        this.length = new SimpleLongProperty(file.length());
        this.selected = new SimpleBooleanProperty(Boolean.FALSE);
        this.type = new SimpleStringProperty(file.getName().substring(
                file.getName().lastIndexOf('.') + 1,
                file.getName().length()).toUpperCase());
        this.fullPath = file.getAbsolutePath();
        this.progress = new SimpleDoubleProperty();
    }


    /**
     * @return the name
     */
    public String getName() {
        return name.get();
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name.set(name);
    }

    /**
     * @return the length
     */
    public long getLength() {
        return length.get();
    }

    /**
     * @param length the length to set
     */
    public void setLength(long length) {
        this.length.set(length);
    }

    /**
     * @return the fullPath
     */
    public String getFullPath() {
        return fullPath;
    }

    public double getProgress() {
        return progress.get();
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress.set(progress);
    }

    public String getType() {
        return type.get();
    }

    public StringProperty typeProperty() {
        return type;
    }

    public void setType(String type) {
        this.type.set(type);
    }

    public boolean getSelected() {
        return selected.get();
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }
}
