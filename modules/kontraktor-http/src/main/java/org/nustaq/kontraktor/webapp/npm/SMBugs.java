package org.nustaq.kontraktor.webapp.npm;

import com.github.jknack.semver.Semver;

public class SMBugs {

    public static void main(String[] args) {
        System.out.println(Semver.create(">=15.0.0 <16.0.0").matches("16.0.0-beta.5")+" should be false");
        System.out.println(Semver.create(">=15.0.0 <16.0.0").matches("16.0.0")+" should be false");
    }
}
