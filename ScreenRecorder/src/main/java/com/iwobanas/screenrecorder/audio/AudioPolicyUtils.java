package com.iwobanas.screenrecorder.audio;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AudioPolicyUtils {

    private static final String TAG = "scr_AudioPolicyUtils";

    public static boolean commentOutOutputs(String inPath, String outPath) throws IOException {
        boolean modified = false;
        BufferedWriter writer = null;
        try {
            List<ConfigLine> lines = getConfigLines(inPath);
            writer = new BufferedWriter(new FileWriter(outPath));

            for (ConfigLine line : lines) {
                if (line.getSection().startsWith(".audio_hw_modules.primary.outputs.") &&
                        !line.getSection().equals(".audio_hw_modules.primary.outputs.primary")) {
                    writer.write("# SCR #");
                    modified = true;
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

    public static int getMaxPrimarySamplingRate(String policyPath) {
        try {
            List<ConfigLine> lines = getConfigLines(policyPath);

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
        } catch (Exception e) {
            Log.w(TAG, "getMaxPrimarySamplingRate ", e);
        }
        return -1;
    }

    private static List<ConfigLine> getConfigLines(String path) throws IOException {
        List<ConfigLine> result = new ArrayList<ConfigLine>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(path));
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