// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.jk1.ytplugin.setup;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.extractMethod.ExtractMethodSettings;
import com.intellij.refactoring.ui.MethodSignatureComponent;
import com.intellij.refactoring.util.AbstractVariableData;
import com.intellij.refactoring.util.SimpleParameterTablePanel;
import com.intellij.ui.DocumentAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewSetupDialog extends DialogWrapper implements ExtractMethodSettings {
  private JPanel rootPane;
  private JTabbedPane mainPane;
  private JTextField inputUrlTextPane;
  private JPasswordField inputTokenField;
  private JRadioButton isAutoTrackingEnabledRadioButton;
  private JRadioButton isManualModeRadioButton;
  private JRadioButton noTrackingButton;
  private JCheckBox postWhenProjectClosedCheckbox;
  private JCheckBox postWhenCommitCheckbox;
  private JCheckBox onASetScheduleCheckBox;
  private JButton saveButton;
  private JButton cancelButton;
  private JButton testConnectionButton;
  private JButton proxyButton;
  private JButton okButton;
  private JButton cancelButton1;
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
  private final Project myProject;
  private final String myDefaultName;

  private AbstractVariableData[] myVariableData;
  private Map<String, AbstractVariableData> myVariablesMap;

  private SetupRepositoryConnector repoConnector = new SetupRepositoryConnector();

  public NewSetupDialog(final Project project) {
    super(project, true);
    myProject = project;
    myDefaultName = "YouTrack";

    $$$setupUI$$$();

    init();
  }

  private void $$$setupUI$$$() {
  }
  @Override
  public void show() {
    init();
    super.show();
  }

  @Override
  protected void init() {
    super.init();
    // Set default name and select it
    myMethodNameTextField.setText(myDefaultName);
    myMethodNameTextField.setSelectionStart(0);
    myMethodNameTextField.setSelectionStart(myDefaultName.length());
    myMethodNameTextField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
      }
    });


    myVariablesMap = createVariableMap(myVariableData);
    myParametersPanel.init(myVariableData);
  }



  @Override
  public JComponent getPreferredFocusedComponent() {
    return myMethodNameTextField;
  }

  public static AbstractVariableData[] createVariableDataByNames(final List<String> args) {
    final AbstractVariableData[] datas = new AbstractVariableData[args.size()];
    for (int i = 0; i < args.size(); i++) {
      final AbstractVariableData data = new AbstractVariableData();
      final String name = args.get(i);
      data.originalName = name;
      data.name = name;
      data.passAsParameter = true;
      datas[i] = data;
    }
    return datas;
  }

  public static Map<String, AbstractVariableData> createVariableMap(final AbstractVariableData[] data) {
    final HashMap<String, AbstractVariableData> map = new HashMap<>();
    for (AbstractVariableData variableData : data) {
      map.put(variableData.getOriginalName(), variableData);
    }
    return map;
  }

  @Override
  protected Action @NotNull [] createActions() {
    return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
  }

  @Override
  protected void doOKAction() {
      if (ApplicationManager.getApplication().isUnitTestMode()){
        return;
      }
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
    return rootPane;
  }


  @NotNull
  private String getPersistenceId() {
    return "visibility.combobox." + getClass().getName();
  }



  @NotNull
  @Override
  public String getMethodName() {
    return myMethodNameTextField.getText().trim();
  }

  @Override
  public AbstractVariableData @NotNull [] getAbstractVariableData() {
    return myVariableData;
  }

  @Nullable
  @Override
  public Object getVisibility() {
    return null;
  }

}