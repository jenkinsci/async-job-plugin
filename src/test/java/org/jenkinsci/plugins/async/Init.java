package org.jenkinsci.plugins.async;

import org.kohsuke.stapler.MetaClass;

/**
 * @author Kohsuke Kawaguchi
 */
public class Init {
    void init() {
        MetaClass.NO_CACHE = true;
        hudson.Main.isUnitTest = false;
    }
}
