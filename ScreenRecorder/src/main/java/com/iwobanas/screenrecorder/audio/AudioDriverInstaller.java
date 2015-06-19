package com.iwobanas.screenrecorder.audio;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.iwobanas.screenrecorder.CameraOverlay;
import com.iwobanas.screenrecorder.NativeCommands;
import com.iwobanas.screenrecorder.R;
import com.iwobanas.screenrecorder.Utils;
import com.iwobanas.screenrecorder.settings.Settings;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;

public class AudioDriverInstaller {
    private static final String TAG = "scr_adi";

    public static final String SCR_AUDIO_DIR = "scr_audio";
    public static final String MODULE_LOG = "scr_audio.log";

    private static final String PRIMARY_DEFAULT = "audio.primary.default.so";
    private static final String PRIMARY_PREFIX = "audio.primary.";
    private static final String ORIGINAL_PRIMARY_PREFIX = "audio.original_primary.";
    private static final String SCR_PRIMARY_DEFAULT = "audio.scr_primary.default.so";
    private static final String SYSTEM_LIB_HW = "/system/lib/hw";
    private static final String SCR_DIR_MARKER = "scr_dir";
    private static final String SYSTEM_FILES_COPIED_MARKER = "system_files_copied";
    private static final String SYSTEM_AUDIO_POLICY = "/system/etc/audio_policy.conf";
    private static final String VENDOR_AUDIO_POLICY = "/vendor/etc/audio_policy.conf";
    private static final String MEDIA_PROFILES = "/system/etc/media_profiles.xml";
    private static final String ORIGINAL_SYSTEM_AUDIO_POLICY = "original_system_audio_policy.conf";
    private static final String ORIGINAL_VENDOR_AUDIO_POLICY = "original_vendor_audio_policy.conf";
    private static final String ORIGINAL_MEDIA_PROFILES = "original_system_media_profiles.xml";
    private static final String SCR_AUDIO_POLICY = "scr_audio_policy.conf";
    private static final String SCR_MEDIA_PROFILES = "scr_media_profiles.xml";
    private static final String LOCAL_SYSTEM_AUDIO_POLICY = "system_audio_policy.conf";
    private static final String LOCAL_VENDOR_AUDIO_POLICY = "vendor_audio_policy.conf";
    private static final String LOCAL_MEDIA_PROFILES = "media_profiles.xml";
    private static final String MEDIASERVER_COMMAND = "/system/bin/mediaserver";
    private static final String UNINSTALLER_SYSTEM = "uninstall_scr.sh";
    private static final String UNINSTALLER_LOCAL = "deinstaller.sh";
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
    }

    private Context context;
    private File localDir;
    private File systemDir;
    private boolean uninstallSuccess;
    private String errorDetails;
    private Boolean mountMaster;
    private boolean hard;

    public boolean install() {
        Log.v(TAG, "Installation started");
        // camera should be turned off during mediaserver restart to avoid lock down on Nexus 4
        CameraOverlay.releaseCamera();
        errorDetails = null;
        try {
            if (isSystemAlphaModuleInstalled()) {
                throw new InstallationException("Unsupported old audio driver installed");
            }
            Settings settings = Settings.getInstance();
            AudioDriver audioDriver = settings.getAudioDriver();
            if (audioDriver.getRequiresHardInstall()) {
                if (isMounted()) {
                    Log.v(TAG, "Uninstalling before hard install");
                    unmount();
                    try {
                        restartMediaserver();
                    } catch (InstallationException e) {
                        Log.e(TAG, "Error restarting mediaserver", e);
                    }
                    validateNotMounted();
                }
                if (!localDir.exists()){
                    createEmptyDir();
                }
                ensureSCRFilesValid();
                extractUninstaller();
                hardInstall();
            } else {
                if (!systemFilesValid()) {
                    initializeDir();
                }
                ensureSCRFilesValid();
                switchToSCRFiles();
                mountAndRestart();
                if (!isMounted()) {
                    Log.w(TAG, "Unmount happened after restart. Attempting to mount again");
                    mountAndRestart();
                }

                if (!waitForModuleLoaded()) {
                    Log.w(TAG, "Module not loaded on time. Switching to hard-install scenario");
                    audioDriver.setRequiresHardInstall();
                    if (settings.getDisableAudioWarning()) {
                        return install();
                    } else {
                        audioDriver.setRetryHardInstall(true);
                        if (errorDetails == null) {
                            errorDetails = "not loaded";
                        }
                        return false;
                    }
                }
            }
            Log.v(TAG, "Installation completed successfully");
        } catch (InstallationException e) {
            Log.e(TAG, "Installation failed", e);
            dumpState();
            errorDetails = e.getMessage();
            Throwable cause = e.getCause();
            if (cause != null && cause != e) {
                errorDetails += " => " + cause.getMessage();
            }
            return false;
        } finally {
            CameraOverlay.reconnectCamera();
        }
        return true;
    }

    private void hardInstall() throws InstallationException {
        hard = true;
        int result = NativeCommands.getInstance().installAudio(localDir.getAbsolutePath().toString());
        if (result == 202) {
            // don't treat this as error but report to stats
            if (errorDetails == null) {
                errorDetails = "read only remount error";
            }
        } else if (result != 0 && result != 200) {
            throw new InstallationException("Hard install error: " + result);
        }
    }

    private void hardUninstall() {
        hard = true;
        int result = NativeCommands.getInstance().uninstallAudio();
        if (result == 202) {
            // don't treat this as error but report to stats
            if (errorDetails == null) {
                errorDetails = "read only remount error";
            }
        } else if (result != 0 && result != 200) {
            uninstallError("Hard uninstall error: " + result);
        }
    }

    public boolean uninstall() {
        Log.v(TAG, "Uninstall started");
        // camera should be turned off during mediaserver restart to avoid lock down on Nexus 4
        CameraOverlay.releaseCamera();
        uninstallSuccess = true;
        errorDetails = null;
        if (isHardInstalled()) {
            hardUninstall();
        } else {
            unmount();
            switchToSystemFiles();
            try {
                restartMediaserver();
            } catch (InstallationException e) {
                Log.e(TAG, "Error restarting mediaserver", e);
            }
            validateNotMounted();
        }
        if (uninstallSuccess) {
            Log.v(TAG, "Uninstall completed");
        } else {
            Log.w(TAG, "Uninstall completed with errors");
            dumpState();
        }
        CameraOverlay.reconnectCamera();
        return uninstallSuccess;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public Boolean getMountMaster() {
        return mountMaster;
    }

    public boolean isHard() {
        return hard;
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

        return configFilesValid() && Utils.allFilesCopied(systemDir, localDir, true);
    }

    private boolean configFilesValid() {
        return configFileValid(SYSTEM_AUDIO_POLICY, ORIGINAL_SYSTEM_AUDIO_POLICY)
                && configFileValid(VENDOR_AUDIO_POLICY, ORIGINAL_VENDOR_AUDIO_POLICY)
                && configFileValid(MEDIA_PROFILES, ORIGINAL_MEDIA_PROFILES);
    }

    private boolean configFileValid(String configPath, String originalName) {
        File configFile = new File(configPath);
        File originalFile = new File(localDir, originalName);
        return !configFile.exists() || Utils.filesEqual(configFile, originalFile);
    }

    private void ensureSCRFilesValid() throws InstallationException {
        extractSCRModule();
        createLogFile(MODULE_LOG);
        createConfigFiles();
    }

    private void createLogFile(String name) throws InstallationException {
        File logFile = new File(localDir, name);
        try {
            logFile.createNewFile();
            logFile.setReadable(true, false);
            logFile.setWritable(true, false);
        } catch (IOException e) {
            throw new InstallationException("Can't create log file: " + logFile.getAbsolutePath(), e);
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
        Utils.setGlobalReadable(primaryModule);
    }

    private void applySCRConfigFiles() throws InstallationException {
        applySCRConfigFile(LOCAL_SYSTEM_AUDIO_POLICY, SCR_AUDIO_POLICY);
        applySCRConfigFile(LOCAL_VENDOR_AUDIO_POLICY, SCR_AUDIO_POLICY);
        applySCRConfigFile(LOCAL_MEDIA_PROFILES, SCR_MEDIA_PROFILES);
    }

    private void applySCRConfigFile(String localName, String scrName) throws InstallationException {
        File localFile = new File(localDir, localName);
        File scrFile = new File(localDir, scrName);
        if (scrFile.exists() && !Utils.copyFile(scrFile, localFile)) {
            throw new InstallationException("Error copying config from " + scrFile.getAbsolutePath() + " to " + localFile.getAbsolutePath());
        }
        Utils.setGlobalReadable(localFile);
    }

    private void mountAndRestart() throws InstallationException {
        if (!isMounted()) {
            mount();
            validateMounted();
        }
        Log.v(TAG, "Mounted. Restarting");
        restartMediaserver();
    }

    private void mount() throws InstallationException {
        mountMaster = Boolean.TRUE;
        int result = NativeCommands.getInstance().mountAudioMaster(localDir.getAbsolutePath().toString());
        if (result == 255 || result < 150) {
            Log.w(TAG, "Retrying without mount master");
            mountMaster = Boolean.FALSE;
            result = NativeCommands.getInstance().mountAudio(localDir.getAbsolutePath().toString());
        }
        if (result != 0 && result != 200) {
            throw new InstallationException("Mount command failed with error code: " + result);
        }
    }

    private void validateMounted() throws InstallationException {
        if (!isMounted()) {
            throw new InstallationException("Drivers appears not to be mounted correctly");
        }
        if (!isGloballyMounted()) {
            throw new InstallationException("Drivers directory not mounted globally");
        }
    }

    private boolean isMounted() {
        File markerFile = new File(systemDir, SCR_DIR_MARKER);
        return markerFile.exists();
    }

    /**
     * If init process had pid different to 1 or IOException occurs revert to isMounted()
     */
    private boolean isGloballyMounted() {
        File initMountInfo = new File(INIT_MOUNTINFO);
        try {
            if (initMountInfo.exists() && Utils.grepFile(initMountInfo, SYSTEM_LIB_HW).size() == 0) {
                return false;
            }
        } catch (IOException ignored) {}
        return isMounted();
    }

    private void restartMediaserver() throws InstallationException {
        terminateMediaserver();
        waitForMediaserver();
    }

    private void terminateMediaserver() throws InstallationException {
        int pid = Utils.findProcessByCommand(MEDIASERVER_COMMAND);
        if (pid > 0) {
            NativeCommands.getInstance().termSignal(pid);
            if (!waitForProcessToStop(pid, 1000, 100)) {
                Log.v(TAG, "mediaserver not terminating. killing");
                NativeCommands.getInstance().killSignal(pid);
            }
            if (!waitForProcessToStop(pid, 500, 100)) {
                throw new InstallationException("Can't restart mediaserver");
            }
        }
    }

    private boolean waitForProcessToStop(int pid, long timeout, long checkFrequency) {
        long startTime = System.nanoTime();
        long timeoutNs = timeout * 1000000l;
        while ((System.nanoTime() - startTime) < timeoutNs) {
            if (!Utils.processExists(pid)) {
                return true;
            }
            try {
                Thread.sleep(checkFrequency);
            } catch (InterruptedException ignored) {}
        }
        return false;
    }

    private void waitForMediaserver() throws InstallationException {
        if (Utils.waitForProcess(MEDIASERVER_COMMAND, 7000) == -1) {
            throw new InstallationException("mediaserver not appearing");
        }
    }

    private boolean waitForModuleLoaded() throws InstallationException {
        int mediaServerPid = Utils.waitForProcess(MEDIASERVER_COMMAND, 1000);
        boolean mediaServerDied = false;

        long startTime = System.nanoTime();
        long timeoutNs = 3 * 1000000000l;

        while ((System.nanoTime() - startTime) < timeoutNs) {
            if (isModuleLoaded()) {
                Log.v(TAG, "Module loaded after " + (System.nanoTime() - startTime) / 1000000 + "ms");
                return true;
            }

            if (!Utils.processExists(mediaServerPid)) {
                if (mediaServerDied) {
                    throw new InstallationException("mediaserver died");
                } else {
                    mediaServerDied = true;
                    mediaServerPid = Utils.waitForProcess(MEDIASERVER_COMMAND, 1000);
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        return false;
    }

    private boolean isModuleLoaded() {
        BufferedReader reader = null;
        try {
            File logFile = new File(localDir, MODULE_LOG);
            reader = new BufferedReader(new FileReader(logFile));
            String lastLoadedLine = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("loaded ")) {
                    lastLoadedLine = line;
                }
            }
            if (lastLoadedLine == null)
                return false;

            long timestamp = Long.parseLong(lastLoadedLine.split(" ")[1]);
            long uptime = System.currentTimeMillis() / 1000l - timestamp;

            return uptime >= 0 && uptime < 10;

        } catch (IOException e) {
            Log.d(TAG, "Exception reading log", e);
        } catch (NumberFormatException e) {
            Log.d(TAG, "Exception parsing log", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        return false;
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
        localDir.setExecutable(true, false); // set x (searchable) permission on directory
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
        if (!Utils.copyDir(systemDir, localDir, true)) {
            throw new InstallationException("Error copying modules directory");
        }

    }

    private void copySystemConfigFiles() throws InstallationException {
        copySystemConfigFile(SYSTEM_AUDIO_POLICY, ORIGINAL_SYSTEM_AUDIO_POLICY);
        copySystemConfigFile(VENDOR_AUDIO_POLICY, ORIGINAL_VENDOR_AUDIO_POLICY);
        copySystemConfigFile(MEDIA_PROFILES, ORIGINAL_MEDIA_PROFILES);
    }

    private void copySystemConfigFile(String path, String copyName) throws InstallationException {
        File configFile = new File(path);
        File copy = new File(localDir, copyName);
        if (configFile.exists() && !Utils.copyFile(configFile, copy)) {
            throw new InstallationException("Can't copy config file from: " + configFile.getAbsolutePath() + " to " + copy.getAbsolutePath());
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

    private void extractUninstaller() throws InstallationException {
        File uninstallerFile = new File(localDir, UNINSTALLER_LOCAL);
        try {
            Utils.extractResource(context, R.raw.uninstall_scr, uninstallerFile);
            uninstallerFile.setReadable(true, false);
            uninstallerFile.setExecutable(true, false);
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
        createPolicyFiles();
        createMediaProfilesFiles();
    }

    private void createPolicyFiles() throws InstallationException {
        File systemConf = new File(localDir, ORIGINAL_SYSTEM_AUDIO_POLICY);
        File vendorConf = new File(localDir, ORIGINAL_VENDOR_AUDIO_POLICY);
        File scrConf = new File(localDir, SCR_AUDIO_POLICY);

        if (vendorConf.exists()) {
            try {
                AudioPolicyUtils.fixPolicyFile(vendorConf, scrConf);
            } catch (IOException e) {
                throw new InstallationException("Error creating policy file", e);
            }
        } else if (systemConf.exists()) {
            try {
                AudioPolicyUtils.fixPolicyFile(systemConf, scrConf);
            } catch (IOException e) {
                throw new InstallationException("Error creating policy file", e);
            }
        } else if (Build.VERSION.SDK_INT <= 15) {
            Log.w(TAG, "No policy file found");
        }
    }

    private void createMediaProfilesFiles() throws InstallationException {
        File systemFile = new File(localDir, ORIGINAL_MEDIA_PROFILES);
        File scrFile = new File(localDir, SCR_MEDIA_PROFILES);
        if (systemFile.exists()) {
            try {
                MediaProfilesUtils.fixMediaProfiles(systemFile, scrFile);
            } catch (Exception e) {
                throw new InstallationException("Error creating mdedia profiles file", e);
            }
        }
    }

    private void fixPermissions() {
        Utils.setGlobalReadable(localDir);
    }

    private void switchToSystemFiles() {
        applyOriginalPrimaryModules();
        applyOriginalConfigFiles();
    }

    private void applyOriginalPrimaryModules() {
        String[] systemFiles = localDir.list(ORIGINAL_PRIMARY_FILENAME_FILTER);

        if (systemFiles == null) {
            uninstallError("No system modules found");
            return;
        }

        for (String fileName : systemFiles) {
            File originalPrimary = new File(localDir, fileName);
            File primary = new File(localDir, getPrimaryName(fileName));

            if (!Utils.copyFile(originalPrimary, primary)) {
                uninstallError("Error copying original module from " + originalPrimary.getAbsolutePath() + " to " + primary.getAbsolutePath());
            }
            Utils.setGlobalReadable(primary);
        }
    }

    private String getPrimaryName(String fileName) {
        return fileName.replaceFirst(ORIGINAL_PRIMARY_PREFIX, PRIMARY_PREFIX);
    }

    private void uninstallError(String error) {
        Log.e(TAG, error);
        uninstallSuccess = false;
        if (errorDetails == null) {
            errorDetails = error;
        } else {
            errorDetails += "; " + error;
        }
    }

    private void applyOriginalConfigFiles() {
        applyOriginalConfigFile(LOCAL_SYSTEM_AUDIO_POLICY, ORIGINAL_SYSTEM_AUDIO_POLICY);
        applyOriginalConfigFile(LOCAL_VENDOR_AUDIO_POLICY, ORIGINAL_VENDOR_AUDIO_POLICY);
        applyOriginalConfigFile(LOCAL_MEDIA_PROFILES, ORIGINAL_MEDIA_PROFILES);
    }

    private void applyOriginalConfigFile(String localName, String originalName) {
        File localFile = new File(localDir, localName);
        File originalFile = new File(localDir, originalName);
        if (originalFile.exists() && !Utils.copyFile(originalFile, localFile)) {
            uninstallError("Error copying config from " + originalFile.getAbsolutePath() + " to " + localFile.getAbsolutePath());
        }
        Utils.setGlobalReadable(localFile);
    }

    private void unmount() {
        int result = NativeCommands.getInstance().unmountAudioMaster();
        if (result == 255 || result < 150) {
            Log.w(TAG, "Retrying without mount master");
            result = NativeCommands.getInstance().unmountAudio();
        }
        if (result != 0 && result != 200) {
            uninstallError("Unmount command failed with error code: " + result);
        }
    }

    private void validateNotMounted() {
        if (isMounted()) {
            Log.w(TAG, "Still mounted after restart");
        }
    }

    public InstallationStatus checkStatus() {
        if (isSystemAlphaModuleInstalled()) {
            return InstallationStatus.OUTDATED;
        }
        if (!isMounted()) {
            if (isHardInstalled()) {
                return InstallationStatus.OUTDATED;
            }
            return InstallationStatus.NOT_INSTALLED;
        }
        if (!isGloballyMounted()) {
            return InstallationStatus.INSTALLATION_FAILURE;
        }
        if (scrModuleInstalled() && scrModuleUpToDate()) {
            return InstallationStatus.INSTALLED;
        }
        if (originalPrimaryModulesInstalled()) {
            return InstallationStatus.NOT_INSTALLED;
        }
        return InstallationStatus.UNSPECIFIED;
    }

    private boolean isSystemAlphaModuleInstalled() {
        File versionFile = new File("/system/lib/hw/scr_module_version");
        return versionFile.exists();
    }

    private boolean isHardInstalled() {
        if (isMounted())
            return false;
        File uninstallerFile = new File(systemDir, UNINSTALLER_SYSTEM);
        return uninstallerFile.exists();
    }

    private boolean scrModuleInstalled() {
        File scrModule = new File(localDir, SCR_PRIMARY_DEFAULT);
        File primaryModule = new File(systemDir, PRIMARY_DEFAULT);
        return Utils.filesEqual(scrModule, primaryModule);
    }

    private boolean scrModuleUpToDate() {
        File moduleFile = new File(systemDir, PRIMARY_DEFAULT);
        try {
            return Utils.resourceFileValid(context, getDriverResourceId(), moduleFile);
        } catch (IOException e) {
            return false;
        } catch (InstallationException e) {
            return false;
        }
    }

    private boolean originalPrimaryModulesInstalled() {
        String[] systemFiles = systemDir.list(ORIGINAL_PRIMARY_FILENAME_FILTER);

        if (systemFiles == null) {
            return false;
        }

        for (String fileName : systemFiles) {
            File originalPrimary = new File(localDir, fileName);
            File primary = new File(localDir, getPrimaryName(fileName));

            if (!Utils.filesEqual(originalPrimary, primary)) {
                return false;
            }
        }
        return true;
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
