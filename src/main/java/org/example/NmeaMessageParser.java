package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.*;

public class NmeaMessageParser {

    private static final Pattern NMEA_PATTERN = Pattern.compile(
            "\\$(GP|GN|GL|GA|GB|GQ|PSSN|PS|PM|PC)[^$]*\\*[0-9A-Fa-f]{2}"
    );

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\$[A-Z]{2,5},(\\d{6}(\\.\\d{1,3})?)");

    private final List<List<String>> messagesBySecond;
    private final LocalDateTime intervalStart;
    private final int totalSeconds;

    public NmeaMessageParser(String filePath, String intervalStartStr, String intervalEndStr) throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        this.intervalStart = LocalDateTime.parse(intervalStartStr, formatter);
        LocalDateTime intervalEnd = LocalDateTime.parse(intervalEndStr, formatter);
        this.totalSeconds = (int) ChronoUnit.SECONDS.between(intervalStart, intervalEnd) + 1;

        this.messagesBySecond = new ArrayList<>(totalSeconds);
        for (int i = 0; i < totalSeconds; i++) {
            messagesBySecond.add(new ArrayList<>());
        }

        loadAndDistributeMessages(filePath);
    }

    public  LocalDateTime getIntervalStart() {
        return intervalStart;
    }

    private void loadAndDistributeMessages(String filePath) throws IOException {
        String rawContent;
        try (InputStream is = new FileInputStream(filePath)) {
            rawContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        Matcher matcher = NMEA_PATTERN.matcher(rawContent);
        while (matcher.find()) {
            String message = matcher.group();
            LocalDateTime ts = extractTimestamp(message);
            if (ts != null) {
                long secondsOffset = ChronoUnit.SECONDS.between(intervalStart, ts.truncatedTo(ChronoUnit.SECONDS));
                if (secondsOffset >= 0 && secondsOffset < totalSeconds) {
                    messagesBySecond.get((int) secondsOffset).add(message);
                }
            }
        }
    }

    private LocalDateTime extractTimestamp(String message) {
        Matcher matcher = TIMESTAMP_PATTERN.matcher(message);
        if (matcher.find()) {
            try {
                String ts = matcher.group(1); // e.g. "123519.00"
                String hhmmss = ts.substring(0, 6);
                int hour = Integer.parseInt(hhmmss.substring(0, 2));
                int minute = Integer.parseInt(hhmmss.substring(2, 4));
                int second = Integer.parseInt(hhmmss.substring(4, 6));

                return intervalStart.withHour(hour)
                        .withMinute(minute)
                        .withSecond(second)
                        .withNano(0); // strip fraction
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    public List<List<String>> getMessagesBySecond() {
        return messagesBySecond;
    }
}
