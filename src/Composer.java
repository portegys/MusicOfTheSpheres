/*

Music of the Spheres composer application.

Description:

Provides a composition feature for creating new musical pieces.
Sounds and colors to load are specified in palette files.
Compositions may be saved in files and loaded from files or URLs.

Usage:

java Composer [<screen width> <screen height>]

*/

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.applet.Applet;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;

// Music composer.
public class Composer implements Runnable
{
    // Parameters.
    static final int DEFAULT_SCREEN_WIDTH = 500;
    static final int DEFAULT_SCREEN_HEIGHT = 500;
    static final int DEFAULT_SPHERES = 4;
    static final int DEFAULT_SPHERE_RADIUS  = 20;
    static final int DEFAULT_STRINGS = 3;
    static final int DEFAULT_STRING_SIZE  = 70;
    static final int BOUNDING_DIST = 5;
    static final int UPDATE_FREQUENCY = 50; // Milliseconds between screen updates.
    static final int STOP_CHECK_FREQUENCY = 1000;

    // Music.
    Music music;

    // Screen and canvas (buffered).
    JFrame screen;
    Canvas canvas;
    Dimension canvasSize;
    Graphics canvasGraphics;
    Image canvasImage;
    Graphics canvasImageGraphics;

    // State.
    static final int PLAY = 0;
    static final int COMPOSE = 1;
    static final int FILE = 2;
    static final int COMPOSE_ADD = 3;
    static final int COMPOSE_PALETTE = 4;
    int mode;
    String musicName;
    String paletteName;
    boolean skipload;
    boolean rewind;
    boolean frozen;
    static final int EDIT_SIZE = 0;
    static final int EDIT_ANGLE = 1;
    static final int EDIT_SPEED = 2;
    int editMode;
    boolean composeChange;
    boolean dragging;
    static final int LOAD_MUSIC = 0;
    static final int SAVE_MUSIC = 1;
    static final int LOAD_PALETTE = 2;
    static final int UPDATE = 3;
    int fileOperation;
    String statusMessage;
    URL baseURL;
    double sdx, sdy;

    // Control panels.
    JTabbedPane controlTabs;
    JPanel playPanel;
    Button rewindButton;
    Checkbox freezeCheck;
    Checkbox muteCheck;
    JPanel composePanel;
    JPanel composePanelA;
    CheckboxGroup editGroup;
    Checkbox sizeCheck;
    Checkbox angleCheck;
    Checkbox speedCheck;
    JSlider editSlider;
    JPanel composePanelB;
    Button addButton;
    Button deleteButton;
    Button paletteButton;
    Checkbox markFinisCheck;
    JPanel filePanel;
    Choice fileOperationChoice;
    JComboBox musicComboBox;
    Label paletteLabel;
    JComboBox paletteComboBox;

    // Spheres and strings.
    SphereSprite addSphere;
    StringSprite addHorizontalString;
    StringSprite addVerticalString;
    int currentSphere;
    int currentString;

    // Sound and color palette.
    int paletteXY;
    SphereSprite paletteSphere;
    StringSprite paletteString;

    // Font.
    Font font = new Font("Helvetica", Font.BOLD, 12);
    FontMetrics fontMetrics;
    int fontWidth;
    int fontHeight;

    // Main.
    public static void main(String[] args) 
    {
        Dimension screenSize;

        // Get screen size.
        screenSize = new Dimension(DEFAULT_SCREEN_WIDTH, DEFAULT_SCREEN_HEIGHT);
        if (args.length == 2)
        {
            screenSize.width = Integer.parseInt(args[0]);
            screenSize.height = Integer.parseInt(args[1]);
        } else if (args.length != 0)
        {
            System.err.println("java Composer [<screen width> <screen height>]");
            System.exit(1);
        }
        if (screenSize.width <= 0 || screenSize.height <= 0)
        {
            System.err.println("Invalid screen size");
            System.exit(1);
        }

        // Create the composer.
        new Composer(screenSize);
    }

    // Constructor.
    public Composer(Dimension screenSize)
    {
        // Create screen.
        screen = new JFrame("Composer");
        screen.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { System.exit(0); }
        });
        screen.setSize(screenSize);
        screen.setVisible(true);

        // Create canvas.
        screen.getContentPane().setLayout(new BorderLayout());
        canvas = new Canvas();
        canvasSize = new Dimension(screenSize.width, (int)((double)screenSize.height * .80));
        canvas.setBounds(0, 0, canvasSize.width, canvasSize.height);
        canvas.addMouseListener(new canvasMouseListener());
        canvas.addMouseMotionListener(new canvasMouseMotionListener());
        screen.getContentPane().add(canvas, BorderLayout.NORTH);
        canvasGraphics = canvas.getGraphics();
        canvasImage = screen.getContentPane().createImage(canvasSize.width, canvasSize.height);
        canvasImageGraphics = canvasImage.getGraphics();

        // Create music.
        music = new Music(canvasSize);

        // Create control panels.
        controlTabs = new JTabbedPane();
        controlTabs.addChangeListener(new controlTabsChangeListener());
        playPanel = new JPanel();
        rewindButton = new Button("Rewind");
        rewindButton.addActionListener(new rewindButtonActionListener());
        playPanel.add(rewindButton);
        freezeCheck = new Checkbox("Freeze");
        freezeCheck.addItemListener(new freezeCheckItemListener());
        playPanel.add(freezeCheck);
        muteCheck = new Checkbox("Mute");
        muteCheck.addItemListener(new muteCheckItemListener());
        playPanel.add(muteCheck);
        composePanel = new JPanel();
        composePanelA = new JPanel();
        editGroup = new CheckboxGroup();
        sizeCheck = new Checkbox("Size", editGroup, true);
        sizeCheck.addItemListener(new sizeCheckItemListener());
        composePanelA.add(sizeCheck);
        angleCheck = new Checkbox("Angle", editGroup, false);
        angleCheck.addItemListener(new angleCheckItemListener());
        composePanelA.add(angleCheck);
        speedCheck = new Checkbox("Speed", editGroup, false);
        speedCheck.addItemListener(new speedCheckItemListener());
        composePanelA.add(speedCheck);
        editSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        editSlider.addChangeListener(new editSliderChangeListener());
        composePanelA.add(editSlider);
        composePanel.add(composePanelA);
        composePanelB = new JPanel();
        addButton = new Button("Add");
        addButton.addActionListener(new addButtonActionListener());
        composePanelB.add(addButton);
        deleteButton = new Button("Delete");
        deleteButton.addActionListener(new deleteButtonActionListener());
        composePanelB.add(deleteButton);
        paletteButton = new Button("Palette");
        paletteButton.addActionListener(new paletteButtonActionListener());
        composePanelB.add(paletteButton);
        markFinisCheck = new Checkbox("Mark finis");
        markFinisCheck.addItemListener(new markFinisCheckItemListener());
        composePanelB.add(markFinisCheck);
        composePanel.add(composePanelB);
        filePanel = new JPanel();
        fileOperationChoice = new Choice();
        fileOperationChoice.addItemListener(new fileOperationChoiceItemListener());
        fileOperationChoice.add("Load music:");
        fileOperationChoice.add("Save music:");
        filePanel.add(fileOperationChoice);
        musicComboBox = new JComboBox();
        musicComboBox.setEditable(true);
        musicComboBox.setEnabled(true);
        musicComboBox.addActionListener(new musicComboBoxActionListener());
        filePanel.add(musicComboBox);
        paletteLabel = new Label("Palette:");
        filePanel.add(paletteLabel);
        paletteComboBox = new JComboBox();
        paletteComboBox.setEditable(true);
        paletteComboBox.setEnabled(true);
        paletteComboBox.addActionListener(new paletteComboBoxActionListener());
        filePanel.add(paletteComboBox);
        controlTabs.addTab("Play", null, playPanel, "Play");
        controlTabs.addTab("Compose", null, composePanel, "Compose");
        controlTabs.addTab("File", null, filePanel, "File operations");
        screen.getContentPane().add(controlTabs);
        controlTabs.setSelectedIndex(2);
        controlTabs.setSelectedIndex(1);
        try { Thread.sleep(1000); } catch(InterruptedException e) {} // for drawing glitch?
        controlTabs.setSelectedIndex(0);

        // Set font data.
        Graphics g = canvasImage.getGraphics();
        g.setFont(font);
        fontMetrics = g.getFontMetrics();
        fontWidth = fontMetrics.getMaxAdvance();
        fontHeight = fontMetrics.getHeight();

        // Initialize state.
        mode = PLAY;
        musicName = "";
        paletteName = "";
        skipload = false;
        rewind = false;
        frozen = false;
        editMode = EDIT_SIZE;
        composeChange = false;
        dragging = false;
        fileOperation = LOAD_MUSIC;
        statusMessage = "";
        currentSphere = currentString = -1;
        for (paletteXY = music.MIN_SPHERE_RADIUS;
            paletteXY > 0 && Math.floor(canvasSize.width / paletteXY) *
            Math.floor(canvasSize.height / paletteXY) <
            ((music.MAX_SOUND_COLOR + 1) * 2); paletteXY--);
        paletteSphere = new SphereSprite();
        paletteSphere.radius = paletteXY / 4;
        paletteString = new StringSprite();

        // Get base URL.
        try {
            String s = System.getProperty("user.dir");
            baseURL = new File(s).toURL();
        }
        catch (SecurityException e) {
            System.err.println("Cannot get property for current directory");
            System.exit(1);
        }
        catch (MalformedURLException e) {
            System.err.println("Cannot get URL of current directory");
            System.exit(1);
        }

        // Initialize canvas.
        initCanvas();

        // Start the composer.
        new Thread(this).start();
    }

    // Initialize canvas.
    public void initCanvas()
    {
        int i, l, r;

        // Create positions and movements for each sphere.
        for (i = 0; i < DEFAULT_SPHERES; i++)
        {
            music.spheres[i] = new SphereSprite();
            music.spheres[i].radius = r = music.MIN_SPHERE_RADIUS + (int)(Math.random() *
                (double)(music.MAX_SPHERE_RADIUS - music.MIN_SPHERE_RADIUS));
            music.spheres[i].x = (int)(Math.random() * (double)(canvasSize.width - (r * 2))) + r;
            music.spheres[i].y = (int)(Math.random() * (double)(canvasSize.height - (r * 2))) + r;
            music.spheres[i].dx = Math.random() * (double)music.MAX_SPHERE_SPEED * Math.sqrt(0.5);
            if (Math.random() < 0.5)
            {
                music.spheres[i].dx = -music.spheres[i].dx;
            }
            music.spheres[i].dy = Math.random() * (double)music.MAX_SPHERE_SPEED * Math.sqrt(0.5);
            if (Math.random() < 0.5)
            {
                music.spheres[i].dy = -music.spheres[i].dy;
            }
            music.spheres[i].paletteIndex = -1;
            music.spheresRewindTo[i] = new SphereSprite();
            music.spheresRewindTo[i].radius = music.spheres[i].radius;
            music.spheresRewindTo[i].x = music.spheres[i].x;
            music.spheresRewindTo[i].y = music.spheres[i].y;
            music.spheresRewindTo[i].dx = music.spheres[i].dx;
            music.spheresRewindTo[i].dy = music.spheres[i].dy;
            music.spheresRewindTo[i].paletteIndex = music.spheres[i].paletteIndex;
        }

        // Create positions for each string.
        for (i = 0; i < DEFAULT_STRINGS; i++)
        {
            music.strings[i] = new StringSprite();
            l = music.MIN_STRING_SIZE + (int)(Math.random() *
                (double)(music.MAX_STRING_SIZE - music.MIN_STRING_SIZE));
            music.strings[i].x1 = (int)(Math.random() * (double)canvasSize.width);
            music.strings[i].y1 = (int)(Math.random() * (double)canvasSize.height);
            if (Math.random() < 0.5)
            {
                music.strings[i].x2 = music.strings[i].x1;
                music.strings[i].y2 = music.strings[i].y1 + (l / 2);
                music.strings[i].y1 -= (l / 2);
            } else {
                music.strings[i].y2 = music.strings[i].y1;
                music.strings[i].x2 = music.strings[i].x1 + (l / 2);
                music.strings[i].x1 -= (l / 2);
            }
            music.strings[i].paletteIndex = -1;
        }

        // Position sphere and strings for add mode.
        addSphere = new SphereSprite();
        addSphere.radius = music.MIN_SPHERE_RADIUS;
        addSphere.x = canvasSize.width / 4;
        addSphere.y = canvasSize.height / 2;
        addHorizontalString = new StringSprite();
        addHorizontalString.x1 = (canvasSize.width / 2) - (music.MIN_STRING_SIZE / 2);
        addHorizontalString.x2 = (canvasSize.width / 2) + (music.MIN_STRING_SIZE / 2);
        addHorizontalString.y1 = canvasSize.height / 2;
        addHorizontalString.y2 = canvasSize.height / 2;
        addVerticalString = new StringSprite();
        addVerticalString.x1 = (3 * canvasSize.width) / 4;
        addVerticalString.x2 = (3 * canvasSize.width) / 4;
        addVerticalString.y1 = (canvasSize.height / 2) - (music.MIN_STRING_SIZE / 2);
        addVerticalString.y2 = (canvasSize.height / 2) + (music.MIN_STRING_SIZE / 2);
    }

    // Run.
    public void run()
    {
        // Lower this thread's priority.
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);

        // Music update loop.
        while (true)
        {
            // Synchronize with filing.
            synchThreads(UPDATE);

            // Sleep until the next loop.
            try
            {
                Thread.sleep(UPDATE_FREQUENCY);
            } catch(InterruptedException e) { break; }
        }
    }

    // Synchronize filing and update threads to avoid inconsistency.
    public synchronized void synchThreads(int which)
    {
        switch(which)
        {
        case LOAD_MUSIC:
            statusMessage = music.load(musicName, baseURL);
            loadsounds();
            break;
        case SAVE_MUSIC:
            statusMessage = music.save(musicName);
            break;
        case LOAD_PALETTE:
            statusMessage = music.palette.load(paletteName, baseURL);
            loadsounds();
            break;
        case UPDATE:
            updateMusic();
            break;
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

        // Advance music a beat.
        if (mode == PLAY && !frozen)
        {
            music.movement();
        }

        // Update canvas.
        updateCanvas();
    }

    // Control tabs listener.
    class controlTabsChangeListener implements ChangeListener
    {
        public void stateChanged(ChangeEvent evt)
        {
            int i;

            // Save new composition if changed.
            if (composeChange)
            {
                composeChange = false;
                for (i = 0; music.spheres[i] != null; i++)
                {
                    if (music.spheresRewindTo[i] == null)
                    {
                        music.spheresRewindTo[i] = new SphereSprite();
                    }
                    music.spheresRewindTo[i].radius = music.spheres[i].radius;
                    music.spheresRewindTo[i].x = music.spheres[i].x;
                    music.spheresRewindTo[i].y = music.spheres[i].y;
                    music.spheresRewindTo[i].dx = music.spheres[i].dx;
                    music.spheresRewindTo[i].dy = music.spheres[i].dy;
                    music.spheresRewindTo[i].paletteIndex = music.spheres[i].paletteIndex;
                }
                for (; music.spheresRewindTo[i] != null; i++)
                {
                    music.spheresRewindTo[i] = null;
                }
            }
            currentSphere = currentString = -1;
            statusMessage = "";

            // Set current mode.
            mode = controlTabs.getSelectedIndex();
        }
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

    // Edit size check button listener.
    class sizeCheckItemListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent evt)
        {
            mode = COMPOSE;
            editMode = EDIT_SIZE;
            setEditSlider();
        }
    }

    // Edit angle check button listener.
    class angleCheckItemListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent evt)
        {
            mode = COMPOSE;
            editMode = EDIT_ANGLE;
            setEditSlider();
        }
    }

    // Edit speed check button listener.
    class speedCheckItemListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent evt)
        {
            mode = COMPOSE;
            editMode = EDIT_SPEED;
            setEditSlider();
        }
    }

    // Add button listener.
    class addButtonActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent evt)
        {
            mode = COMPOSE_ADD;
            currentSphere = currentString = -1;
        }
    }

    // Delete button listener.
    class deleteButtonActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent evt)
        {
            int i;

            mode = COMPOSE;
            if (currentSphere != -1)
            {
                for (i = currentSphere; music.spheres[i] != null; i++)
                {
                    music.spheres[i] = music.spheres[i+1];
                }
                currentSphere = -1;
                markComposeChange();
            }
            if (currentString != -1)
            {
                for (i = currentString; music.strings[i] != null; i++)
                {
                    music.strings[i] = music.strings[i+1];
                }
                currentString = -1;
                markComposeChange();
            }
        }
    }

    // Palette button listener.
    class paletteButtonActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent evt)
        {
            mode = COMPOSE_PALETTE;
            currentSphere = currentString = -1;
            paletteSphere.paletteIndex = -1;
            paletteString.paletteIndex = -1;
        }
    }

    // Mark finis check button listener.
    class markFinisCheckItemListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent evt)
        {
            mode = COMPOSE;
            if (markFinisCheck.getState())
            {
                music.finisBeat = music.beat;
            } else {
                music.finisBeat = -1;
            }
        }
    }

    // Set edit slider.
    public void setEditSlider()
    {
        double d, dx, dy, a;
        int i;

        switch(editMode)
        {
        case EDIT_SIZE:
            if (currentSphere != -1)
            {
                d = (double)(music.spheres[currentSphere].radius - music.MIN_SPHERE_RADIUS) /
                        (double)(music.MAX_SPHERE_RADIUS - music.MIN_SPHERE_RADIUS);
                editSlider.setValue((int)(100.0 * d));
            } else if (currentString != -1)
            {
                if (music.strings[currentString].x1 == music.strings[currentString].x2)
                {
                    i = music.strings[currentString].y2 - music.strings[currentString].y1;
                } else {
                    i = music.strings[currentString].x2 - music.strings[currentString].x1;
                }
                d = (double)(i - music.MIN_STRING_SIZE) /
                        (double)(music.MAX_STRING_SIZE - music.MIN_STRING_SIZE);
                editSlider.setValue((int)(100.0 * d));
            } else {
                editSlider.setValue(0);
            }
            break;

        case EDIT_ANGLE:
            if (currentSphere != -1)
            {
                dx = music.spheres[currentSphere].dx;
                dy = music.spheres[currentSphere].dy;
                if (dx == 0.0 && dy == 0.0)
                {
                    a = 0.0;
                } else if (dx == 0.0)
                {
                    if (dy > 0.0)
                    {
                        a = 75.0;
                    } else {
                        a = 25.0;
                    }
                } else {
                    a = Math.atan(Math.abs(dy / dx));
                    a = (a / (Math.PI / 2.0)) * 25.0;
                    if (dx < 0.0)
                    {
                        if (dy < 0.0)
                        {
                            a = 50.0 - a;
                        } else {
                            a += 50.0;
                        }
                    } else {
                        if (dy > 0.0)
                        {
                            a = 100.0 - a;
                        }
                    }
                }
                editSlider.setValue((int)a);
            } else {
                editSlider.setValue(0);
            }
            break;

        case EDIT_SPEED:
            if (currentSphere != -1)
            {
                dx = music.spheres[currentSphere].dx;
                dy = music.spheres[currentSphere].dy;
                d = Math.sqrt((dx * dx) + (dy * dy));
                d = d / (double)music.MAX_SPHERE_SPEED;
                editSlider.setValue((int)(100.0 * d));
            } else {
                editSlider.setValue(0);
            }
            break;
        }
    }

    // Edit slider listener.
    class editSliderChangeListener implements ChangeListener
    {
        public void stateChanged(ChangeEvent evt)
        {
            int i, j;
            double d, dx, dy, a;

            mode = COMPOSE;
            switch(editMode)
            {
            case EDIT_SIZE:
                if (currentSphere != -1)
                {
                    d = (double)editSlider.getValue() / 100.0;
                    music.spheres[currentSphere].radius =
                        (int)((double)(music.MAX_SPHERE_RADIUS - music.MIN_SPHERE_RADIUS) * d) +
                        music.MIN_SPHERE_RADIUS;
                    markComposeChange();
                } else if (currentString != -1)
                {
                    d = (double)editSlider.getValue() / 100.0;
                    i = (int)((double)(music.MAX_STRING_SIZE - music.MIN_STRING_SIZE) * d) +
                        music.MIN_STRING_SIZE;
                    if (music.strings[currentString].x1 == music.strings[currentString].x2)
                    {
                        j = (music.strings[currentString].y2 + music.strings[currentString].y1) / 2;
                        music.strings[currentString].y1 = j - (i / 2);
                        music.strings[currentString].y2 = j + (i / 2);
                    } else {
                        j = (music.strings[currentString].x2 + music.strings[currentString].x1) / 2;
                        music.strings[currentString].x1 = j - (i / 2);
                        music.strings[currentString].x2 = j + (i / 2);
                    }
                    markComposeChange();
                } else {
                    editSlider.setValue(0);
                }
                break;
   
            case EDIT_ANGLE:
                if (currentSphere != -1)
                {
                    i = editSlider.getValue();
                    for (j = 0; i > 25; i -= 25, j++);
                    a = 0.0;
                    switch(j)
                    {
                    case 0:
                    case 2:
                        a = ((double)i / 25.0) * (Math.PI / 2.0);
                        break;
                    case 1:
                    case 3:
                        a = ((25.0 - (double)i) / 25.0) * (Math.PI / 2.0);
                        break;
                    }
                    dx = music.spheres[currentSphere].dx;
                    dy = music.spheres[currentSphere].dy;
                    d = Math.sqrt((dx * dx) + (dy * dy));
                    dy = d * Math.sin(a);
                    dx = d * Math.cos(a);
                    switch(j)
                    {
                    case 0:
                        dx = Math.abs(dx);
                        dy = -Math.abs(dy);
                        break;
                    case 1:
                        dx = -Math.abs(dx);
                        dy = -Math.abs(dy);
                        break;
                    case 2:
                        dx = -Math.abs(dx);
                        dy = Math.abs(dy);
                        break;
                    case 3:
                        dx = Math.abs(dx);
                        dy = Math.abs(dy);
                        break;
                    }
                    music.spheres[currentSphere].dx = dx;
                    music.spheres[currentSphere].dy = dy;
                    markComposeChange();
                } else {
                    editSlider.setValue(0);
                }
                break;

            case EDIT_SPEED:
                if (currentSphere != -1)
                {
                    d = ((double)editSlider.getValue() / 100.0) * (double)music.MAX_SPHERE_SPEED;
                    dx = music.spheres[currentSphere].dx;
                    dy = music.spheres[currentSphere].dy;
                    if (Math.abs(dx) > 0.0)
                    {
                        a = Math.atan(Math.abs(dy / dx));
                        dy = d * Math.sin(a);
                        dx = d * Math.cos(a);
                    } else {
                        if (Math.abs(dy) > 0.0)
                        {
                            dx = 0.0;
                            dy = d;
                        } else {
                            dx = d;
                            dy = 0.0;
                        }
                    }
                    if (music.spheres[currentSphere].dx >= 0.0)
                    {
                        music.spheres[currentSphere].dx = dx;
                    } else {
                        music.spheres[currentSphere].dx = -dx;
                    }
                    if (music.spheres[currentSphere].dy >= 0.0)
                    {
                        music.spheres[currentSphere].dy = dy;
                    } else {
                        music.spheres[currentSphere].dy = -dy;
                    }
                    markComposeChange();
                } else {
                    editSlider.setValue(0);
                }
                break;
            }
        }
    }

    // Canvas mouse listener.
    class canvasMouseListener extends MouseAdapter
    {

        // Mouse pressed.
        public void mousePressed(MouseEvent evt)
        {
            int x, y, i, j, d, d2, px, py;

            dragging = false;
            x = evt.getX();
            y = evt.getY();

            switch(mode)
            {
            case COMPOSE:

                // Select current sphere or string.
                currentSphere = currentString = -1;
                d2 = 0;
                for (i = 0, j = -1; music.spheres[i] != null; i++)
                {
                    d = (int)music.pointDist(x, y, music.spheres[i].x, music.spheres[i].y);
                    if (d <= music.spheres[i].radius)
                    {
                        if (j == -1 || d < d2)
                        {
                            j = i;
                            d2 = d;
                        }
                    }
                }
                if (j != -1)
                {
                    currentSphere = j;
                    setEditSlider();
                    dragging = true;
                    canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    if (!music.mute && music.spheres[currentSphere].paletteIndex != -1 &&
                        music.palette.soundClips[music.spheres[currentSphere].paletteIndex] != null)
                    {
                        music.palette.soundClips[music.spheres[currentSphere].paletteIndex].play();
                    }
                    return;
                }
                for (i = 0, j = -1; music.strings[i] != null; i++)
                {
                    if (music.strings[i].x1 == music.strings[i].x2)
                    {
                        if (y >= music.strings[i].y1 && y <= music.strings[i].y2)
                        {
                            if ((d = Math.abs(x - music.strings[i].x1)) <= BOUNDING_DIST)
                            {
                                if (j == -1 || d < d2)
                                {
                                    j = i;
                                    d2 = d;
                                }
                            }
                        }
                    } else {
                        if (x >= music.strings[i].x1 && x <= music.strings[i].x2)
                        {
                            if ((d = Math.abs(y - music.strings[i].y1)) <= BOUNDING_DIST)
                            {
                                if (j == -1 || d < d2)
                                {
                                    j = i;
                                    d2 = d;
                                }
                            }
                        }
                    }
                }
                if (j != -1)
                {
                    currentString = j;
                    setEditSlider();
                    dragging = true;
                    canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    if (!music.mute && music.strings[currentString].paletteIndex != -1 &&
                        music.palette.soundClips[music.strings[currentString].paletteIndex] != null)
                    {
                        music.palette.soundClips[music.strings[currentString].paletteIndex].play();
                    }
                    return;
                }
                setEditSlider();
                break;

            case COMPOSE_ADD:

                currentSphere = currentString = -1;

                // Add sphere?
                d = (int)music.pointDist(x, y, addSphere.x, addSphere.y);
                if (d <= addSphere.radius)
                {
                    for (i = 0; music.spheres[i] != null; i++);
                    if (i == music.MAX_SPHERES)
                    {
                        System.err.println("Cannot add sphere - maximum = " + music.MAX_SPHERES);
                    } else {
                        music.spheres[i] = new SphereSprite();
                        music.spheres[i].radius = addSphere.radius;
                        music.spheres[i].x = addSphere.x;
                        music.spheres[i].y = addSphere.y;
                        music.spheres[i].dx = addSphere.dx;
                        music.spheres[i].dy = addSphere.dy;
                        music.spheres[i].paletteIndex = addSphere.paletteIndex;
                        currentSphere = i;
                        setEditSlider();
                        dragging = true;
                        canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    }
                }

                // Add horizontal string?
                if (currentSphere == -1)
                {
                    if (x >= addHorizontalString.x1 && x <= addHorizontalString.x2)
                    {
                        if ((d = Math.abs(y - addHorizontalString.y1)) <= BOUNDING_DIST)
                        {
                            for (i = 0; music.strings[i] != null; i++);
                            if (i == music.MAX_STRINGS)
                            {
                                System.err.println("Cannot add string - maximum = " + music.MAX_STRINGS);
                            } else {
                                music.strings[i] = new StringSprite();
                                music.strings[i].x1 = addHorizontalString.x1;
                                music.strings[i].y1 = addHorizontalString.y1;
                                music.strings[i].x2 = addHorizontalString.x2;
                                music.strings[i].y2 = addHorizontalString.y2;
                                music.strings[i].paletteIndex = addHorizontalString.paletteIndex;
                                currentString = i;
                                setEditSlider();
                                dragging = true;
                                canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                            }
                        }
                    }
                }

                // Add vertical string?
                if (currentSphere == -1 && currentString == -1)
                {
                    if (y >= addVerticalString.y1 && y <= addVerticalString.y2)
                    {
                        if ((d = Math.abs(x - addVerticalString.x1)) <= BOUNDING_DIST)
                        {
                            for (i = 0; music.strings[i] != null; i++);
                            if (i == music.MAX_STRINGS)
                            {
                                System.err.println("Cannot add string - maximum = " + music.MAX_STRINGS);
                            } else {
                                music.strings[i] = new StringSprite();
                                music.strings[i].x1 = addVerticalString.x1;
                                music.strings[i].y1 = addVerticalString.y1;
                                music.strings[i].x2 = addVerticalString.x2;
                                music.strings[i].y2 = addVerticalString.y2;
                                music.strings[i].paletteIndex = addVerticalString.paletteIndex;
                                currentString = i;
                                setEditSlider();
                                dragging = true;
                                canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                            }
                        }
                    }
                }
                mode = COMPOSE;
                break;

            case COMPOSE_PALETTE:

                currentSphere = currentString = -1;
                paletteSphere.paletteIndex = -1;
                paletteString.paletteIndex = -1;

                // Which palette entry?
                for (i = px = py = 0; music.palette.spherePaletteIndexes[i] != -1 &&
                     i < (music.MAX_SOUND_COLOR - 1); i++)
                {            
                    j = music.palette.spherePaletteIndexes[i];
                    if (x >= px && x <= (px + paletteXY) && y >= py && y <= (py + paletteXY))
                    {
                        dragging = true;
                        canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                        paletteSphere.paletteIndex = j;
                        if (!music.mute && music.palette.soundClips[j] != null)
                        {
                            music.palette.soundClips[j].play();
                        }
                        return;
                    }
                    px += paletteXY;
                    if (px > (canvasSize.width - paletteXY))
                    {
                        px = 0;
                        py += paletteXY;
                    }
                }
                if (x >= px && x <= (px + paletteXY) && y >= py && y <= (py + paletteXY))
                {
                    dragging = true;
                    canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    paletteSphere.paletteIndex = music.MAX_SOUND_COLOR;  // Black for delete.
                    return;
                }
                px += paletteXY;
                if (px > (canvasSize.width - paletteXY))
                {
                    px = 0;
                    py += paletteXY;
                }
                for (i = 0; music.palette.stringPaletteIndexes[i] != -1 &&
                     i < (music.MAX_SOUND_COLOR - 1); i++)
                {            
                    j = music.palette.stringPaletteIndexes[i];
                    if (x >= px && x <= (px + paletteXY) && y >= py && y <= (py + paletteXY))
                    {
                        dragging = true;
                        canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                        paletteString.paletteIndex = j;
                        if (!music.mute && music.palette.soundClips[j] != null)
                        {
                            music.palette.soundClips[j].play();
                        }
                        return;
                    }
                    px += paletteXY;
                    if (px > (canvasSize.width - paletteXY))
                    {
                        px = 0;
                        py += paletteXY;
                    }
                }
                if (x >= px && x <= (px + paletteXY) && y >= py && y <= (py + paletteXY))
                {
                    dragging = true;
                    canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    paletteString.paletteIndex = music.MAX_SOUND_COLOR;  // Black for delete.
                    return;
                }
                mode = COMPOSE;
                break;
            }
        }

        // Mouse released.
        public void mouseReleased(MouseEvent evt)
        {
            int x, y, i, j, xd, yd, d, d2;

            if (!dragging)
            {
                return;
            }
            dragging = false;
            canvas.setCursor(Cursor.getDefaultCursor());
            x = evt.getX();
            y = evt.getY();

            switch(mode)
            {
            case COMPOSE:

                if (currentSphere != -1)
                {
                    if (x < 0 || x >= canvasSize.width || y < 0 || y >= canvasSize.height)
                    {
                        for (i = currentSphere; music.spheres[i] != null; i++)
                        {
                            music.spheres[i] = music.spheres[i+1];
                        }
                        currentSphere = -1;
                    } else {
                        music.spheres[currentSphere].x = x;
                        music.spheres[currentSphere].y = y;
                    }
                    markComposeChange();
                } else if (currentString != -1)
                {
                    if (x < 0 || x >= canvasSize.width || y < 0 || y >= canvasSize.height)
                    {
                        for (i = currentString; music.strings[i] != null; i++)
                        {
                            music.strings[i] = music.strings[i+1];
                        }
                        currentString = -1;
                    } else {
                        if (music.strings[currentString].x1 == music.strings[currentString].x2)
                        {
                            xd = x - music.strings[currentString].x1;
                            yd = (music.strings[currentString].y2 + music.strings[currentString].y1) / 2;
                            yd = y - yd;
                        } else {
                            xd = (music.strings[currentString].x2 + music.strings[currentString].x1) / 2;
                            xd = x - xd;
                            yd = y - music.strings[currentString].y1;
                        }
                        music.strings[currentString].x1 += xd;
                        music.strings[currentString].x2 += xd;
                        music.strings[currentString].y1 += yd;
                        music.strings[currentString].y2 += yd;
                    }
                    markComposeChange();
                }
                break;

            case COMPOSE_PALETTE:

                if (paletteSphere.paletteIndex != -1)
                {
                    d2 = 0;
                    for (i = 0, j = -1; music.spheres[i] != null; i++)
                    {
                        d = (int)music.pointDist(x, y, music.spheres[i].x, music.spheres[i].y);
                        if (d <= music.spheres[i].radius)
                        {
                            if (j == -1 || d < d2)
                            {
                                j = i;
                                d2 = d;
                            }
                        }
                    }
                    if (j != -1)
                    {
                        if (paletteSphere.paletteIndex == music.MAX_SOUND_COLOR)
                        {
                            music.spheres[j].paletteIndex = -1;
                        } else {
                            music.spheres[j].paletteIndex = paletteSphere.paletteIndex;
                        }
                        currentSphere = j;
                        markComposeChange();
                    }
                } else if (paletteString.paletteIndex != -1)
                {
                    d2 = 0;
                    for (i = 0, j = -1; music.strings[i] != null; i++)
                    {
                        if (music.strings[i].x1 == music.strings[i].x2)
                        {
                            if (y >= music.strings[i].y1 && y <= music.strings[i].y2)
                            {
                                if ((d = Math.abs(x - music.strings[i].x1)) <= BOUNDING_DIST)
                                {
                                    if (j == -1 || d < d2)
                                    {
                                        j = i;
                                        d2 = d;
                                    }
                                }
                            }
                        } else {
                            if (x >= music.strings[i].x1 && x <= music.strings[i].x2)
                            {
                                if ((d = Math.abs(y - music.strings[i].y1)) <= BOUNDING_DIST)
                                {
                                    if (j == -1 || d < d2)
                                    {
                                        j = i;
                                        d2 = d;
                                    }
                                }
                            }
                        }
                    }
                    if (j != -1)
                    {
                        if (paletteString.paletteIndex == music.MAX_SOUND_COLOR)
                        {
                            music.strings[j].paletteIndex = -1;
                        } else {
                            music.strings[j].paletteIndex = paletteString.paletteIndex;
                        }
                        currentString = j;
                        markComposeChange();
                    }
                }
                paletteSphere.paletteIndex = -1;
                paletteString.paletteIndex = -1;
                mode = COMPOSE;
                break;
            }
        }
    }

    // Canvas mouse motion listener.
    class canvasMouseMotionListener extends MouseMotionAdapter
    {

        // Mouse dragged.
        public void mouseDragged(MouseEvent evt)
        {
            int x, y, xd, yd;

            if (!dragging)
            {
                return;
            }
            x = evt.getX();
            y = evt.getY();

            switch(mode)
            {
            case COMPOSE:

                if (currentSphere != -1)
                {
                    music.spheres[currentSphere].x = x;
                    music.spheres[currentSphere].y = y;
                } else if (currentString != -1)
                {
                    if (music.strings[currentString].x1 == music.strings[currentString].x2)
                    {
                        xd = x - music.strings[currentString].x1;
                        yd = (music.strings[currentString].y2 + music.strings[currentString].y1) / 2;
                        yd = y - yd;
                    } else {
                        xd = (music.strings[currentString].x2 + music.strings[currentString].x1) / 2;
                        xd = x - xd;
                        yd = y - music.strings[currentString].y1;
                    }
                    music.strings[currentString].x1 += xd;
                    music.strings[currentString].x2 += xd;
                    music.strings[currentString].y1 += yd;
                    music.strings[currentString].y2 += yd;
                }
                break;

            case COMPOSE_PALETTE:

                if (paletteSphere.paletteIndex != -1)
                {
                    paletteSphere.x = x;
                    paletteSphere.y = y;
                } else if (paletteString.paletteIndex != -1)
                {
                    if (paletteString.x1 == paletteString.x2)
                    {
                        xd = x - paletteString.x1;
                        yd = (paletteString.y2 + paletteString.y1) / 2;
                        yd = y - yd;
                    } else {
                        xd = (paletteString.x2 + paletteString.x1) / 2;
                        xd = x - xd;
                        yd = y - paletteString.y1;
                    }
                    paletteString.x1 += xd;
                    paletteString.x2 += xd;
                    paletteString.y1 += yd;
                    paletteString.y2 += yd;
                }
                break;
            }
        }
    }

    // Composition has changed.
    void markComposeChange()
    {
        composeChange = true;
        music.beat = 0;
        if (music.finisBeat != -1)
        {
            music.finisBeat = -1;
            markFinisCheck.setState(false);
        }
        musicComboBox.setSelectedItem((Object)"");
    }

    // File choice listener.
    class fileOperationChoiceItemListener implements ItemListener
    {
        public void itemStateChanged(ItemEvent evt)
        {
            // Set current operation and get list of matching files.
            fileOperation = fileOperationChoice.getSelectedIndex();
            musicComboBox.setSelectedItem((Object)"");
        }
    }

    // Music combo box selection listener.
    class musicComboBoxActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent evt)
        {
            if ((musicName = (String)musicComboBox.getSelectedItem()) != null &&
                !musicName.equals(""))
            {
                synchThreads(fileOperation);
                updateFiles();
            }
        }
    }

    // Palette combo box selection listener.
    class paletteComboBoxActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent evt)
        {
            if (skipload == true)
            {
                skipload = false;
                return;
            }
            if ((paletteName = (String)paletteComboBox.getSelectedItem()) != null &&
                !paletteName.equals(""))
            {
                synchThreads(LOAD_PALETTE);
                updateFiles();
            }
        }
    }

    // Update music and palette file selections.
    void updateFiles()
    {
        int i,j;
        String s;

        // Update music selections.
        if (!music.name.equals(""))
        {
            for (i = 0, j = musicComboBox.getItemCount(); i < j; i++)
            {
                s = (String)musicComboBox.getItemAt(i);
                if (s.equals(music.name)) break;
            }
            if (i == j)
            {
                musicComboBox.addItem((Object)music.name);
            }
            musicComboBox.setSelectedIndex(i);
            musicComboBox.setSelectedItem((Object)music.name);
        }

        // Update palette selections.
        if (music.palette.name.equals(""))
        {
            paletteComboBox.setSelectedItem((Object)"");
            return;
        }
        for (i = 0, j = paletteComboBox.getItemCount(); i < j; i++)
        {
            s = (String)paletteComboBox.getItemAt(i);
            if (s.equals(music.palette.name)) break;
        }
        if (i == j)
        {
            skipload = true;  // else palette will again load
            paletteComboBox.addItem((Object)music.palette.name);
        }
        paletteComboBox.setSelectedIndex(i);
        paletteComboBox.setSelectedItem((Object)music.palette.name);
    }

    // Update canvas.
    public void updateCanvas()
    {
        // Clear.
        canvasImageGraphics.setColor(Color.white);
        canvasImageGraphics.fillRect(0, 0, canvasSize.width, canvasSize.height);

        // Mode-specific updates.
        switch(mode)
        {
        case PLAY:
        case COMPOSE:
            updateNormalCanvas();
            break;

        case COMPOSE_ADD:
            updateAddCanvas();
            break;

        case COMPOSE_PALETTE:
            updatePaletteCanvas();
            break;

        case FILE:
            if (!statusMessage.equals(""))
            {
                canvasImageGraphics.setColor(Color.black);
                canvasImageGraphics.drawString(statusMessage,
                    (canvasSize.width - fontMetrics.stringWidth(statusMessage)) / 2,
                     canvasSize.height / 2);
            }
            break;
        }

        // Copy the off-screen buffer to the screen.
        canvasGraphics.drawImage(canvasImage, 0, 0, screen.getContentPane());
    }

    // Update normal canvas.
    public void updateNormalCanvas()
    {
        Dimension d;
        int i, x, y, r, w, h;
        String s;

        // Draw music boundaries.
        canvasImageGraphics.setFont(font);
        canvasImageGraphics.setColor(Color.black);
        canvasImageGraphics.drawLine(music.size.width, 0, music.size.width, music.size.height);
        canvasImageGraphics.drawLine(0, music.size.height, music.size.width, music.size.height);

        // Draw the spheres.
        d = canvasSize;
        for (i = 0; music.spheres[i] != null; i++)
        {
            if (mode == COMPOSE && currentSphere == i)
            {
                canvasImageGraphics.setColor(Color.lightGray);
                x = music.spheres[i].x - music.spheres[i].radius - BOUNDING_DIST;
                y = music.spheres[i].y - music.spheres[i].radius - BOUNDING_DIST;
                w = h = (music.spheres[i].radius * 2) + (BOUNDING_DIST * 2);
                canvasImageGraphics.fillRect(x, y, w, h);
            }
            if (music.spheres[i].paletteIndex != -1 && music.palette.soundColors[music.spheres[i].paletteIndex] != null)
            {
                canvasImageGraphics.setColor(music.palette.soundColors[music.spheres[i].paletteIndex]);
            } else {
                canvasImageGraphics.setColor(Color.black);
            }
            x = music.spheres[i].x - music.spheres[i].radius;
            y = music.spheres[i].y - music.spheres[i].radius;
            r = music.spheres[i].radius * 2;
            canvasImageGraphics.fillOval(x, y, r, r);
            if (mode != PLAY)
            {
                canvasImageGraphics.setColor(Color.white);
                scaleSphereVelocity(music.spheres[i].dx, music.spheres[i].dy);
                canvasImageGraphics.drawLine(music.spheres[i].x, music.spheres[i].y,
                    music.spheres[i].x + (int)sdx, music.spheres[i].y + (int)sdy);
            }
        }

        // Draw the strings.
        for (i = 0; music.strings[i] != null; i++)
        {
            if (mode == COMPOSE && currentString == i)
            {
                canvasImageGraphics.setColor(Color.lightGray);
                x = music.strings[i].x1 - BOUNDING_DIST;
                y = music.strings[i].y1 - BOUNDING_DIST;
                if (music.strings[i].x1 == music.strings[i].x2)
                {
                    w = (BOUNDING_DIST * 2);
                    h = (music.strings[i].y2 - music.strings[i].y1) + (BOUNDING_DIST * 2);
                } else {
                    w = (music.strings[i].x2 - music.strings[i].x1) + (BOUNDING_DIST * 2);
                    h = (BOUNDING_DIST * 2);
                }
                canvasImageGraphics.fillRect(x, y, w, h);
            }
            if (music.strings[i].paletteIndex != -1 && music.palette.soundColors[music.strings[i].paletteIndex] != null)
            {
                canvasImageGraphics.setColor(music.palette.soundColors[music.strings[i].paletteIndex]);
            } else {
                canvasImageGraphics.setColor(Color.black);
            }
            canvasImageGraphics.drawLine(music.strings[i].x1, music.strings[i].y1,
                music.strings[i].x2, music.strings[i].y2);
        }

        // State-specific displays.
        canvasImageGraphics.setFont(font);
        canvasImageGraphics.setColor(Color.black);
        switch(mode)
        {
        case PLAY:
            if (music.beat == music.finisBeat)
            {
                s = "F I N I S";
                canvasImageGraphics.drawString(s, 
                    (d.width - fontMetrics.stringWidth(s)) / 2, d.height / 2);
            }
            break;
        case COMPOSE:
            s = "Click to select";
            canvasImageGraphics.drawString(s, 
                (d.width - fontMetrics.stringWidth(s)) / 2, d.height / 2);
            break;
        }
    }

    // Update add canvas.
    public void updateAddCanvas()
    {
        Dimension d;
        int x, y, r;
        String s;

        // Draw the add sphere and strings.
        d = canvasSize;
        canvasImageGraphics.setColor(Color.black);
        x = addSphere.x - addSphere.radius;
        y = addSphere.y - addSphere.radius;
        r = addSphere.radius * 2;
        canvasImageGraphics.fillOval(x, y, r, r);
        canvasImageGraphics.drawLine(addHorizontalString.x1, addHorizontalString.y1, 
            addHorizontalString.x2, addHorizontalString.y2);
        canvasImageGraphics.drawLine(addVerticalString.x1, addVerticalString.y1, 
            addVerticalString.x2, addVerticalString.y2);

        canvasImageGraphics.setFont(font);
        canvasImageGraphics.setColor(Color.black);
        s = "Drag and drop";
        canvasImageGraphics.drawString(s, 
            (d.width - fontMetrics.stringWidth(s)) / 2, (3 * d.height) / 4);
    }

    // Update palette canvas.
    public void updatePaletteCanvas()
    {
        Dimension d;
        int x, y, i, j, p, p1, p2, p3;

        d = canvasSize;
        p = paletteXY;
        p1 = paletteXY / 4;
        p2 = paletteXY / 2;
        p3 = (3 * paletteXY) / 4;
        if (!dragging)
        {
            // Draw palette.
            for (i = x = y = 0; music.palette.spherePaletteIndexes[i] != -1 &&
                 i < (music.MAX_SOUND_COLOR - 1); i++)
            {            
                j = music.palette.spherePaletteIndexes[i];
                canvasImageGraphics.setColor(music.palette.soundColors[j]);
                canvasImageGraphics.fillRect(x, y, p, p);
                canvasImageGraphics.setColor(Color.white);
                canvasImageGraphics.drawOval(x + p1, y + p1, p2, p2);
                x += p;
                if (x > (d.width - p))
                {
                    x = 0;
                    y += p;
                }
            }
            canvasImageGraphics.setColor(Color.black);    // Black for delete.
            canvasImageGraphics.fillRect(x, y, p, p);
            canvasImageGraphics.setColor(Color.white);
            canvasImageGraphics.drawOval(x + p1, y + p1, p2, p2);
            x += p;
            if (x > (d.width - p))
            {
                x = 0;
                y += p;
            }
            for (i = 0; music.palette.stringPaletteIndexes[i] != -1 &&
                 i < (music.MAX_SOUND_COLOR - 1); i++)
            {            
                j = music.palette.stringPaletteIndexes[i];
                canvasImageGraphics.setColor(music.palette.soundColors[j]);
                canvasImageGraphics.fillRect(x, y, p, p);
                canvasImageGraphics.setColor(Color.white);
                canvasImageGraphics.drawLine(x + p2, y + p1, x + p2, y + p3);
                x += p;
                if (x > (d.width - p))
                {
                    x = 0;
                    y += p;
                }
            }
            canvasImageGraphics.setColor(Color.black);    // Black for delete.
            canvasImageGraphics.fillRect(x, y, p, p);
            canvasImageGraphics.setColor(Color.white);
            canvasImageGraphics.drawLine(x + p2, y + p1, x + p2, y + p3);
            canvasImageGraphics.setColor(Color.white);
            for (i = p; i < d.width; i += p)
            {
                canvasImageGraphics.drawLine(i, 0, i, d.height);
            }
            for (i = p; i < d.height; i += p)
            {
                canvasImageGraphics.drawLine(0, i, d.width, i);
            }

        } else {    // Draw dragging palette item.

            updateNormalCanvas();
            if (paletteSphere.paletteIndex != -1)
            {
                if (paletteSphere.paletteIndex == music.MAX_SOUND_COLOR)
                {
                    canvasImageGraphics.setColor(Color.black);
                } else {
                    canvasImageGraphics.setColor(music.palette.soundColors[paletteSphere.paletteIndex]);
                }
                x = paletteSphere.x;
                y = paletteSphere.y;
                canvasImageGraphics.fillRect(x - p2, y - p2, p, p);
                canvasImageGraphics.setColor(Color.white);
                canvasImageGraphics.drawOval(x - p1 , y - p1, p2, p2);
            }
            if (paletteString.paletteIndex != -1)
            {
                if (paletteString.paletteIndex == music.MAX_SOUND_COLOR)
                {
                    canvasImageGraphics.setColor(Color.black);
                } else {
                    canvasImageGraphics.setColor(music.palette.soundColors[paletteString.paletteIndex]);
                }
                x = (paletteString.x2 + paletteString.x1) / 2;
                y = (paletteString.y2 + paletteString.y1) / 2;
                canvasImageGraphics.fillRect(x - p2, y - p2, p, p);
                canvasImageGraphics.setColor(Color.white);
                canvasImageGraphics.drawLine(paletteString.x1, paletteString.y1, 
                    paletteString.x2, paletteString.y2);
            }
        }
    }

    // Scale sphere velocity to minimum radius (for drawing).
    // Result in sdx, sdy.
    void scaleSphereVelocity(double dx, double dy)
    {
        double d, a;

        sdx = dx;
        sdy = dy;
        d = Math.sqrt((sdx * sdx) + (sdy * sdy));
        d = (d / (double)music.MAX_SPHERE_SPEED) * (double)music.MIN_SPHERE_RADIUS;
        if (Math.abs(sdx) > 0.0)
        {
            a = Math.atan(Math.abs(sdy / sdx));
            sdy = d * Math.sin(a);
            sdx = d * Math.cos(a);
        } else {
            if (Math.abs(sdy) > 0.0)
            {
                sdx = 0.0;
                sdy = d;
            } else {
                sdx = 0.0;
                sdy = 0.0;
            }
        }
        if (dx < 0.0)
        {
            sdx = -sdx;
        }
        if (dy < 0.0)
        {
            sdy = -sdy;
        }
    }

    // Load sounds if necessary by playing and immediately stopping them.
    void loadsounds()
    {
        int i;

        for (i = 0; music.palette.soundURLs[i] != null; i++)
        {
            if (music.palette.soundClips[i] == null)
            {
                music.palette.soundClips[i] = Applet.newAudioClip(music.palette.soundURLs[i]);
                music.palette.soundClips[i].play();
                music.palette.soundClips[i].stop();
            }
        }
    }
}
