/*

Music of the Spheres palette.

A palette contains a set of associations between sounds and colors which may
be applied to spheres and/or strings in a composition.  It is specified as
the name or URL of a directory containing a palette.dat and sound files.

palette.dat format:

# Sound and color palette data file.
# Entry format:
# <Sound file> <R-G-B color (0-255)> <Applies to strings? (y|n)> <Applies to spheres? (y|n)>
Ered.au         255-0-0         y       y
Aorange.au      255-200-0       y       y
Dyellow.au      255-255-0       y       y
Ggreen.au       0-255-0         y       y
Bblue.au        0-0-255         y       y
Emagenta.au     255-0-255       y       y

*/

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.applet.Applet;
import java.applet.AudioClip;

// Palette class.
public class Palette
{
    // Parameters.
    static final int MAX_SPHERES = 50;     // Spheres.
    static final int MIN_SPHERE_RADIUS  = 20;
    static final int MAX_SPHERE_RADIUS  = 40;
    static final int MAX_SPHERE_SPEED = 12;
    static final int MAX_STRINGS = 50;     // Strings.
    static final int MIN_STRING_SIZE  = 20;
    static final int MAX_STRING_SIZE  = 100;
    static final int MAX_SOUND_COLOR = 40;    // Sounds/colors.
    static final int MAX_NAME_LENGTH = 100;

    // Palette name.
    String name = "";

    // Sounds and colors.
    AudioClip[] soundClips = new AudioClip[MAX_SOUND_COLOR];
    URL[] soundURLs = new URL[MAX_SOUND_COLOR+1];
    Color[] soundColors = new Color[MAX_SOUND_COLOR];

    // Palette indexes.
    int[] stringPaletteIndexes = new int[MAX_SOUND_COLOR];
    int[] spherePaletteIndexes = new int[MAX_SOUND_COLOR];

    // Palette file error.
    boolean fileError = false;

    // Constructor.
    public Palette()
    {
        clear();
    }

    // Clear palette.
    public void clear()
    {
        int i;

        name = "";
        for (i = 0; i < MAX_SOUND_COLOR; i++)
        {
            soundClips[i] = null;
            soundURLs[i] = null;
            soundColors[i] = null;
            stringPaletteIndexes[i] = -1;
            spherePaletteIndexes[i] = -1;
        }
        soundURLs[i] = null;
    }

    // Load palette.
    // Audio clips must be loaded externally from URLs.
    public String load(String paletteString, URL baseURL)
    {
        int i, j, p, q, r, g, b;
        StringBuffer sb;
        char c;
        String s;
        URL u;
        BufferedReader f;
        StreamTokenizer t;
        String statusMessage = "";

        // Clear palette.
        clear();

        // Check name.
        fileError = false;
        paletteString = paletteString.trim();
        sb = new StringBuffer(paletteString);
        if ((j = sb.length()) == 0) return("");
        for (i = 0, c = sb.charAt(i); i < j; c = sb.charAt(i), i++)
        {
            if (Character.isWhitespace(c) || Character.isISOControl(c))
            {
                fileError = true;
                statusMessage = "Invalid palette name";
                return(statusMessage);
            }
        }
        name = new String(paletteString);

        // Read and parse palette.
        try
        {
            try { u = new URL(name + "/palette.dat"); }
            catch(MalformedURLException e) {
                u = new URL(baseURL, name + "/palette.dat");
            }
            f = new BufferedReader(new InputStreamReader(u.openStream()));
            t = new StreamTokenizer(f);
            t.commentChar('#');
            t.eolIsSignificant(true);
            i = j = p = q = r = g = b = 0;
            while (t.nextToken() != StreamTokenizer.TT_EOF)
            {
                switch(t.ttype)
                {
                case StreamTokenizer.TT_EOL:
                    if (j != 0 && j != 6)
                    {
                        statusMessage = "Corrupt palette " + name;
                        fileError = true;
                        throw(new IOException());
                    }
                    if (j == 6)
                    {
                        i++;
                    }
                    j = 0;
                    break;

                case StreamTokenizer.TT_NUMBER:
                    switch(j)
                    {
                    case 1:
                        if ((r = (int)Math.abs(t.nval)) > 255)
                        {
                            statusMessage = "Invalid Red RGB value (" + r + ") in palette " + name;
                            fileError = true;
                            throw(new IOException());
                        }
                        break;
                    case 2:
                        if ((g = (int)Math.abs(t.nval)) > 255)
                        {
                            statusMessage = "Invalid Green RGB value (" + g + ") in palette " + name;
                            fileError = true;
                            throw(new IOException());
                        }
                        break;
                    case 3:
                        if ((b = (int)Math.abs(t.nval)) > 255)
                        {
                            statusMessage = "Invalid Blue RGB value (" + b + ") in palette " + name;
                            fileError = true;
                            throw(new IOException());
                        }
                        if (r == 0 && g == 0 && b == 0)
                        {
                            statusMessage = "Black is reserved RGB value";
                            fileError = true;
                            throw(new IOException());
                        }
                        soundColors[i] = new Color(r, g, b);
                        break;
                    default:
                        statusMessage = "Corrupt palette " + name;
                        fileError = true;
                        throw(new IOException());
                    }
                    j++;
                    break;

                case StreamTokenizer.TT_WORD:
                    s = new String(t.sval);
                    switch(j)
                    {
                    case 0:
                        if (i >= (MAX_SOUND_COLOR - 1))
                        {
                            statusMessage = "Too many entries in palette " + name + " - maximum = " +
                                        (MAX_SOUND_COLOR - 1);
                            fileError = true;
                            throw(new IOException());
                        }
                        try
                        {
                            try { u = new URL(name + "/" + s); }
                            catch(MalformedURLException e) {
                                u = new URL(baseURL, name + "/" + s);
                            }
                            soundURLs[i] = u;
                            j++;
                        } catch(MalformedURLException e) {
                            statusMessage = "Cannot get audio clip " + s;
                            fileError = true;
                            throw(new IOException());
                        }
                        break;
                    case 4:
                        if (s.equals("y") || s.equals("Y"))
                        {
                            stringPaletteIndexes[p] = i;
                            p++;
                        }
                        j++;
                        break;
                    case 5:
                        if (s.equals("y") || s.equals("Y"))
                        {
                            spherePaletteIndexes[q] = i;
                            q++;
                        }
                        j++;
                        break;
                    default:
                        statusMessage = "Corrupt palette " + name;
                        fileError = true;
                        throw(new IOException());
                    }
                    break;

                default:
                    statusMessage = "Corrupt palette " + name;
                    fileError = true;
                    throw(new IOException());
                }
            }
            f.close();

            if (i == 0)
            {
                statusMessage = "Palette " + name + " is empty";
            }

        } catch(MalformedURLException e) {
            if (!fileError)
            {
                statusMessage = "Bad URL for palette " + name;
                fileError = true;
            }
        } catch(IOException e) {
            if (!fileError)
            {
                statusMessage = "Error loading palette " + name;
                fileError = true;
            }
        }

        // If error, clear loaded palette.
        if (fileError)
        {
            clear();
        } else {
            statusMessage = "Palette " + name + " loaded";
        }

        return(statusMessage);
    }
}
