package it.pagopa.pn.deliverypushworkflow.action.utils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReworkIdBuilder {
    private static final Pattern TRY_PATTERN = Pattern.compile("TRY_(\\d+)");
    private static final Pattern REWORK_PATTERN = Pattern.compile("REWORK_(\\d+)");

    public static Integer extractTryIdx(String reworkId) {
        Matcher matcher = TRY_PATTERN.matcher(reworkId);
        return matcher.find() ? Optional.ofNullable(matcher.group(1)).map(Integer::parseInt).orElse(0) : 0;
    }
    public static Integer extractReworkIdx(String reworkId) {
        Matcher matcher = REWORK_PATTERN.matcher(reworkId);
        return matcher.find() ? Optional.ofNullable(matcher.group(1)).map(Integer::parseInt).orElse(0) : 0;
    }

    public static String build(Integer reworkIdx, Integer tryIdx) {
        return String.format("REWORK_%d.TRY_%d", reworkIdx, tryIdx);
    }
}