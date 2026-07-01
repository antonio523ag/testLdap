package dev.antoniogrillo.testldap.security;

/** Utility anti open-redirect: accetta solo path relativi same-origin. */
public final class SafeRedirects {

    private SafeRedirects() {
    }

    /**
     * Restituisce il redirect solo se e' un path relativo same-origin.
     * Rifiuta protocol-relative ("//host"), backslash ("/\host"),
     * backslash interni e control characters (CR/LF/TAB). Altrimenti "/".
     */
    public static String sanitize(String redirect) {
        if (redirect == null) {
            return "/";
        }
        String r = redirect.trim();
        if (r.isEmpty() || r.charAt(0) != '/') {
            return "/";
        }
        if (r.length() >= 2) {
            char c1 = r.charAt(1);
            if (c1 == '/' || c1 == '\\') {
                return "/";
            }
        }
        for (int i = 0; i < r.length(); i++) {
            char c = r.charAt(i);
            if (c == '\\' || c < 0x20) {
                return "/";
            }
        }
        return r;
    }
}
