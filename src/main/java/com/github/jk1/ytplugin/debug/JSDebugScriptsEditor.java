package com.github.jk1.ytplugin.debug;

import com.github.jk1.ytplugin.ComponentAware;
import com.github.jk1.ytplugin.scriptsDebug.JSRemoteScriptsDebugConfiguration;
import com.github.jk1.ytplugin.tasks.YouTrackServer;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.browsers.*;
import com.intellij.ide.browsers.impl.WebBrowserServiceImpl;
import com.intellij.javascript.debugger.JSDebuggerBundle;
import com.intellij.javascript.debugger.JavaScriptDebugEngine;
import com.intellij.javascript.debugger.JavaScriptDebugEngineKt;
import com.intellij.javascript.debugger.execution.JSLocalFilesMappingPanel;
import com.intellij.javascript.debugger.execution.JavaScriptDebugConfiguration;
import com.intellij.javascript.debugger.execution.RemoteUrlMappingBean;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.DocumentAdapter;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.Url;
import com.intellij.util.io.URLUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.debugger.sourcemap.SourceResolver;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class JSDebugScriptsEditor extends SettingsEditor<JSRemoteScriptsDebugConfiguration> {

  protected final Project project;
  private JPanel mainPanel;
  private TextFieldWithBrowseButton folderField;


  public JSDebugScriptsEditor(@NotNull Project project) {
    this.project = project;
  }

  protected void resetEditorFrom(@NotNull JSRemoteScriptsDebugConfiguration configuration) {

    String userFolder = configuration.getFolder();
    folderField.setText(userFolder);

  }

  @Override
  protected void applyEditorTo(JSRemoteScriptsDebugConfiguration configuration) {
    List<YouTrackServer> repositories = ComponentAware.Companion.of(project).getTaskManagerComponent().getAllConfiguredYouTrackRepositories();
    if (!repositories.isEmpty()) {
//      logger.info("Apply Editor: $host, $port");
      try {
        configuration.setHost(new URL(repositories.get(0).getUrl()).getHost());
        configuration.setPort(new URL(repositories.get(0).getUrl()).getPort());

      } catch (MalformedURLException e) {
        e.printStackTrace();
      }
      configuration.setFolder(folderField.getText());
    }
  }

  public static void setupUrlField(@NotNull TextFieldWithBrowseButton field, @NotNull final Project project) {
    FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false) {
      @Override
      public boolean isFileSelectable(VirtualFile file) {
        return WebBrowserXmlService.getInstance().isHtmlFile(file) || virtualFileToUrl(file, project) != null;
      }
    };
    descriptor.setTitle(IdeBundle.message("javascript.debugger.settings.choose.file.title"));
    descriptor.setDescription(IdeBundle.message("javascript.debugger.settings.choose.file.subtitle"));
    descriptor.setRoots(ProjectRootManager.getInstance(project).getContentRoots());

    field.addBrowseFolderListener(new TextBrowseFolderListener(descriptor, project) {
      @NotNull
      @Override
      protected String chosenFileToResultingText(@NotNull VirtualFile chosenFile) {
        if (chosenFile.isDirectory()) {
          return chosenFile.getPath();
        }

        Url url = virtualFileToUrl(chosenFile, project);
        return url == null ? chosenFile.getUrl() : url.toDecodedForm();
      }
    });
  }

  @Nullable
  private static Url virtualFileToUrl(@NotNull VirtualFile file, @NotNull Project project) {
    PsiFile psiFile = ReadAction.compute(() -> PsiManager.getInstance(project).findFile(file));
    return WebBrowserServiceImpl.getDebuggableUrl(psiFile);
  }


  @Override
  @NotNull
  protected JComponent createEditor() {
    setupUrlField(folderField, project);
//    localFilesMappingPanel.initUI();
    return mainPanel;
  }

  {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
    $$$setupUI$$$();
  }

  /**
   * Method generated by IntelliJ IDEA GUI Designer
   * >>> IMPORTANT!! <<<
   * DO NOT edit this method OR call it in your code!
   *
   * @noinspection ALL
   */
  private void $$$setupUI$$$() {
    mainPanel = new JPanel();
    mainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
    final JPanel panel1 = new JPanel();
    panel1.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
    mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    folderField = new TextFieldWithBrowseButton();
    folderField.setText("youtrack-scripts");
    panel1.add(folderField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JLabel label1 = new JLabel();
    label1.setText("Load scripts to folder:");
    panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    final JPanel panel2 = new JPanel();
    panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
    panel1.add(panel2, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    label1.setLabelFor(folderField);
  }

  /**
   * @noinspection ALL
   */
  public JComponent $$$getRootComponent$$$() {
    return mainPanel;
  }
}