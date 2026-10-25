package vn.lighttv.home;

import android.content.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

final class PhotoServer {
    static final int PORT = 8787;
    private static final int MAX_BYTES = 6 * 1024 * 1024;

    interface Listener {
        void onSaved();
    }

    private final Context context;
    private final Listener listener;
    private volatile boolean running;
    private ServerSocket server;
    private Thread thread;

    PhotoServer(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    void start() throws Exception {
        stop();
        server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new java.net.InetSocketAddress(PORT));
        server.setSoTimeout(700);
        running = true;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                acceptLoop();
            }
        }, "photo-upload");
        thread.start();
    }

    void stop() {
        running = false;
        ServerSocket open = server;
        server = null;
        if (open != null) {
            try {
                open.close();
            } catch (Exception ignored) {
                // Closing the socket is enough to end the accept loop.
            }
        }
    }

    static String address() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            String fallback = null;
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface item = interfaces.nextElement();
                if (!item.isUp() || item.isLoopback()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = item.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (!(address instanceof Inet4Address) || address.isLoopbackAddress()) {
                        continue;
                    }
                    String host = address.getHostAddress();
                    if (host.startsWith("192.168.")) {
                        return host;
                    }
                    if (fallback == null) {
                        fallback = host;
                    }
                }
            }
            return fallback;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void acceptLoop() {
        while (running) {
            ServerSocket open = server;
            if (open == null) {
                return;
            }
            Socket client = null;
            try {
                client = open.accept();
                client.setSoTimeout(8000);
                handle(client);
            } catch (Exception ignored) {
                // Timeout or a closed socket just retries until stop().
            } finally {
                if (client != null) {
                    try {
                        client.close();
                    } catch (Exception ignored) {
                        // Already finished with this phone.
                    }
                }
            }
        }
    }

    private void handle(Socket client) throws Exception {
        InputStream input = client.getInputStream();
        ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
        int previous = -1;
        int guards = 0;
        while (guards < 8192) {
            int value = input.read();
            if (value < 0) {
                return;
            }
            headerBytes.write(value);
            guards++;
            if (previous == '\r' && value == '\n' && endsWithBlankLine(headerBytes.toByteArray())) {
                break;
            }
            previous = value;
        }
        String header = new String(headerBytes.toByteArray(), StandardCharsets.ISO_8859_1);
        int length = contentLength(header);
        if (length < 0 || length > MAX_BYTES) {
            write(client, 413, page("Ảnh quá lớn", "Chọn ảnh nhỏ hơn 6 MB."));
            return;
        }
        byte[] body = readExact(input, length);
        String path = pathOf(header);
        if (header.startsWith("GET ") && "/".equals(path)) {
            write(client, 200, formPage());
            return;
        }
        if (!header.startsWith("POST ") || !"/upload".equals(path)) {
            write(client, 404, page("Không có trang này", "Chỉ dùng địa chỉ trang chủ."));
            return;
        }
        byte[] image = extractImage(body, boundary(header));
        if (image == null || !Backgrounds.save(context, image, image.length)) {
            write(client, 400, page("Không nhận được ảnh", "Chọn một file JPG hoặc PNG."));
            return;
        }
        listener.onSaved();
        write(client, 200, page("Đã đặt hình nền", "Quay lại TV và bấm nút Quay lại trên remote."));
    }

    private static byte[] extractImage(byte[] body, String boundary) {
        if (boundary == null || body == null) {
            return null;
        }
        byte[] marker = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
        byte[] separator = new byte[]{13, 10, 13, 10};
        int start = indexOf(body, marker, 0);
        if (start < 0) {
            return null;
        }
        int headerEnd = indexOf(body, separator, start);
        if (headerEnd < 0) {
            return null;
        }
        int dataStart = headerEnd + separator.length;
        int next = indexOf(body, marker, dataStart);
        if (next < 2) {
            return null;
        }
        int dataEnd = next - 2;
        if (dataEnd <= dataStart) {
            return null;
        }
        byte[] image = new byte[dataEnd - dataStart];
        System.arraycopy(body, dataStart, image, 0, image.length);
        return image;
    }

    private static String formPage() {
        return page("Đổi hình nền TV",
                "<p>Chọn một ảnh JPG hoặc PNG trên điện thoại.</p>"
                        + "<form method=\"post\" action=\"/upload\" enctype=\"multipart/form-data\">"
                        + "<input type=\"file\" name=\"photo\" accept=\"image/jpeg,image/png\" required>"
                        + "<button type=\"submit\">Đặt làm nền</button></form>");
    }

    private static String page(String title, String body) {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<title>" + title + "</title><style>"
                + "body{margin:0;background:#10131a;color:#f5f7fb;font-family:sans-serif}"
                + "main{max-width:28rem;margin:0 auto;padding:2rem 1.2rem}"
                + "h1{font-size:1.7rem} input,button{font-size:1.1rem}"
                + "button{margin-top:1rem;background:#f5f7fb;color:#111;border:0;border-radius:999px;padding:.8rem 1.2rem}"
                + "</style></head><body><main><h1>" + title + "</h1>" + body + "</main></body></html>";
    }

    private static void write(Socket client, int code, String html) throws Exception {
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        String status = code == 200 ? "OK" : code == 400 ? "Bad Request" : code == 413 ? "Payload Too Large" : "Not Found";
        String head = "HTTP/1.1 " + code + " " + status + "\r\n"
                + "Content-Type: text/html; charset=utf-8\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n\r\n";
        OutputStream output = client.getOutputStream();
        output.write(head.getBytes(StandardCharsets.ISO_8859_1));
        output.write(body);
        output.flush();
    }

    private static byte[] readExact(InputStream input, int length) throws Exception {
        byte[] body = new byte[length];
        int offset = 0;
        while (offset < length) {
            int count = input.read(body, offset, length - offset);
            if (count < 0) {
                break;
            }
            offset += count;
        }
        return body;
    }

    private static boolean endsWithBlankLine(byte[] data) {
        int n = data.length;
        return n >= 4 && data[n - 4] == '\r' && data[n - 3] == '\n' && data[n - 2] == '\r' && data[n - 1] == '\n';
    }

    private static int contentLength(String header) {
        String[] lines = header.split("\r\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].regionMatches(true, 0, "Content-Length:", 0, 15)) {
                try {
                    return Integer.parseInt(lines[i].substring(15).trim());
                } catch (NumberFormatException ignored) {
                    return -1;
                }
            }
        }
        return 0;
    }

    private static String pathOf(String header) {
        int start = header.indexOf(' ');
        int end = header.indexOf(' ', start + 1);
        if (start < 0 || end < 0) {
            return "/";
        }
        String path = header.substring(start + 1, end);
        int query = path.indexOf('?');
        return query >= 0 ? path.substring(0, query) : path;
    }

    private static String boundary(String header) {
        String mark = "boundary=";
        int at = header.toLowerCase().indexOf(mark);
        if (at < 0) {
            return null;
        }
        String value = header.substring(at + mark.length()).trim();
        int end = value.indexOf('\r');
        if (end >= 0) {
            value = value.substring(0, end);
        }
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static int indexOf(byte[] data, byte[] needle, int from) {
        if (needle.length == 0 || data.length < needle.length) {
            return -1;
        }
        for (int i = Math.max(0, from); i <= data.length - needle.length; i++) {
            boolean match = true;
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1;
    }
}
