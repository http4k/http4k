

package org.http4k;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.StringTokenizer;

public class MimeTypeFile {
    private String fname = null;
    private Hashtable type_hash = new Hashtable();

    public MimeTypeFile(String new_fname) throws IOException {
        File mime_file;
        FileReader fr;
        fname = new_fname;
        mime_file = new File(fname);
        fr = new FileReader(mime_file);

        try {
            parse(new BufferedReader(fr));
        } finally {
            try {
                fr.close();
            } catch (IOException var10) {
                ;
            }

        }

    }

    public MimeTypeFile() {
    }

    public MimeTypeFile(InputStream is) throws IOException {
        parse(new BufferedReader(new InputStreamReader(is, "iso-8859-1")));
    }

    public MimeTypeEntry getMimeTypeEntry(String file_ext) {
        return (MimeTypeEntry)type_hash.get(file_ext);
    }

    public String getMIMETypeString(String file_ext) {
        MimeTypeEntry entry = getMimeTypeEntry(file_ext);
        return entry != null ? entry.getMIMEType() : null;
    }

    public void appendToRegistry(String mime_types) {
        try {
            parse(new BufferedReader(new StringReader(mime_types)));
        } catch (IOException var3) {
            ;
        }

    }

    private void parse(BufferedReader buf_reader) throws IOException {
        String line;
        String prev = null;

        while(true) {
            while((line = buf_reader.readLine()) != null) {
                if (prev == null) {
                    prev = line;
                } else {
                    prev = prev + line;
                }

                int end = prev.length();
                if (prev.length() > 0 && prev.charAt(end - 1) == '\\') {
                    prev = prev.substring(0, end - 1);
                } else {
                    parseEntry(prev);
                    prev = null;
                }
            }

            if (prev != null) {
                parseEntry(prev);
            }

            return;
        }
    }

    private void parseEntry(String line) {
        String mime_type = null;
        String file_ext;
        line = line.trim();
        if (line.length() != 0) {
            if (line.charAt(0) != '#') {
                String value;
                if (line.indexOf(61) > 0) {
                    LineTokenizer lt = new LineTokenizer(line);

                    do {
                        while (lt.hasMoreTokens()) {
                            String name = lt.nextToken();
                            value = null;
                            if (lt.hasMoreTokens() && lt.nextToken().equals("=") && lt.hasMoreTokens()) {
                                value = lt.nextToken();
                            }

                            if (value == null) {
                                return;
                            }

                            if (name.equals("type")) {
                                mime_type = value;
                            } else if (name.equals("exts")) {
                                StringTokenizer st = new StringTokenizer(value, ",");

                                while (st.hasMoreTokens()) {
                                    file_ext = st.nextToken();
                                    MimeTypeEntry entry = new MimeTypeEntry(mime_type);
                                    type_hash.put(file_ext, entry);
                                }
                            }
                        }

                        return;
                    } while (true);
                } else {
                    StringTokenizer strtok = new StringTokenizer(line);
                    int num_tok = strtok.countTokens();
                    if (num_tok != 0) {
                        mime_type = strtok.nextToken();

                        while(strtok.hasMoreTokens()) {
                            file_ext = strtok.nextToken();
                            MimeTypeEntry entry = new MimeTypeEntry(mime_type);
                            type_hash.put(file_ext, entry);
                        }

                    }
                }
            }
        }
    }
}
