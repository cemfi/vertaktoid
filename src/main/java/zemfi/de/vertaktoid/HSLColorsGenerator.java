package zemfi.de.vertaktoid;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by eugen on 13.01.17.
 */

public class HSLColorsGenerator {
    private static float _golden_ratio_conjugate = 0.618033988749895f;
    private static float h = -_golden_ratio_conjugate;
    private static Random rnd = new Random();

    public static HSLColor generateColor(float s, float l, float a)
    {
        h += _golden_ratio_conjugate;
        h %= 1;
        return new HSLColor(h * 360, s, l, a);
    }

    public static ArrayList<HSLColor> generateColorSet(int n, float s, float l, float a)
    {
        ArrayList<HSLColor> colors = new ArrayList<>();

        for (int i = 0; i < n; i++)
            colors.add(generateColor(s, l, a));

        return colors;
    }

    public static void resetHueToRandom()
    {
        h = rnd.nextFloat();
    }

    public static void resetHueToDefault() { h = -_golden_ratio_conjugate;}

}
