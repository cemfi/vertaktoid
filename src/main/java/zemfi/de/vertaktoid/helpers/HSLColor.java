package zemfi.de.vertaktoid.helpers;

import android.graphics.Color;

/**
 * HSLColor class represents a color in the HSLA color scheme.
 * HSLA color scheme operates with following parameters:
 * h - The hue value.
 * s - The saturation value.
 * l - The lightness value.
 * a - The alpha value.
 * HSLColor class defines the import and exports methods for colors in RGB(ARGB) color scheme.
 */

public class HSLColor extends Color {

    public float h = 0f;
    public float s = 0f;
    public float l = 0f;
    public float a = 1f;

    /**
     * Constructor. Creates new color in HSLA scheme.
     * @param h The hue value.
     * @param s The saturation value.
     * @param l The lightness value.
     * @param a The alpha value.
     */
    public HSLColor(float h, float s, float l, float a) {
        this.h = h;
        this.s = s;
        this.l = l;
        this.a = a;
    }

    /**
     * Construct with default empty values. Creates new color in HSLA scheme.
     */
    public HSLColor () {
        this.h = 0f;
        this.s = 0f;
        this.l = 0f;
        this.a = 1f;
    }

    /**
     * Converts a color in ARGB scheme to a equivalent color in HSLA scheme.
     * @param r The red color part.
     * @param g The green color part.
     * @param b The blue color part.
     * @param a The alpha value.
     * @return color in HSLA scheme
     */
    private static HSLColor fromRGB(float r, float g, float b, float a){

        //	Minimum and Maximum RGB values are used in the HSL calculations

        float min = Math.min(r, Math.min(g, b));
        float max = Math.max(r, Math.max(g, b));

        //  Calculate the Hue

        float h = 0;

        if (max == min)
            h = 0;
        else if (max == r)
            h = ((60 * (g - b) / (max - min)) + 360) % 360;
        else if (max == g)
            h = (60 * (b - r) / (max - min)) + 120;
        else if (max == b)
            h = (60 * (r - g) / (max - min)) + 240;

        //  Calculate the Luminance

        float l = (max + min) / 2;

        //  Calculate the Saturation

        float s = 0;

        if (max == min)
            s = 0;
        else if (l <= .5f)
            s = (max - min) / (max + min);
        else
            s = (max - min) / (2 - max - min);

        return new HSLColor(h, s * 100, l * 100, a);
    }

    /**
     * Converts a color in RGB scheme to a equivalent color in HSLA scheme.
     * @param color hexadecimal integer value of RGB color
     * @return color in HSLA scheme
     */
    public static HSLColor fromRGB(int color)
    {
        //  Get RGB values in the range 0 - 1

        float r = ((color >> 16) & 0xFF) / 255.f;
        float g = ((color >> 8) & 0xFF) / 255.f;
        float b = (color & 0xFF) / 255.f;

        return fromRGB(r, g, b, 1.0f);
    }

    /**
     * Converts a color in ARGB scheme to a equivalent color in HSLA scheme.
     * @param color The hexadecimal integer value of ARGB color.
     * @return The color in HSLA scheme.
     */
    public static HSLColor fromARGB(int color)
    {
        //  Get RGB values in the range 0 - 1

        float r = ((color >> 16) & 0xFF) / 255.f;
        float g = ((color >> 8) & 0xFF) / 255.f;
        float b = (color & 0xFF) / 255.f;
        float a = ((color >> 24) & 0xFF) / 255.f;

        return fromRGB(r, g, b, a);
    }

    /**
     * Converts giving color in HSLA scheme to an equivalent color in RGB scheme.
     * @param hsla The HSLA color value.
     * @return The RGB color value.
     */
    public static int toRGB(HSLColor hsla) {
        return toRGB(hsla.h, hsla.s, hsla.l, hsla.a);
    }

    /**
     * Converts giving color in HSLA scheme to an equivalent color in RGB scheme.
     * @param h The hue value.
     * @param s The saturation value.
     * @param l The lightness value.
     * @param alpha The alpha value.
     * @return The RGB color value.
     */
    public static int toRGB(float h, float s, float l, float alpha)
    {
        if (s <0.0f || s > 100.0f)
        {
            String message = "Color parameter outside of expected range - Saturation";
            throw new IllegalArgumentException( message );
        }

        if (l <0.0f || l > 100.0f)
        {
            String message = "Color parameter outside of expected range - Luminance";
            throw new IllegalArgumentException( message );
        }

        if (alpha <0.0f || alpha > 1.0f)
        {
            String message = "Color parameter outside of expected range - Alpha";
            throw new IllegalArgumentException( message );
        }

        //  Formula needs all values between 0 - 1.

        h = h % 360.0f;
        h /= 360f;
        s /= 100f;
        l /= 100f;

        float q = 0;

        if (l < 0.5)
            q = l * (1 + s);
        else
            q = (l + s) - (s * l);

        float p = 2 * l - q;

        float r = Math.max(0, HueToRGB(p, q, h + (1.0f / 3.0f)));
        float g = Math.max(0, HueToRGB(p, q, h));
        float b = Math.max(0, HueToRGB(p, q, h - (1.0f / 3.0f)));

        r = Math.min(r, 1.0f);
        g = Math.min(g, 1.0f);
        b = Math.min(b, 1.0f);

        return 0xFF000000 | (((int) (r * 255.0f)) << 16) |
            (((int) (g * 255.0f)) << 8) | ((int) (b * 255.0f));
    }

    /**
     * Converts giving color in HSLA scheme to an equivalent color in ARGB scheme.
     * @param hsla The HSLA color value.
     * @return The ARBG color as hexadecimal integer value.
     */
    public static int toARGB(HSLColor hsla) {
        return toARGB(hsla.h, hsla.s, hsla.l, hsla.a);
    }

    /**
     * Converts giving color in HSLA scheme to an equivalent color in ARGB scheme.
     * @param h The hue value.
     * @param s The saturation value.
     * @param l The lightness value.
     * @param alpha The alpha value.
     * @return The ARBG color as hexadecimal integer value.
     */
    public static int toARGB(float h, float s, float l, float alpha)
    {
        if (s <0.0f || s > 100.0f)
        {
            String message = "Color parameter outside of expected range - saturation.";
            throw new IllegalArgumentException( message );
        }

        if (l <0.0f || l > 100.0f)
        {
            String message = "Color parameter outside of expected range - Luminance";
            throw new IllegalArgumentException( message );
        }

        if (alpha <0.0f || alpha > 1.0f)
        {
            String message = "Color parameter outside of expected range - Alpha";
            throw new IllegalArgumentException( message );
        }

        //  Formula needs all values between 0 - 1.

        h = h % 360.0f;
        h /= 360f;
        s /= 100f;
        l /= 100f;

        float q = 0;

        if (l < 0.5)
            q = l * (1 + s);
        else
            q = (l + s) - (s * l);

        float p = 2 * l - q;

        float r = Math.max(0, HueToRGB(p, q, h + (1.0f / 3.0f)));
        float g = Math.max(0, HueToRGB(p, q, h));
        float b = Math.max(0, HueToRGB(p, q, h - (1.0f / 3.0f)));

        r = Math.min(r, 1.0f);
        g = Math.min(g, 1.0f);
        b = Math.min(b, 1.0f);

        return (((int) (alpha * 255.0f)) << 24) | (((int) (r * 255.0f)) << 16) |
                (((int) (g * 255.0f)) << 8) | ((int) (b * 255.0f));
    }


    private static float HueToRGB(float p, float q, float h)
    {
        if (h < 0) h += 1;

        if (h > 1 ) h -= 1;

        if (6 * h < 1)
        {
            return p + ((q - p) * 6 * h);
        }

        if (2 * h < 1 )
        {
            return  q;
        }

        if (3 * h < 2)
        {
            return p + ( (q - p) * 6 * ((2.0f / 3.0f) - h) );
        }

        return p;
    }
}
