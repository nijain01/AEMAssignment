package aemAssignment.core.bean;

public class PendingActionReport implements Cloneable {
    private String initiator;
    private String payload;
    private String comment;
    private String workflowTitle;
    private String currentAssignee;
    private String offTime;
    private String onTime;
    private String workflowInitiatetime;
    private String title;
    private String workflowStartTime;
    private String payloadAccessURL;
    private String userEmail;
    private String userName;
    private String currentAssigneeDateTime;
    private String pendingDuration;
    private String originalParticipant;
    private String currentWorkflowStep;
    private String currentTransitionComment;

    public String getInitiator() {
        return initiator;
    }

    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getWorkflowTitle() {
        return workflowTitle;
    }

    public void setWorkflowTitle(String workflowTitle) {
        this.workflowTitle = workflowTitle;
    }

    public String getCurrentAssignee() {
        return currentAssignee;
    }

    public void setCurrentAssignee(String currentAssignee) {
        this.currentAssignee = currentAssignee;
    }

    public String getOffTime() {
        return offTime;
    }

    public void setOffTime(String offTime) {
        this.offTime = offTime;
    }

    public String getOnTime() {
        return onTime;
    }

    public void setOnTime(String onTime) {
        this.onTime = onTime;
    }

    public String getWorkflowInitiatetime() {
        return workflowInitiatetime;
    }

    public void setWorkflowInitiatetime(String workflowInitiatetime) {
        this.workflowInitiatetime = workflowInitiatetime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getWorkflowStartTime() {
        return workflowStartTime;
    }

    public void setWorkflowStartTime(String workflowStartTime) {
        this.workflowStartTime = workflowStartTime;
    }

    public String getPayloadAccessURL() {
        return payloadAccessURL;
    }

    public void setPayloadAccessURL(String payloadAccessURL) {
        this.payloadAccessURL = payloadAccessURL;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCurrentAssigneeDateTime() {
        return currentAssigneeDateTime;
    }

    public void setCurrentAssigneeDateTime(String currentAssigneeDateTime) {
        this.currentAssigneeDateTime = currentAssigneeDateTime;
    }

    public String getPendingDuration() {
        return pendingDuration;
    }

    public void setPendingDuration(String pendingDuration) {
        this.pendingDuration = pendingDuration;
    }

    public String getOriginalParticipant() {
        return originalParticipant;
    }

    public void setOriginalParticipant(String originalParticipant) {
        this.originalParticipant = originalParticipant;
    }

    public PendingActionReport clone() throws CloneNotSupportedException {
        return (PendingActionReport) super.clone();
    }

    public String getCurrentWorkflowStep() {
        return currentWorkflowStep;
    }

    public void setCurrentWorkflowStep(String currentWorkflowStep) {
        this.currentWorkflowStep = currentWorkflowStep;
    }

    public String getCurrentTransitionComment() {
        return currentTransitionComment;
    }

    public void setCurrentTransitionComment(String currentTransitionComment) {
        this.currentTransitionComment = currentTransitionComment;
    }
}
