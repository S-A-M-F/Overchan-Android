## ATTENTION!!!

This app - and many more - can end up nearly completely impossible to use in barely a few months for nearly all the modern Android users. _Thank Google for that._
[More info here](https://keepandroidopen.org/)

## Overchan Android

Overchan Android (Meta Imageboard Client) is an application for browsing imageboards.

[Releases](https://github.com/S-A-M-F/Overchan-Android/releases)  
[F-Droid](https://f-droid.org/repository/browse/?fdid=nya.miku.wishmaster) Currently deprecated - may change in future.

[Supported Imageboards](https://github.com/S-A-M-F/Overchan-Android/blob/master/Imageboards.md)  
[Custom Themes](https://github.com/S-A-M-F/Overchan-Themes)

## Acknowledgements

* **[Miku Nyan](https://github.com/miku-nyan)** — original creator of Overchan Android
* **[Eilhart](https://github.com/Eilhart)** — long-term maintenance and countless improvements
* **[AliceCA](https://github.com/AliceCA)** — significant contributions over the years
* **[Anonymous](https://www.youtube.com/watch?v=dQw4w9WgXcQ)** — one that never forgets of forgives - and not forgotten either
* **Kiririn**, **Kalaver**, **Kantrael**, **Sich**, **Weloxux**, **xD**, **Pwnicorn**, **Keiran Rowan**, **Andrey Bogdanov**, **hey-red**, **Klaster_1**, **Kohlchan**, **rngnrs**, **Chinese Dog** — you all were here at some point

## Building Source Code

### Dependencies

* [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html) (JRE alone is not sufficient)
* [Android SDK](https://developer.android.com/sdk/index.html#Other)
* [Android NDK](https://developer.android.com/tools/sdk/ndk/index.html#Downloads)

### Using Gradle / Android Studio

Open the source code directory and run (in the command line):

`gradlew.bat build`

Or import into **Android Studio** (File → New → Import Project).

The Gradle wrapper (`gradlew.bat`) will download the correct Gradle version automatically. The debug APK will be signed with the debug key.

For a release build:

`gradlew.bat assembleRelease`

## License

Overchan Android is licensed under the [GPLv3](http://www.gnu.org/licenses/gpl-3.0.txt).
