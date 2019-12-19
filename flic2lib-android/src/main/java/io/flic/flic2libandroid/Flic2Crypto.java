package io.flic.flic2libandroid;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

class Flic2Crypto {
    static class Fe {
        public int a, b, c, d, e, f, g, h, i, j;

        public Fe() {

        }

        public Fe(Fe o) {
            this.a = o.a;
            this.b = o.b;
            this.c = o.c;
            this.d = o.d;
            this.e = o.e;
            this.f = o.f;
            this.g = o.g;
            this.h = o.h;
            this.i = o.i;
            this.j = o.j;
        }
    }

    /*public static void mul2(Fe out, Fe f, Fe g) {
        int f0 = (int) f.a, f1 = (int) f.b, f2 = (int) f.c, f3 = (int) f.d, f4 = (int) f.e, f5 = (int) f.f, f6 = (int) f.g, f7 = (int) f.h, f8 = (int) f.i, f9 = (int) f.j;
        int g0 = (int) g.a, g1 = (int) g.b, g2 = (int) g.c, g3 = (int) g.d, g4 = (int) g.e, g5 = (int) g.f, g6 = (int) g.g, g7 = (int) g.h, g8 = (int) g.i, g9 = (int) g.j;
        long h0, h1, h2, h3, h4, h5, h6, h7, h8, h9;

        h9 = (long)f1 * g8;
        h8 = (long)f1 * g7;
        h7 = (long)f1 * g6;
        h6 = (long)f1 * g5;
        h9 += (long)f3 * g6;
        h8 += (long)f3 * g5;
        h7 += (long)f3 * g4;
        h6 += (long)f3 * g3;
        h9 += (long)f5 * g4;
        h8 += (long)f5 * g3;
        h7 += (long)f5 * g2;
        h6 += (long)f5 * g1;
        h9 += (long)f7 * g2;
        h8 += (long)f7 * g1;
        h7 += (long)f7 * g0;

        f7 *= 19;
        int f9_19 = f9 * 19;
        int f8_19 = f8 * 19;
        int f6_19 = f6 * 19;

        h6 += (f7 & 0xffffffffL) * (g9 & 0xffffffffL);
        h9 += (long)f9 * g0;
        h8 += (f9_19 & 0xffffffffL) * (g9 & 0xffffffffL);
        h7 += (f9_19 & 0xffffffffL) * (g8 & 0xffffffffL);
        h6 += (f9_19 & 0xffffffffL) * (g7 & 0xffffffffL);

        h8 += h8;
        h9 += (long)f0 * g9;
        h6 += h6;
        h8 += (long)f0 * g8;
        h7 += (long)f0 * g7;
        h6 += (long)f0 * g6;

        h9 += (long)f2 * g7;
        h8 += (long)f2 * g6;
        h7 += (long)f2 * g5;
        h6 += (long)f2 * g4;

        h9 += (long)f4 * g5;
        h8 += (long)f4 * g4;
        h7 += (long)f4 * g3;
        h6 += (long)f4 * g2;

        h9 += (long)f6 * g3;
        h8 += (long)f6 * g2;
        h7 += (long)f6 * g1;
        h6 += (long)f6 * g0;

        h9 += (long)f8 * g1;
        h8 += (long)f8 * g0;
        h7 += (f8_19 & 0xffffffffL) * (g9 & 0xffffffffL);
        h6 += (f8_19 & 0xffffffffL) * (g8 & 0xffffffffL);

        h5 = (f9_19 & 0xffffffffL) * (g6 & 0xffffffffL);
        h4 = (f9_19 & 0xffffffffL) * (g5 & 0xffffffffL);
        h3 = (f9_19 & 0xffffffffL) * (g4 & 0xffffffffL);

        h5 += (long)f5 * g0;
        f5 *= 19;
        h4 += (f7 & 0xffffffffL) * (g7 & 0xffffffffL);
        h3 += (f7 & 0xffffffffL) * (g6 & 0xffffffffL);

        h5 += (f7 & 0xffffffffL) * (g8 & 0xffffffffL);
        h4 += (f5 & 0xffffffffL) * (g9 & 0xffffffffL);
        h3 += (f5 & 0xffffffffL) * (g8 & 0xffffffffL);

        h5 += (long)f3 * g2;
        h4 += (long)f3 * g1;
        h3 += (long)f3 * g0;

        h5 += (long)f1 * g4;
        h4 += (long)f1 * g3;
        h3 += (long)f1 * g2;

        int f4_19 = f4 * 19;
        h4 += h4;
        h5 += (f8_19 & 0xffffffffL) * (g7 & 0xffffffffL);
        h4 += (f8_19 & 0xffffffffL) * (g6 & 0xffffffffL);
        h3 += (f8_19 & 0xffffffffL) * (g5 & 0xffffffffL);

        h5 += (f6_19 & 0xffffffffL) * (g9 & 0xffffffffL);
        h4 += (f6_19 & 0xffffffffL) * (g8 & 0xffffffffL);
        h3 += (f6_19 & 0xffffffffL) * (g7 & 0xffffffffL);

        h5 += (long)f4 * g1;
        h4 += (long)f4 * g0;
        h3 += (f4_19 & 0xffffffffL) * (g9 & 0xffffffffL);

        h5 += (long)f2 * g3;
        h4 += (long)f2 * g2;
        h3 += (long)f2 * g1;

        h5 += (long)f0 * g5;
        h4 += (long)f0 * g4;
        h3 += (long)f0 * g3;

        int f3_19 = f3 * 19;
        h5 += h4 >>> 26;
        h4 &= 0x3ffffff;
        h6 += h5 >>> 25;
        h5 &= 0x1ffffff;
        h7 += h6 >>> 26;
        h6 &= 0x3ffffff;
        h8 += h7 >>> 25;
        h7 &= 0x1ffffff;
        h9 += h8 >>> 26;
        h8 &= 0x3ffffff;

        long o1 = h9 & ~0x3ffffff;
        h0 = o1 >>> 26;
        h9 &= 0x3ffffff;
        h0 += o1 >>> 25;
        h2 = (f9_19 & 0xffffffffL) * (g3 & 0xffffffffL);
        h0 += o1 >>> 22;
        h1 = (f9_19 & 0xffffffffL) * (g2 & 0xffffffffL);
        h0 += (f9_19 & 0xffffffffL) * (g1 & 0xffffffffL);

        h2 += (f7 & 0xffffffffL) * (g5 & 0xffffffffL);
        h1 += (f7 & 0xffffffffL) * (g4 & 0xffffffffL);
        h0 += (f7 & 0xffffffffL) * (g3 & 0xffffffffL);

        int f1_19 = f1 * 19;
        h1 += (f5 & 0xffffffffL) * (g6 & 0xffffffffL);
        h0 += (f5 & 0xffffffffL) * (g5 & 0xffffffffL);
        h2 += (f5 & 0xffffffffL) * (g7 & 0xffffffffL);

        h1 += (f3_19 & 0xffffffffL) * (g8 & 0xffffffffL);
        h0 += (f3_19 & 0xffffffffL) * (g7 & 0xffffffffL);
        h2 += (f3_19 & 0xffffffffL) * (g9 & 0xffffffffL);

        h1 += (long)f1 * g0;
        h0 += (f1_19 & 0xffffffffL) * (g9 & 0xffffffffL);
        h2 += (long)f1 * g1;

        int f2_19 = f2 * 19;
        h0 += h0;
        h2 += h2;

        h0 += (f8_19 & 0xffffffffL) * (g2 & 0xffffffffL);
        h1 += (f8_19 & 0xffffffffL) * (g3 & 0xffffffffL);
        h2 += (f8_19 & 0xffffffffL) * (g4 & 0xffffffffL);

        h0 += (f6_19 & 0xffffffffL) * (g4 & 0xffffffffL);
        h1 += (f6_19 & 0xffffffffL) * (g5 & 0xffffffffL);
        h2 += (f6_19 & 0xffffffffL) * (g6 & 0xffffffffL);

        h0 += (f4_19 & 0xffffffffL) * (g6 & 0xffffffffL);
        h1 += (f4_19 & 0xffffffffL) * (g7 & 0xffffffffL);
        h2 += (f4_19 & 0xffffffffL) * (g8 & 0xffffffffL);

        h0 += (f2_19 & 0xffffffffL) * (g8 & 0xffffffffL);
        h1 += (f2_19 & 0xffffffffL) * (g9 & 0xffffffffL);
        h2 += (long)f2 * g0;

        h0 += (long)f0 * g0;
        h1 += (long)f0 * g1;
        h2 += (long)f0 * g2;

        h1 += h0 >>> 26;
        h0 &= 0x3ffffff;
        h2 += h1 >>> 25;
        h1 &= 0x1ffffff;
        h3 += h2 >>> 26;
        h2 &= 0x3ffffff;
        h4 += h3 >>> 25;
        h3 &= 0x1ffffff;
        h5 += h4 >>> 26;
        h4 &= 0x3ffffff;
        h5 &= 0x3ffffff;

        out.a = (int)h0;
        out.b = (int)h1;
        out.c = (int)h2;
        out.d = (int)h3;
        out.e = (int)h4;
        out.f = (int)h5;
        out.g = (int)h6;
        out.h = (int)h7;
        out.i = (int)h8;
        out.j = (int)h9;
    }*/

    public static void mul(Fe out, Fe f, Fe g) {
        long f0 = (int) f.a, f1 = (int) f.b, f2 = (int) f.c, f3 = (int) f.d, f4 = (int) f.e, f5 = (int) f.f, f6 = (int) f.g, f7 = (int) f.h, f8 = (int) f.i, f9 = (int) f.j;
        long g0 = (int) g.a, g1 = (int) g.b, g2 = (int) g.c, g3 = (int) g.d, g4 = (int) g.e, g5 = (int) g.f, g6 = (int) g.g, g7 = (int) g.h, g8 = (int) g.i, g9 = (int) g.j;
        long h0, h1, h2, h3, h4, h5, h6, h7, h8, h9;

        h9 = (long)f1 * g8;
        h8 = (long)f1 * g7;
        h7 = (long)f1 * g6;
        h6 = (long)f1 * g5;
        h9 += (long)f3 * g6;
        h8 += (long)f3 * g5;
        h7 += (long)f3 * g4;
        h6 += (long)f3 * g3;
        h9 += (long)f5 * g4;
        h8 += (long)f5 * g3;
        h7 += (long)f5 * g2;
        h6 += (long)f5 * g1;
        h9 += (long)f7 * g2;
        h8 += (long)f7 * g1;
        h7 += (long)f7 * g0;

        f7 = f7 * 19 & 0xffffffffL;
        long f9_19 = f9 * 19 & 0xffffffffL;
        long f8_19 = f8 * 19 & 0xffffffffL;
        long f6_19 = f6 * 19 & 0xffffffffL;

        //long g0L = (g0 & 0xffffffffL);
        long g2L = (g2 & 0xffffffffL);
        long g3L = (g3 & 0xffffffffL);
        long g4L = (g4 & 0xffffffffL);
        long g5L = (g5 & 0xffffffffL);
        long g6L = (g6 & 0xffffffffL);
        long g7L = (g7 & 0xffffffffL);
        long g8L = (g8 & 0xffffffffL);
        long g9L = (g9 & 0xffffffffL);

        /*int g0L = (g0);
        int g2L = (g2 );
        int g3L = (g3 );
        int g4L = (g4 );
        int g5L = (g5 );
        int g6L = (g6 );
        int g7L = (g7 );
        int g8L = (g8 );
        int g9L = (g9 );*/

        h6 += f7 * g9L;
        h9 += (long)f9 * g0;
        h8 += f9_19 * g9L;
        h7 += f9_19 * g8L;
        h6 += f9_19 * g7L;

        h8 += h8;
        h9 += (long)f0 * g9;
        h6 += h6;
        h8 += (long)f0 * g8;
        h7 += (long)f0 * g7;
        h6 += (long)f0 * g6;

        h9 += (long)f2 * g7;
        h8 += (long)f2 * g6;
        h7 += (long)f2 * g5;
        h6 += (long)f2 * g4;

        h9 += (long)f4 * g5;
        h8 += (long)f4 * g4;
        h7 += (long)f4 * g3;
        h6 += (long)f4 * g2;

        h9 += (long)f6 * g3;
        h8 += (long)f6 * g2;
        h7 += (long)f6 * g1;
        h6 += (long)f6 * g0;

        h9 += (long)f8 * g1;
        h8 += (long)f8 * g0;
        h7 += f8_19 * g9L;
        h6 += f8_19 * g8L;

        h5 = f9_19 * g6L;
        h4 = f9_19 * g5L;
        h3 = f9_19 * g4L;

        h5 += (long)f5 * g0;
        f5 = f5 * 19 & 0xffffffffL;
        h4 += f7 * g7L;
        h3 += f7 * g6L;

        h5 += f7 * g8L;
        h4 += f5 * g9L;
        h3 += f5 * g8L;

        h5 += (long)f3 * g2;
        h4 += (long)f3 * g1;
        h3 += (long)f3 * g0;

        h5 += (long)f1 * g4;
        h4 += (long)f1 * g3;
        h3 += (long)f1 * g2;

        long f4_19 = f4 * 19 & 0xffffffffL;
        h4 += h4;
        h5 += f8_19 * g7L;
        h4 += f8_19 * g6L;
        h3 += f8_19 * g5L;

        h5 += f6_19 * g9L;
        h4 += f6_19 * g8L;
        h3 += f6_19 * g7L;

        h5 += (long)f4 * g1;
        h4 += (long)f4 * g0;
        h3 += f4_19 * g9L;

        h5 += (long)f2 * g3;
        h4 += (long)f2 * g2;
        h3 += (long)f2 * g1;

        h5 += (long)f0 * g5;
        h4 += (long)f0 * g4;
        h3 += (long)f0 * g3;

        long f3_19 = f3 * 19 & 0xffffffffL;
        h5 += h4 >>> 26;
        h4 &= 0x3ffffff;
        h6 += h5 >>> 25;
        h5 &= 0x1ffffff;
        h7 += h6 >>> 26;
        h6 &= 0x3ffffff;
        h8 += h7 >>> 25;
        h7 &= 0x1ffffff;
        h9 += h8 >>> 26;
        h8 &= 0x3ffffff;

        long o1 = h9 & ~0x3ffffff;
        h0 = o1 >>> 26;
        h9 &= 0x3ffffff;
        h0 += o1 >>> 25;
        h2 = f9_19 * g3L;
        h0 += o1 >>> 22;
        h1 = f9_19 * g2L;
        h0 += f9_19 * (g1 & 0xffffffffL);

        h2 += f7 * g5L;
        h1 += f7 * g4L;
        h0 += f7 * g3L;

        long f1_19 = f1 * 19 & 0xffffffffL;
        h1 += (f5 & 0xffffffffL) * g6L;
        h0 += (f5 & 0xffffffffL) * g5L;
        h2 += (f5 & 0xffffffffL) * g7L;

        h1 += f3_19 * g8L;
        h0 += f3_19 * g7L;
        h2 += f3_19 * g9L;

        h1 += (long)f1 * g0;
        h0 += f1_19 * g9L;
        h2 += (long)f1 * g1;

        long f2_19 = f2 * 19 & 0xffffffffL;
        h0 += h0;
        h2 += h2;

        h0 += f8_19 * g2L;
        h1 += f8_19 * g3L;
        h2 += f8_19 * g4L;

        h0 += f6_19 * g4L;
        h1 += f6_19 * g5L;
        h2 += f6_19 * g6L;

        h0 += f4_19 * g6L;
        h1 += f4_19 * g7L;
        h2 += f4_19 * g8L;

        h0 += f2_19 * g8L;
        h1 += f2_19 * g9L;
        h2 += (long)f2 * g0;

        h0 += (long)f0 * g0;
        h1 += (long)f0 * g1;
        h2 += (long)f0 * g2;

        h1 += h0 >>> 26;
        h0 &= 0x3ffffff;
        h2 += h1 >>> 25;
        h1 &= 0x1ffffff;
        h3 += h2 >>> 26;
        h2 &= 0x3ffffff;
        h4 += h3 >>> 25;
        h3 &= 0x1ffffff;
        h5 += h4 >>> 26;
        h4 &= 0x3ffffff;
        //h5 &= 0x3ffffff;

        out.a = (int)h0;
        out.b = (int)h1;
        out.c = (int)h2;
        out.d = (int)h3;
        out.e = (int)h4;
        out.f = (int)h5;
        out.g = (int)h6;
        out.h = (int)h7;
        out.i = (int)h8;
        out.j = (int)h9;
    }

    public static void sqr(Fe out, Fe f) {
        long f0 = f.a & 0xffffffffL;
        long f1 = f.b & 0xffffffffL;
        long f2 = f.c & 0xffffffffL;
        long f3 = f.d & 0xffffffffL;
        long f4 = f.e & 0xffffffffL;
        long f5 = f.f & 0xffffffffL;
        long f6 = f.g & 0xffffffffL;
        long f7 = f.h & 0xffffffffL;
        long f8 = f.i & 0xffffffffL;
        long f9 = f.j & 0xffffffffL;

        long f9_2 = (f9 + f9) & 0xffffffffL;
        long f8_2 = (f8 + f8) & 0xffffffffL;
        long f7_2 = (f7 + f7) & 0xffffffffL;
        long f6_2 = (f6 + f6) & 0xffffffffL;
        long f5_2 = (f5 + f5) & 0xffffffffL;
        long f4_2 = (f4 + f4) & 0xffffffffL;
        long f3_2 = (f3 + f3) & 0xffffffffL;
        long f2_2 = (f2 + f2) & 0xffffffffL;
        long f1_2 = (f1 + f1) & 0xffffffffL;

        long h0, h1, h2, h3, h4, h5, h6, h7, h8, h9;

        h8 = f4 * f4;
        h9 = f4 * f5_2;

        f9 = f9 * 19 & 0xffffffffL;
        f7 = f7 * 19 & 0xffffffffL;
        f5 = f5 * 19 & 0xffffffffL;

        h8 += f9 * f9_2;
        h9 += f0 * f9_2;

        h0 = f0 * f0;
        h1 = f0 * f1_2;
        h2 = f0 * f2_2;
        h3 = f0 * f3_2;
        h4 = f0 * f4_2;
        h5 = f0 * f5_2;
        h6 = f0 * f6_2;
        h7 = f0 * f7_2;
        h8 += f0 * f8_2;
        long f6_19 = f6 * 19 & 0xffffffffL;

        h2 += f1 * f1_2;
        h3 += f1 * f2_2;
        h4 += f1_2 * f3_2;
        h5 += f1 * f4_2;
        h6 += f1_2 * f5_2;
        h7 += f1 * f6_2;
        h8 += f1_2 * f7_2;
        h9 += f1 * f8_2;
        long f8_19 = f8 * 19 & 0xffffffffL;

        h4 += f2 * f2;
        h5 += f2 * f3_2;
        h6 += f2 * f4_2;
        h7 += f2 * f5_2;
        h8 += f2 * f6_2;
        h9 += f2 * f7_2;

        h6 += f3 * f3_2;
        h7 += f3 * f4_2;
        h8 += f3_2 * f5_2;
        h9 += f3 * f6_2;

        h6 += f8_19 * f8;
        h2 += f6_19 * f6;

        h9 += h8 >>> 26;
        h0 += f5 * f5_2;
        h0 += h9 >>> 25;
        long o = h9 & ~0x1ffffffL;
        h0 += o >>> 24;
        h9 &= 0x1ffffff;
        h0 += o >>> 21;

        h4 += f7 * f7_2;

        long f1_4 = (f1_2 + f1_2) & 0xffffffffL;
        long f3_4 = (f3_2 + f3_2) & 0xffffffffL;
        long f5_4 = (f5_2 + f5_2) & 0xffffffffL;
        long f7_4 = (f7_2 + f7_2) & 0xffffffffL;

        h0 += f6_19 * f4_2;
        h1 += f6_19 * f5_2;

        h8 &= 0x3ffffff;

        h0 += f7 * f3_4;
        h1 += f7 * f4_2;
        h2 += f7 * f5_4;
        h3 += f7 * f6_2;

        h0 += f8_19 * f2_2;
        h1 += f8_19 * f3_2;
        h2 += f8_19 * f4_2;
        h3 += f8_19 * f5_2;
        h4 += f8_19 * f6_2;
        h5 += f8_19 * f7_2;

        h0 += f9 * f1_4;
        h1 += f9 * f2_2;
        h2 += f9 * f3_4;
        h3 += f9 * f4_2;
        h4 += f9 * f5_4;
        h5 += f9 * f6_2;
        h6 += f9 * f7_4;
        h7 += f9 * f8_2;

        h1 += h0 >>> 26;
        h0 &= 0x3ffffff;
        h2 += h1 >>> 25;
        h1 &= 0x1ffffff;
        h3 += h2 >>> 26;
        h2 &= 0x3ffffff;
        h4 += h3 >>> 25;
        h3 &= 0x1ffffff;
        h5 += h4 >>> 26;
        h4 &= 0x3ffffff;
        h6 += h5 >>> 25;
        h5 &= 0x1ffffff;
        h7 += h6 >>> 26;
        h6 &= 0x3ffffff;
        h8 += h7 >>> 25;
        h7 &= 0x1ffffff;
        h9 += h8 >>> 26;
        h8 &= 0x3ffffff;
        //h9 &= 0x3ffffff;

        out.a = (int)h0;
        out.b = (int)h1;
        out.c = (int)h2;
        out.d = (int)h3;
        out.e = (int)h4;
        out.f = (int)h5;
        out.g = (int)h6;
        out.h = (int)h7;
        out.i = (int)h8;
        out.j = (int)h9;
    }

    /*public static void mul121666_2(Fe out, Fe in) {
        Fe m = new Fe();
        m.a = 121666;
        mul(out, in, m);
    }*/

    public static void mul121666(Fe out, Fe f) {
        long h0 = (long)f.a * 121666, h1 = (long)f.b * 121666, h2 = (long)f.c * 121666, h3 = (long)f.d * 121666, h4 = (long)f.e * 121666, h5 = (long)f.f * 121666, h6 = (long)f.g * 121666, h7 = (long)f.h * 121666, h8 = (long)f.i * 121666, h9 = (long)f.j * 121666;

        h0 += (h9 >>> 25) * 19;
        h9 &= 0x1ffffff;

        h1 += h0 >>> 26;
        out.a = (int)h0 & 0x3ffffff;
        h2 += h1 >>> 25;
        out.b = (int)h1 & 0x1ffffff;
        h3 += h2 >>> 26;
        out.c = (int)h2 & 0x3ffffff;
        h4 += h3 >>> 25;
        out.d = (int)h3 & 0x1ffffff;
        h5 += h4 >>> 26;
        out.e = (int)h4 & 0x3ffffff;
        h6 += h5 >>> 25;
        out.f = (int)h5 & 0x1ffffff;
        h7 += h6 >>> 26;
        out.g = (int)h6 & 0x3ffffff;
        h8 += h7 >>> 25;
        out.h = (int)h7 & 0x1ffffff;
        h9 += h8 >>> 26;
        out.i = (int)h8 & 0x3ffffff;
        out.j = (int)h9;
    }

    public static void add(Fe out, Fe f, Fe g) {
        out.a = f.a + g.a;
        out.b = f.b + g.b;
        out.c = f.c + g.c;
        out.d = f.d + g.d;
        out.e = f.e + g.e;
        out.f = f.f + g.f;
        out.g = f.g + g.g;
        out.h = f.h + g.h;
        out.i = f.i + g.i;
        out.j = f.j + g.j;
    }

    // Each limb in g must be at most 26 bits
    public static void sub(Fe out, Fe f, Fe g) {
        out.a = f.a - g.a + 0x7ffffb4;
        out.b = f.b - g.b + 0x7fffffe;
        out.c = f.c - g.c + 0x7fffffc;
        out.d = f.d - g.d + 0x7fffffe;
        out.e = f.e - g.e + 0x7fffffc;
        out.f = f.f - g.f + 0x7fffffe;
        out.g = f.g - g.g + 0x7fffffc;
        out.h = f.h - g.h + 0x7fffffe;
        out.i = f.i - g.i + 0x7fffffc;
        out.j = f.j - g.j + 0x7fffffe;
    }

    public static void reduceOnce(Fe f) {
        f.a += (f.j >> 25) * 19;
        f.j &= 0x1ffffff;
        f.b += f.a >> 26;
        f.a &= 0x3ffffff;
        f.c += f.b >> 25;
        f.b &= 0x1ffffff;
        f.d += f.c >> 26;
        f.c &= 0x3ffffff;
        f.e += f.d >> 25;
        f.d &= 0x1ffffff;
        f.f += f.e >> 26;
        f.e &= 0x3ffffff;
        f.g += f.f >> 25;
        f.f &= 0x1ffffff;
        f.h += f.g >> 26;
        f.g &= 0x3ffffff;
        f.i += f.h >> 25;
        f.h &= 0x1ffffff;
        f.j += f.i >> 26;
        f.i &= 0x3ffffff;
    }

    public static void sqrMany(Fe out, Fe in, int times) {
        sqr(out, in);
        for (int i = 1; i < times; i++) {
            sqr(out, out);
        }
    }

    public static void pow(Fe out, Fe in, boolean doSqrt) {
        Fe t0 = new Fe(), t1 = new Fe(), t2 = new Fe(), t3 = new Fe();

        sqr(t0, in);                // 2^1

        sqr(t1, t0);                // 2^2
        sqr(t1, t1);                // 2^3
        mul(t1, t1, in);            // 2^3 + 2^0 = 9
        mul(t0, t1, t0);            // 9 + 2^1 = 11
        sqr(t2, t0);                // 11*2 = 22
        mul(t1, t2, t1);            // 22 + 9 = 2^5 - 2^0
        sqrMany(t2, t1, 5);         // 2^10 - 2^5

        mul(t1, t2, t1);            // 2^10 - 2^0
        sqrMany(t2, t1, 10);        // 2^20 - 2^10
        mul(t2, t2, t1);            // 2^20 - 2^0
        sqrMany(t3, t2, 20);        // 2^40 - 2^20
        mul(t2, t3, t2);            // 2^40 - 2^0
        sqrMany(t2, t2, 10);        // 2^50 - 2^10
        mul(t1, t2, t1);            // 2^50 - 2^0
        sqrMany(t2, t1, 50);        // 2^100 - 2^50
        mul(t2, t2, t1);            // 2^100 - 2^0
        sqrMany(t3, t2, 100);       // 2^200 - 2^100
        mul(t2, t3, t2);            // 2^200 - 2^0
        sqrMany(t2, t2, 50);        // 2^250 - 2^50
        mul(t2, t2, t1);            // 2^250 - 2^0
        if (!doSqrt) {
            sqrMany(t2, t2, 5);     // 2^255 - 2^5
            mul(out, t2, t0);       // 2^255 - 21
        } else {
            sqrMany(t2, t2, 2);     // 2^252 - 2^2
            mul(out, t2, in);       // 2^252 - 3
        }
    }

    public static void csel(Fe out, Fe in1, Fe in2, int sel) {
        int sel0 = sel - 1;
        int sel1 = -sel;
        out.a = (in1.a & sel0) | (in2.a & sel1);
        out.b = (in1.b & sel0) | (in2.b & sel1);
        out.c = (in1.c & sel0) | (in2.c & sel1);
        out.d = (in1.d & sel0) | (in2.d & sel1);
        out.e = (in1.e & sel0) | (in2.e & sel1);
        out.f = (in1.f & sel0) | (in2.f & sel1);
        out.g = (in1.g & sel0) | (in2.g & sel1);
        out.h = (in1.h & sel0) | (in2.h & sel1);
        out.i = (in1.i & sel0) | (in2.i & sel1);
        out.j = (in1.j & sel0) | (in2.j & sel1);
    }

    public static byte[] curve25519(byte[] point, byte[] scalar) {
        Fe x = new Fe();
        fromBytes(x, point);
        Fe x2 = new Fe(), z2 = new Fe(), x3 = new Fe(x), z3 = new Fe();
        Fe b = new Fe(), d = new Fe(), a = new Fe(), c = new Fe(), aa = new Fe(), bb = new Fe(), e = new Fe();
        Fe f = new Fe(), g = new Fe(), da = new Fe(), cb = new Fe(), t1 = new Fe(), t2 = new Fe();

        x2.a = 1;
        z3.a = 1;

        scalar[31] = (byte)((scalar[31] & 0x7f) | 0x40);
        scalar[0] &= 0xf8;

        int last = 0;
        for (int i = 255; i >= 0; i--) {
            int bit = (scalar[i >>> 3] >>> (i & 7)) & 1;
            int val = bit ^ last;
            last = bit;
            sub(b, x2, z2);
            sub(d, x3, z3);
            add(a, x2, z2);
            add(c, x3, z3);
            csel(f, a, c, val);
            csel(g, b, d, val);
            sqr(aa, f);
            sqr(bb, g);
            sub(e, aa, bb);
            mul121666(z2, e);
            add(z2, bb, z2);
            mul(z2, z2, e);
            mul(da, d, a);
            mul(cb, c, b);
            add(t1, da, cb);
            sub(t2, da, cb);
            sqr(x3, t1);
            sqr(t2, t2);
            mul(x2, aa, bb);
            mul(z3, x, t2);
        }
        pow(z2, z2, false);
        mul(x2, x2, z2);
        return toBytes(x2);
    }

    public static byte[] curve25519Base(byte[] scalar) {
        byte[] bp = new byte[32];
        bp[0] = 9;
        return curve25519(bp, scalar);
    }

    public static void edwardsDbl(Fe outT, Fe outX, Fe outY, Fe outZ, Fe inX, Fe inY, Fe inZ, Fe tmp) {
        add(tmp, inX, inY);
        sqr(tmp, tmp);
        sqr(outX, inX);
        sqr(outT, inZ);
        add(outT, outT, outT);
        sqr(outZ, inY);
        add(outT, outT, outX);
        sub(outT, outT, outZ);
        add(outY, outX, outZ);
        reduceOnce(outY);
        sub(outZ, outZ, outX);
        sub(outX, tmp, outY);
    }

    // overwrites inT
    public static void edwardsAddSub(Fe outT, Fe outX, Fe outY, Fe outZ, Fe inX, Fe inY, Fe inZ, Fe inT, boolean sub, Fe qYpX, Fe qYmX, Fe qT2d) {
        add(outT, inY, inX);
        sub(outY, inY, inX);
        mul(outT, outT, sub ? qYmX : qYpX);
        mul(outY, outY, sub ? qYpX : qYmX);
        sub(outX, outT, outY);
        add(outY, outT, outY);
        mul(inT, qT2d, inT);
        add(outZ, inZ, inZ);
        if (sub) {
            add(outT, outZ, inT);
            sub(outZ, outZ, inT);
            reduceOnce(outZ);
        } else {
            sub(outT, outZ, inT);
            add(outZ, outZ, inT);
        }
    }

    public static void edwardsP1P1ToP3(Fe outX, Fe outY, Fe outZ, Fe outT, Fe inT, Fe inX, Fe inY, Fe inZ) {
        mul(outT, inY, inX);
        mul(outX, inX, inT);
        mul(outY, inY, inZ);
        mul(outZ, inZ, inT);
    }

    public static void edwardsP1P1ToP2(Fe outX, Fe outY, Fe outZ, Fe inT, Fe inX, Fe inY, Fe inZ) {
        mul(outX, inX, inT);
        mul(outY, inY, inZ);
        mul(outZ, inZ, inT);
    }

    private static long loadLong(byte[] in, int offset) {
        return (in[offset] & 0xff) | ((in[offset + 1] & 0xff) << 8) | ((in[offset + 2] & 0xff) << 16) | ((long)(in[offset + 3] & 0xff) << 24) |
                ((long)(in[offset + 4] & 0xff) << 32) | ((long)(in[offset + 5] & 0xff) << 40) | ((long)(in[offset + 6] & 0xff) << 48) | ((long)(in[offset + 7] & 0xff) << 56);
    }

    private static void storeLong(byte[] out, int offset, long v) {
        out[offset] = (byte)v;
        out[offset + 1] = (byte)(v >> 8);
        out[offset + 2] = (byte)(v >> 16);
        out[offset + 3] = (byte)(v >> 24);
        out[offset + 4] = (byte)(v >> 32);
        out[offset + 5] = (byte)(v >> 40);
        out[offset + 6] = (byte)(v >> 48);
        out[offset + 7] = (byte)(v >> 56);
    }

    public static void fromBytes(Fe out, byte[] in) {
        long v0 = loadLong(in, 0);
        long v1 = loadLong(in, 8);
        long v2 = loadLong(in, 16);
        long v3 = loadLong(in, 24);
        out.a = (int)(v0 & 0x3ffffff);
        out.b = (int)((v0 >> 26) & 0x1ffffff);
        out.c = (int)((v0 >>> 51) | ((v1 & 0x1fff) << 13));
        out.d = (int)((v1 >> 13) & 0x1ffffff);
        out.e = (int)(v1 >>> 38);
        out.f = (int)(v2 & 0x1ffffff);
        out.g = (int)((v2 >> 25) & 0x3ffffff);
        out.h = (int)((v2 >>> 51) | (v3 & 0xfff) << 13);
        out.i = (int)((v3 >> 12) & 0x3ffffff);
        out.j = (int)((v3 >> 38) & 0x1ffffff);
    }

    private static int addOverflows(long a, long b) {
        return (int)((((a & 0x7fffffffffffffffL) + (b & 0x7fffffffffffffffL)) >>> 63) + (a >>> 63) + (b >>> 63)) >> 1;
    }

    public static byte[] toBytes(Fe in) {
        //reduceOnce(in);
        // We take care of when in.g and in.j have 26 bits

        long v0 = in.a | ((long)in.b << 26) | ((long)in.c << 51);
        long v1 = (in.c >> 13) | ((long)in.d << 13) | ((long)in.e << 38);
        long v2 = in.f + ((long)in.g << 25);
        long v2_2 = ((long)in.h << 51);
        long v3 = (in.h >> 13) | ((long)in.i << 12) | ((long)in.j << 38);

        int v3Extra = addOverflows(v2, v2_2);
        v2 += v2_2;
        int v4 = addOverflows(v3, v3Extra);
        v3 += v3Extra;

        int tst = ((int)(v3 >>> 63) | (v4 << 1)) * 19 + 19;

        tst = addOverflows(v0, tst);
        tst = addOverflows(v1, tst);
        tst = addOverflows(v2, tst);
        int carry = addOverflows(v3, tst);
        carry = (int)(((v4 + carry) << 1) | ((v3 + tst) >>> 63) * 19);

        int carry2 = addOverflows(v0, carry);
        v0 += carry;
        carry = addOverflows(v1, carry2);
        v1 += carry2;
        carry2 = addOverflows(v2, carry);
        v2 += carry;
        v3 = (v3 + carry2) & 0x7fffffffffffffffL;

        byte[] out = new byte[32];
        storeLong(out, 0, v0);
        storeLong(out, 8, v1);
        storeLong(out, 16, v2);
        storeLong(out, 24, v3);
        return out;
    }

    /*private static byte[] slide(byte[] in) {
        byte[] out = new byte[256];
        for (int i = 0; i < 32; i++) {
            int word = in[i] & 0xff;
            for (int j = 0; j < 8; j++) {
                out[i * 8 + j] = (byte)(word & 1);
                word >>= 1;
            }
        }

        int outPos = 0;
        for (int i = 0; i < 255; i++) {
            int v = out[outPos];
            if (v != 0) {
                for (int j = 1; j <= 4 && i + j <= 255; j++) {
                    int o = out[outPos + j];
                    if (o == 0) {
                        continue;
                    }
                    o <<= j;
                    int a = v + o;
                    if (a <= 15) {
                        v = a;
                        out[outPos + j] = 0;
                        continue;
                    }
                    a = v - o;
                    if (a < -15) {
                        break;
                    }
                    v = a;
                    int outPos2 = outPos + j;
                    do {
                        out[outPos2++] = 0;
                    } while (out[outPos2] != 0);
                    out[outPos2] = 1;
                    j = outPos2 - outPos;
                }
            }
            out[outPos++] = (byte)v;
        }
        return out;
    }*/

    public static class Precomp {
        public Fe YpX, YmX, T2d;

        private static int decodeBase64Char(char c) {
            if (c >= 'A' && c <= 'Z') {
                return c - 'A';
            }
            if (c >= 'a' && c <= 'z') {
                return c - 'a' + 26;
            }
            if (c >= '0' && c <= '9') {
                return c - '0' + 52;
            }
            return c == '+' ? 62 : 63;
        }
        private static byte[] base64Decode(String str) {
            byte[] ret = new byte[864];
            int outpos = 0;
            for (int i = 0; i < 1152; i += 4) {
                int v = decodeBase64Char(str.charAt(i + 3)) | (decodeBase64Char(str.charAt(i + 2)) << 6) | (decodeBase64Char(str.charAt(i + 1)) << 12) | (decodeBase64Char(str.charAt(i)) << 18);
                ret[outpos++] = (byte)(v >> 16);
                ret[outpos++] = (byte)(v >> 8);
                ret[outpos++] = (byte)v;
            }
            return ret;
        }

        public static Precomp[] fromString(String str) {
            Precomp[] res = new Precomp[9];
            byte[] arr = base64Decode(str);
            for (int i = 0; i < 9; i++) {
                Precomp p = new Precomp();
                p.YpX = new Fe();
                p.YmX = new Fe();
                p.T2d = new Fe();
                fromBytes(p.YpX, Arrays.copyOfRange(arr, i * 96, i * 96 + 32));
                fromBytes(p.YmX, Arrays.copyOfRange(arr, i * 96 + 32, i * 96 + 64));
                fromBytes(p.T2d, Arrays.copyOfRange(arr, i * 96 + 64, i * 96 + 96));
                res[i] = p;
            }
            return res;
        }
    }

    private static final Precomp[] precompA = Precomp.fromString("9Lekk7h2EVU2zoom9xFdttKpIOwuy8ZZ0CVy6965iV69y9W4t9OEASv/peGJ9SPAByywumIKE5NBx+ZVqsi0V31bihCJx0+oYW9LYWyy0XhN/ACe9MCloX7MmNpxPjVfodTIubsArEYKQAVEvwzPpWD3ma8raLzshs5gZ5tFKipAT4i7kK+y+e+RvcMdmLuc/iCPHE+G+3TbbadwndO5clmCheArFkwL/tvkPJW2L7mrSEJ+75O96j2mnKQA4qFLqcMGH/5BiJDxMpyxRfgoMDEhpVzi3zyOgoLjME+KNz7AVBPo64O8uxQ9WdhNvso2VyqVZ8iifXSN5UIcA+2sYFkQBQ3C4b2CysRQrWY/uQ955NvkRfq5r4v84RuKxTUymZIwxIwKh+s2aLrjAUmi9hZzRJDtJ00Ym6qpJoQL6wfzoEaxMC/J5g4M98F4Gz+rscXdDNs7D0aW1fzrvrt5BfjDngYu4//RutH7k99ivotkY7mF4Zl5ses37SdmXyodHuktfKgHGn+Z9+Vp35WiiIKRWILLsOuZzMfodpm+s0J84j2YsR+lMvDc5Dr1LeDZL6JiGrkZTNkukdg6I9GhIZjFL0KxvjjF/MgnieD4T+RVslBqAytMTpOIe4YhXX9yYjoTVeJll5RfuXPVvZI9b7uqQaYb3arBATsKUYBbRFf83FNpGGNq4QWuwuipjM8XmvGJIanbsMvTmHJ3RnlQEc8BCBcnp2ERFkGSZVpvzPY/Oqfa/KRtkgqZy5d8reAporQIIDxm1txOnbvtZHC2PdHykP3/nPNEwYwEg8IzpE1C0rj5A+Yk/Kak9uksv32fC0I0yuFcbaBHw84W3xZUM/6yqQAhFFTwKlk3r4czixhFi8/QpcnA4ERM2i5GSBAHGkIGNPWzcG+JBFrH7ZIg6544NySq/G/wEzkB4bWZ4TeV3haJ7cW5IwFbGwH7tQxG52qBlEg9DKgn1Ht6WnMKLjAI5VTUd9KfNlyOUb27S+kxsTTprnESVgZmET2dTeptnVTEJ7AQzzqZAtPoEZH5JWaYnVGz1A3sGOnYYXRhNX/2KoRYCpmX/MI3y5gUy/pdSlmyPc/7sJX3AHtd8v9rRqaG0Dx/SQe2F+dvPXiiOtsrcnVK3Hd6LJMnav+eMEg5");
    private static final Precomp[] precompB = Precomp.fromString("PjhzJMiisEtUSJrTHNmTi1GLRaruwoNkJ2gISEaUzkut0IjbE6ji48FNsVs9sUBv0iseVSVUccZtOWTErrcFQBx57FuC4YDMJXZtr9CptOCYLrsvfTVNYdpDpmN2u3cc52g+rlm10ha9rEdYc2LFb3UIKZZUH4m5Zorzb8/9qEOwNHGdddLe6Q0Ke1STtbzn+0RzAvBlvF6ngLyJjzVPZrTrFWdkOdQ98aPkO/GSTRFZwdRXZ4tD4YVs73ycfLVRgUcErAU+FJlShnLGpv2AVdK8FQzZhPQ+VybOSB6cnR3J1Q1aZRszH9MrhslhX1QLM3hzFbOJlzlc1EN3kHNQE6serfUvhuATetFxkLlj+v7rjILzk2Y32FoFXUqx51YKnIxfJWrOz63uiETJQXfYi/1bRATUlDoJihTp/PMRJXs+WyO/90ZCEst92B5Ubn7oklU4wTTsUngTX0JliFoYfO+v2Z6OgU+XGXwGawE5MwOl8GWh3Zp8RLVXGBSgBGRZjQy/2B8biffJrgufiydSszPizaw5FyYIXMjZ8lSHaG4tSpSEkY9jggQwgpetrlRh7qzAL99Jo3jAWeBHpN2qEbnPVPLZM2Oj2sPvDVQ8+L6iym/1w/iWjrutd0sjssxCM9n9d2j90sxCnnVD/m53gnTDl+Sf9MTeGowkSFdpRF6rcZovxID4YzoGApGwkQtF0Rsp1IJ86UZu+6j4s/7SQ/4fCZd0Fr0oCKdLndCVKXCU7QBfGtM3+7wSFJ/v+04VolNgFcguewUpkf3yJrSsDdXdEL0gjQ3jLrObB52WtyedERonsHR+CEIfDgsn4MBnj2QrDZ2D3O6Lipz7lqDmaoGCjZBaPiSn0YxRj9qM4XKs3I0fO+jj8jx7j726PdFwQrj7R2frfhOLGoFgdVzfeZrI+HFvpytawv/IO1YqlQk8+H7cS8uooibCk1/6xrWWpeNkBhvr69Qvz8bl3EqbQMRQQ4O5PdVEtAX1pQWTKYkv/0lZovoi+2R9ZQSnaLlphTuM9caTvC8ZDoz7xi2Tz8JCPWSYSAsnZbrUMzqdzwc+kUDXBTkQnbO+QNEFnzn9CYqPaDSEwaVnEviYki/9RGiqeocFEsmrnsSqzCPo2SaMWUPdy30bWqhlDJ9oexFv");
    private static BigInteger order = new BigInteger("1000000000000000000000000000000014def9dea2f79cd65812631a5cf5d3ed", 16);
    public static boolean ed25519VerifyHram(byte[] signature, byte[] hram) {
        if ((signature[63] & 0xe0) != 0) {
            return false;
        }
        byte[] hramBigEndian = new byte[64];
        for (int i = 0; i < 64; i++) {
            hramBigEndian[i] = hram[63 - i];
        }
        byte[] reducedHramBigEndian = new BigInteger(1, hramBigEndian).mod(order).toByteArray();
        byte[] reducedHram = new byte[33];
        for (int i = 0; i < 32; i++) {
            if (i < reducedHramBigEndian.length) {
                reducedHram[i] = reducedHramBigEndian[reducedHramBigEndian.length - 1 - i];
            }
        }
        byte[] aScalar = reducedHram;
        byte[] bScalar = new byte[33];
        for (int i = 0; i < 32; i++) {
            bScalar[i] = signature[32 + i];
        }

        Fe tX = new Fe(), tY = new Fe(), tZ = new Fe(), tT1 = new Fe(), tT2 = new Fe();
        tY.a = 1;
        tZ.a = 1;

        aScalar[32] = 1;
        bScalar[32] = 1;

        for (int i = 64; i > 0; i--) {
            edwardsDbl(tT1, tX, tY, tZ, tX, tY, tZ, tT2);

            int iMod8 = i & 7;
            int a = ((aScalar[i >> 3] >> iMod8) & 1) |
                    (((aScalar[(i + 64) >> 3] >> iMod8) & 1) << 1) |
                    (((aScalar[(i + 128) >> 3] >> iMod8) & 1) << 2);
            int negateA = 1 - ((aScalar[(i + 192) >> 3] >> iMod8) & 1);
            if (negateA != 0) {
                a = ~a & 7;
            }
            Precomp qA = precompA[a];

            edwardsP1P1ToP3(tX, tY, tZ, tT2, tT1, tX, tY, tZ);
            edwardsAddSub(tT1, tX, tY, tZ, tX, tY, tZ, tT2, negateA == 0, qA.YpX, qA.YmX, qA.T2d);

            int b = ((bScalar[i >> 3] >> iMod8) & 1) |
                    (((bScalar[(i + 64) >> 3] >> iMod8) & 1) << 1) |
                    (((bScalar[(i + 128) >> 3] >> iMod8) & 1) << 2);
            int negateB = 1 - ((bScalar[(i + 192) >> 3] >> iMod8) & 1);
            if (negateB != 0) {
                b = ~b & 7;
            }
            Precomp qB = precompB[b];
            edwardsP1P1ToP3(tX, tY, tZ, tT2, tT1, tX, tY, tZ);
            edwardsAddSub(tT1, tX, tY, tZ, tX, tY, tZ, tT2, negateB != 0, qB.YpX, qB.YmX, qB.T2d);

            if (i > 1) {
                edwardsP1P1ToP2(tX, tY, tZ, tT1, tX, tY, tZ);
            }
        }

        if ((aScalar[0] & 1) == 0) {
            Precomp qA = precompA[8];
            edwardsP1P1ToP3(tX, tY, tZ, tT2, tT1, tX, tY, tZ);
            edwardsAddSub(tT1, tX, tY, tZ, tX, tY, tZ, tT2, false, qA.YpX, qA.YmX, qA.T2d);
        }
        if ((bScalar[0] & 1) == 0) {
            Precomp qB = precompB[8];
            edwardsP1P1ToP3(tX, tY, tZ, tT2, tT1, tX, tY, tZ);
            edwardsAddSub(tT1, tX, tY, tZ, tX, tY, tZ, tT2, true, qB.YpX, qB.YmX, qB.T2d);
        }
        edwardsP1P1ToP2(tX, tY, tZ, tT1, tX, tY, tZ);
        pow(tZ, tZ, false);
        mul(tX, tX, tZ);
        mul(tY, tY, tZ);
        byte[] yBytes = toBytes(tY);
        byte[] xBytes = toBytes(tX);
        yBytes[31] |= xBytes[0] << 7;
        return Arrays.equals(yBytes, Arrays.copyOfRange(signature, 0, 32));
    }

    private static byte[] pubkeyBytes = new byte[] {(byte)211, 63, 36, 64, (byte)221, 84, (byte)179, 27, 46, 29, (byte)207, 64, 19, 46, (byte)250, 65, (byte)216, (byte)248, (byte)167, 71, 65, 104, (byte)223, 64, 8, (byte)245, (byte)169, 95, (byte)179, (byte)176, (byte)208, 34};
    public static boolean ed25519Verify(byte[] signature, byte[] message) {
        byte[] hram;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(signature, 0, 32);
            md.update(pubkeyBytes);
            md.update(message);
            hram = md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return ed25519VerifyHram(signature, hram);
    }

    public static int loadInt(byte[] in, int offset) {
        return (in[offset] & 0xff) | ((in[offset + 1] & 0xff) << 8) | ((in[offset + 2] & 0xff) << 16) | ((in[offset + 3] & 0xff) << 24);
    }

    public static int[] chaskeyGenerateSubkeys(byte[] key) {
        int[] ret = new int[12];

        int v0, v1, v2, v3;
        v0 = loadInt(key, 0);
        v1 = loadInt(key, 4);
        v2 = loadInt(key, 8);
        v3 = loadInt(key, 12);
        ret[0] = v0;
        ret[1] = v1;
        ret[2] = v2;
        ret[3] = v3;

        int c = (v3 >>> 31) * 0x87;
        v3 = (v3 << 1) | (v2 >>> 31);
        v2 = (v2 << 1) | (v1 >>> 31);
        v1 = (v1 << 1) | (v0 >>> 31);
        v0 = (v0 << 1) ^ c;
        ret[4] = v0;
        ret[5] = v1;
        ret[6] = v2;
        ret[7] = v3;

        c = (v3 >>> 31) * 0x87;
        v3 = (v3 << 1) | (v2 >>> 31);
        v2 = (v2 << 1) | (v1 >>> 31);
        v1 = (v1 << 1) | (v0 >>> 31);
        v0 = (v0 << 1) ^ c;
        ret[8] = v0;
        ret[9] = v1;
        ret[10] = v2;
        ret[11] = v3;

        return ret;
    }

    public static byte[] chaskeyWithDirAndPacketCounter(int[] keys, int dir, long counter, byte[] data) {
        if (data.length == 0) {
            throw new IllegalArgumentException();
        }

        int v0, v1, v2, v3;
        v0 = keys[0] ^ (int)counter;
        v1 = keys[1] ^ (int)(counter >> 32);
        v2 = keys[2] ^ dir;
        v3 = keys[3];

        int offset = 0;
        int len = data.length;
        boolean first = true;
        for (;;) {
            int keysOffset = 0;
            if (!first) {
                if (len >= 16) {
                    v0 ^= loadInt(data, offset);
                    v1 ^= loadInt(data, offset + 4);
                    v2 ^= loadInt(data, offset + 8);
                    v3 ^= loadInt(data, offset + 12);
                    offset += 16;
                    len -= 16;
                    if (len == 0) {
                        keysOffset = 4;
                    }
                } else {
                    byte[] tmp = new byte[16];
                    System.arraycopy(data, offset, tmp, 0, len);
                    tmp[len] = 0x01;
                    v0 ^= loadInt(tmp, 0);
                    v1 ^= loadInt(tmp, 4);
                    v2 ^= loadInt(tmp, 8);
                    v3 ^= loadInt(tmp, 12);
                    keysOffset = 8;
                }

                if (keysOffset != 0) {
                    v0 ^= keys[keysOffset];
                    v1 ^= keys[keysOffset + 1];
                    v2 ^= keys[keysOffset + 2];
                    v3 ^= keys[keysOffset + 3];
                }
            } else {
                first = false;
            }

            v2 = (v2 >>> 16) | (v2 << 16);
            for (int i = 0; i < 16; i++) {
                v0 = v0 + v1;
                v1 = v0 ^ ((v1 >>> 27) | (v1 << 5));
                v2 = v3 + ((v2 >>> 16) | (v2 << 16));
                v3 = v2 ^ ((v3 >>> 24) | (v3 << 8));
                v2 = v2 + v1;
                v0 = v3 + ((v0 >>> 16) | (v0 << 16));
                v1 = v2 ^ ((v1 >>> 25) | (v1 << 7));
                v3 = v0 ^ ((v3 >>> 19) | (v3 << 13));
            }
            v2 = (v2 >>> 16) | (v2 << 16);

            if (keysOffset != 0) {
                v0 ^= keys[keysOffset];
                v1 ^= keys[keysOffset + 1];
                byte[] ret = new byte[5];
                ret[0] = (byte)v0;
                ret[1] = (byte)(v0 >> 8);
                ret[2] = (byte)(v0 >> 16);
                ret[3] = (byte)(v0 >> 24);
                ret[4] = (byte)v1;
                return ret;
            }
        }
    }

    public static byte[] chaskey16Bytes(int[] keys, byte[] data) {
        int v0, v1, v2, v3;
        v0 = keys[0] ^ keys[4] ^ loadInt(data, 0);
        v1 = keys[1] ^ keys[5] ^ loadInt(data, 4);
        v2 = keys[2] ^ keys[6] ^ loadInt(data, 8);
        v3 = keys[3] ^ keys[7] ^ loadInt(data, 12);

        v2 = (v2 >>> 16) | (v2 << 16);
        for (int i = 0; i < 16; i++) {
            v0 = v0 + v1;
            v1 = v0 ^ ((v1 >>> 27) | (v1 << 5));
            v2 = v3 + ((v2 >>> 16) | (v2 << 16));
            v3 = v2 ^ ((v3 >>> 24) | (v3 << 8));
            v2 = v2 + v1;
            v0 = v3 + ((v0 >>> 16) | (v0 << 16));
            v1 = v2 ^ ((v1 >>> 25) | (v1 << 7));
            v3 = v0 ^ ((v3 >>> 19) | (v3 << 13));
        }
        v2 = (v2 >>> 16) | (v2 << 16);

        v0 ^= keys[4];
        v1 ^= keys[5];
        v2 ^= keys[6];
        v3 ^= keys[7];

        byte[] res = new byte[16];
        storeLong(res, 0, (v0 & 0xffffffffL) | ((long)v1 << 32));
        storeLong(res, 8, (v2 & 0xffffffffL) | ((long)v3 << 32));
        return res;
    }
}
