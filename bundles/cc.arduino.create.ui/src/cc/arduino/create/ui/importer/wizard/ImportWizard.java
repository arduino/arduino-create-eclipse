/**
 * Copyright (C) 2019 TypeFox and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package cc.arduino.create.ui.importer.wizard;

import static org.eclipse.jface.dialogs.IMessageProvider.ERROR;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import cc.arduino.create.ui.UIActivator;
import cc.arduino.create.ui.importer.operation.ImportProjectOperation;

public class ImportWizard extends Wizard implements IImportWizard {

    private static final String DIALOG_SETTINGS_ID = ImportWizard.class.getName();

    private final ImportWizardModel model;
    private final ImportWizardPage page;

    private IDialogSettings wizardSettings;

    public ImportWizard() {
        this.model = new ImportWizardModel();
        setNeedsProgressMonitor(true);
        page = new ImportWizardPage(model);

        IDialogSettings workbenchSettings = UIActivator.getDefault().getDialogSettings();
        wizardSettings = workbenchSettings.getSection(DIALOG_SETTINGS_ID);
        if (wizardSettings == null) {
            wizardSettings = workbenchSettings.addNewSection(DIALOG_SETTINGS_ID);
        }
        setDialogSettings(wizardSettings);
        model.restoreState(wizardSettings);
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        // NOOP
    }

    @Override
    public void addPages() {
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        try {
            getContainer().run(true, false, monitor -> new ImportProjectOperation(model).run(monitor));
            storeState();
            return true;
        } catch (InvocationTargetException | InterruptedException e) {
            String message = e.getMessage();
            if (e instanceof InvocationTargetException) {
                message = ((InvocationTargetException) e).getTargetException().getMessage();
            }
            IWizardPage currentPage = getContainer().getCurrentPage();
            if (currentPage instanceof DialogPage) {
                ((DialogPage) currentPage).setMessage(message, ERROR);
            } else {
                currentPage.setDescription(message);
            }
        }
        return false;
    }

    private void storeState() {
        page.storeWidgetState();
        model.storeState(wizardSettings);
    }

}
