package com.github.jk1.ytplugin.timeTracker;

import com.github.jk1.ytplugin.ComponentAware;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.ConcurrentHashMap;

public class ConfirmDiscardDialog extends DialogWrapper {
    private JPanel contentPane;

    @NotNull
    private final Project project;
    private JBTable timeTrackerItemsTable;

    DiscardAction discardAction = new DiscardAction();

    public ConfirmDiscardDialog(@NotNull Project project, JBTable timeTrackerItemsTable) {
        super(project, true);
        this.project = project;
        this.timeTrackerItemsTable = timeTrackerItemsTable;
        setTitle("Discard Tracked Time");
        $$$setupUI$$$();
        init();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Are you sure you want to permanently discard selected items?");
        contentPane.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }


    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{discardAction, getCancelAction()};
    }


    protected class DiscardAction extends DialogWrapperAction {
        protected DiscardAction() {
            super("Discard");
        }

        @Override
        protected void doAction(ActionEvent e) {
            ConcurrentHashMap<String, Long> selectedItems =
                    AllSavedTimerItemsDialog.pickSelectedTimeTrackerItemsOnly(timeTrackerItemsTable, project);
            SpentTimePerTaskStorage storage = ComponentAware.Companion.of(project).getSpentTimePerTaskStorage();
            selectedItems.forEach((task, time) -> {
                storage.resetSavedTimeForLocalTask(task);
                TrackerNotification trackerNote = new TrackerNotification();
                trackerNote.notify("Discarded " + TimeTracker.Companion.formatTimePeriod(time) +
                        " of tracked time for " + task, NotificationType.INFORMATION);
            });

            close(0);
        }

    }
}
