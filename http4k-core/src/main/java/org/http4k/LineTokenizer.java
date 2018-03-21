package org.http4k;


import java.util.NoSuchElementException;
import java.util.Vector;

class LineTokenizer {
    private int currentPosition = 0;
    private int maxPosition;
    private String str;
    private Vector stack = new Vector();

    public LineTokenizer(String str) {
        str = str;
        maxPosition = str.length();
    }

    private void skipWhiteSpace() {
        while (currentPosition < maxPosition && Character.isWhitespace(str.charAt(currentPosition))) {
            ++currentPosition;
        }

    }

    public boolean hasMoreTokens() {
        if (stack.size() > 0) {
            return true;
        } else {
            skipWhiteSpace();
            return currentPosition < maxPosition;
        }
    }

    public String nextToken() {
        int size = stack.size();
        if (size > 0) {
            String t = (String) stack.elementAt(size - 1);
            stack.removeElementAt(size - 1);
            return t;
        } else {
            skipWhiteSpace();
            if (currentPosition >= maxPosition) {
                throw new NoSuchElementException();
            } else {
                int start = currentPosition;
                char c = str.charAt(start);
                if (c == '"') {
                    ++currentPosition;
                    boolean filter = false;

                    while (currentPosition < maxPosition) {
                        c = str.charAt(currentPosition++);
                        if (c == '\\') {
                            ++currentPosition;
                            filter = true;
                        } else if (c == '"') {
                            String s;
                            if (filter) {
                                StringBuffer sb = new StringBuffer();

                                for (int i = start + 1; i < currentPosition - 1; ++i) {
                                    c = str.charAt(i);
                                    if (c != '\\') {
                                        sb.append(c);
                                    }
                                }

                                s = sb.toString();
                            } else {
                                s = str.substring(start + 1, currentPosition - 1);
                            }

                            return s;
                        }
                    }
                } else if ("=".indexOf(c) >= 0) {
                    ++currentPosition;
                } else {
                    while (currentPosition < maxPosition && "=".indexOf(str.charAt(currentPosition)) < 0 && !Character.isWhitespace(str.charAt(currentPosition))) {
                        ++currentPosition;
                    }
                }

                return str.substring(start, currentPosition);
            }
        }
    }
}
