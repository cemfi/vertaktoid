package zemfi.de.vertaktoid;

import java.util.ArrayList;
import java.util.Random;

/**
 * HSLColorsGenerator is a helper class for generating of color sets.
 * The defined algorithm, that uses the HSLA color scheme and golden ration constant,
 * guaranties good recognition of neighbor colors.
 */

public class HSLColorsGenerator {
    private static float _golden_ratio_conjugate = 0.618033988749895f;
    private static float h = -_golden_ratio_conjugate;
    private static Random rnd = new Random();

    /**
     * Generates one HSLA color, that can be good recognized from last color.
     * @param s The saturation value.
     * @param l The lightness value.
     * @param a The alpha value.
     * @return New HSLA color.
     */
    public static HSLColor generateColor(float s, float l, float a)
    {
        h += _golden_ratio_conjugate;
        h %= 1;
        return new HSLColor(h * 360, s, l, a);
    }

    /**
     * Generates HSLA color set. Each color can be good recognized from neighbor colors.
     * @param n The number of colors to generate.
     * @param s The saturation value.
     * @param l The lightness value.
     * @param a The alpha value.
     * @return
     */
    public static ArrayList<HSLColor> generateColorSet(int n, float s, float l, float a)
    {
        ArrayList<HSLColor> colors = new ArrayList<>();

        for (int i = 0; i < n; i++)
            colors.add(generateColor(s, l, a));

        return colors;
    }

    /**
     * Reset the used hue to a random.
     */
    public static void resetHueToRandom()
    {
        h = rnd.nextFloat();
    }

    /**
     * Reset the hue to a value equals negative golden ratio.
     * The next generates color start than from hue equals 0.
     */
    public static void resetHueToDefault() {
        h = -_golden_ratio_conjugate;}
}
