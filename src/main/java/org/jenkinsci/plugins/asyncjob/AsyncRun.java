package org.jenkinsci.plugins.asyncjob;

import hudson.model.BallColor;
import hudson.model.Executor;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Calendar;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class AsyncRun<P extends AsyncJob<P,R>, R extends AsyncRun<P,R>> extends Run<P,R> implements Queue.Executable {
    private boolean isAsyncCompleted;

    private transient TaskListener listener;

    protected AsyncRun(P job) throws IOException {
        super(job);
    }

    protected AsyncRun(P job, Calendar timestamp) {
        super(job, timestamp);
    }

    protected AsyncRun(P job, long timestamp) {
        super(job, timestamp);
    }

    protected AsyncRun(P project, File buildDir) throws IOException {
        super(project, buildDir);
    }

    /**
     * Marks the asynchronous task complete.
     */
    public void markCompleted(Result result) throws IOException {
        if (!isAsyncCompleted) {
            isAsyncCompleted = true;
            duration = System.currentTimeMillis()-getTimeInMillis();
            this.result = result;   // violation of the base class semantics
            save();
        }
    }

    @Override
    public boolean isBuilding() {
        return !isAsyncCompleted || super.isBuilding();
    }

    /**
     * Indicates if the actual asynchronous portion of the work has been completed
     * (by invoking {@link #markCompleted(Result)}.
     */
    public boolean isAsyncCompleted() {
        return isAsyncCompleted;
    }

    @Override
    public BallColor getIconColor() {
        if (isAsyncCompleted)
            return super.getIconColor();
        else {
            R pb = getPreviousBuild();
            if (pb!=null)   return pb.getIconColor().anime();
            return BallColor.GREY_ANIME;
        }
    }

    /**
     * For writing stuff from asynchronous tasks.
     */
    public synchronized TaskListener createListener() throws IOException {
        if (listener==null)
            listener = new StreamTaskListener(new FileOutputStream(getLogFile(),true), Charset.defaultCharset());
        return listener;
    }

    /**
     * Aborts the build if it's indeed in progress.
     */
    public abstract void doStop(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException;

}
