package org.jenkinsci.plugins.asyncjob;

import hudson.model.BuildListener;
import hudson.model.Result;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;

import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class TestAsyncRun extends AsyncRun<TestAsyncJob,TestAsyncRun> {
    public TestAsyncRun(TestAsyncJob job) throws IOException {
        super(job);
    }

    public TestAsyncRun(TestAsyncJob project, File buildDir) throws IOException {
        super(project, buildDir);
    }

    public void run() {
        run(new Runner() {
            @Override
            public Result run(BuildListener listener) throws Exception {
                return Result.SUCCESS;
            }

            @Override
            public void post(BuildListener listener) throws Exception {
            }

            @Override
            public void cleanUp(BuildListener listener) throws Exception {
            }
        });
    }

    public HttpResponse doComplete() throws IOException {
        markCompleted();
        return HttpResponses.redirectToDot();
    }
}
