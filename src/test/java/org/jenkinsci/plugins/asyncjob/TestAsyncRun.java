package org.jenkinsci.plugins.asyncjob;

import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.TaskListener;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 * @author Kohsuke Kawaguchi
 */
public class TestAsyncRun extends AsyncRun<TestAsyncJob,TestAsyncRun> {
    private String referenceToStuffGoingOnOutsieJenkins;

    public TestAsyncRun(TestAsyncJob job) throws IOException {
        super(job);
    }

    public TestAsyncRun(TestAsyncJob project, File buildDir) throws IOException {
        super(project, buildDir);
    }

    @Override
    public void doStop(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        {
            // This is where you talk to the external system to actually abort stuff
            System.out.println("aborting");
            abortStuffOutsideJenkins(referenceToStuffGoingOnOutsieJenkins);

            if (false) {
                // if you couldn't abort...
                // this would still keep the build marked as running
                // alternatively maybe you want to call markCompleted(FAILURE) to mark it as failed?
                throw new IOException("Failed to abort");
            }
        }

        markCompleted(Result.ABORTED);
        rsp.forwardToPreviousPage(req);
    }

    private void abortStuffOutsideJenkins(String referenceToStuffGoingOnOutsieJenkins) {
        // in real code, something interesting happens here
    }

    public void run() {
        run(new Runner() {
            @Override
            public Result run(BuildListener listener) throws Exception {
                referenceToStuffGoingOnOutsieJenkins = launchSomeActivityOutsideJenkins(listener);
                return Result.SUCCESS;
            }

            private String launchSomeActivityOutsideJenkins(BuildListener listener) {
                listener.getLogger().println("Launching something here");
                // in real code, something interesting happens here
                return UUID.randomUUID().toString();
            }

            @Override
            public void post(BuildListener listener) throws Exception {
            }

            @Override
            public void cleanUp(BuildListener listener) throws Exception {
            }
        });
    }

    public HttpResponse doComplete(@QueryParameter String result) throws IOException {
        markCompleted(Result.fromString(result));
        return HttpResponses.redirectToDot();
    }

    public HttpResponse doWriteLog() throws IOException {
        TaskListener l = createListener();
        l.getLogger().println("Current time is "+new Date());
        return HttpResponses.redirectTo("console");
    }
}
