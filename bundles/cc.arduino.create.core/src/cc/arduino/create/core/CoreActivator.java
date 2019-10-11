package cc.arduino.create.core;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class CoreActivator extends Plugin {

    public static final String PLUGIN_ID = "cc.arduino.create.core"; //$NON-NLS-1$

    private static CoreActivator plugin;

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }

    public static CoreActivator getDefault() {
        return plugin;
    }

}
