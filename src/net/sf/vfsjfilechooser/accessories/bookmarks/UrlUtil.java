package net.sf.vfsjfilechooser.accessories.bookmarks;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtil {

    public static Pattern p = Pattern
            .compile("^((\\w+):\\/)?\\/?((.*?)(:(.*?)|)@)?([^:\\/\\s]+)(:([^\\/]*))?((\\/\\w+)*\\/)([-\\w.]+[^#?\\s]*)?(\\?([^#]*))?(#(.*))?$");
    private static int REGEX_SCHEMA = 2;
    private static int REGEX_USER = 4;
    private static int REGEX_PASSWORD = 6;
    private static int REGEX_HOST = 7;
    private static int REGEX_PORT = 8;
    private static int REGEX_PATH = 10;
    private static int REGEX_FILE = 12;
    private static int REGEX_QUERY = 13;
    private static int REGEX_FRAGMENT = 15;

    public static String removePasswordFromUrl(String url) {
        String s = url;
        Matcher matcher = p.matcher(s);        
        if (matcher.find()) {
            String schema = matcher.group(REGEX_SCHEMA);
            String user = matcher.group(REGEX_USER);
            String pass = matcher.group(REGEX_PASSWORD);
            String host = matcher.group(REGEX_HOST);
            String port = matcher.group(REGEX_PORT);
            String path = matcher.group(REGEX_PATH);
            String file = matcher.group(REGEX_FILE);
            String query = matcher.group(REGEX_QUERY);
            String fragment = matcher.group(REGEX_FRAGMENT);
            StringBuilder sb = new StringBuilder();
            if (schema != null) {
                sb.append(schema);
                sb.append("://");
            }
            if (user != null) {
                sb.append(user);
                if (pass != null) {
                    sb.append(":");
                    sb.append("***");
                }
                sb.append("@");
            }
            if (host != null) {
                sb.append(host);
            }
            if (port != null) {
                sb.append(port);
            }
            if (path != null) {
                sb.append(path);
            }
            if (file != null) {
                sb.append(file);
            }
            if (query != null) {
                sb.append(query);
            }
            if (fragment != null) {
                sb.append(fragment);
            }
            s=sb.toString();
        }
        return s;
    }
}
