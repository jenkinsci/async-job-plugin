package org.jenkinsci.plugins.asyncjob;

import org.jvnet.hudson.test.HudsonHomeLoader;
import org.jvnet.hudson.test.HudsonTestCase;
import org.mortbay.jetty.bio.SocketConnector;

import java.io.File;

/**
 * Sample Main program. Run this and use http://localhost:8888/
 *
 * @author Kohsuke Kawaguchi
 */
public class Main extends HudsonTestCase {
    @Override
    protected void setUp() throws Exception {
        homeLoader = new HudsonHomeLoader() {
            public File allocate() throws Exception {
                return new File("./work");
            }
        };
        super.setUp();
        new Init().init();
    }

    public void test1() throws Exception {
        SocketConnector connector = new SocketConnector();
        connector.setPort(8888);
        connector.setHeaderBufferSize(12 * 1024); // use a bigger buffer as Stapler traces can get pretty large on deeply nested URL
        server.addConnector(connector);
        connector.start();

        if (jenkins.getItem("foo")==null)
            jenkins.createProject(TestAsyncJob.class, "foo");

        interactiveBreak();
    }
}
