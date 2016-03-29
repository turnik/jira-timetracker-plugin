/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.jira.reporting.plugin.web;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.everit.jira.reporting.plugin.ReportingPlugin;
import org.everit.jira.timetracker.plugin.JiraTimetrackerAnalytics;
import org.everit.jira.timetracker.plugin.JiraTimetrackerPlugin;
import org.everit.jira.timetracker.plugin.dto.ReportingSettingsValues;
import org.everit.jira.timetracker.plugin.util.JiraTimetrackerUtil;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.web.action.JiraWebActionSupport;

/**
 * Admin settings page.
 */
public class ReportingSettingsWebAction extends JiraWebActionSupport {

  private static final int DEFAULT_PAGE_SIZE = 20;

  private static final String FREQUENT_FEEDBACK = "jttp.plugin.frequent.feedback";

  private static final String JIRA_HOME_URL = "/secure/Dashboard.jspa";

  private static final String NOT_RATED = "Not rated";

  /**
   * Serial version UID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * The first day of the week.
   */
  private String contextPath;

  private boolean feedBackSendAviable;

  private boolean isUseNoWorks;

  private JiraTimetrackerPlugin jiraTimetrackerPlugin;

  /**
   * The message.
   */
  private String message = "";

  private int pageSize;

  private List<String> reportingGroups;

  private ReportingPlugin reportingPlugin;

  public ReportingSettingsWebAction(
      final JiraTimetrackerPlugin jiraTimetrackerPlugin, final ReportingPlugin reportingPlugin) {
    this.jiraTimetrackerPlugin = jiraTimetrackerPlugin;
    this.reportingPlugin = reportingPlugin;
  }

  private void checkMailServer() {
    feedBackSendAviable = ComponentAccessor.getMailServerManager().isDefaultSMTPMailServerDefined();
  }

  @Override
  public String doDefault() throws ParseException {
    boolean isUserLogged = JiraTimetrackerUtil.isUserLogged();
    if (!isUserLogged) {
      setReturnUrl(JIRA_HOME_URL);
      return getRedirect(NONE);
    }
    normalizeContextPath();
    checkMailServer();
    // TODO add Reporting settigns load part

    loadPluginSettingAndParseResult();

    return INPUT;
  }

  @Override
  public String doExecute() throws ParseException {
    boolean isUserLogged = JiraTimetrackerUtil.isUserLogged();
    if (!isUserLogged) {
      setReturnUrl(JIRA_HOME_URL);
      return getRedirect(NONE);
    }
    normalizeContextPath();
    checkMailServer();

    loadPluginSettingAndParseResult();

    if (getHttpRequest().getParameter("sendfeedback") != null) {
      String feedbacktResult = parseFeedback();
      if (feedbacktResult != null) {
        return feedbacktResult;
      }
    }

    if (getHttpRequest().getParameter("savesettings") != null) {
      String parseResult = parseSaveSettings(getHttpRequest());
      if (parseResult != null) {
        return parseResult;
      }
      savePluginSettings();
      // TODO FIXME later set Reporting
      setReturnUrl("/secure/JiraTimetrackerWebAction!default.jspa");
      return getRedirect(INPUT);
    }
    setReturnUrl("/secure/admin/JiraTimetrackerReportingSettingsWebAction!default.jspa");
    return getRedirect(INPUT);
  }

  public String getContextPath() {
    return contextPath;
  }

  public boolean getFeedBackSendAviable() {
    return feedBackSendAviable;
  }

  public boolean getIsUseNoWork() {
    return isUseNoWorks;
  }

  public String getMessage() {
    return message;
  }

  public int getPageSize() {
    return pageSize;
  }

  public List<String> getReportingGroups() {
    return reportingGroups;
  }

  /**
   * Load the plugin settings and set the variables.
   */
  public void loadPluginSettingAndParseResult() {
    ReportingSettingsValues pluginSettingsValues = reportingPlugin
        .loadReportingSettings();
    isUseNoWorks = pluginSettingsValues.isUseNoWorks;
    reportingGroups = pluginSettingsValues.reportingGroups;
    pageSize = pluginSettingsValues.pageSize;
  }

  private void normalizeContextPath() {
    String path = getHttpRequest().getContextPath();
    if ((path.length() > 0) && "/".equals(path.substring(path.length() - 1))) {
      contextPath = path.substring(0, path.length() - 1);
    } else {
      contextPath = path;
    }
  }

  private String parseFeedback() {
    if (JiraTimetrackerUtil.loadAndCheckFeedBackTimeStampFromSession(getHttpSession())) {
      String feedBackValue = getHttpRequest().getParameter("feedbackinput");
      String ratingValue = getHttpRequest().getParameter("rating");
      String customerMail =
          JiraTimetrackerUtil.getCheckCustomerMail(getHttpRequest().getParameter("customerMail"));
      String feedBack = "";
      String rating = NOT_RATED;
      if (feedBackValue != null) {
        feedBack = feedBackValue.trim();
      }
      if (ratingValue != null) {
        rating = ratingValue;
      }
      String mailSubject = JiraTimetrackerUtil
          .createFeedbackMailSubject(JiraTimetrackerAnalytics.getPluginVersion());
      String mailBody =
          JiraTimetrackerUtil.createFeedbackMailBody(customerMail, rating, feedBack);
      jiraTimetrackerPlugin.sendEmail(mailSubject, mailBody);
      JiraTimetrackerUtil.saveFeedBackTimeStampToSession(getHttpSession());
    } else {
      message = FREQUENT_FEEDBACK;
      return SUCCESS;
    }
    return null;
  }

  private void parseNoWorkUse(final String noWorkUse) {
    if (noWorkUse != null) {
      isUseNoWorks = true;
    } else {
      isUseNoWorks = false;
    }
  }

  private void parsePageSizeInput(final String pageSizeInputValuse) {
    if (pageSizeInputValuse == null) {
      pageSize = DEFAULT_PAGE_SIZE; // FIXME 20 can be the default?
    } else {
      pageSize = Integer.parseInt(pageSizeInputValuse);
    }
  }

  private void parseReportingGroups(final String[] reportingGroupsValue) {
    if (reportingGroupsValue == null) {
      reportingGroups = new ArrayList<String>();
    } else {
      reportingGroups = Arrays.asList(reportingGroupsValue);
    }
  }

  /**
   * Parse the request after the save button was clicked. Set the variables.
   *
   * @param request
   *          The HttpServletRequest.
   */
  public String parseSaveSettings(final HttpServletRequest request) {
    String noWorkUse = request.getParameter("noWorkUse");
    String[] reportingGroupSelectValue = request.getParameterValues("reportingGroupSelect");
    String pageSizeValue = request.getParameter("pageSizeInput");
    parseReportingGroups(reportingGroupSelectValue);
    parseNoWorkUse(noWorkUse);
    parsePageSizeInput(pageSizeValue);
    return null;
  }

  /**
   * Save the plugin settings.
   */
  public void savePluginSettings() {
    ReportingSettingsValues reportingSettingsValues =
        new ReportingSettingsValues().isUseNoWorks(isUseNoWorks).reportingGroups(reportingGroups)
            .pageSize(pageSize);
    reportingPlugin.saveReportingSettings(reportingSettingsValues);
  }

  public void setContextPath(final String contextPath) {
    this.contextPath = contextPath;
  }

  public void setFeedBackSendAviable(final boolean feedBackSendAviable) {
    this.feedBackSendAviable = feedBackSendAviable;
  }

  public void setIsUseNoWork(final boolean isUseNoWorks) {
    this.isUseNoWorks = isUseNoWorks;
  }

  public void setMessage(final String message) {
    this.message = message;
  }

  public void setPageSize(final int pageSize) {
    this.pageSize = pageSize;
  }

  public void setReportingGroups(final List<String> reportingGroups) {
    this.reportingGroups = reportingGroups;
  }
}
