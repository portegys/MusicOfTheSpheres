javac -d . src/*.java
jar cfm Composer.jar composer.mf *.class
jar cfm Player.jar player.mf *.class
rm *.class




