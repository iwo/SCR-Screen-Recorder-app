package com.iwobanas.screenrecorder.audio;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.iwobanas.screenrecorder.R;
import com.iwobanas.screenrecorder.ShellCommand;
import com.iwobanas.screenrecorder.Utils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class AudioDriverInstaller {
    private static final String TAG = "scr_adi";

    private static final String PRIMARY_DEFAULT = "audio.primary.default.so";
    private static final String PRIMARY_PREFIX = "audio.primary.";
    private static final String ORIGINAL_PRIMARY_PREFIX = "audio.original_primary.";
    private static final String SCR_PRIMARY_DEFAULT = "audio.scr_primary.default.so";
    private static final String SYSTEM_LIB_HW = "/system/lib/hw";
    private static final String SCR_DIR_MARKER = "scr_dir";
    private static final String SYSTEM_FILES_COPIED_MARKER = "system_files_copied";
    private static final String SCR_AUDIO_DIR = "scr_audio";
    private static final String SYSTEM_AUDIO_POLICY = "/system/etc/audio_policy.conf";
    private static final String VENDOR_AUDIO_POLICY = "/vendor/etc/audio_policy.conf";
    private static final String ORIGINAL_SYSTEM_AUDIO_POLICY = "original_system_audio_policy.conf";
    private static final String ORIGINAL_VENDOR_AUDIO_POLICY = "original_vendor_audio_policy.conf";
    private static final String SCR_SYSTEM_AUDIO_POLICY = "scr_system_audio_policy.conf";
    private static final String SCR_VENDOR_AUDIO_POLICY = "scr_vendor_audio_policy.conf";
    private static final String LOCAL_SYSTEM_AUDIO_POLICY = "system_audio_policy.conf";
    private static final String LOCAL_VENDOR_AUDIO_POLICY = "vendor_audio_policy.conf";
    private static final FilenameFilter PRIMARY_FILENAME_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return filename.startsWith(PRIMARY_PREFIX);
        }
    };
    private static final FilenameFilter ORIGINAL_PRIMARY_FILENAME_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            return filename.startsWith(ORIGINAL_PRIMARY_PREFIX);
        }
    };
    public static final String INIT_MOUNTINFO = "/proc/1/mountinfo";
    public static final String SELF_MOUNTINFO = "/proc/self/mountinfo";

    public AudioDriverInstaller(Context context) {
        this.context = context;
        localDir = new File(context.getFilesDir(), SCR_AUDIO_DIR);
        systemDir = new File(SYSTEM_LIB_HW);
        //TODO: make sure that installer is executed after binary is extracted
        installer = new File(context.getFilesDir(), "screenrec");
    }

    private Context context;
    private File localDir;
    private File systemDir;
    private File installer;

    public void install() {
        Log.v(TAG, "Installation started");
        try {
            if (!systemFilesValid()) {
                initializeDir();
            }
            ensureSCRFilesValid();
            switchToSCRFiles();
            mount();
            validateMounted();
            Log.v(TAG, "Installation completed successfully");
        } catch (InstallationException e) {
            Log.e(TAG, "Installation failed", e);
            dumpState();
        }
    }

    public void uninstall() {
        Log.v(TAG, "Uninstall started");
        try {
            unmount();
        } catch (InstallationException e) {
            Log.e(TAG, "Unmount failed", e);
            dumpState();
        }
        validateNotMounted();
        try {
            switchToSystemFiles();
        } catch (InstallationException e) {
            Log.e(TAG, "Uninstall failed", e);
            dumpState();
        }
        Log.v(TAG, "Uninstall completed");
    }

    private void dumpState() {
        Utils.logDirectoryListing(TAG, systemDir);
        Utils.logDirectoryListing(TAG, localDir);
        Utils.logFileContent(TAG, new File(INIT_MOUNTINFO));
        Utils.logFileContent(TAG, new File(SELF_MOUNTINFO));
    }

    private boolean systemFilesValid() {
        File markerFile = new File(localDir, SYSTEM_FILES_COPIED_MARKER);
        if (!markerFile.exists()) {
            return false;
        }
        if (isMounted()) {
            return true; // we can't do more checking if mounted
        }

        return configFilesValid() && Utils.allFilesCopied(systemDir, localDir);
    }

    private boolean configFilesValid() {
        File systemConf = new File(SYSTEM_AUDIO_POLICY);
        File vendorConf = new File(VENDOR_AUDIO_POLICY);
        File originalSystemConf = new File(localDir, ORIGINAL_SYSTEM_AUDIO_POLICY);
        File originalVendorConf = new File(localDir, ORIGINAL_VENDOR_AUDIO_POLICY);
        if (vendorConf.exists() && !Utils.filesEqual(vendorConf, originalVendorConf)) {
            return false;
        }
        if (systemConf.exists() && !Utils.filesEqual(systemConf, originalSystemConf)) {
            return false;
        }
        return true;
    }

    private void ensureSCRFilesValid() throws InstallationException {
        extractSCRModule();
        if (!isMounted()) {
            createConfigFiles();
        }
    }

    private void switchToSCRFiles() throws InstallationException {
        deletePrimaryModules();
        applySCRModule();
        applySCRConfigFiles();
    }

    private void deletePrimaryModules() throws InstallationException {
        String[] systemFiles = localDir.list(PRIMARY_FILENAME_FILTER);

        if (systemFiles != null) {
            for (String fileName : systemFiles) {
                File primaryFile = new File(localDir, fileName);
                if (!primaryFile.delete()) {
                    throw new InstallationException("Can't delete: " + primaryFile);
                }
            }
        }
    }

    private void applySCRModule() throws InstallationException {
        File scrModule = new File(localDir, SCR_PRIMARY_DEFAULT);
        File primaryModule = new File(localDir, PRIMARY_DEFAULT);
        if (!Utils.copyFile(scrModule, primaryModule)) {
            throw new InstallationException("Can't copy scr module from " + scrModule.getAbsolutePath() + " to " + primaryModule.getAbsolutePath());
        }
    }

    private void applySCRConfigFiles() throws InstallationException {
        File localSystemConf = new File(localDir, LOCAL_SYSTEM_AUDIO_POLICY);
        File localVendorConf = new File(localDir, LOCAL_VENDOR_AUDIO_POLICY);
        File scrSystemConf = new File(localDir, SCR_SYSTEM_AUDIO_POLICY);
        File scrVendorConf = new File(localDir, SCR_VENDOR_AUDIO_POLICY);

        if (scrVendorConf.exists() && !Utils.copyFile(scrVendorConf, localVendorConf)) {
            throw new InstallationException("Error copying config from " + scrVendorConf.getAbsolutePath() + " to " + localVendorConf.getAbsolutePath());
        }
        Utils.setGlobalReadable(localVendorConf);
        if (scrSystemConf.exists() && !Utils.copyFile(scrSystemConf, localSystemConf)) {
            throw new InstallationException("Error copying config from " + scrSystemConf.getAbsolutePath() + " to " + localSystemConf.getAbsolutePath());
        }
        Utils.setGlobalReadable(localSystemConf);
    }

    private void mount() throws InstallationException {
        String commandInput = "mount_audio\n" + localDir.getAbsolutePath() + "\n";
        int result = runInstaller(commandInput);
        if (result != 0) {
            throw new InstallationException("Mount command failed with error code: " + result);
        }
    }

    private int runInstaller(String commandInput) {
        ShellCommand command = new ShellCommand(new String[]{"su", "--mount-master", "-c", installer.getAbsolutePath()});
        command.setInput(commandInput);
        command.execute();
        if (!command.getOutput().startsWith("ready")) {
            command = new ShellCommand(new String[]{"su", "-c", installer.getAbsolutePath()});
            command.setInput(commandInput);
            command.execute();
        }
        if (!command.isExecutionCompleted()) {
            return -1;
        }
        return command.exitValue();
    }


    private void validateMounted() throws InstallationException {
        if (!isMounted()) {
            throw new InstallationException("Drivers appears not to be mounted correctly");
        }
        File initMountInfo = new File(INIT_MOUNTINFO);
        try {
            if (initMountInfo.exists() && Utils.grepFile(initMountInfo, SYSTEM_LIB_HW).size() == 0) {
                throw new InstallationException("Drivers directory not mounted globally");
            }
        } catch (IOException ignored) {}
    }

    private boolean isMounted() {
        File markerFile = new File(systemDir, SCR_DIR_MARKER);
        return markerFile.exists();
    }

    private void initializeDir() throws InstallationException {
        Log.v(TAG, "Initializing modules directory");
        checkNotMounted();

        cleanup();

        createEmptyDir();

        createMarkerFile();

        copySystemModules();

        copySystemConfigFiles();

        movePrimaryToOriginalPrimary();

        markSystemFilesCopied();

        extractSCRModule();

        createConfigFiles();

        fixPermissions();
        Log.v(TAG, "Modules directory initialized");
    }

    private void checkNotMounted() throws InstallationException {
        if (isMounted()) {
            throw new InstallationException("System files are not available because SCR dir is already mounted.");
        }
    }

    private void cleanup() throws InstallationException {
        if (localDir.exists()) {
            if (!Utils.deleteDir(localDir)) {
                throw new InstallationException("Couldn't remove local modules directory");
            }
        }
    }

    private void createEmptyDir() throws InstallationException {
        if (!localDir.mkdirs()) {
            throw new InstallationException("Couldn't create local modules directory");
        }
    }


    private void createMarkerFile() throws InstallationException {
        File markerFile = new File(localDir, SCR_DIR_MARKER);
        try {
            if (!markerFile.createNewFile()) {
                throw new InstallationException("Couldn't create marker file");
            }
        } catch (IOException e) {
            throw new InstallationException("Couldn't create marker file", e);
        }
    }

    private void copySystemModules() throws InstallationException {
        if (!Utils.copyDir(systemDir, localDir)) {
            throw new InstallationException("Error copying modules directory");
        }

    }

    private void copySystemConfigFiles() throws InstallationException {
        File systemConf = new File(SYSTEM_AUDIO_POLICY);
        File vendorConf = new File(VENDOR_AUDIO_POLICY);
        File originalSystemConf = new File(localDir, ORIGINAL_SYSTEM_AUDIO_POLICY);
        File originalVendorConf = new File(localDir, ORIGINAL_VENDOR_AUDIO_POLICY);

        if (vendorConf.exists() && !Utils.copyFile(vendorConf, originalVendorConf)) {
            throw new InstallationException("Can't copy config file from: " + vendorConf.getAbsolutePath() + " to " + originalVendorConf.getAbsolutePath());
        }

        if (systemConf.exists() && !Utils.copyFile(systemConf, originalSystemConf)) {
            throw new InstallationException("Can't copy config file from: " + systemConf.getAbsolutePath() + " to " + originalSystemConf.getAbsolutePath());
        }
    }

    private void movePrimaryToOriginalPrimary() throws InstallationException {
        String[] systemFiles = localDir.list(PRIMARY_FILENAME_FILTER);

        if (systemFiles == null) {
            throw new InstallationException("No system modules found");
        }

        for (String fileName : systemFiles) {
            File primaryFile = new File(localDir, fileName);
            File originalPrimary = new File(localDir, getOriginalPrimaryName(fileName));
            if (originalPrimary.exists()) {
                throw new InstallationException("File " + originalPrimary.getAbsolutePath() + " should not exist.");
            }
            if (!primaryFile.renameTo(originalPrimary)) {
                throw new InstallationException("Error renaming " + primaryFile.getAbsolutePath() + " to " + originalPrimary.getAbsolutePath());
            }
        }
    }

    private String getOriginalPrimaryName(String fileName) {
        return fileName.replaceFirst(PRIMARY_PREFIX, ORIGINAL_PRIMARY_PREFIX);
    }

    private void markSystemFilesCopied() throws InstallationException {
        File markerFile = new File(localDir, SYSTEM_FILES_COPIED_MARKER);
        try {
            if (!markerFile.createNewFile()) {
                throw new InstallationException("Can't create marker file " + markerFile.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new InstallationException("Can't create marker file " + markerFile.getAbsolutePath(), e);
        }
    }

    private void extractSCRModule() throws InstallationException {
        File scrModule = new File(localDir, SCR_PRIMARY_DEFAULT);
        try {
            Utils.extractResource(context, getDriverResourceId(), scrModule);
        } catch (IOException e) {
            throw new InstallationException("Error extracting module", e);
        }
    }

    private int getDriverResourceId() throws InstallationException {
        if (Utils.isArm()) {
            return R.raw.audio;
        } else if (Utils.isX86()) {
            return R.raw.audio_x86;
        } else {
            throw new InstallationException("Unsupported CPU: " + Build.CPU_ABI);
        }
    }

    private void createConfigFiles() throws InstallationException {
        File systemConf = new File(SYSTEM_AUDIO_POLICY);
        File vendorConf = new File(VENDOR_AUDIO_POLICY);
        File scrSystemConf = new File(localDir, SCR_SYSTEM_AUDIO_POLICY);
        File scrVendorConf = new File(localDir, SCR_VENDOR_AUDIO_POLICY);

        if (vendorConf.exists()) {
            try {
                AudioPolicyUtils.fixPolicyFile(vendorConf, scrVendorConf);
            } catch (IOException e) {
                throw new InstallationException("Error creating policy file", e);
            }
            if (!Utils.copyFile(scrVendorConf, scrSystemConf)) {
                throw new InstallationException("Error copying policy file");
            }
        } else {
            try {
                AudioPolicyUtils.fixPolicyFile(systemConf, scrSystemConf);
            } catch (IOException e) {
                throw new InstallationException("Error creating policy file", e);
            }
        }
    }

    private void fixPermissions() {
        Utils.setGlobalReadable(localDir);
    }

    private void switchToSystemFiles() throws InstallationException {
        applyOriginalPrimaryModules();
        applyOriginalConfigFiles();
    }

    private void applyOriginalPrimaryModules() throws InstallationException {
        String[] systemFiles = localDir.list(ORIGINAL_PRIMARY_FILENAME_FILTER);

        if (systemFiles == null) {
            throw new InstallationException("No system modules found");
        }

        for (String fileName : systemFiles) {
            File originalPrimary = new File(localDir, fileName);
            File primary = new File(localDir, getPrimaryName(fileName));

            if (!Utils.copyFile(originalPrimary, primary)) {
                //TODO: consider if we shouldn't continue with deinstallation on failures
                throw new InstallationException("Error copying original module from " + originalPrimary.getAbsolutePath() + " to " + primary.getAbsolutePath());
            }
        }
    }

    private String getPrimaryName(String fileName) {
        return fileName.replaceFirst(ORIGINAL_PRIMARY_PREFIX, PRIMARY_PREFIX);
    }

    private void applyOriginalConfigFiles() throws InstallationException {
        File localSystemConf = new File(localDir, LOCAL_SYSTEM_AUDIO_POLICY);
        File localVendorConf = new File(localDir, LOCAL_VENDOR_AUDIO_POLICY);
        File originalSystemConf = new File(localDir, ORIGINAL_SYSTEM_AUDIO_POLICY);
        File originalVendorConf = new File(localDir, ORIGINAL_VENDOR_AUDIO_POLICY);

        //TODO: consider if we shouldn't continue with deinstallation on failures
        if (originalVendorConf.exists() && !Utils.copyFile(originalVendorConf, localVendorConf)) {
            throw new InstallationException("Error copying config from " + originalVendorConf.getAbsolutePath() + " to " + localVendorConf.getAbsolutePath());
        }
        Utils.setGlobalReadable(localVendorConf);
        if (originalSystemConf.exists() && !Utils.copyFile(originalSystemConf, localSystemConf)) {
            throw new InstallationException("Error copying config from " + originalSystemConf.getAbsolutePath() + " to " + localSystemConf.getAbsolutePath());
        }
        Utils.setGlobalReadable(localSystemConf);
    }

    private void unmount() throws InstallationException {
        String commandInput = "unmount_audio\n";
        int result = runInstaller(commandInput);
        if (result != 0) {
            throw new InstallationException("Unmount command failed with error code: " + result);
        }
    }

    private void validateNotMounted() {
    }



    static class InstallationException extends Exception {

        InstallationException(String detailMessage) {
            super(detailMessage);
        }

        InstallationException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }
}
