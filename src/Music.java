/*

Music of the Spheres.

Description:

Labeled with color/sound combinations, bouncing spheres and strings
produce compositions of sound, color and motion.

*/

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.applet.Applet;
import java.applet.AudioClip;

// SphereSprite defines a sphere.
class SphereSprite
{

    // Data.
    int radius;         // Radius.
    int x, y;           // Position.
    double dx, dy;      // Velocity.
    double ndx, ndy;
    int paletteIndex;   // Musical sound and color.

    // Constructor.
    public SphereSprite()
    {
        this.radius = -1;
        this.x = 0;
        this.y = 0;
        this.dx = 0.0;
        this.dy = 0.0;
        this.ndx = 0.0;
        this.ndy = 0.0;
        this.paletteIndex = -1;
    }
}

// StringSprite defines a string.
class StringSprite
{

    // Data.
    int x1, y1, x2, y2;     // Position.
    int paletteIndex;       // Musical sound and color.

    // Constructor.
    public StringSprite()
    {
        this.x1 = this.y1 = 0;
        this.x2 = this.y2 = 0;
        this.paletteIndex = -1;
    }
}

// Music class.
public class Music
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

    // Music name.
    String name = "";

    // Sound/color palette.
    Palette palette = new Palette();

    // Spacial dimensions.
    Dimension size;

    // Spheres and strings.
    SphereSprite[] spheres = new SphereSprite[MAX_SPHERES+1];
    StringSprite[] strings = new StringSprite[MAX_STRINGS+1];
    SphereSprite[] spheresRewindTo = new SphereSprite[MAX_SPHERES+1];
    boolean[] playSounds = new boolean[MAX_SOUND_COLOR];

    // State.
    int beat = 0;
    int finisBeat = 0;
    boolean mute = false;
    boolean fileError = false;

    // Constructor.
    public Music(Dimension d)
    {
        size = new Dimension(d);
        clear();
    }

    // Music movement.
    public void movement()
    {
        int i, j, x, y, r;
        double dx, dy, xd, yd, d1, d2;
        boolean b;

        // End of composition?
        if (finisBeat != -1 && beat >= finisBeat) return;
        beat++;

        // Clear sound play flags.
        for (i = 0; i < MAX_SOUND_COLOR; i++)
        {
            playSounds[i] = false;
        }

        // Check for collisions.
        for (i = 0; spheres[i] != null; i++)
        {
            spheres[i].ndx = spheres[i].dx;
            spheres[i].ndy = spheres[i].dy;
        }
        for (i = 0; spheres[i] != null; i++)
        {
            x = spheres[i].x;
            y = spheres[i].y;
            dx = spheres[i].dx;
            dy = spheres[i].dy;
            r = spheres[i].radius;

            // Bounce spheres off of walls.
            b = false;
            if ((x >= (size.width - r) && dx > 0.0) ||
                (x <= r && dx < 0.0))
            {
                spheres[i].ndx = -dx;
                b = true;
            }
            if ((y >= (size.height - r) && dy > 0.0) ||
                (y <= r && dy < 0.0))
            {
                spheres[i].ndy = -dy;
                b = true;
            }
            if (b && spheres[i].paletteIndex != -1)
            {
                playSounds[spheres[i].paletteIndex] = true;
            }

            // Bounce spheres on strings.
            for (j = 0; strings[j] != null; j++)
            {
                b = false;
                if (strings[j].x1 == strings[j].x2)
                {
                    if (bouncesOnString(i, strings[j].x1, strings[j].y1,
                        strings[j].x2, strings[j].y2))
                    {
                        spheres[i].ndx = -dx;
                        b = true;
                    }
                    if (dy > 0.0 && bouncesOnString(i, strings[j].x1 - 2,
                               strings[j].y1, strings[j].x1 + 2, strings[j].y1))
                    {
                        spheres[i].ndy = -dy;
                        b = true;
                    }
                    if (dy < 0.0 && bouncesOnString(i, strings[j].x2 - 2,
                               strings[j].y2, strings[j].x2 + 2, strings[j].y2))
                    {
                        spheres[i].ndy = -dy;
                        b = true;
                    }
                } else { // horizontal
                    if (bouncesOnString(i, strings[j].x1, strings[j].y1,
                        strings[j].x2, strings[j].y2))
                    {
                        spheres[i].ndy = -dy;
                        b = true;
                    }
                    if (dx > 0.0 && bouncesOnString(i, strings[j].x1,
                               strings[j].y1 - 2, strings[j].x1, strings[j].y1 + 2))
                    {
                        spheres[i].ndx = -dx;
                        b = true;
                    }
                    if (dx < 0.0 && bouncesOnString(i, strings[j].x2,
                               strings[j].y2 - 2, strings[j].x2, strings[j].y2 + 2))
                    {
                        spheres[i].ndx = -dx;
                        b = true;
                    }
                }
                if (b)
                {
                    if (spheres[i].paletteIndex != -1)
                    {
                        playSounds[spheres[i].paletteIndex] = true;
                    }
                    if (strings[j].paletteIndex != -1)
                    {
                        playSounds[strings[j].paletteIndex] = true;
                    }
                }
            }

            // Bounce colliding spheres which are moving toward each other.
            for (j = i+1; spheres[j] != null; j++)
            {
                d1 = (double)(r + spheres[j].radius);
                xd = (double)(x - spheres[j].x);
                if (Math.abs(xd) > d1) continue;
                yd = (double)(y - spheres[j].y);
                if (Math.abs(yd) > d1) continue;
                d2 = Math.sqrt((xd * xd) + (yd * yd));
                if (d2 <= d1)
                {
                    xd = (x + dx) - (spheres[j].x + spheres[j].dx);
                    yd = (y + dy) - (spheres[j].y + spheres[j].dy);
                    d1 = (int)Math.sqrt((double)((xd * xd) + (yd * yd)));
                    if (d1 < d2)
                    {
                        spheres[i].ndx = spheres[j].dx;
                        spheres[i].ndy = spheres[j].dy;
                        spheres[j].ndx = dx;
                        spheres[j].ndy = dy;
                        if (spheres[i].paletteIndex != -1)
                        {
                            playSounds[spheres[i].paletteIndex] = true;
                        }
                        if (spheres[j].paletteIndex != -1)
                        {
                            playSounds[spheres[j].paletteIndex] = true;
                        }
                    }
                }
            }
        }

        // Move spheres.
        for (i = 0; spheres[i] != null; i++)
        {
            spheres[i].dx = spheres[i].ndx;
            spheres[i].dy = spheres[i].ndy;
            spheres[i].x += spheres[i].dx;
            spheres[i].y += spheres[i].dy;
        }

        // Play bounce sounds.
        if (!mute)
        {
            for (i = 0; i < MAX_SOUND_COLOR && palette.soundClips[i] != null; i++)
            {
                if (playSounds[i])
                {
                    palette.soundClips[i].play();
                }
            }
        }
    }

    // Sphere bounces on string?
    private boolean bouncesOnString(int i, int x1, int y1, int x2, int y2)
    {
        int x, y, r;
        double dx, dy;

        x = spheres[i].x;
        dx = spheres[i].dx;
        y = spheres[i].y;
        dy = spheres[i].dy;
        r = spheres[i].radius;

        if (x1 == x2)    // vertical?
        {
            if ((x <= x1 && x >= (x1 - r) && dx >= 0.0) ||
                (x >= x1 && x <= (x1 + r) && dx <= 0.0))
            {
                if (y >= (y1 - r) && y <= (y2 + r))
                {
                    if (y >= y1 && y <= y2)
                    {
                        return(true);
                    } else {
                        if (y < y1)
                        {
                            if (pointDist(x, y, x1, y1) <= (double)r)
                            {
                                return(true);
                            }
                        } else {
                            if (pointDist(x, y, x2, y2) <= (double)r)
                            {
                                return(true);
                            }
                        }
                    }
                }
            }
        } else {    // horizontal
            if ((y <= y1 && y >= (y1 - r) && dy >= 0.0) ||
                (y >= y1 && y <= (y1 + r) && dy <= 0.0))
            {
                if (x >= (x1 - r) && x <= (x2 + r))
                {
                    if (x >= x1 && x <= x2)
                    {
                        return(true);
                    } else {
                        if (x < x1)
                        {
                            if (pointDist(x, y, x1, y1) <= (double)r)
                            {
                                return(true);
                            }
                        } else {
                            if (pointDist(x, y, x2, y2) <= (double)r)
                            {
                                return(true);
                            }
                        }
                    }
                }
            }
        }
        return(false);
    }

    // Point-to-point distance.
    double pointDist(int x1, int y1, int x2, int y2)
    {
        return(pointDist((double)x1, (double)y1, (double)x2, (double)y2));
    }
    double pointDist(double x1, double y1, double x2, double y2)
    {
        double xd, yd;

        xd = x2 - x1;
        yd = y2 - y1;
        return(Math.sqrt((xd * xd) + (yd * yd)));
    }

    // Clear music.
    public void clear()
    {
        int i;

        // Clear music name.
        name = "";

        // Clear spheres and strings.
        for (i = 0; i < MAX_SPHERES; i++)
        {
            spheres[i] = null;
            spheresRewindTo[i] = null;
        }
        for (i = 0; i < MAX_STRINGS; i++)
        {
            strings[i] = null;
        }
        for (i = 0; i < MAX_SOUND_COLOR; i++)
        {
            playSounds[i] = false;
        }
        beat = 0;
        finisBeat = -1;
    }

    // Rewind music.
    public void rewind()
    {
        int i;

        for (i = 0; spheres[i] != null; i++)
        {
            spheres[i].x = spheresRewindTo[i].x;
            spheres[i].y = spheresRewindTo[i].y;
            spheres[i].dx = spheresRewindTo[i].dx;
            spheres[i].dy = spheresRewindTo[i].dy;
        }
        beat = 0;
    }

    // Load music.
    public String load(String musicString, URL baseURL)
    {
        int i, j, p, q;
        String s;
        BufferedReader in;
        URL u;
        StreamTokenizer t;
        SphereSprite sphere;
        StringSprite string;
        String statusMessage = "";

        // Clear music.
        clear();

        // Check music name.
        fileError = false;
        statusMessage = getName(musicString);
        if (name.equals("")) return(statusMessage);

        // Load the music (and its palette).
        try
        {
            try { u = new URL(name); }
            catch(MalformedURLException e) {
                u = new URL(baseURL, name);
            }
            in = new BufferedReader(new InputStreamReader(u.openStream()));

            // Load the dimensions.
            if ((s = in.readLine()) == null)
            {
                throw(new IOException());
            }
            try
            {
                size.width = Integer.parseInt(s, 10);
            } catch(NumberFormatException e) {
                statusMessage = "Invalid width value " + s + " in music " + name;
                fileError = true;
                throw(new IOException());
            }
            if ((s = in.readLine()) == null)
            {
                throw(new IOException());
            }
            try
            {
                size.height = Integer.parseInt(s, 10);
            } catch(NumberFormatException e) {
                statusMessage = "Invalid height value " + s + " in music " + name;
                fileError = true;
                throw(new IOException());
            }

            // Load the palette for this music.
            if ((s = in.readLine()) == null)
            {
                throw(new IOException());
            }
            if (!s.equals(palette.name))
            {
                statusMessage = palette.load(s, baseURL);
                if (palette.fileError)
                {
                    fileError = true;
                    throw(new IOException());
                }
            }

            // Load finis beat.
            if ((s = in.readLine()) == null)
            {
                throw(new IOException());
            }
            try
            {
                finisBeat = Integer.parseInt(s, 10);
            } catch(NumberFormatException e) {
                statusMessage = "Invalid finisBeat value " + s + " in music " + name;
                fileError = true;
                throw(new IOException());
            }

            // Load the spheres and strings.
            t = new StreamTokenizer(in);
            t.commentChar('#');
            t.eolIsSignificant(true);
            i = j = p = q = 0;
            sphere = null;
            string = null;
            while (t.nextToken() != StreamTokenizer.TT_EOF)
            {
                switch(t.ttype)
                {
                case StreamTokenizer.TT_EOL:
                    if (i != 0)
                    {
                        statusMessage = "Corrupt music " + name;
                        fileError = true;
                        throw(new IOException());
                    }
                    i = 0;
                    break;

                case StreamTokenizer.TT_NUMBER:
                    if (sphere != null)
                    {
                        switch(i)
                        {
                        case 1:
                            if ((sphere.radius = (int)t.nval) < MIN_SPHERE_RADIUS ||
                                 sphere.radius > MAX_SPHERE_RADIUS)
                            {
                                statusMessage = "Invalid sphere radius " +
                                                    sphere.radius + " in music " + name;
                                fileError = true;
                                throw(new IOException());
                            }
                            i++;
                            break;
                        case 2:
                            if ((sphere.x = (int)t.nval) < 0 || sphere.x >= size.width)
                            {
                                statusMessage = "Invalid sphere x " +
                                                    sphere.x + " in music " + name;
                                fileError = true;
                                throw(new IOException());
                            }
                            i++;
                            break;
                        case 3:
                            if ((sphere.y = (int)t.nval) < 0 || sphere.y >= size.height)
                            {
                                statusMessage = "Invalid sphere y " +
                                                    sphere.y + " in music " + name;
                                fileError = true;
                                throw(new IOException());
                            }
                            i++;
                            break;
                        case 4:
                            if ((sphere.dx = t.nval) < -MAX_SPHERE_SPEED ||
                                 sphere.dx > MAX_SPHERE_SPEED)
                            {
                                statusMessage = "Invalid sphere dx " +
                                                    sphere.dx + " in music " + name;
                                fileError = true;
                                throw(new IOException());
                            }
                            i++;
                            break;
                        case 5:
                            if ((sphere.dy = t.nval) < -MAX_SPHERE_SPEED ||
                                 sphere.dy > MAX_SPHERE_SPEED)
                            {
                                statusMessage = "Invalid sphere dy " +
                                                    sphere.dy + " in music " + name;
                                fileError = true;
                                throw(new IOException());
                            }
                            if (pointDist(0.0, 0.0, sphere.dx, sphere.dy) > MAX_SPHERE_SPEED)
                            {
                                statusMessage = "Invalid sphere speed (" +
                                                    sphere.dx + "," + sphere.dy +
                                                    ") in music " + name;
                                fileError = true;
                                throw(new IOException());
                            }
                            i++;
                            break;
                        case 6:
                            if ((sphere.paletteIndex = (int)t.nval) < -1 ||
                                 sphere.paletteIndex >= MAX_SOUND_COLOR)
                            {
                                statusMessage = "Invalid sphere paletteIndex " +
                                                    sphere.paletteIndex +
                                                    " in music " + name;
                                fileError = true;
                                throw(new IOException());
                            }
                            spheres[p] = sphere;
                            spheresRewindTo[p] = new SphereSprite();
                            spheresRewindTo[p].radius = spheres[p].radius;
                            spheresRewindTo[p].x = spheres[p].x;
                            spheresRewindTo[p].y = spheres[p].y;
                            spheresRewindTo[p].dx = spheres[p].dx;
                            spheresRewindTo[p].dy = spheres[p].dy;
                            spheresRewindTo[p].paletteIndex = spheres[p].paletteIndex;
                            p++;
                            sphere = null;
                            i = 0;
                            break;
                        default:
                            statusMessage = "Corrupt music " + name;
                            fileError = true;
                            throw(new IOException());
                        }

                    } else {    // string

                        switch(i)
                        {
                        case 1:
                            if ((string.x1 = (int)t.nval) >= size.width)
                            {
                                statusMessage = "Invalid string x1 " +
                                                    string.x1 + " in music " + name;
                                fileError = true;
                                throw(new IOException());
                            }
                            i++;
                            break;
                        case 2:
                            if ((string.y1 = (int)t.nval) >= size.height)
                            {
                                statusMessage = "Invalid string y1 " +
                                                    string.y1 + " in music " + name;
                                fileError = true;
                                throw(new IOException());
                            }
                            i++;
                            break;
                        case 3:
                            if ((string.x2 = (int)t.nval) < 0 || string.x2 < string.x1)
                            {
                                statusMessage = "Invalid string x2 " +
                                                    string.x2 + " in music " + name;
                                fileError = true;
                                throw(new IOException());
                            }
                            i++;
                            break;
                        case 4:
                            if ((string.y2 = (int)t.nval) < 0 || string.y2 < string.y1)
                            {
                                statusMessage = "Invalid string y2 " +
                                                    string.y2 + " in music " + name;
                                fileError = true;
                                throw(new IOException());
                            }
                            i++;
                            break;
                        case 5:
                            if (string.x1 == string.x2)
                            {
                                j = string.y2 - string.y1;
                                if (j < MIN_STRING_SIZE || j > MAX_STRING_SIZE)
                                {
                                   statusMessage = "Invalid string size " +
                                                       j + " in music " + name;
                                   fileError = true;
                                   throw(new IOException());
                                }
                                j = (string.y2 + string.y1) / 2;
                                if (j < 0 || j > size.height)
                                {
                                   statusMessage = "Invalid string vertical location " +
                                                       j + " in music " + name;
                                   fileError = true;
                                   throw(new IOException());
                                }
                            } else {
                                j = string.x2 - string.x1;
                                if (j < MIN_STRING_SIZE || j > MAX_STRING_SIZE)
                                {
                                   statusMessage = "Invalid string size " +
                                                       j + " in music " + name;
                                   fileError = true;
                                   throw(new IOException());
                                }
                                j = (string.x2 + string.x1) / 2;
                                if (j < 0 || j > size.width)
                                {
                                   statusMessage = "Invalid string horizontal location " +
                                                       j + " in music " + name;
                                   fileError = true;
                                   throw(new IOException());
                                }

                            }
                            if ((string.paletteIndex = (int)t.nval) < -1 ||
                                 string.paletteIndex >= MAX_SOUND_COLOR)
                            {
                                statusMessage = "Invalid string paletteIndex " +
                                                    string.paletteIndex +
                                                    " in music " + name;
                                fileError = true;
                                throw(new IOException());
                            }
                            strings[q] = string;
                            q++;
                            string = null;
                            i = 0;
                            break;
                        default:
                            statusMessage = "Corrupt music " + name;
                            fileError = true;
                            throw(new IOException());
                        }
                    }
                    break;

                case StreamTokenizer.TT_WORD:
                    if (i != 0)
                    {
                        statusMessage = "Corrupt music " + name;
                        fileError = true;
                        throw(new IOException());
                    }
                    i++;
                    s = new String(t.sval);
                    if (s.equals("sphere"))
                    {
                        if (p >= MAX_SPHERES)
                        {
                            statusMessage = "Too many spheres defined in " + name +
                                " - maximum = " + MAX_SPHERES;
                            fileError = true;
                            throw(new IOException());
                        }
                        sphere = new SphereSprite();
                    } else if (s.equals("string"))
                    {
                        if (q >= MAX_STRINGS)
                        {
                            statusMessage = "Too many strings defined in " + name +
                                " - maximum = " + MAX_STRINGS;
                            fileError = true;
                            throw(new IOException());
                        }
                        string = new StringSprite();
                    } else {
                        statusMessage = "Corrupt music " + name;
                        fileError = true;
                        throw(new IOException());
                    }
                    break;

                default:
                    statusMessage = "Corrupt music " + name;
                    fileError = true;
                    throw(new IOException());
                }
            }
            in.close();

        } catch(MalformedURLException e) {
            if (!fileError)
            {
                statusMessage = "Error loading music " + name + " - bad URL";
                fileError = true;
            }
        } catch(IOException e) {
            if (!fileError)
            {
                statusMessage = "Error loading music " + name;
                fileError = true;
            }
        }

        // If error, clear loaded music.
        if (fileError)
        {
	    clear();
        } else {
            statusMessage = "Music " + name + " loaded";
        }

        // Return load statusMessage.
        return(statusMessage);
    }

    // Save music to file.
    public String save(String musicString)
    {
        int i,j;
        PrintWriter out;
        String statusMessage = "";

        // Check music name.
        fileError = false;
        statusMessage = getName(musicString);
        if (name.equals("")) return(statusMessage);
        if (name.substring(0,7).equals("http://"))
        {
            fileError = true;
            statusMessage = "Invalid file name: " + name;
            name = "";
            return(statusMessage);
        }

        // Save the music.
        try
        {
            out = new PrintWriter(new BufferedWriter(new FileWriter(name)));
            out.println("" + size.width);
            out.println("" + size.height);
            out.println(palette.name);
            out.println("" + finisBeat);
            for (i = 0; spheresRewindTo[i] != null; i++)
            {
                out.println("sphere " + spheresRewindTo[i].radius + " " +
                            spheresRewindTo[i].x + " " + spheresRewindTo[i].y + " " +
                            spheresRewindTo[i].dx + " " + spheresRewindTo[i].dy + " " +
                            spheresRewindTo[i].paletteIndex);
            }
            for (i = 0; strings[i] != null; i++)
            {
                out.println("string " +
                            strings[i].x1 + " " + strings[i].y1 + " " +
                            strings[i].x2 + " " + strings[i].y2 + " " +
                            strings[i].paletteIndex);
            }
            out.close();

        } catch(IOException e) {
            fileError = true;
            statusMessage = "Error saving music file " + name;
            return(statusMessage);
        }

        statusMessage = "Music " + name + " saved";
        return(statusMessage);
    }

    // Get music name.
    public String getName(String nameString)
    {
        int i, j;
        StringBuffer b;
        char c;

        name = "";
        nameString = nameString.trim();
        b = new StringBuffer(nameString);
        if ((j = b.length()) == 0) return("");
        if (j > MAX_NAME_LENGTH)
        {
            fileError = true;
            return("Name too long - maximum " + MAX_NAME_LENGTH + " characters");
        }
        for (i = 0, c = b.charAt(i); i < j; c = b.charAt(i), i++)
        {
            if (Character.isWhitespace(c) || Character.isISOControl(c))
            {
                fileError = true;
                return("Invalid name");
            }
        }
        name = new String(nameString);
        return("");
    }
}
