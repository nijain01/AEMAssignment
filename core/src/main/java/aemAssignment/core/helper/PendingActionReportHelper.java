package aemAssignment.core.helper;

import static aemAssignment.core.constants.ReportingConstants.DAYS;
import static aemAssignment.core.constants.ReportingConstants.HOURS;
import static aemAssignment.core.constants.ReportingConstants.JCR_CONTENT_OFF_TIME;
import static aemAssignment.core.constants.ReportingConstants.JCR_CONTENT_ON_TIME;
import static aemAssignment.core.constants.ReportingConstants.MINUTES;
import static aemAssignment.core.constants.ReportingConstants._1000;
import static aemAssignment.core.constants.ReportingConstants._24;
import static aemAssignment.core.constants.ReportingConstants._60;
import static com.google.common.base.Preconditions.checkNotNull;

import aemAssignment.core.bean.PendingActionReport;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.exec.WorkflowData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PendingActionReportHelper {
    private static final String COMMENT = "comment";
    private static final Logger LOGGER = LoggerFactory.getLogger(PendingActionReportHelper.class);
    private WorkflowService workflowService;
    private ResourceResolver resourceResolver;
    private SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

    public PendingActionReportHelper(WorkflowService workflowService, ResourceResolver resourceResolver) {
        this.workflowService = workflowService;
        this.resourceResolver = resourceResolver;
    }

    public List<PendingActionReport> getPendingActionReport() throws WorkflowException, RepositoryException {
        LOGGER.info("inside getPendingActionReport");

        checkNotNull(resourceResolver, "resourceResolver is null");
        checkNotNull(workflowService, "workflowService is null");

        List<PendingActionReport> actionList = new ArrayList<>();
        Session session = resourceResolver.adaptTo(Session.class);
        WorkflowSession wfSession = workflowService.getWorkflowSession(session);

        //States for which data needs to be fetched
        String[] states = new String[]{"RUNNING"};
        Workflow[] instances = wfSession.getWorkflows(states);
        LOGGER.info("Total RUNNING workflow instances count: " + instances.length);

        for (int i = 0; i < instances.length; i++) {
            PendingActionReport report = new PendingActionReport();
            LOGGER.info("iterating over Workflow instances");
            Workflow instance = instances[i];
            WorkflowData data = instance.getWorkflowData();
            getPayload(report, instance, data);
            //Setting the title
            report.setTitle(data.getMetaDataMap().get("workflowTitle", StringUtils.EMPTY) != null ? data.getMetaDataMap().get("workflowTitle", StringUtils.EMPTY) : StringUtils.EMPTY);
            //Set comment
            report.setComment(data.getMetaDataMap().get("startComment", ""));
            //Setting Workflow initiator data
            report.setInitiator(instance.getInitiator());
            LOGGER.info("instance.getInitiator: " + instance.getInitiator());
            //Setting the Start time
            report.setWorkflowStartTime(formatDateTimeToString(instance.getTimeStarted()));
            LOGGER.info("startTime: " + formatDateTimeToString(instance.getTimeStarted()));

	        /*
             * To fetch details from payload
	         */
            Node node = session.getNode(report.getPayload());
            LOGGER.info(node.getPrimaryNodeType().toString());
            //Set OFF time
            if (node.hasProperty(JCR_CONTENT_OFF_TIME)) {
                report.setOffTime(formatDateTimeToString(node.getProperty(JCR_CONTENT_OFF_TIME).getDate().getTime()));
                LOGGER.info("offtime: " + formatDateTimeToString(node.getProperty(JCR_CONTENT_OFF_TIME).getDate().getTime()));
            }
            //Set ON time
            if (node.hasProperty(JCR_CONTENT_ON_TIME)) {
                report.setOnTime(formatDateTimeToString(node.getProperty(JCR_CONTENT_ON_TIME).getDate().getTime()));
                LOGGER.info("offtime: " + formatDateTimeToString(node.getProperty(JCR_CONTENT_ON_TIME).getDate().getTime()));
            }
            getPendingDurationWithAssignee(report);
            actionList.add(report);
        }
        if (session != null) {
            session.logout();
        }
        return actionList;
    }

    private void getPayload(PendingActionReport report, Workflow instance, WorkflowData data) {
        String payloadPath = "";
        String currentAssignee = "";
        List<WorkItem> workItems = instance.getWorkItems();
        if (isPayloadTypeValid(data) && workItems != null && workItems.size() > 0) {
            WorkItem cqWorkItem = workItems.get(0);
            com.adobe.granite.workflow.exec.WorkItem wi = null;
            if (cqWorkItem instanceof Adaptable) {
                wi = ((Adaptable) cqWorkItem).adaptTo(com.adobe.granite.workflow.exec.WorkItem.class);
                if (wi != null) {
                    //Set Current Assignee details
                    currentAssignee = wi.getCurrentAssignee();
                    report.setCurrentAssignee(currentAssignee);
                    LOGGER.info("current assignee: " + currentAssignee);
                    // Set Current Asignee Date and time
                    report.setCurrentAssigneeDateTime(formatDateTimeToString(wi.getTimeStarted()));
                    LOGGER.info("Current Asignee Date and time: " + formatDateTimeToString(wi.getTimeStarted()));
                    //Set Delegated Assignee
                    report.setOriginalParticipant(getOriginalParticipantForCurrentWorkflow(wi));
                    payloadPath = data.getPayload().toString();
                    //Set Current Workflow Step
                    report.setCurrentWorkflowStep(wi.getNode().getTitle());
                    LOGGER.info("Current Workflow Step: " + report.getCurrentWorkflowStep());
                    //Set Current node transition Comment
                    report.setCurrentTransitionComment(getcurrentNodetransitionComment(wi));
                }
            }
        } else {
            if (data.getPayload() == null) {
                payloadPath = "[no Payload specified]";
            } else {
                payloadPath = data.getPayload().toString();
            }
        }
        //Set payload path
        LOGGER.info("payloadPath: " + payloadPath);
        report.setPayload(payloadPath);
    }

    private String getOriginalParticipantForCurrentWorkflow(
            com.adobe.granite.workflow.exec.WorkItem wi) {
        return wi.getMetaDataMap() != null && wi.getMetaDataMap().get("ORIGINAL_PARTICIPANT") != null ? wi.getMetaDataMap().get("ORIGINAL_PARTICIPANT", StringUtils.EMPTY) : StringUtils.EMPTY;
    }

    private String getcurrentNodetransitionComment(
            com.adobe.granite.workflow.exec.WorkItem wi) {
        return wi.getMetaDataMap() != null && wi.getMetaDataMap().get(COMMENT) != null ? wi.getMetaDataMap().get(COMMENT, StringUtils.EMPTY) : StringUtils.EMPTY;
    }

    private boolean isPayloadTypeValid(WorkflowData data) {
        return data.getPayloadType() != null
                && (data.getPayloadType().equals("JCR_PATH") || data.getPayloadType().equals("URL"));
    }

    private String formatDateTimeToString(final Date date) {
        return formatter.format(date);
    }

    private void getPendingDurationWithAssignee(PendingActionReport report) {
        Date currentDate = new Date();
        if (report != null && report.getCurrentAssigneeDateTime() != null && !report.getCurrentAssigneeDateTime().equals(StringUtils.EMPTY)) {
            Date currentAssigneeDateTime;
            try {
                currentAssigneeDateTime = formatter.parse(report.getCurrentAssigneeDateTime());
                long diff = currentDate.getTime() - currentAssigneeDateTime.getTime();
                long diffMinutes = diff / (_60 * _1000) % _60;
                long diffHours = diff / (_60 * _60 * _1000) % _24;
                long diffDays = diff / (_24 * _60 * _60 * _1000);
                LOGGER.info(diffDays + DAYS);
                LOGGER.info(diffHours + HOURS);
                LOGGER.info(diffMinutes + MINUTES);
                report.setPendingDuration(getDuration(diffDays, diffHours, diffMinutes));
            } catch (ParseException e) {
                LOGGER.error("Parse Exception", e);
            }
        }
    }

    private String getDuration(long diffDays, long diffHours, long diffMinutes) {
        StringBuilder builder = new StringBuilder("0");
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
}
