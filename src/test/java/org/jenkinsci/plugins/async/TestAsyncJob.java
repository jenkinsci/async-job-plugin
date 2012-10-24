package org.jenkinsci.plugins.async;

import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.model.Queue.FlyweightTask;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import jenkins.model.Jenkins;

/**
 * @author Kohsuke Kawaguchi
 */
public class TestAsyncJob extends AsyncJob<TestAsyncJob,TestAsyncRun> implements FlyweightTask, TopLevelItem {
    public TestAsyncJob(ItemGroup parent, String name) {
        super(parent, name);
    }

    public TopLevelItemDescriptor getDescriptor() {
        return (TopLevelItemDescriptor)Jenkins.getInstance().getDescriptor(getClass());
    }

    @Extension
    public static class DescriptorImpl extends TopLevelItemDescriptor {
        @Override
        public String getDisplayName() {
            return "foobar";
        }

        @Override
        public TopLevelItem newInstance(ItemGroup parent, String name) {
            return new TestAsyncJob(parent,name);
        }
    }
}
