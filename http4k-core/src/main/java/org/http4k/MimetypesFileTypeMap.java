package org.http4k;//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Vector;

public class MimetypesFileTypeMap {
    private MimeTypeFile[] DB;
    private static String defaultType = "application/octet-stream";

    private MimeTypeFile loadResource(String name) {
        InputStream clis = null;

        MimeTypeFile var4;
        try {
            clis = SecuritySupport.getResourceAsStream(this.getClass(), name);
            if (clis == null) {
                return null;
            }

            MimeTypeFile mf = new MimeTypeFile(clis);
            var4 = mf;
        } catch (IOException var17) {
            return null;
        } catch (SecurityException var18) {
            return null;
        } finally {
            try {
                if (clis != null) {
                    clis.close();
                }
            } catch (IOException var16) {
                ;
            }

        }

        return var4;
    }

    private void loadAllResources(Vector v, String name) {
        boolean anyLoaded = false;

        try {
            ClassLoader cld;
            cld = SecuritySupport.getContextClassLoader();
            if (cld == null) {
                cld = this.getClass().getClassLoader();
            }

            URL[] urls;
            if (cld != null) {
                urls = SecuritySupport.getResources(cld, name);
            } else {
                urls = SecuritySupport.getSystemResources(name);
            }

            if (urls != null) {
                for(int i = 0; i < urls.length; ++i) {
                    URL url = urls[i];
                    InputStream clis = null;
                    try {
                        clis = SecuritySupport.openStream(url);
                        if (clis != null) {
                            v.addElement(new MimeTypeFile(clis));
                            anyLoaded = true;
                        }
                    } catch (IOException var21) {
                    } catch (SecurityException var22) {
                    } finally {
                        try {
                            if (clis != null) {
                                clis.close();
                            }
                        } catch (IOException var20) {
                            ;
                        }

                    }
                }
            }
        } catch (Exception var24) {
        }

        if (!anyLoaded) {
            MimeTypeFile mf = this.loadResource("/" + name);
            if (mf != null) {
                v.addElement(mf);
            }
        }

    }

    private MimeTypeFile loadFile(String name) {
        MimeTypeFile mtf = null;

        try {
            mtf = new MimeTypeFile(name);
        } catch (IOException var4) {
            ;
        }

        return mtf;
    }

    public MimetypesFileTypeMap(InputStream is) {
        Vector dbv = new Vector(5);
        MimeTypeFile mf = null;
        dbv.addElement((Object)null);

        String system_mimetypes;
        try {
            system_mimetypes = System.getProperty("user.home");
            if (system_mimetypes != null) {
                String path = system_mimetypes + File.separator + ".mime.types";
                mf = this.loadFile(path);
                if (mf != null) {
                    dbv.addElement(mf);
                }
            }
        } catch (SecurityException var6) {
            ;
        }

        try {
            system_mimetypes = System.getProperty("java.home") + File.separator + "lib" + File.separator + "mime.types";
            mf = this.loadFile(system_mimetypes);
            if (mf != null) {
                dbv.addElement(mf);
            }
        } catch (SecurityException var5) {
            ;
        }

        this.loadAllResources(dbv, "META-INF/mime.types");
        mf = this.loadResource("/META-INF/mimetypes.default");
        if (mf != null) {
            dbv.addElement(mf);
        }

        this.DB = new MimeTypeFile[dbv.size()];
        dbv.copyInto(this.DB);

        try {
            this.DB[0] = new MimeTypeFile(is);
        } catch (IOException var3) {
            ;
        }

    }

    public synchronized void addMimeTypes(String mime_types) {
        if (this.DB[0] == null) {
            this.DB[0] = new MimeTypeFile();
        }

        this.DB[0].appendToRegistry(mime_types);
    }

    public String getContentType(File f) {
        return this.getContentType(f.getName());
    }

    public synchronized String getContentType(String filename) {
        int dot_pos = filename.lastIndexOf(".");
        if (dot_pos < 0) {
            return defaultType;
        } else {
            String file_ext = filename.substring(dot_pos + 1);
            if (file_ext.length() == 0) {
                return defaultType;
            } else {
                for(int i = 0; i < this.DB.length; ++i) {
                    if (this.DB[i] != null) {
                        String result = this.DB[i].getMIMETypeString(file_ext);
                        if (result != null) {
                            return result;
                        }
                    }
                }

                return defaultType;
            }
        }
    }
}
