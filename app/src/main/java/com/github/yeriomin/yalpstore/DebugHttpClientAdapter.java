package com.github.yeriomin.yalpstore;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

public class DebugHttpClientAdapter extends NativeHttpClientAdapter {

    static private final String DUMP_DIR = "YalpStoreDebug";

    static private File dumpDirectory;

    public DebugHttpClientAdapter() {
        dumpDirectory = new File(Environment.getExternalStorageDirectory(), DUMP_DIR);
        if (!dumpDirectory.exists()) {
            dumpDirectory.mkdirs();
        }
    }

    @Override
    protected byte[] request(HttpURLConnection connection, byte[] body, Map<String, String> headers) throws IOException {
        String url = connection.getURL().getPath() + "." + connection.getURL().getQuery();
        StringBuilder requestHeaders = new StringBuilder();
        for (String key: headers.keySet()) {
            requestHeaders.append(key).append(": ").append(headers.get(key)).append("\n");
        }
        write(getFileName(url, true, true), requestHeaders.toString().getBytes());
        write(getFileName(url, true, false), body);
        byte[] responseBody;
        try {
            responseBody = super.request(connection, body, headers);
            write(getFileName(url, false, false), responseBody);
        } catch (IOException e) {
            responseBody = new byte[0];
        } finally {
            StringBuilder responseHeaders = new StringBuilder();
            for (String key: connection.getHeaderFields().keySet()) {
                List<String> header = connection.getHeaderFields().get(key);
                if (null == header || header.isEmpty()) {
                    continue;
                }
                if (TextUtils.isEmpty(key)) {
                    responseHeaders.append(header.get(0)).append("\n");
                } else {
                    for (String duplicate: header) {
                        responseHeaders.append(key).append(": ").append(duplicate).append("\n");
                    }
                }
            }
            write(getFileName(url, false, true), responseHeaders.toString().getBytes());
        }
        return responseBody;
    }

    private static void write(String fileName, byte[] body) {
        if (null == body) {
            return;
        }
        String path = new File(dumpDirectory, fileName).getAbsolutePath();
        Log.i(DebugHttpClientAdapter.class.getName(), "Writing to " + path);
        FileOutputStream stream;
        try {
            stream = new FileOutputStream(path);
            stream.write(body);
            stream.close();
        } catch (IOException e) {
            Log.e(DebugHttpClientAdapter.class.getName(), "Could not dump request/response to " + path + ": " + e.getMessage());
        }
    }

    private static String getFileName(String url, boolean request, boolean headers) {
        return new StringBuilder()
            .append(System.currentTimeMillis())
            .append(".")
            .append(request ? "request" : "response")
            .append(url.replace("=", ".").replace("/", ".").replace(":", "."))
            .append(headers ? ".txt" : ".bin")
            .toString()
        ;
    }
}
