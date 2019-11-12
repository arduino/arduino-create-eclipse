/**
 * Copyright (C) 2019 Arduino SA and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package cc.arduino.create.ui;

import static com.google.common.base.Suppliers.memoize;
import static java.io.File.separator;
import static org.eclipse.jface.resource.ImageDescriptor.getMissingImageDescriptor;
import static org.eclipse.ui.plugin.AbstractUIPlugin.imageDescriptorFromPlugin;

import java.util.concurrent.ExecutionException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public enum ImageDescriptorCache {

    INSTANCE;

    private static final String PLUGIN_ID = UIActivator.getDefault().getBundle().getSymbolicName();
    private static final String ICON_FOLDER = "icons";
    private static final Supplier<Image> MISSING = memoize(() -> getMissingImageDescriptor().createImage());
    private static final LoadingCache<ImageDescriptor, Image> CACHE = CacheBuilder.newBuilder()
            .build(new ImageDescriptorCacheLoader());

    /**
     * Do <b>not</b> {@link Resource#dispose() dispose} the images retrieved via {@link ImageRef#asImage()}. The SWT
     * rule of thumb applies here as well; if you create an {@link Resource SWT Resource}, you have to dispose it.
     * Otherwise, no.
     */
    public static enum ImageRef {

        ARDUINO("arduino.png"),

        IMPORT_WIZARD_BANNER("import_wiz.png");

        private static final Logger LOGGER = LoggerFactory.getLogger(ImageRef.class);

        private final String fileName;

        private ImageRef(final String fileName) {
            this.fileName = fileName;
        }

        public ImageDescriptor asImageDescriptor() {
            return ImageDescriptorCache.INSTANCE.getImageDescriptor(this);
        }

        public Image asImage() {
            try {
                return ImageDescriptorCache.CACHE.get(asImageDescriptor());
            } catch (final ExecutionException e) {
                LOGGER.warn("Error when trying to get image from image descriptor of: " + this);
                return MISSING.get();
            }
        }

    }

    private synchronized ImageDescriptor getImageDescriptor(ImageRef ref) {
        if (UIActivator.getDefault() == null || UIActivator.getDefault().getImageRegistry() == null) {
            return null;
        }
        ImageRegistry registry = UIActivator.getDefault().getImageRegistry();
        ImageDescriptor descriptor = registry.getDescriptor(ref.fileName);
        if (descriptor == null) {
            final String imageFilePath = ICON_FOLDER + separator + ref.fileName;
            descriptor = imageDescriptorFromPlugin(PLUGIN_ID, imageFilePath);
            registry.put(ref.fileName, descriptor);
        }
        return descriptor;
    }

    private static final class ImageDescriptorCacheLoader extends CacheLoader<ImageDescriptor, Image> {

        @Override
        public Image load(ImageDescriptor key) throws Exception {
            return null == key ? MISSING.get() : key.createImage();
        }

    }

}
