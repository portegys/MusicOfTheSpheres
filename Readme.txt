Music of the Spheres.

Labeled with color/sound combinations, bouncing spheres and strings
produce compositions of sound, color and motion.  Includes a Player
JAVA applet and Composer JAVA application.

Contains:

1. Source code in src directory.
2. Sound/color palettes in *Palette directories.
3. Sample music in repository directory.

To build:

CLASSPATH=<current directory path>:$CLASSPATH
cd src
javac Music.java Palette.java
mv *.class ..
javac Player.java
mv *.class ..
javac Composer.java
mv *.class ..
cd ..

To test Player applet:

appletviewer index.html

To test Composer application:

1. java Composer
2. Select File tab.
3. Load repository/sample.dat music.
4. Select Play tab.
5. To modify, select Compose tab.
