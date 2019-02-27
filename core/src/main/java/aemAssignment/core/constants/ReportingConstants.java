package aemAssignment.core.constants;

public final class ReportingConstants {
    public static final int _24 = 24;
    public static final int _60 = 60;
    public static final int _1000 = 1000;
    public static final String MINUTES = " minutes ";
    public static final String HOURS = " hours ";
    public static final String DAYS = " days ";
    public static final String JCR_CONTENT_ON_TIME = "jcr:content/onTime";
    public static final String JCR_CONTENT_OFF_TIME = "jcr:content/offTime";
    public static final String HYPHEN = "-";
    public static final String ADDRESS_EXCEPTION_EXCEPTION = "AddressException Exception";
    public static final String EMAIL_EXCEPTION_EXCEPTION = "EmailException Exception";
    public static final String WORKFLOW_EXCEPTION = "Workflow Exception";
    public static final String REPOSITORY_EXCEPTION = "Repository Exception";
    public static final String SPACE = " ";
    public static final String PROFILE_GIVEN_NAME = "./profile/givenName";
    public static final String PROFILE_FAMILY_NAME = "./profile/familyName";
    public static final String USER_EMAIL_NODE_RELATIVE_PATH = "./profile/email";
    //Private constructor to avoid Sonar issue
    private ReportingConstants() {
    }
}
