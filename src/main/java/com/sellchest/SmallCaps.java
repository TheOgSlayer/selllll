package com.sellchest;

/** Turns plain text into small caps (ꜱᴍᴀʟʟ ᴄᴀᴘꜱ). Digits and symbols are left alone. */
public final class SmallCaps {

    private static final String NORMAL = "abcdefghijklmnopqrstuvwxyz";
    private static final String SMALL  = "\u1D00\u0299\u1D04\u1D05\u1D07\uA730\u0262\u029C\u026A\u1D0A\u1D0B\u029F\u1D0D"
                                       + "\u0274\u1D0F\u1D18\u01EB\u0280\uA731\u1D1B\u1D1C\u1D20\u1D21x\u028F\u1D22";

    private SmallCaps() {}

    public static String of(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            int i = NORMAL.indexOf(Character.toLowerCase(c));
            sb.append(i >= 0 ? SMALL.charAt(i) : c);
        }
        return sb.toString();
    }
}
