package io.github.mabartos.context.device;

import java.util.regex.Pattern;

public final class VisitorIdUtils {

    public static final Pattern VISITOR_ID_PATTERN = Pattern.compile("^[a-f0-9]{32}$");

    private VisitorIdUtils() {
    }

    public static boolean isValidVisitorId(String visitorId) {
        return visitorId != null && VISITOR_ID_PATTERN.matcher(visitorId).matches();
    }
}
