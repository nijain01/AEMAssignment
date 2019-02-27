package aemAssignment.core.schedulers;

import static aemAssignment.core.constants.ReportingConstants.ADDRESS_EXCEPTION_EXCEPTION;
import static aemAssignment.core.constants.ReportingConstants.DAYS;
import static aemAssignment.core.constants.ReportingConstants.EMAIL_EXCEPTION_EXCEPTION;
import static aemAssignment.core.constants.ReportingConstants.HOURS;
import static aemAssignment.core.constants.ReportingConstants.HYPHEN;
import static aemAssignment.core.constants.ReportingConstants.MINUTES;
import static aemAssignment.core.constants.ReportingConstants.PROFILE_FAMILY_NAME;
import static aemAssignment.core.constants.ReportingConstants.PROFILE_GIVEN_NAME;
import static aemAssignment.core.constants.ReportingConstants.REPOSITORY_EXCEPTION;
import static aemAssignment.core.constants.ReportingConstants.SPACE;
import static aemAssignment.core.constants.ReportingConstants.USER_EMAIL_NODE_RELATIVE_PATH;
import static aemAssignment.core.constants.ReportingConstants.WORKFLOW_EXCEPTION;
import static aemAssignment.core.constants.ReportingConstants._1000;
import static aemAssignment.core.constants.ReportingConstants._24;
import static aemAssignment.core.constants.ReportingConstants._60;

import aemAssignment.core.bean.PendingActionReport;
import aemAssignment.core.helper.PendingActionReportHelper;
import com.day.cq.mailer.MessageGateway;
import com.day.cq.mailer.MessageGatewayService;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/*
 * This is to send the email to all users who has any workflow assigned to them and has some pending actions with them
 * Sinlge mail will be send to one user even in case of more than one action item assigned to them
 *  The mail should contain details of all action items pending to them
 */

@Component(
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    configurationPid = "aemAssignment.core.schedulers.PendingActionsMailScheduler",
    name = "Pending Action Items - Scheduled Service"
    // private properties
    // property = {
    // "service.enabled:Boolean=true",
    // "scheduler.concurrent:Boolean=true",
    // "scheduler.immediate:Boolean=true",
    // "scheduler.name=PendingActionsMailScheduler",
    // "scheduler.immediate:Boolean=true"
    //}
)

@Designate(ocd = SchedulerInterfaceConfiguration.class)
public class PendingActionsMailScheduler implements Runnable {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public static final String DATE_FORMAT = "dd-MMM-yyyy";
    private static final String TABLE_HEADER_CLOSED_TAG = "</th>";
    private static final String TABLE_HEADER_FOR_EMAIL = "<th style=\"border: 1px solid black;\">";
    private static final String TR_STYLE_WITH_INLINCE_CSS = "<tr style=\"border: 1px solid black;\">";
    private static final String TD_WITH_INLINE_CSS = "</td><td style=\"border: 1px solid black;\">";
    private static final String CLONE_NOT_SUPPORTED_EXCEPTION = "Clone Not Supported Exception";
    private String templatePath;
    private String schedulerExpression;
    private String fromAddress;
    private String contextPath;
    private String pendingDuration;
    private String contentExpirationDuration;
    private SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    @Reference
    private WorkflowService workflowService;
    @Reference
    private MessageGatewayService messageGatewayService;

    @Override
    public void run() {
        LOGGER.info("Inside PendingActionsMailScheduler, log generated via scheduler");

        Map<String, Object> userMap = new HashMap<>();
        userMap.put(ResourceResolverFactory.SUBSERVICE, "datawrite");

        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory.getResourceResolver(userMap);

            PendingActionReportHelper helper = new PendingActionReportHelper(workflowService,
                resourceResolver);

            List<PendingActionReport> actionList =
                helper.getPendingActionReport();

            generateListAndMailReport(resourceResolver, actionList);
        } catch (LoginException e1) {
            LOGGER.error("Login Exception", e1);
        } catch (WorkflowException e) {
            LOGGER.error(WORKFLOW_EXCEPTION, e);
        } catch (RepositoryException e) {
            LOGGER.error(REPOSITORY_EXCEPTION, e);
        } catch (EmailException e) {
            LOGGER.error(EMAIL_EXCEPTION_EXCEPTION, e);
        } catch (CloneNotSupportedException e) {
            LOGGER.error(CLONE_NOT_SUPPORTED_EXCEPTION, e);
        } catch (IOException e) {
            LOGGER.error("IOException", e);
        } catch (ParseException e) {
            LOGGER.error("ParseException", e);
        } catch (AddressException e) {
            LOGGER.error(ADDRESS_EXCEPTION_EXCEPTION, e);
        } finally {
            if (null != resourceResolver) {
                resourceResolver.close();
            }
        }

    }

    private ListMultimap<String, PendingActionReport> generateListAndMailReport(
        ResourceResolver resourceResolver,
        List<PendingActionReport> actionList) throws RepositoryException,
        CloneNotSupportedException, IOException, AddressException, EmailException, ParseException {
        ListMultimap<String, PendingActionReport> multimap =
            generateMapWithUserDetailsAndPendingItems(actionList, resourceResolver);
        Session session = resourceResolver.adaptTo(Session.class);
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        Set<String> userIDs = multimap.keySet();
        for (String id : userIDs) {
            List<PendingActionReport> reportListForUser = multimap.get(id);
            if (!reportListForUser.isEmpty()) {
                String toAddress = reportListForUser.get(0).getUserEmail();
                if (isToAddress(toAddress)) {
                    String userName = reportListForUser.get(0).getUserName();
                    String subjectVal =
                        "Generated Report-" + dateFormat.format(date) + " for " + id;
                    // Generate Mail Body
                    String mailBody = generateMailBodyForUserWithReports(reportListForUser);

                    LOGGER.info("Subject in submit servlet is :" + subjectVal);
                    LOGGER.info("to Address in submit servlet is :" + toAddress);
                    String templateReference = templatePath.substring(1) + "/jcr:content";
                    LOGGER.info("Template Path: " + templateReference);
                    Node root = session.getRootNode();
                    // Check for Template
                    if (isTemplateReferenceAvailable(templateReference, root)) {
                        // Send Email
                        sendMailToUser(toAddress, userName, subjectVal, mailBody, templateReference,
                            root);
                    } else {
                        LOGGER.info("Mail template does not exists");
                        break;
                    }
                } else {
                    LOGGER.info("Email is not present for user: " + id);
                }
            }
        }
        if (null != session) {
            session.logout();
        }
        return multimap;
    }

    private boolean isTemplateReferenceAvailable(String templateReference, Node root)
        throws RepositoryException {
        return root.getNode(templateReference) != null;
    }

    private void sendMailToUser(String toAddress, String userName, String subjectVal,
        String mailBody, String templateReference, Node root)
        throws RepositoryException, IOException, AddressException, EmailException {
        List<InternetAddress> emailRecipients = new ArrayList<>();
        InputStream inputStream;
        BufferedInputStream bufferedInputStream;
        Node jcrContent = root.getNode(templateReference);
        inputStream = jcrContent.getProperty("jcr:data").getBinary().getStream();
        bufferedInputStream = new BufferedInputStream(inputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int resultNumber = bufferedInputStream.read();
        while (resultNumber != -1) {
            byte b = (byte) resultNumber;
            byteArrayOutputStream.write(b);
            resultNumber = bufferedInputStream.read();
        }
        String finalMsg = byteArrayOutputStream.toString();
        finalMsg = finalMsg.replace("${user}", userName);
        finalMsg = finalMsg.replace("${message}", mailBody);
        sendMail(toAddress, subjectVal, finalMsg);
        emailRecipients.clear();
        bufferedInputStream.close();
        inputStream.close();
    }

    private boolean isToAddress(String toAddress) {
        return null != toAddress &&
            !toAddress.equals(StringUtils.EMPTY);
    }

    private void sendMail(String toAddress, String subjectVal, String finalMsg)
        throws AddressException, EmailException {
        List<InternetAddress> emailRecipients = new ArrayList<>();
        HtmlEmail email = new HtmlEmail();
        emailRecipients.add(new InternetAddress(toAddress));
        email.setCharset("UTF-8");
        //email.setFrom("no-reply@walmart.com")
        email.setFrom(fromAddress);
        email.setTo(emailRecipients);
        email.setSubject(subjectVal);
        email.setHtmlMsg(finalMsg);
        MessageGateway<HtmlEmail> messageGateway = this.messageGatewayService
            .getGateway(HtmlEmail.class);
        messageGateway.send(email);
    }

    private String generateMailBodyForUserWithReports(
        List<PendingActionReport> reportListForUser) {
        StringBuilder builder = new StringBuilder("<table style=\"border: 1px solid black;\">");
        builder.append(TR_STYLE_WITH_INLINCE_CSS).append(getTableHeaderForEmail());
        Iterator<PendingActionReport> reportIterator = reportListForUser.iterator();
        while (reportIterator.hasNext()) {
            builder.append(TR_STYLE_WITH_INLINCE_CSS)
                .append("<td style=\"border: 1px solid black;\">");
            PendingActionReport report = reportIterator.next();
            builder.append(getCurrentAssigneeForEmailReport(report)).append(TD_WITH_INLINE_CSS);
            builder.append(report.getInitiator()).append(TD_WITH_INLINE_CSS);
            builder.append(getTitle(report)).append(TD_WITH_INLINE_CSS);
            builder.append(getComment(report)).append(TD_WITH_INLINE_CSS);
            builder.append(report.getWorkflowStartTime()).append(TD_WITH_INLINE_CSS);
            builder.append(report.getCurrentWorkflowStep()).append(TD_WITH_INLINE_CSS);
            builder.append("<a href=\"").append(getItemUrl(report)).append("\">")
                .append(report.getPayload()).append("</a>").append(TD_WITH_INLINE_CSS);
            builder.append(getOnTimeForEmail(report)).append(TD_WITH_INLINE_CSS);
            builder.append(getOffTimeForEmail(report)).append(TD_WITH_INLINE_CSS);
            builder.append(report.getCurrentAssigneeDateTime()).append(TD_WITH_INLINE_CSS);
            builder.append(getCurrentTransitionComments(report)).append(TD_WITH_INLINE_CSS);
            builder.append(getRemarks(report));
            builder.append("</td></tr>");
        }
        builder.append("</table>");
        LOGGER.info("Action list table: " + builder.toString());
        return builder.toString();
    }

    private String getCurrentAssigneeForEmailReport(PendingActionReport report) {
        if (report.getOriginalParticipant() != null && !report.getOriginalParticipant()
            .equals(StringUtils.EMPTY)) {
            return report.getCurrentAssignee() + "(" + report.getOriginalParticipant() + ")";
        } else {
            return report.getCurrentAssignee();
        }
    }

    private String getCurrentTransitionComments(PendingActionReport report) {
        return report.getCurrentTransitionComment() != null
            && report.getCurrentTransitionComment().length() == 0 ? HYPHEN
            : report.getCurrentTransitionComment();
    }

    private String getComment(PendingActionReport report) {
        return report.getComment() != null && report.getComment().length() == 0 ? HYPHEN
            : report.getComment();
    }

    private String getTitle(PendingActionReport report) {
        return report.getWorkflowTitle() == null ? HYPHEN : report.getWorkflowTitle();
    }

    private String getTableHeaderForEmail() {
        StringBuilder builder = new StringBuilder();
        builder.append(TABLE_HEADER_FOR_EMAIL).append("Current Asignee/Group")
            .append(TABLE_HEADER_CLOSED_TAG);
        builder.append(TABLE_HEADER_FOR_EMAIL).append("Initiator").append(TABLE_HEADER_CLOSED_TAG);
        builder.append(TABLE_HEADER_FOR_EMAIL).append("Workflow Title")
            .append(TABLE_HEADER_CLOSED_TAG);
        builder.append(TABLE_HEADER_FOR_EMAIL).append("Workflow Intiation Comments")
            .append(TABLE_HEADER_CLOSED_TAG);
        builder.append(TABLE_HEADER_FOR_EMAIL).append("Workflow Start Time")
            .append(TABLE_HEADER_CLOSED_TAG);
        builder.append(TABLE_HEADER_FOR_EMAIL).append("Current Workflow Step")
            .append(TABLE_HEADER_CLOSED_TAG);
        builder.append(TABLE_HEADER_FOR_EMAIL).append("Payload").append(TABLE_HEADER_CLOSED_TAG);
        builder.append(TABLE_HEADER_FOR_EMAIL).append("Page On Time")
            .append(TABLE_HEADER_CLOSED_TAG);
        builder.append(TABLE_HEADER_FOR_EMAIL).append("Page Off Time")
            .append(TABLE_HEADER_CLOSED_TAG);
        builder.append(TABLE_HEADER_FOR_EMAIL).append("Assigned On")
            .append(TABLE_HEADER_CLOSED_TAG);
        builder.append(TABLE_HEADER_FOR_EMAIL).append("Current Transition Comments")
            .append(TABLE_HEADER_CLOSED_TAG);
        builder.append(TABLE_HEADER_FOR_EMAIL).append("Pending Duration")
            .append(TABLE_HEADER_CLOSED_TAG);
        return builder.toString();
    }

    private String getItemUrl(PendingActionReport report) {
        if (report.getPayload().contains("dam")) {
            return contextPath + "/assetdetails.html" + report.getPayload();
        } else {
            return contextPath + "/editor.html" + report.getPayload() + ".html";
        }
    }

    private Object getRemarks(PendingActionReport report) {
        StringBuilder builder = new StringBuilder();
        if (report.getOffTime() == null) {
            builder.append("Report is pending for your action for " + report.getPendingDuration());
        } else if (!getExpireDuration(report).equals(StringUtils.EMPTY)) {
            builder.append("Report is about to expire in: " + getExpireDuration(report));
        } else {
            builder.append("Content has already reached the off time.");
        }
        return builder.toString();
    }

    private String getExpireDuration(PendingActionReport report) {
        // TODO Auto-generated method stub
        Date currentDate = new Date();
        String expireDuration = StringUtils.EMPTY;
        if (report != null && report.getCurrentAssigneeDateTime() != null && !report
            .getCurrentAssigneeDateTime().equals(StringUtils.EMPTY)) {
            Date assetOffDateTime;
            try {
                assetOffDateTime = formatter.parse(report.getOffTime());
                if (assetOffDateTime.getTime() > currentDate.getTime()) {
                    long diff = assetOffDateTime.getTime() - currentDate.getTime();
                    long diffMinutes = diff / (_60 * _1000) % _60;
                    long diffHours = diff / (_60 * _60 * _1000) % _24;
                    long diffDays = diff / (_24 * _60 * _60 * _1000);
                    LOGGER.info(diffDays + DAYS);
                    LOGGER.info(diffHours + HOURS);
                    LOGGER.info(diffMinutes + MINUTES);
                    expireDuration = getDuration(diffDays, diffHours, diffMinutes);
                }
            } catch (ParseException e) {
                LOGGER.error("Parse Exception", e);
            }
        }
        return expireDuration;
    }

    private String getDuration(long diffDays, long diffHours, long diffMinutes) {
        StringBuilder builder = new StringBuilder("");
        if (diffDays != 0) {
            builder.append(diffDays).append(DAYS);
        }
        if (diffHours != 0) {
            builder.append(diffHours).append(HOURS);
        }
        if (diffMinutes != 0) {
            builder.append(diffMinutes).append(MINUTES);
        }
        return builder.toString();
    }

    private String getOffTimeForEmail(PendingActionReport report) {
        return report.getOffTime() == null ? HYPHEN : report.getOffTime();
    }

    private String getOnTimeForEmail(PendingActionReport report) {
        return report.getOnTime() == null ? HYPHEN : report.getOnTime();
    }

    private ListMultimap<String, PendingActionReport> generateMapWithUserDetailsAndPendingItems(
        List<PendingActionReport> actionList, ResourceResolver resourceResolver)
        throws RepositoryException, CloneNotSupportedException, ParseException {
        UserManager userManager = resourceResolver.adaptTo(UserManager.class);
        Iterator<PendingActionReport> actionReportIterator = actionList.iterator();
        ListMultimap<String, PendingActionReport> multimap = ArrayListMultimap.create();
        while (actionReportIterator.hasNext()) {
            PendingActionReport report = actionReportIterator.next();
            Authorizable authorizable = userManager.getAuthorizable(report.getCurrentAssignee());
            LOGGER
                .info("generateMapWithUserDetailsAndPendingItems: Payload: " + report.getPayload());
            LOGGER.info("generateMapWithUserDetailsAndPendingItems: Current Asignee: " + report
                .getCurrentAssignee());
            if (isCurrentUserAGroup(authorizable)) {
                updateMapForReport(multimap, authorizable, userManager, report);
            } else {
                User user = (User) authorizable;
                LOGGER.info(
                    "generateMapWithUserDetailsAndPendingItems: PendingActionsMailScheduler: email: "
                        + getUserEmailId(user) + " for user: " + report.getCurrentAssignee());
                //updated the current assignee details
                PendingActionReport tempReport = report.clone();
                LOGGER.info("user id: " + user.getID());
                //tempReport.setCurrentAssignee(user.getID())
                tempReport.setUserEmail(getUserEmailId(user));
                LOGGER.info("user name: " + getUserName(user));
                tempReport.setUserName(getUserName(user));
                if (isItemValidForEmail(tempReport)) {
                    multimap.put(user.getID(), tempReport);
                }
            }
        }
        return multimap;
    }

    private void updateMapForReport(
        ListMultimap<String, PendingActionReport> multimap,
        Authorizable authorizable, UserManager userManager,
        PendingActionReport report)
        throws RepositoryException, CloneNotSupportedException, ParseException {
        Group group = (Group) authorizable;
        Iterator<Authorizable> groupMemebersiterator = group.getMembers();
        while (groupMemebersiterator.hasNext()) {
            User user = (User) groupMemebersiterator.next();
            LOGGER.info(user.getID());
            if (isCurrentUserAGroup(userManager.getAuthorizable(user.getID()))) {
                updateMapForReport(multimap, authorizable, userManager, report);
            } else {
                LOGGER.info(
                    "updateMapForReport: PendingActionsMailScheduler: email: " + getUserEmailId(
                        user) + " for user: " + user.getID());
                //updated the current assignee details
                PendingActionReport tempReport = report.clone();
                LOGGER.info("user id: " + user.getID());
                //tempReport.setCurrentAssignee(user.getID())
                tempReport.setUserEmail(getUserEmailId(user));
                LOGGER.info("user name: " + getUserName(user));
                tempReport.setUserName(getUserName(user));
                LOGGER.info("Before saving in multimap; user name: " + tempReport.getUserName()
                    + " ; Current assignee:  " + tempReport.getCurrentAssignee());
                if (isItemValidForEmail(tempReport)) {
                    multimap.put(user.getID(), tempReport);
                }
                LOGGER.info("Size of map after saving details: " + multimap.size());
                //LOGGER.info("Content of map after saving details: " + multimap.asMap().toString())
            }
        }
    }

    private boolean isItemValidForEmail(PendingActionReport report) throws ParseException {
        LOGGER.info("inside isItemValidForEmail");

        /*
         * if off time is present
         * 		difference B/W offtime - current time should be (less than or equal to) Days set in config
         *
         * else ==> off time id not present
         * 		difference b/w current time - assignment time (greater than or equal to) Days set in config
         */
        Date currentDate = new Date();
        if (isOffTimeValidAndHasValue(report)) {
            //Off time is present
            LOGGER.info("off time: " + report.getOffTime());
            Date assetOffDateTime = formatter.parse(report.getOffTime());
            if (assetOffDateTime.getTime() > currentDate.getTime()) {
                //Expiration date is not reached
                long diff = assetOffDateTime.getTime() - currentDate.getTime();
                long diffDays = diff / (_24 * _60 * _60 * _1000);
                LOGGER
                    .info("contentExpirationDuration: " + Long.valueOf(contentExpirationDuration));
                LOGGER.info("diffDays: " + diffDays);
                return (Long.valueOf(contentExpirationDuration) >= diffDays);
            } else {
                // item is already expired
                return false;
            }
        } else {
            // Off time is not configured
            LOGGER.info("Current Assignee time: " + report.getCurrentAssigneeDateTime());
            Date currentAssigneeDateTime = formatter.parse(report.getCurrentAssigneeDateTime());
            long diff = currentDate.getTime() - currentAssigneeDateTime.getTime();
            long diffDays = diff / (_24 * _60 * _60 * _1000);
            LOGGER.info("Long.valueOf(contentExpirationDuration): " + Long
                .valueOf(contentExpirationDuration));
            LOGGER.info("Long.valueOf(pendingDuration): " + Long.valueOf(pendingDuration));
            LOGGER.info("diffDays: " + diffDays);
            return (diffDays >= Long.valueOf(pendingDuration));
        }
    }

    private boolean isOffTimeValidAndHasValue(PendingActionReport report) {
        return isToAddress(report.getOffTime());
    }

    private String getUserName(User user) {
        StringBuilder builder = new StringBuilder();
        try {
            if (getGiverNameForUser(user) != null) {
                builder.append(getGiverNameForUser(user)[0].getString()).append(SPACE);
            }
            if (getFamilyNameForUser(user) != null) {
                builder.append(getFamilyNameForUser(user)[0].getString());
            }
        } catch (RepositoryException e) {
            LOGGER.error(REPOSITORY_EXCEPTION, e);
        }
        return builder.toString();
    }

    private Value[] getFamilyNameForUser(User user) throws RepositoryException {
        return user.getProperty(PROFILE_FAMILY_NAME);
    }

    private Value[] getGiverNameForUser(User user) throws RepositoryException {
        return user.getProperty(PROFILE_GIVEN_NAME);
    }

    private String getUserEmailId(User user) throws RepositoryException,
        ValueFormatException {
        return user.getProperty(USER_EMAIL_NODE_RELATIVE_PATH) != null ? user
            .getProperty(USER_EMAIL_NODE_RELATIVE_PATH)[0].getString() : StringUtils.EMPTY;
    }

    private boolean isCurrentUserAGroup(Authorizable authorizable) {
        return authorizable.isGroup();
    }

    @Activate
    protected final void activate(SchedulerInterfaceConfiguration configuration) {
        LOGGER.info("activate PendingActionsMailScheduler");
        templatePath = configuration.templatePath();
        LOGGER.info("templatePath: " + templatePath);
        this.fromAddress = configuration.fromAddress();
        LOGGER.info("fromAddress: " + fromAddress);
        this.schedulerExpression = configuration.scheduler_expression();
        LOGGER.info("schedulerExpression :" + schedulerExpression);
        this.contextPath = configuration.context_path();
        LOGGER.info("contextPath: " + contextPath);
        this.pendingDuration = configuration.pendingDuration();
        LOGGER.info("pending Duration value: " + pendingDuration);
        this.contentExpirationDuration = configuration.contentExpirationDuration();
        LOGGER.info("content Expiration Duration value: " + contentExpirationDuration);
    }

    @Deactivate
    protected final void deactivate() {
        LOGGER.info("deactivate PendingActionsMailScheduler");
    }
}
