// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.jk1.ytplugin.setup;

import com.github.jk1.ytplugin.ComponentAware;
import com.github.jk1.ytplugin.YouTrackPluginApiService;
import com.github.jk1.ytplugin.commands.ICommandService;
import com.github.jk1.ytplugin.issues.IssueStoreUpdaterService;
import com.github.jk1.ytplugin.issues.PersistentIssueStore;
import com.github.jk1.ytplugin.navigator.SourceNavigatorService;
import com.github.jk1.ytplugin.tasks.TaskManagerProxyService;
import com.github.jk1.ytplugin.tasks.YouTrackServer;
import com.github.jk1.ytplugin.timeTracker.IssueWorkItemsStoreUpdaterService;
import com.github.jk1.ytplugin.timeTracker.PersistentIssueWorkItemsStore;
import com.github.jk1.ytplugin.timeTracker.TimeTracker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.ui.MethodSignatureComponent;
import com.intellij.refactoring.util.AbstractVariableData;
import com.intellij.refactoring.util.SimpleParameterTablePanel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Map;

public class NewSetupDialog extends DialogWrapper implements ComponentAware {
  private JPanel myRootPane;
  private JTabbedPane mainPane;
  private JTextField inputUrlTextPane;
  private JPasswordField inputTokenField;
  private JRadioButton isAutoTrackingEnabledRadioButton;
  private JRadioButton isManualModeRadioButton;
  private JRadioButton noTrackingButton;
  private JCheckBox postWhenProjectClosedCheckbox;
  private JCheckBox postWhenCommitCheckbox;
  private JCheckBox onASetScheduleCheckBox;
  private JButton testConnectionButton;
  private JButton proxyButton;
  private JLabel serverUrlLabel;
  private JLabel advertiserLabel;
  private JLabel getTokenInfoLabel;
  private JCheckBox shareUrlCheckBox;
  private JCheckBox useProxyCheckBox;
  private JLabel tokenLabel;
  private JLabel notifyFieldLabel;
  private JPanel autoPanel;
  private JPanel trackingModePanel;
  private JLabel typeLabel;
  private JTextField commentTextField;
  private JLabel commentLabel;
  private JComboBox typeComboBox;
  private JPanel preferencesPanel;
  private JTextField inactivityHourInputField;
  private JTextField inactivityMinutesInputField;
  private JLabel hourLabel1;
  private JLabel minuteLabel1;
  private JPanel inactivityPeriodPanel;
  private JLabel inactivityTextField;
  private JTextField scheduledHour;
  private JTextField scheduledMinutes;
  private JLabel minuteLabel2;
  private JLabel hourLabel2;
  private JPanel timePanel;
  private JPanel timeTrackingTab;
  private JPanel connectionTab;
  private SimpleParameterTablePanel myParametersPanel;
  private JTextField myMethodNameTextField;
  private MethodSignatureComponent mySignaturePreviewTextArea;
  private JTextArea myOutputVariablesTextArea;
//  private Project myProject;
//  private String myDefaultName;

  private boolean fromTracker;


  @NotNull
  private Project project;

  @NotNull
  private  YouTrackServer repo;

  private SetupRepositoryConnector repoConnector = new SetupRepositoryConnector();


  private void $$$setupUI$$$() {
  }


  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return myRootPane;
  }

  public NewSetupDialog(@NotNull Project project) {
    super(project, true);
    setTitle("YouTrack");
    init();
  }

//  @Override
//  protected void init() {
//    super.init();
//  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myRootPane;
  }

  @Override
  protected Action @NotNull [] createActions() {
    return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
  }

  @Override
  protected void doOKAction() {
    if (Messages.showOkCancelDialog( RefactoringBundle.message("do.you.wish.to.continue"), RefactoringBundle.message("warning.title"), Messages.getWarningIcon()) != Messages.OK){
      return;
    }

    super.doOKAction();
  }

  @Override
  protected String getHelpId() {
    return "refactoring.extractMethod";
  }

  @Override
  protected JComponent createCenterPanel() {
    return myRootPane;
  }

  @NotNull
  public Project getProject() {
    return this.project;
  }

  @NotNull
  public final YouTrackServer getRepo() {
    return this.repo;
  }

  public final boolean getFromTracker() {
    return this.fromTracker;
  }

  @NotNull
  public TaskManagerProxyService getTaskManagerComponent() {
    return DefaultImpls.getTaskManagerComponent(this);
  }

  @NotNull
  public ICommandService getCommandComponent() {
    return DefaultImpls.getCommandComponent(this);
  }

  @NotNull
  public SourceNavigatorService getSourceNavigatorComponent() {
    return DefaultImpls.getSourceNavigatorComponent(this);
  }

  @NotNull
  public PersistentIssueWorkItemsStore getIssueWorkItemsStoreComponent() {
    return DefaultImpls.getIssueWorkItemsStoreComponent(this);
  }

  @NotNull
  public IssueWorkItemsStoreUpdaterService getIssueWorkItemsUpdaterComponent() {
    return DefaultImpls.getIssueWorkItemsUpdaterComponent(this);
  }

  @NotNull
  public PersistentIssueStore getIssueStoreComponent() {
    return DefaultImpls.getIssueStoreComponent(this);
  }

  @NotNull
  public IssueStoreUpdaterService getIssueUpdaterComponent() {
    return DefaultImpls.getIssueUpdaterComponent(this);
  }

  @NotNull
  public YouTrackPluginApiService getPluginApiComponent() {
    return DefaultImpls.getPluginApiComponent(this);
  }

  @NotNull
  public TimeTracker getTimeTrackerComponent() {
    return DefaultImpls.getTimeTrackerComponent(this);
  }

  @NotNull
  public CredentialsChecker getCredentialsCheckerComponent() {
    return DefaultImpls.getCredentialsCheckerComponent(this);
  }


}