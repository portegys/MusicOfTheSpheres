/*

Music of the Spheres player applet.

Description:

Compositions may be loaded from web addresses or selected from an
optionally provided list.

Usage:

<applet code="Player.class" width=w height=h>
[<param name=Music value="<file|URL of initial music>">]
[<param name=MusicList value="<file|URL of list of music to choose from>">]
</applet>

*/

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.applet.Applet;

// Music player applet.
public class Player extends Applet implements Runnable
{
    // Parameters.
    static final int UPDATE_FREQUENCY = 50; // Milliseconds between screen updates.
    static final int STOP_CHECK_FREQUENCY = 1000;

    // Load/update music values.
    static final int UPDATE = 0;
    static final int LOAD = 1;

    // Music.
    Music music;

    // Screen dimensions.
    Dimension screenSize;

    // Canvas (buffered).
    Canvas canvas;
    Dimension canvasSize;
    Graphics canvasGraphics;
    Image canvasImage;
    Graphics canvasImageGraphics;

    // State.
    boolean rewind;
    boolean frozen;
    String musicName;
    String musicList;
    boolean stopUpdate;
    URL baseURL;

    // Control panel.
    Panel controlPanel,filePanel,buttonPanel;
    Button rewindButton;
    Checkbox freezeCheck;
    Checkbox muteCheck;
    Label musicLabel;
    TextField musicText;
    Choice musicChoice;

    // Font.
    Font font = new Font("Helvetica", Font.BOLD, 12);
    FontMetrics fontMetrics;
    int fontWidth;
    int fontHeight;

    // Screen update thread.
    Thread updateThread;

    // Applet information.
    public String getAppletInfo()
    {
        return("Music of the Spheres player by Tom Portegys (portegys@lucent.com) - Version 1.0, February 2000");
    }

    // Initialize.
    public void init()
    {
        int i, l, r;

        // Find the size of the screen.
        screenSize = getSize();

        // Create canvas.
        setLayout(new BorderLayout());
        canvas = new Canvas();
        canvasSize = new Dimension(screenSize.width, (int)((double)screenSize.height * .80));
        canvas.setBounds(0, 0, canvasSize.width, canvasSize.height);
        add(canvas, BorderLayout.NORTH);
        canvasGraphics = canvas.getGraphics();
        canvasImage = createImage(canvasSize.width, canvasSize.height);
        canvasImageGraphics = canvasImage.getGraphics();

        // Create music.
        music = new Music(canvasSize);

        // Create panels.
        controlPanel = new Panel();
        filePanel = new Panel();
        buttonPanel = new Panel();
        musicLabel = new Label("Play:");
        filePanel.add(musicLabel);
        musicText = new TextField("", 20);
        musicText.addActionListener(new musicTextActionListener());
        filePanel.add(musicText);
        musicChoice = new Choice();
        musicChoice.addItemListener(new musicChoiceItemListener());
        musicChoice.add("Music:");
        filePanel.add(musicChoice);
        controlPanel.add(filePanel);
        rewindButton = new Button("Rewind");
        rewindButton.addActionListener(new rewindButtonActionListener());
        buttonPanel.add(rewindButton);
        freezeCheck = new Checkbox("Freeze");
        freezeCheck.addItemListener(new freezeCheckItemListener());
        buttonPanel.add(freezeCheck);
        muteCheck = new Checkbox("Mute");
        muteCheck.addItemListener(new muteCheckItemListener());
        buttonPanel.add(muteCheck);
        controlPanel.add(buttonPanel);
        add(controlPanel);

        // Set font data.
        Graphics g = getGraphics();
        g.setFont(font);
        fontMetrics = g.getFontMetrics();
        fontWidth = fontMetrics.getMaxAdvance();
        fontHeight = fontMetrics.getHeight();

        // Initialize state.
        rewind = false;
        frozen = false;
        musicName = "";
        musicList = "";
        stopUpdate = true;
        baseURL = getCodeBase();

        // List selectable music.
        if ((musicList = getParameter("MusicList")) != null)
        {
            listMusic();
        }

        // Load optional initial music.
        if ((musicName = getParameter("Music")) != null)
        {
            if (!musicName.equals("Music:"))
            {
                showStatus(music.load(musicName, baseURL));
                loadsounds();
                updateFiles();
            } else {
                showStatus("Invalid music name: " + musicName);
            }
        }
    }

    // List selectable music.
    void listMusic()
    {
        URL u;
        BufferedReader in;
        String s;

        // Clear list.
        musicChoice.removeAll();
        musicChoice.add("Music:");

        // Create music choice list.
        try
        {
            try { u = new URL(musicList); }
            catch(MalformedURLException e) {
                u = new URL(baseURL, musicList);
            }
            in = new BufferedReader(new InputStreamReader(u.openStream()));
            while ((s = in.readLine()) != null)
            {
               musicChoice.add(s);
            }
        } catch(MalformedURLException e) {
            showStatus("Cannot load music list " + musicList);
        } catch(IOException e) {
            showStatus("Cannot load music list " + musicList);
        }
    }

    // Start.
    public void start()
    {
        if (updateThread == null)
        {
            updateThread = new Thread(this);
            updateThread.start();
        }
        stopUpdate = false;
    }

    // Stop.
    public void stop()
    {
        stopUpdate = true;
    }

    // Run.
    public void run()
    {
        long t;

        // Lower this thread's priority.
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        // This is the music update loop.
        while (Thread.currentThread() == updateThread)
        {
            // Do not update while stopped.
            if (stopUpdate)
            {
                t = STOP_CHECK_FREQUENCY;

            } else {

                // Synchronized update.
                synchThreads(UPDATE);

                t = UPDATE_FREQUENCY;
            }

            // Sleep until the next loop.
            try
            {
                Thread.sleep(t);
            } catch(InterruptedException e) { break; }
        }
    }

    // Synchronize load and update threads to avoid inconsistency.
    public synchronized void synchThreads(int which)
    {
        if (which == UPDATE)
        {
            updateMusic();
        } else {
            showStatus(music.load(musicName, baseURL));
            loadsounds();
        }
    }

    // Update music.
    public void updateMusic()
    {
        // Rewind?
        if (rewind)
        {
            music.rewind();
            rewind = false;
        }

        // Move music.
        if (!frozen)
        {
            music.movement();
        }

        // Update canvas.
        updateCanvas();
    }

    // Paint.
    public void paint(Graphics g)
    {
        updateCanvas();
        super.paint(g);
    }

    // Music text selection listener.
    class musicTextActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent evt)
        {
            musicName = musicText.getText();
            if (!musicName.equals("Music:"))
            {
                synchThreads(LOAD);
                updateFiles();
            } else {
                showStatus("Invalid music name: " + musicName);
            }
        }
    }

    // Music choice listener.
    class musicChoiceItemListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent evt)
        {
            musicName = musicChoice.getItem(musicChoice.getSelectedIndex());
            if (!musicName.equals("Music:"))
            {
                musicChoice.select(0);
                synchThreads(LOAD);
                updateFiles();
            }
        }
    }

    // Update file selection.
    public void updateFiles()
    {
        int i, j;
        String s;

        if (music.name.equals("")) return;

        musicText.setText(music.name);

        // Add selection to choices.
        for (i = 0, j = musicChoice.getItemCount(); i < j; i++)
        {
            s = musicChoice.getItem(i);
            if (s.equals(music.name)) return;
        }
        musicChoice.add(music.name);
    }

    // Rewind button listener.
    class rewindButtonActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent evt)
        {
            rewind = true;
        }
    }

    // Freeze check button listener.
    class freezeCheckItemListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent evt)
        {
            if (freezeCheck.getState())
            {
                frozen = true;
            } else {
                frozen = false;
            }
        }
    }

    // Mute check button listener.
    class muteCheckItemListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent evt)
        {
            if (muteCheck.getState())
            {
                music.mute = true;
            } else {
                music.mute = false;
            }
        }
    }

    // Update canvas.
    public void updateCanvas()
    {
        Dimension d;
        int i, x, y, r, w, h;
        String s;

        // Clear.
        canvasImageGraphics.setColor(Color.white);
        canvasImageGraphics.fillRect(0, 0, canvasSize.width, canvasSize.height);

        // Draw the spheres.
        d = canvasSize;
        for (i = 0; music.spheres[i] != null; i++)
        {
            if (music.spheres[i].paletteIndex != -1 &&
                music.palette.soundColors[music.spheres[i].paletteIndex] != null)
            {
                canvasImageGraphics.setColor(music.palette.soundColors[music.spheres[i].paletteIndex]);
            } else {
                canvasImageGraphics.setColor(Color.black);
            }
            x = music.spheres[i].x - music.spheres[i].radius;
            y = music.spheres[i].y - music.spheres[i].radius;
            r = music.spheres[i].radius * 2;
            canvasImageGraphics.fillOval(x, y, r, r);
        }

        // Draw the strings.
        for (i = 0; music.strings[i] != null; i++)
        {
            if (music.strings[i].paletteIndex != -1 &&
                music.palette.soundColors[music.strings[i].paletteIndex] != null)
            {
                canvasImageGraphics.setColor(music.palette.soundColors[music.strings[i].paletteIndex]);
            } else {
                canvasImageGraphics.setColor(Color.black);
            }
            canvasImageGraphics.drawLine(music.strings[i].x1, music.strings[i].y1,
                music.strings[i].x2, music.strings[i].y2);
        }

        // Draw music boundaries.
        canvasImageGraphics.setFont(font);
        canvasImageGraphics.setColor(Color.black);
        canvasImageGraphics.drawLine(music.size.width, 0, music.size.width, music.size.height);
        canvasImageGraphics.drawLine(0, music.size.height, music.size.width, music.size.height);

        // State-specific displays.
        if (music.beat == music.finisBeat)
        {
            s = "F I N I S";
            canvasImageGraphics.drawString(s, 
                (d.width - fontMetrics.stringWidth(s)) / 2, d.height / 2);
        }

        // Copy the off-screen buffer to the screen.
        canvasGraphics.drawImage(canvasImage, 0, 0, this);
    }

    // Load sounds if necessary by playing and immediately stopping them.
    void loadsounds()
    {
        int i;

        for (i = 0; music.palette.soundURLs[i] != null; i++)
        {
            if (music.palette.soundClips[i] == null)
            {
                music.palette.soundClips[i] = getAudioClip(music.palette.soundURLs[i]);
                music.palette.soundClips[i].play();
                music.palette.soundClips[i].stop();
            }
        }
    }
}
