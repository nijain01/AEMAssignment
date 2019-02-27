package aemAssignment.core.models;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import aemAssignment.core.services.AEPShortTextDurationPresetsConfiguration;
import aemAssignment.core.bean.CollectionLinkComponentWithoutPresetsBean;
import aemAssignment.core.bean.CollectionLinkComponentWithoutPresetsMetadataBean;
import com.adobe.cq.sightly.WCMUsePojo;
import com.day.cq.commons.date.RelativeTimeFormat;
import com.day.cq.wcm.api.Page;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CollectionLinkComponentWithoutPresetsComponent extends WCMUsePojo {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String DAM_FOLDER_NODE_TYPE = "sling:OrderedFolder";
    private static final String CQ_PAGE_NODE_TYPE = "cq:Page";
    private static final String SHOW_SHORT_TEXT = "showShortText";
    private static final String SHOW_LAST_MODIFIED_DATE = "showLastModifiedDate";
    private static final String SHOW_SHORT_DESCRIPTION = "showShortDescription";
    private static final String SORT_BY_LAST_MODIFIED_DATE = "sortByLastModifiedDate";
    private static final String SORT_BY_TITLE = "sortByTitle";
    private static final String JCR_CONTENT_STATUS_FOR_ASSETS = "jcr:content/metadata/contentStatus";
    private static final String FORMAT_FOR_ASSETS = "jcr:content/metadata/dc:format";
    private static final String PDF_FORMAT = "application/pdf";
    private static final String JCR_CONTENT = "jcr:content/";
    private static final String HEADING = "heading";
    private static final String
            PUBLISHED_DATE_TIME_VALUE = "publishedDateTimeValue";
    private static final String PATH = "path";
    private static final String PDF_SHORT_TEXT = "PDF";
    private static final int SIXTY_SECONDS = 60;
    private static final int MILLISECOND = 1000;
    private static final String SHORT_DESCRIPTION_METADATA_FOR_ASSETS = "jcr:content/metadata/dc:description";
    private static final String TITLE_NODE_FOR_ASSETS = "jcr:content/metadata/dc:title";
    private static final String DAM_ASSET_NODE_TYPE = "dam:Asset";
    private static final String PUBLISHED_DATE_TIME_VALUE_METADATA_FOR_ASSETS = "jcr:content/publishedDateTimeValue";
    private static final String DESCRIPTION_METADATA_FOR_SITES = "jcr:description";
    private static final String SLING_FOLDER_NAME = "sling:Folder";
    private static final int SIXTY_MINUTES = 60;
    private static final long DEFAULT_LONG_VALUE = 0l;
    private static final String ONE_HOUR_AGO = "1 hour ago";
    private static final String MINUTES = "minutes";
    private static final String SECONDS = "seconds";
    private static final String RELATIVE_TIME_FORMAT = "r";
    private static final String CONTENT_STATUS = "contentStatus";
    private static final String HTML_FILE_EXTENSION = ".html";
    private static final String DEFAULT_DURATION = "0";
    private CollectionLinkComponentWithoutPresetsMetadataBean metadataBean = null;
    private CollectionLinkComponentWithoutPresetsBean bean = null;
    private AEPShortTextDurationPresetsConfiguration durationPresetsConfiguration = null;

    /**
     * @throws Exception
     */
    @Override
    public void activate() throws Exception {
        log.info("inside CollectionLinkComponentWithoutPresetsComponent");
        Session session;
        Node pathNode;
        Node currentNode = getResource().adaptTo(Node.class);
        this.bean = new CollectionLinkComponentWithoutPresetsBean();
        durationPresetsConfiguration = this.getSlingScriptHelper().getService(AEPShortTextDurationPresetsConfiguration.class);
        log.info(currentNode.getPrimaryNodeType().getName());
        log.info(currentNode.getPrimaryNodeType().getPrimaryItemName());
        log.info("sling:OrderedFolder: " + currentNode.getPrimaryNodeType().isNodeType(DAM_FOLDER_NODE_TYPE));
        if(currentNode.hasProperty(HEADING)){
            bean.setHeading(currentNode.getProperty(HEADING).getValue().getString());
        }
        if (currentNode.hasProperty(PATH)) {
            log.info("path: " + currentNode.getProperty(PATH).getValue().getString());
            session = getResourceResolver().adaptTo(Session.class);
            pathNode = session.getNode(currentNode.getProperty(PATH).getValue().getString());
            log.info(pathNode.getPrimaryNodeType().getName());
            log.info(pathNode.getPrimaryNodeType().getPrimaryItemName());
            if (pathNode.getPrimaryNodeType().getName().equals(DAM_FOLDER_NODE_TYPE) || pathNode.getPrimaryNodeType().getName().equals(SLING_FOLDER_NAME)) {
                //assets
                Node assetNode = getResourceResolver().getResource(currentNode.getProperty("path").getValue().getString()).adaptTo(Node.class);
                bean.setPathTitle(assetNode.hasProperty("jcr:title") ? assetNode.getProperty("jcr:title").getString() : assetNode.getName());
                log.info("assets ; path title: " + bean.getPathTitle());
                bean.setMetadataBeanList(iterateAssets(currentNode, assetNode));
            } else if (pathNode.getPrimaryNodeType().getName().equals(CQ_PAGE_NODE_TYPE)) {
                //sites
                Page page = getResourceResolver().getResource(currentNode.getProperty("path").getValue().getString()).adaptTo(Page.class);
                bean.setPathTitle(page.getTitle());
                log.info("pages ; path title: " + bean.getPathTitle());
                bean.setMetadataBeanList(iteratePages(page, currentNode));
            }
        }
    }

    /**
     * @param currentNode
     * @return
     * @throws RepositoryException
     */
    private List<CollectionLinkComponentWithoutPresetsMetadataBean> iterateAssets(Node currentNode, Node assetNode) throws RepositoryException {
        List<CollectionLinkComponentWithoutPresetsMetadataBean> listBean = new ArrayList<>();
        boolean isShortTextVisible = isValid(currentNode, SHOW_SHORT_TEXT);
        boolean isLastModifiedDateVisible = isValid(currentNode, SHOW_LAST_MODIFIED_DATE);
        boolean isShortDescriptionVisible = isValid(currentNode, SHOW_SHORT_DESCRIPTION);
        NodeIterator nodeItr = assetNode.getNodes();
        while (nodeItr.hasNext()) {
            Node node = nodeItr.nextNode();
            if (node.getPrimaryNodeType().getName().equals(DAM_ASSET_NODE_TYPE)) {
                log.info(node.getName());
                metadataBean = new CollectionLinkComponentWithoutPresetsMetadataBean();
                if (node.hasProperty(PUBLISHED_DATE_TIME_VALUE_METADATA_FOR_ASSETS)) {
                    metadataBean.setTitle(node.hasProperty(TITLE_NODE_FOR_ASSETS) ? node.getProperty(TITLE_NODE_FOR_ASSETS).getValue().getString() : node.getName());
                    metadataBean.setPath(node.getPath());
                    metadataBean.setLastModifiedDate(isLastModifiedDateVisible ? getLastModifiedDateForAssets(node) : EMPTY);
                    metadataBean.setlLastModifiedDate(isLastModifiedDateVisible ? getLongLastModifiedDateForAssets(node) : DEFAULT_LONG_VALUE);
                    metadataBean.setShortDescription(getShortDescriptionForAssets(isShortDescriptionVisible, node));
                    metadataBean.setShortText(isShortTextVisible ? getShortTextForAssets(node) : EMPTY);
                    metadataBean.setShortTextForPdf(isShortTextVisible ? getShortTextForAssetsForAssets(node) : EMPTY);
                    listBean.add(metadataBean);
                }
            }
        }
        return sortList(listBean, currentNode);
    }

    /**
     *
     * @param node
     * @return
     * @throws RepositoryException
     */
    private String getShortTextForAssetsForAssets(Node node) throws RepositoryException {

        int duration = Integer.valueOf(getDuration());
        DateTime lastModifiedDate;
        DateTime shortTextValidity = null;
        if (node.hasProperty(JCR_CONTENT + PUBLISHED_DATE_TIME_VALUE)) {
            lastModifiedDate = new DateTime(node.getProperty(JCR_CONTENT + PUBLISHED_DATE_TIME_VALUE).getValue().getDate());
            shortTextValidity = lastModifiedDate.plusDays(duration);
        }

        if (node.hasProperty(JCR_CONTENT_STATUS_FOR_ASSETS) && (shortTextValidity != null && shortTextValidity.isAfterNow())) {
            return node.getProperty(JCR_CONTENT_STATUS_FOR_ASSETS).getValue().getString();
        }
        return EMPTY;
    }

    /**
     * @param isShortDescriptionVisible
     * @param node
     * @return short description
     * @throws RepositoryException
     */
    private String getShortDescriptionForAssets(boolean isShortDescriptionVisible, Node node) throws RepositoryException {
        if (isShortDescriptionVisible && node.hasProperty(SHORT_DESCRIPTION_METADATA_FOR_ASSETS)) {
            return node.getProperty(SHORT_DESCRIPTION_METADATA_FOR_ASSETS).getValue().getString();
        }
        return EMPTY;
    }

    private String getShortTextForAssets(Node node) throws RepositoryException {
        if (node.hasProperty(FORMAT_FOR_ASSETS) && node.getProperty(FORMAT_FOR_ASSETS).getValue().getString().equals(PDF_FORMAT)) {
            return PDF_SHORT_TEXT;
        }
        return EMPTY;
    }

    private String getDuration() {
        return durationPresetsConfiguration.getShortTextDuration().length() > 0 ? durationPresetsConfiguration.getShortTextDuration() : DEFAULT_DURATION;
    }

    /**
     * @param page
     * @param currentNode
     * @return ArrayList<CollectionLinkComponentWithoutPresetsMetadataBean>
     * @throws RepositoryException
     */
    private List<CollectionLinkComponentWithoutPresetsMetadataBean> iteratePages(Page page, Node currentNode) throws RepositoryException {
        Iterator<Page> pageItr = page.listChildren(null, false);
        List<CollectionLinkComponentWithoutPresetsMetadataBean> listBean = new ArrayList<>();
        boolean isShortTextVisible = isValid(currentNode, SHOW_SHORT_TEXT);
        boolean isLastModifiedDateVisible = isValid(currentNode, SHOW_LAST_MODIFIED_DATE);
        boolean isShortDescriptionVisible = isValid(currentNode, SHOW_SHORT_DESCRIPTION);
        while (pageItr.hasNext()) {
            metadataBean = new CollectionLinkComponentWithoutPresetsMetadataBean();
            Page childPage = pageItr.next();
            log.info("childPage: " + childPage.getName());
            if (childPage.getProperties().containsKey(PUBLISHED_DATE_TIME_VALUE)) {
                metadataBean.setTitle((childPage.getTitle() != null && !childPage.getTitle().equals(EMPTY)) ? childPage.getTitle() : childPage.getName());
                metadataBean.setPath(childPage.getPath() + HTML_FILE_EXTENSION);
                metadataBean.setLastModifiedDate(isLastModifiedDateVisible ? getLastModifiedDate(childPage) : EMPTY);
                metadataBean.setlLastModifiedDate(isLastModifiedDateVisible ? getLongLastModifiedDate(childPage) : DEFAULT_LONG_VALUE);
                metadataBean.setShortDescription(isShortDescriptionVisible ? getShortDescription(childPage) : EMPTY);
                metadataBean.setShortText(isShortTextVisible ? getShortText(childPage) : EMPTY);
                listBean.add(metadataBean);
            }
        }
        return sortList(listBean, currentNode);
    }

    private List<CollectionLinkComponentWithoutPresetsMetadataBean> sortList(List<CollectionLinkComponentWithoutPresetsMetadataBean> listBean, Node currentNode) throws RepositoryException {
        boolean isSortingOnLastModifiedDate = isValid(currentNode, SORT_BY_LAST_MODIFIED_DATE);
        boolean isSortingOnTitle = isValid(currentNode, SORT_BY_TITLE);
        if (isSortingOnLastModifiedDate) {
            Comparator<CollectionLinkComponentWithoutPresetsMetadataBean> comparator =
                    Comparator.comparingLong(CollectionLinkComponentWithoutPresetsMetadataBean::getlLastModifiedDate).reversed()
                            .thenComparing(CollectionLinkComponentWithoutPresetsMetadataBean::getTitle, String.CASE_INSENSITIVE_ORDER);
            // Sort the stream:
            Stream<CollectionLinkComponentWithoutPresetsMetadataBean> personStream = listBean.stream().sorted(comparator);
            // Make sure that the output is as expected:
            return personStream.collect(Collectors.toList());
        } else if (isSortingOnTitle && !isSortingOnLastModifiedDate) {
            Comparator<CollectionLinkComponentWithoutPresetsMetadataBean> comparator =
                    Comparator.comparing(CollectionLinkComponentWithoutPresetsMetadataBean::getTitle, String.CASE_INSENSITIVE_ORDER);
            // Sort the stream:
            Stream<CollectionLinkComponentWithoutPresetsMetadataBean> personStream = listBean.stream().sorted(comparator);
            // Make sure that the output is as expected:
            return personStream.collect(Collectors.toList());
        }
        return listBean;
    }

    private String getShortDescription(Page childPage) {
        if (childPage.getProperties().containsKey(DESCRIPTION_METADATA_FOR_SITES)) {
            return childPage.getProperties().get(DESCRIPTION_METADATA_FOR_SITES).toString();
        }
        return EMPTY;
    }

    /**
     * @param page
     * @return String[]
     */
    private String getShortText(Page page) {
        int duration = Integer.valueOf(getDuration());
        DateTime lastModifiedDate;
        DateTime shortTextValidity = null;
        if (page.getProperties().containsKey(PUBLISHED_DATE_TIME_VALUE)) {
            lastModifiedDate = new DateTime(page.getProperties().get(PUBLISHED_DATE_TIME_VALUE));
            shortTextValidity = lastModifiedDate.plusDays(duration);
        }
        if (page.getProperties().containsKey(CONTENT_STATUS) && (shortTextValidity != null && shortTextValidity.isAfterNow())) {
             return page.getProperties().get(CONTENT_STATUS).toString();
        }
        return EMPTY;
    }

    /**
     * Function to get Last Modified Date
     * Last Modified date here refers to last published date
     *
     * @param page
     * @return String
     */
    private String getLastModifiedDate(Page page) {
        if (page.getProperties().containsKey(PUBLISHED_DATE_TIME_VALUE)) {
            DateTime lastModifiedDate = new DateTime(page.getProperties().get(PUBLISHED_DATE_TIME_VALUE));

            RelativeTimeFormat rtf = new RelativeTimeFormat(RELATIVE_TIME_FORMAT);
            String difference =  rtf.format(lastModifiedDate.getMillis(), true);
            if(difference.contains(MINUTES) || difference.contains(SECONDS)){
                return ONE_HOUR_AGO;
            }
            return difference;
        }
        return EMPTY;
    }

    private long getLongLastModifiedDate(Page page) {
        if (page.getProperties().containsKey(PUBLISHED_DATE_TIME_VALUE)) {
            DateTime lastModifiedDate = new DateTime(page.getProperties().get(PUBLISHED_DATE_TIME_VALUE));
            return lastModifiedDate.getMillis()/(SIXTY_SECONDS*MILLISECOND* SIXTY_MINUTES);
        }
        return DEFAULT_LONG_VALUE;
    }

    /**
     * @param node
     * @return String
     * @throws RepositoryException
     */
    private String getLastModifiedDateForAssets(Node node) throws RepositoryException {
        if (node.hasProperty(JCR_CONTENT + PUBLISHED_DATE_TIME_VALUE)) {
            DateTime lastModifiedDate = new DateTime(node.getProperty(JCR_CONTENT + PUBLISHED_DATE_TIME_VALUE).getValue().getDate());
            RelativeTimeFormat rtf = new RelativeTimeFormat(RELATIVE_TIME_FORMAT);
            String difference =  rtf.format(lastModifiedDate.getMillis(), true);
            if(difference.contains(MINUTES) || difference.contains(SECONDS)){
                return ONE_HOUR_AGO;
            }
            return difference;
        }
        return EMPTY;
    }

    private long getLongLastModifiedDateForAssets(Node node) throws RepositoryException {
        if (node.hasProperty(JCR_CONTENT + PUBLISHED_DATE_TIME_VALUE)) {
            DateTime lastModifiedDate = new DateTime(node.getProperty(JCR_CONTENT + PUBLISHED_DATE_TIME_VALUE).getValue().getDate());
            return lastModifiedDate.getMillis()/(SIXTY_SECONDS * MILLISECOND*SIXTY_MINUTES);
        }
        return DEFAULT_LONG_VALUE;
    }

    /**
     * Checks is property should be visible
     *
     * @param currentNode
     * @param property
     * @return boolean
     * @throws RepositoryException
     */
    private boolean isValid(Node currentNode, String property) throws RepositoryException {
        if (currentNode.hasProperty(property)) {
            return currentNode.getProperty(property).getBoolean();
        }
        return true;
    }

    public CollectionLinkComponentWithoutPresetsBean getBean() {
        return this.bean;
    }
}
