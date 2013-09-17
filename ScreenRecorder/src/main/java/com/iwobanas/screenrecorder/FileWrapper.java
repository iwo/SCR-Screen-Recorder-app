package com.iwobanas.screenrecorder;

import java.io.File;

class FileWrapper implements Comparable<FileWrapper> {
    private File file;
    private String label;

    public FileWrapper(File file, String label) {
        this.file = file;
        this.label = label;
    }

    FileWrapper(File file) {
        this(file, file.getName());
    }

    public File getFile() {
        return file;
    }

    @Override
    public String toString() {
        return label;
    }

    @Override
    public int compareTo(FileWrapper another) {
        return label.compareToIgnoreCase(another.label);
    }
}
