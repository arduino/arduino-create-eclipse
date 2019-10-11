package cc.arduino.create.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class UIActivator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "cc.arduino.create.ui"; //$NON-NLS-1$

    private static UIActivator plugin;

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

    public static UIActivator getDefault() {
        return plugin;
    }

}
