package aemAssignment.core.servlets;

import aemAssignment.core.bean.PendingActionReport;
import aemAssignment.core.helper.PendingActionReportHelper;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.google.gson.Gson;

import javax.servlet.Servlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.List;

/*
* Servlet to send the details in JSON FORM
* This is used by the utility witten in miscadmin to show the complete list of pending action items
* It gives the list of workflows which are yet to be acted upon and with whom the action item is pending
 */
@Component(
    service= Servlet.class,
    property={
        Constants.SERVICE_DESCRIPTION + "=Reporting Servlet",
        "sling.servlet.methods=" + HttpConstants.METHOD_GET,
        "sling.servlet.paths="+ "/services/aep/generatereport",
        "sling.servlet.extensions=" + "json"
    })
public class ReportingServlet extends SlingSafeMethodsServlet {
    private static final long serialVersionUID = 1L;
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Reference
    WorkflowService workflowService;

    protected void doGet(SlingHttpServletRequest request,
        SlingHttpServletResponse response) throws
        IOException {

        LOGGER.info("inside ReportingServlet");
        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = request.getResourceResolver();
            PendingActionReportHelper helper = new PendingActionReportHelper(
                workflowService, resourceResolver);
            List<PendingActionReport> actionList = helper
                .getPendingActionReport();
            Gson gson = new Gson();
            String json = gson.toJson(actionList);
            response.getWriter().write(json);
        } catch (WorkflowException e) {
            LOGGER.error("Workflow Exception", e);
            response.getWriter().write("Workflow Exception");
        } catch (RepositoryException e) {
            LOGGER.error("Repository Exception", e);
            response.getWriter().write("Repository Exception");
        } finally {
            if (null != resourceResolver) {
                resourceResolver.close();
            }
        }

    }
}
