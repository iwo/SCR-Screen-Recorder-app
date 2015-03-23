package com.iwobanas.screenrecorder.audio;

import android.util.Log;

import com.iwobanas.screenrecorder.settings.Settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AudioPolicyUtils {

    private static final String TAG = "scr_AudioPolicyUtils";
    private static final String VENDOR_AUDIO_POLICY = "/vendor/etc/audio_policy.conf";
    private static final String SYSTEM_AUDIO_POLICY = "/system/etc/audio_policy.conf";

    public static boolean fixPolicyFile(File in, File out) throws IOException {
        boolean modified = false;
        BufferedWriter writer = null;
        try {
            List<ConfigLine> lines = getConfigLines(in);
            writer = new BufferedWriter(new FileWriter(out));

            int outputSamplingRate = getMaxPrimarySamplingRate(lines);

            for (ConfigLine line : lines) {

                if (!Settings.getInstance().getAudioDriver().getRequiresHardInstall() &&
                        line.getSection().startsWith(".audio_hw_modules.primary.outputs.") &&
                        !line.getSection().equals(".audio_hw_modules.primary.outputs.primary")) {
                    writer.write("# SCR #");
                    modified = true;
                }

                if (outputSamplingRate != -1 && line.getSection().equals(".audio_hw_modules.primary.inputs.primary") && line.getText().trim().startsWith("sampling_rates")) {
                    boolean hasOutputRate = false;
                    String[] rates = line.getText().split("[^\\d]+");
                    for (String rateString : rates) {
                        int rate = -1;
                        try {
                            rate = Integer.parseInt(rateString);
                        } catch (NumberFormatException ignored) {
                        }
                        if (rate == outputSamplingRate) {
                            hasOutputRate = true;
                            break;
                        }
                    }

                    if (!hasOutputRate) {
                        String text = line.getText();
                        if (text.contains("#")) {
                            text = text.split("#")[0];
                        }
                        writer.write(text);
                        writer.write("|" + outputSamplingRate);
                        writer.write("\n");
                        writer.write("# SCR #");
                        modified = true;
                    }
                }

                writer.write(line.getText());
                writer.write("\n");
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        return modified;
    }

    public static int getMaxPrimarySamplingRate() {
        try {
            File policyFile = getPolicyFile();
            if (policyFile == null) {
                return -1;
            }
            List<ConfigLine> lines = getConfigLines(policyFile);
            return getMaxPrimarySamplingRate(lines);

        } catch (Exception e) {
            Log.w(TAG, "getMaxPrimarySamplingRate ", e);
        }
        return -1;
    }

    private static int getMaxPrimarySamplingRate(List<ConfigLine> lines) {
        for (ConfigLine line : lines) {
            if (line.getSection().equals(".audio_hw_modules.primary.outputs.primary") && line.getText().trim().startsWith("sampling_rates")) {
                int maxRate = -1;
                String[] rates = line.getText().split("[^\\d]+");
                for (String rateString : rates) {
                    int rate = -1;
                    try {
                        rate = Integer.parseInt(rateString);
                    } catch (NumberFormatException ignored) {
                    }
                    if (rate > maxRate) {
                        maxRate = rate;
                    }
                }
                return maxRate;
            }
        }
        return -1;
    }

    private static List<ConfigLine> getConfigLines(File policyFile) throws IOException {
        List<ConfigLine> result = new ArrayList<ConfigLine>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(policyFile));
            String section = "";
            String line = null;
            String sectionStartPattern = "^(\\w*)(\\W*)(\\{)$";

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (line.contains("#")) {
                    trimmed = line.substring(0, line.indexOf("#")).trim();
                }
                trimmed.trim();

                if (trimmed.matches(sectionStartPattern)) {
                    String subsection = trimmed.replaceAll(sectionStartPattern, "$1");
                    section = section + "." + subsection;
                }

                result.add(new ConfigLine(line, section));
                if (trimmed.equals("}")) {
                    section = section.substring(0, section.lastIndexOf("."));
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        return result;
    }

    public static File getPolicyFile() {
        File vendorConf = new File(VENDOR_AUDIO_POLICY);
        File systemConf = new File(SYSTEM_AUDIO_POLICY);
        if (vendorConf.exists()) {
            Log.v(TAG, "Policy file found: " + VENDOR_AUDIO_POLICY);
            return vendorConf;
        }
        if (!systemConf.exists()) {
            Log.w(TAG, "No policy file found.");
            return null;
        }
        return systemConf;
    }

    static class ConfigLine {
        private String text;
        private String section;

        ConfigLine(String text, String section) {
            this.text = text;
            this.section = section;
        }

        public String getText() {
            return text;
        }

        public String getSection() {
            return section;
        }
    }
}