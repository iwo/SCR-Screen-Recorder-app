package com.iwobanas.screenrecorder.audio;

import android.util.Log;

import com.iwobanas.screenrecorder.Utils;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;


public class MediaProfilesUtils {

    private static final String TAG = "scr_MediaProfiles";

    public static void fixMediaProfiles(File systemFile, File scrFile) throws IOException {

        if (!Utils.copyFile(systemFile, scrFile)) {
            throw new IOException("Error copying file");
        }

        // Apparently the file is sometimes malformed XML so I gave up using proper XML parser
        // instead I hacked together this code to just replace one char in the file without modifying the rest

        RandomAccessFile file = new RandomAccessFile(scrFile, "rw");

        String line;
        long tagStart = -1;
        long readStart = 0;
        while ((line = file.readLine()) != null) {
            if (tagStart != -1) {
                int tagEndOffset = line.indexOf(">");
                if (tagEndOffset != -1) {
                    fixTag(file, tagStart, readStart + tagEndOffset + 1);
                    tagStart = -1;
                    readStart = readStart + tagEndOffset + 1;
                    file.seek(readStart);
                    continue;
                }
            } else {
                int tagStartOffset = line.indexOf("<AudioEncoderCap");
                if (tagStartOffset > -1) {
                    tagStart = readStart + tagStartOffset;
                    readStart = tagStart;
                    file.seek(readStart);
                    continue;
                }
            }
            readStart = file.getFilePointer();
        }
    }

    private static void fixTag(RandomAccessFile file, long tagStart, long tagEnd) throws IOException {
        byte[] buffer = new byte[(int) (tagEnd - tagStart)];
        file.seek(tagStart);
        file.readFully(buffer);
        String tag = new String(buffer);
        if ((tag.contains("name=\"aac\"") || tag.contains("name='aac'")) && (tag.contains("maxChannels=\"1\"") || tag.contains("maxChannels='1'"))) {
            Log.v(TAG, "Updating media profiles config");
            int idx = tag.indexOf("maxChannels=");
            file.seek(tagStart + idx + 13);
            file.write('2');
        }
    }
}
