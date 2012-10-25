package org.jenkinsci.plugins.asyncjob;

import hudson.Functions;
import hudson.model.AbstractProject;
import hudson.model.Cause.UserIdCause;
import hudson.model.CauseAction;
import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Queue.Task;
import hudson.model.ResourceList;
import hudson.model.RunMap;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.SubTask;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedMap;

import static javax.servlet.http.HttpServletResponse.*;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class AsyncJob<P extends AsyncJob<P,R>, R extends AsyncRun<P,R>>
        extends Job<P,R> implements hudson.model.Queue.FlyweightTask {

    // keep track of the previous time we started a build
    private transient long lastBuildStartTime;

    private transient /*almost final*/ RunMap<R> builds = new RunMap<R>();

    protected AsyncJob(ItemGroup parent, String name) {
        super(parent, name);
    }

    /**
     * Schedules a new build command.
     */
    public void doBuild( StaplerRequest req, StaplerResponse rsp ) throws IOException, ServletException {
        // if a build is parameterized, let that take over
        ParametersDefinitionProperty pp = getProperty(ParametersDefinitionProperty.class);
        if (pp != null) {
            pp._doBuild(req,rsp,null);
            return;
        }

        if (!isBuildable())
            throw HttpResponses.error(SC_INTERNAL_SERVER_ERROR, new IOException(getFullName() + " is not buildable"));

        Jenkins.getInstance().getQueue().schedule(this, getDelay(req), new CauseAction(new UserIdCause()));
        rsp.forwardToPreviousPage(req);
    }

    /**
     * Computes the delay by taking the default value and the override in the request parameter into the account.
     *
     * TODO: use TimeDuration and inject it as a parameter
     */
    private int getDelay(StaplerRequest req) throws ServletException {
        String delay = req.getParameter("delay");
        if (delay==null)    return Jenkins.getInstance().getQuietPeriod();

        try {
            // TODO: more unit handling
            if(delay.endsWith("sec"))   delay=delay.substring(0,delay.length()-3);
            if(delay.endsWith("secs"))  delay=delay.substring(0,delay.length()-4);
            return Integer.parseInt(delay);
        } catch (NumberFormatException e) {
            throw new ServletException("Invalid delay parameter value: "+delay);
        }
    }

    /**
     * Most subtypes probably want to support concurrent builds,
     * although some may want to override.
     */
    public boolean isConcurrentBuild() {
        return true;
    }

    @Override
    public boolean isBuildable() {
        return true;
    }

    public final boolean isBuildBlocked() {
        return getCauseOfBlockage()!=null;
    }

    public final String getWhyBlocked() {
        return getCauseOfBlockage().getShortDescription();
    }

    public void checkAbortPermission() {
        checkPermission(AbstractProject.ABORT);
    }

    public boolean hasAbortPermission() {
        return hasPermission(AbstractProject.ABORT);
    }

    public Collection<? extends SubTask> getSubTasks() {
        return Collections.emptyList();
    }

    /**
     * This is the master task.
     */
    public Task getOwnerTask() {
        return this;
    }

    /**
     * No constraint because we don't care where to run.
     */
    public Object getSameNodeConstraint() {
        return null;
    }

    /**
     * Because we are modeling an asynchronous task that runs elsewhere,
     * we don't care where we run.
     */
    public Label getAssignedLabel() {
        return null;
    }

    /**
     * Flyweight task doesn't really care where to run.
     */
    public Node getLastBuiltOn() {
        return null;
    }

    public CauseOfBlockage getCauseOfBlockage() {
        // never blocks on anything
        return null;
    }

    /**
     * Determines Class&lt;R>.
     */
    protected Class<R> getBuildClass() {
        return Functions.getTypeParameter(getClass(), AsyncJob.class,1);
    }

    public synchronized R createExecutable() throws IOException {
    	// make sure we don't start two builds in the same second
    	// so the build directories will be different too
    	long timeSinceLast = System.currentTimeMillis() - lastBuildStartTime;
    	if (timeSinceLast < 1000) {
    		try {
				Thread.sleep(1000 - timeSinceLast);
			} catch (InterruptedException e) {
			}
    	}
    	lastBuildStartTime = System.currentTimeMillis();
        try {
            R lastBuild = getBuildClass().getConstructor(getClass()).newInstance(this);
            builds.put(lastBuild);
            return lastBuild;
        } catch (InstantiationException e) {
            throw new Error(e);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        } catch (InvocationTargetException e) {
            throw new Error(e);
        } catch (NoSuchMethodException e) {
            throw new Error(e);
        }
    }

    @Override
    protected SortedMap<Integer,R> _getRuns() {
        return builds;
    }

    @Override
    protected void removeRun(R run) {
        builds.remove(run);
    }

    /**
     * Default implementation: no constraint.
     */
    public ResourceList getResourceList() {
        return new ResourceList();
    }
}