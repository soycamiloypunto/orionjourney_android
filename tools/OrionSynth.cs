using System;
using System.IO;

public static class OrionSynth
{
    const int SR = 22050;
    static Random rng = new Random(1234);

    static double Hz(int m) { return 440.0 * Math.Pow(2.0, (m - 69) / 12.0); }

    public class Mix
    {
        public float[] dry; public float[] wet;
        public Mix(int n) { dry = new float[n]; wet = new float[n]; }
    }

    static void Add(Mix mx, int i, double v, double send)
    {
        if (i < 0 || i >= mx.dry.Length) return;
        mx.dry[i] += (float)v; mx.wet[i] += (float)(v * send);
    }

    // inst: 0 strings ensemble, 1 brass/horn, 2 harp, 3 bass (cello+contrabass), 4 celesta, 5 soft strings (sad)
    public static void Note(Mix mx, double t0, double dur, int midi, int inst, double vel)
    {
        double f = Hz(midi);
        double att, rel, send;
        switch (inst)
        {
            case 0: att = 0.25; rel = 0.6; send = 0.45; break;
            case 1: att = 0.05; rel = 0.25; send = 0.35; break;
            case 2: att = 0.003; rel = 0.0; send = 0.5; break;
            case 3: att = 0.04; rel = 0.2; send = 0.12; break;
            case 4: att = 0.002; rel = 0.0; send = 0.65; break;
            default: att = 0.6; rel = 1.0; send = 0.6; break;
        }
        bool pluck = (inst == 2 || inst == 4);
        double total = pluck ? Math.Max(dur, 2.2) : dur + rel;
        int s0 = (int)(t0 * SR); int n = (int)(total * SR);
        double det = 0.0035;
        double[] fs = { f * (1 - det), f, f * (1 + det) };
        int H = inst == 0 ? 7 : 4;
        for (int k = 0; k < n; k++)
        {
            double t = (double)k / SR;
            double env;
            if (pluck)
            {
                env = Math.Min(1.0, t / att) * Math.Exp(-t * (inst == 2 ? 2.2 : 3.0));
            }
            else
            {
                env = Math.Min(1.0, t / att);
                if (t > dur) env *= Math.Max(0.0, 1.0 - (t - dur) / rel);
            }
            double v = 0;
            if (inst == 0 || inst == 5)
            {
                for (int d = 0; d < 3; d++)
                {
                    double ph = 2 * Math.PI * fs[d] * t + d * 1.7;
                    for (int h = 1; h <= H; h++) v += Math.Sin(ph * h) / Math.Pow(h, 1.4);
                }
                v *= 0.22 * (1.0 + 0.06 * Math.Sin(2 * Math.PI * 5.0 * t));
            }
            else if (inst == 1)
            {
                double vib = 1.0 + 0.004 * Math.Sin(2 * Math.PI * 5.5 * t) * Math.Min(1.0, t / 0.3);
                double ph = 2 * Math.PI * f * vib * t;
                double bright = 0.40 + 0.45 * Math.Min(1.0, t / 0.08);
                for (int h = 1; h <= 10; h++) v += Math.Sin(ph * h) * Math.Pow(bright, h - 1) / h;
                v *= 0.5;
            }
            else if (inst == 2)
            {
                double ph = 2 * Math.PI * f * t;
                v = Math.Sin(ph) + 0.35 * Math.Sin(2 * ph) * Math.Exp(-t * 4) + 0.12 * Math.Sin(3 * ph) * Math.Exp(-t * 6);
                v *= 0.55;
            }
            else if (inst == 3)
            {
                double ph = 2 * Math.PI * f * t;
                for (int h = 1; h <= 6; h++) v += Math.Sin(ph * h) / Math.Pow(h, 1.6);
                v *= 0.6;
            }
            else if (inst == 4)
            {
                double ph = 2 * Math.PI * f * t;
                v = Math.Sin(ph) + 0.4 * Math.Sin(ph * 2.76) * Math.Exp(-t * 5) + 0.2 * Math.Sin(ph * 5.4) * Math.Exp(-t * 8);
                v *= 0.4;
            }
            Add(mx, s0 + k, v * env * vel, send);
        }
    }

    public static void Kick(Mix mx, double t0, double vel)
    {
        int s0 = (int)(t0 * SR); int n = (int)(0.5 * SR); double ph = 0;
        for (int k = 0; k < n; k++)
        {
            double t = (double)k / SR; double f = 48 + 70 * Math.Exp(-t * 25); ph += 2 * Math.PI * f / SR;
            Add(mx, s0 + k, Math.Sin(ph) * Math.Exp(-t * 7) * vel * 0.9, 0.1);
        }
    }

    public static void Timpani(Mix mx, double t0, int midi, double vel)
    {
        double f = Hz(midi); int s0 = (int)(t0 * SR); int n = (int)(1.8 * SR);
        for (int k = 0; k < n; k++)
        {
            double t = (double)k / SR; double ph = 2 * Math.PI * f * t;
            double v = (Math.Sin(ph) + 0.5 * Math.Sin(ph * 1.5) * Math.Exp(-t * 3) + 0.3 * Math.Sin(ph * 1.99) * Math.Exp(-t * 4)) * Math.Exp(-t * 2.2);
            v += (rng.NextDouble() * 2 - 1) * 0.3 * Math.Exp(-t * 40);
            Add(mx, s0 + k, v * vel * 0.7, 0.35);
        }
    }

    public static void Snare(Mix mx, double t0, double vel)
    {
        int s0 = (int)(t0 * SR); int n = (int)(0.3 * SR); double prev = 0;
        for (int k = 0; k < n; k++)
        {
            double t = (double)k / SR; double nz = rng.NextDouble() * 2 - 1; double hp = nz - prev; prev = nz;
            double v = hp * 0.5 * Math.Exp(-t * 16) + Math.Sin(2 * Math.PI * 185 * t) * 0.4 * Math.Exp(-t * 25);
            Add(mx, s0 + k, v * vel * 0.5, 0.3);
        }
    }

    public static void Hat(Mix mx, double t0, double vel)
    {
        int s0 = (int)(t0 * SR); int n = (int)(0.08 * SR); double prev = 0;
        for (int k = 0; k < n; k++)
        {
            double t = (double)k / SR; double nz = rng.NextDouble() * 2 - 1; double hp = nz - prev; prev = nz;
            Add(mx, s0 + k, hp * Math.Exp(-t * 60) * vel * 0.16, 0.15);
        }
    }

    public static void Crash(Mix mx, double t0, double vel)
    {
        int s0 = (int)(t0 * SR); int n = (int)(2.5 * SR); double prev = 0;
        for (int k = 0; k < n; k++)
        {
            double t = (double)k / SR; double nz = rng.NextDouble() * 2 - 1; double hp = nz - prev; prev = nz;
            Add(mx, s0 + k, hp * Math.Exp(-t * 1.8) * vel * 0.2, 0.4);
        }
    }

    public static void Swell(Mix mx, double t0, double dur, double vel)
    {
        int s0 = (int)(t0 * SR); int n = (int)(dur * SR); double prev = 0;
        for (int k = 0; k < n; k++)
        {
            double t = (double)k / SR; double nz = rng.NextDouble() * 2 - 1; double hp = nz - prev; prev = nz;
            double a = Math.Pow(t / dur, 2.5);
            Add(mx, s0 + k, hp * a * vel * 0.18, 0.5);
        }
    }

    static float[] Reverb(float[] input, double fb, double damp)
    {
        int[] combs = { 1557, 1617, 1491, 1422, 1277, 1356, 1188, 1116 };
        int[] aps = { 556, 441, 341, 225 };
        int n = input.Length; float[] outp = new float[n];
        for (int c = 0; c < combs.Length; c++)
        {
            int L = (int)(combs[c] * SR / 44100.0 * 1.3);
            float[] b = new float[L]; int idx = 0; double store = 0;
            for (int i = 0; i < n; i++)
            {
                double y = b[idx]; store = y * (1 - damp) + store * damp;
                b[idx] = (float)(input[i] * 0.015 + store * fb); idx = (idx + 1) % L; outp[i] += (float)y;
            }
        }
        for (int a = 0; a < aps.Length; a++)
        {
            int L = (int)(aps[a] * SR / 44100.0); float[] b = new float[L]; int idx = 0;
            for (int i = 0; i < n; i++)
            {
                double bo = b[idx]; double inp = outp[i];
                b[idx] = (float)(inp + bo * 0.5); idx = (idx + 1) % L; outp[i] = (float)(-inp + bo);
            }
        }
        return outp;
    }

    static float[] Master(Mix mx, double wetGain)
    {
        float[] rev = Reverb(mx.wet, 0.85, 0.3);
        int n = mx.dry.Length; float[] o = new float[n];
        double peak = 1e-9;
        for (int i = 0; i < n; i++) { o[i] = (float)(mx.dry[i] + rev[i] * wetGain); peak = Math.Max(peak, Math.Abs(o[i])); }
        double norm = 1.0 / peak; double drive = 1.4; double td = Math.Tanh(drive);
        for (int i = 0; i < n; i++) o[i] = (float)(Math.Tanh(o[i] * norm * drive) / td * 0.92);
        return o;
    }

    static void WriteWav(string path, float[] s)
    {
        using (BinaryWriter w = new BinaryWriter(File.Create(path)))
        {
            int dataLen = s.Length * 2;
            w.Write(new char[] { 'R', 'I', 'F', 'F' }); w.Write(36 + dataLen);
            w.Write(new char[] { 'W', 'A', 'V', 'E' }); w.Write(new char[] { 'f', 'm', 't', ' ' });
            w.Write(16); w.Write((short)1); w.Write((short)1); w.Write(SR); w.Write(SR * 2); w.Write((short)2); w.Write((short)16);
            w.Write(new char[] { 'd', 'a', 't', 'a' }); w.Write(dataLen);
            for (int i = 0; i < s.Length; i++) w.Write((short)Math.Max(-32767, Math.Min(32767, s[i] * 32767)));
        }
    }

    // ===================== WORLD 1 THEME =====================
    const double BPM = 108.0;
    const double BEAT = 60.0 / BPM;
    const double BAR = BEAT * 4;
    const int BARS = 32;

    static int ChordBass(string c)
    {
        switch (c) { case "D": return 38; case "A": return 45; case "Bm": return 47; case "G": return 43; case "Em": return 40; default: return 42; }
    }
    static int[] Triad(string c)
    {
        switch (c)
        {
            case "D": return new int[] { 62, 66, 69 };
            case "A": return new int[] { 61, 64, 69 };
            case "Bm": return new int[] { 62, 66, 71 };
            case "G": return new int[] { 62, 67, 71 };
            case "Em": return new int[] { 64, 67, 71 };
            case "F#": return new int[] { 61, 66, 70 };
            default: return new int[] { 61, 66, 69 };
        }
    }

    static void Melody(Mix mx, double off, int startBar, int[] grid, int inst, double vel, int transpose)
    {
        for (int i = 0; i < grid.Length; i++)
        {
            int m = grid[i]; if (m <= 0) continue;
            int len = 1; while (i + len < grid.Length && grid[i + len] == -1) len++;
            double t = off + startBar * BAR + i * BEAT;
            Note(mx, t, len * BEAT * 0.95, m + transpose, inst, vel);
        }
    }

    static void RenderWorld1(Mix mx, double off)
    {
        string[] chords = {
            "D","A","Bm","G","D","A","G","A",
            "D","A","Bm","G","D","A","G","A",
            "Bm","G","Em","F#",
            "G","A","Bm","A",
            "D","A","Bm","G","Em","A","D","A" };

        int[] melB = { 74,-1,78,81, 81,-1,79,76, 78,-1,74,71, 74,-1,-1,-1,
                       74,78,81,86, 85,-1,81,76, 79,78,76,74, 76,-1,-1,-1 };
        int[] melC = { 78,-1,-1,74, 79,-1,78,76, 74,-1,-1,71, 70,-1,-1,-1 };
        int[] melC2 = { 71,74,79,-1, 76,81,-1,-1, 78,-1,74,-1, 76,-1,73,-1 };
        int[] melD = { 86,-1,85,81, 81,-1,76,-1, 78,-1,83,81, 79,-1,-1,74,
                       76,79,83,-1, 81,-1,85,-1, 86,-1,-1,-1, 81,-1,76,73 };

        int[] arp8 = { 0, 1, 2, 3, 2, 1, 2, 1 };

        for (int b = 0; b < BARS; b++)
        {
            double t = off + b * BAR;
            string c = chords[b];
            int[] tri = Triad(c);
            int bass = ChordBass(c);
            int[] arpN = { tri[0] + 12, tri[1] + 12, tri[2] + 12, tri[0] + 24 };
            bool secA = b < 8, secB = b >= 8 && b < 16, secC = b >= 16 && b < 20, secC2 = b >= 20 && b < 24, secD = b >= 24;

            // ---- Pads ----
            double padVel = secA ? 0.30 : secC ? 0.0 : 0.38;
            if (padVel > 0) foreach (int m in tri) Note(mx, t, BAR * 0.98, m, 0, padVel);
            if (secC) foreach (int m in tri) Note(mx, t, BAR * 0.98, m, 5, 0.42);
            if (secD) Note(mx, t, BAR * 0.98, tri[0] - 12, 0, 0.30);

            // ---- Bass ----
            if (secA || secC)
            {
                Note(mx, t, BAR * 0.95, bass, 3, secC ? 0.35 : 0.45);
            }
            else
            {
                for (int e = 0; e < 8; e++)
                    Note(mx, t + e * BEAT * 0.5, BEAT * 0.45, bass + ((e % 2 == 1) ? 12 : 0), 3, secD ? 0.48 : 0.40);
            }

            // ---- Harp arpeggios ----
            if (secC)
            {
                for (int q = 0; q < 4; q++) Note(mx, t + q * BEAT, BEAT, arpN[arp8[q]], 2, 0.22);
            }
            else if (secD)
            {
                for (int s = 0; s < 16; s++) Note(mx, t + s * BEAT * 0.25, BEAT * 0.25, arpN[s % 4], 2, 0.20);
            }
            else
            {
                for (int e = 0; e < 8; e++) Note(mx, t + e * BEAT * 0.5, BEAT * 0.5, arpN[arp8[e]], 2, secA ? 0.38 : 0.30);
            }

            // ---- Celesta space sparkles ----
            if ((secA && b % 2 == 0) || (secD && b % 2 == 1))
            {
                Note(mx, t, BEAT, tri[2] + 24, 4, 0.30);
                Note(mx, t + BEAT * 1.5, BEAT, tri[1] + 24, 4, 0.22);
                Note(mx, t + BEAT * 3, BEAT, tri[0] + 36, 4, 0.18);
            }

            // ---- Percussion ----
            if (secA && b >= 4) { Kick(mx, t, 0.55); Kick(mx, t + 2 * BEAT, 0.45); }
            if (b == 7) for (int s = 0; s < 4; s++) Timpani(mx, t + 3 * BEAT + s * BEAT * 0.25, 45, 0.3 + s * 0.12);
            if (secB || secD)
            {
                Kick(mx, t, 0.8); Kick(mx, t + 2 * BEAT, 0.7);
                if (secD) Kick(mx, t + 2.5 * BEAT, 0.5);
                Snare(mx, t + BEAT, 0.6); Snare(mx, t + 3 * BEAT, 0.65);
                for (int e = 0; e < 8; e++) Hat(mx, t + e * BEAT * 0.5, (e % 2 == 0) ? 0.9 : 0.55);
            }
            if (b == 8 || b == 24 || b == 28) Crash(mx, t, 1.0);
            if (secD && b % 2 == 0) Timpani(mx, t, bass + 12 <= 50 ? bass + 12 : bass, 0.7);
            if (secC2)
            {
                for (int q = 0; q < 4; q++) Timpani(mx, t + q * BEAT, 38, 0.3 + (b - 20) * 0.12 + q * 0.03);
            }
            if (b == 19) Swell(mx, t, BAR, 0.7);
            if (b == 23)
            {
                for (int s = 0; s < 16; s++) Snare(mx, t + s * BEAT * 0.25, 0.2 + s * 0.04);
                Swell(mx, t, BAR, 1.0);
            }
        }

        // ---- Melodies ----
        Melody(mx, off, 8, melB, 1, 0.50, 0);
        Melody(mx, off, 8, melB, 0, 0.18, -12);
        Melody(mx, off, 16, melC, 0, 0.42, 0);
        Melody(mx, off, 16, melC, 4, 0.16, 12);
        Melody(mx, off, 20, melC2, 1, 0.48, 0);
        Melody(mx, off, 24, melD, 1, 0.58, 0);
        Melody(mx, off, 24, melD, 0, 0.30, -12);
    }

    public static void BuildWorld1(string path)
    {
        double songLen = BARS * BAR;
        int n = (int)(songLen * 2 * SR) + SR * 4;
        Mix mx = new Mix(n);
        RenderWorld1(mx, 0);
        RenderWorld1(mx, songLen);
        float[] full = Master(mx, 3.0);
        int s0 = (int)(songLen * SR); int len = (int)(songLen * SR);
        float[] loop = new float[len];
        Array.Copy(full, s0, loop, 0, len);
        WriteWav(path, loop);
    }

    // ===================== LEVEL COMPLETE JINGLE =====================
    public static void BuildLevelComplete(string path)
    {
        double total = 6.0;
        Mix mx = new Mix((int)(total * SR));

        // Pickup: harp glissando up D major
        int[] scale = { 62, 64, 66, 67, 69, 71, 73, 74, 76, 78, 79, 81 };
        for (int i = 0; i < scale.Length; i++) Note(mx, i * 0.025, 0.4, scale[i], 2, 0.22);

        // Chord 1: Bb (bVI) — brass triplet
        double t1 = 0.30;
        foreach (int m in new int[] { 58, 62, 65 }) Note(mx, t1, 0.55, m, 0, 0.35);
        Note(mx, t1, 0.55, 46, 3, 0.5);
        Note(mx, t1, 0.14, 70, 1, 0.55); Note(mx, t1 + 0.14, 0.14, 74, 1, 0.55); Note(mx, t1 + 0.28, 0.14, 77, 1, 0.58);
        Note(mx, t1 + 0.42, 0.14, 82, 1, 0.6);
        Kick(mx, t1, 0.7); Snare(mx, t1, 0.4);

        // Chord 2: C (bVII)
        double t2 = t1 + 0.58;
        foreach (int m in new int[] { 60, 64, 67 }) Note(mx, t2, 0.55, m, 0, 0.38);
        Note(mx, t2, 0.55, 48, 3, 0.5);
        Note(mx, t2, 0.14, 72, 1, 0.58); Note(mx, t2 + 0.14, 0.14, 76, 1, 0.58); Note(mx, t2 + 0.28, 0.14, 79, 1, 0.6);
        Note(mx, t2 + 0.42, 0.14, 84, 1, 0.62);
        Kick(mx, t2, 0.7); Snare(mx, t2, 0.45);
        for (int s = 0; s < 6; s++) Timpani(mx, t2 + 0.28 + s * 0.05, 38, 0.25 + s * 0.08);

        // Chord 3: D (I) — grand resolution
        double t3 = t2 + 0.58;
        foreach (int m in new int[] { 62, 66, 69, 74 }) Note(mx, t3, 2.6, m, 0, 0.42);
        Note(mx, t3, 2.6, 50, 0, 0.30);
        Note(mx, t3, 2.6, 38, 3, 0.6);
        Note(mx, t3, 2.5, 86, 1, 0.62);
        Note(mx, t3, 2.5, 81, 1, 0.42);
        Note(mx, t3, 2.5, 78, 1, 0.38);
        Kick(mx, t3, 0.9); Timpani(mx, t3, 38, 1.0); Crash(mx, t3, 1.1);
        int[] up = { 62, 66, 69, 74, 78, 81, 86, 90 };
        for (int i = 0; i < up.Length; i++) Note(mx, t3 + 0.05 + i * 0.07, 1.5, up[i], 2, 0.25);
        Note(mx, t3 + 0.7, 1.5, 90, 4, 0.30);
        Note(mx, t3 + 0.95, 1.5, 93, 4, 0.26);
        Note(mx, t3 + 1.2, 1.5, 98, 4, 0.22);

        float[] o = Master(mx, 3.2);
        // gentle fade at the very end
        int fade = (int)(0.6 * SR);
        for (int i = 0; i < fade; i++) o[o.Length - 1 - i] *= (float)i / fade;
        WriteWav(path, o);
    }
}
