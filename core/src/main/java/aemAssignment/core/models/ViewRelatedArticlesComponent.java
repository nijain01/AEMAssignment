package aemAssignment.core.models;

import static aemAssignment.core.constants.SampleConstants.HEADING_PATH_FOR_ARTICLE;
import static aemAssignment.core.constants.SampleConstants.IMAGE_FILE_PATH;
import static aemAssignment.core.constants.SampleConstants.IMAGE_FILE_REF_PATH;
import static aemAssignment.core.constants.SampleConstants.IMAGE_NEW_NODE;
import static aemAssignment.core.constants.SampleConstants.JCR_CONTENT;
import static aemAssignment.core.constants.SampleConstants.JCR_TITLE;
import static aemAssignment.core.constants.SampleConstants.LIMIT;
import static aemAssignment.core.constants.SampleConstants.PATH_TO_SEARCH;
import static aemAssignment.core.constants.SampleConstants.PUBLISHED_DATE_TIME_VALUE;

import aemAssignment.core.bean.ViewRelatedArticlesBean;
import com.adobe.cq.sightly.WCMUsePojo;
import com.day.cq.commons.date.RelativeTimeFormat;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.tagging.Tag;
import com.day.cq.wcm.foundation.Image;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Model(adaptables = SlingHttpServletRequest.class)
public class ViewRelatedArticlesComponent extends WCMUsePojo {

    private List<ViewRelatedArticlesBean> articlesBean;
    private Session session;
    private QueryBuilder queryBuilder;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void activate() throws Exception {
        this.session = getResourceResolver().adaptTo(Session.class);
        this.queryBuilder = getResourceResolver().adaptTo(QueryBuilder.class);

        if (getCurrentPage().getTags().length > 0) {
            this.articlesBean = getRelatedArticles();
        } else {
            this.articlesBean = new ArrayList<>();
        }
    }

    /**
     * @return
     * @throws RepositoryException
     */
    private List<ViewRelatedArticlesBean> getRelatedArticles() throws RepositoryException {
        Query query = queryBuilder.createQuery(PredicateGroup.create(getQueryMap()), this.session);
        SearchResult result = query.getResult();
        List<ViewRelatedArticlesBean> listbean = new ArrayList<>();

        for (Hit hit : result.getHits()) {
            Node node = hit.getNode();
            ViewRelatedArticlesBean bean = new ViewRelatedArticlesBean();
            bean.setPath(node.getPath());
            bean.setName(node.getName());
            bean.setRelativePublishedDate(getRelativePublishedDate(node));
            bean.setImagePath(getImagePath(node));
            bean.setTitle(getTitle(node));
            listbean.add(bean);
        }
        return listbean;
    }

    private String getTitle(Node node) throws RepositoryException {
        if (node.hasProperty(JCR_CONTENT + HEADING_PATH_FOR_ARTICLE)) {
            return node.getProperty(JCR_CONTENT + HEADING_PATH_FOR_ARTICLE).getValue().getString();
        } else if (node.hasProperty(JCR_CONTENT + JCR_TITLE)) {
            return node.getProperty(JCR_CONTENT + JCR_TITLE).getValue().getString();
        }
        return StringUtils.EMPTY;
    }

    private String getImagePath(Node node) throws RepositoryException {
        if (node.hasNode(JCR_CONTENT + IMAGE_FILE_PATH)) {
            logger.info("fileReference Node not present, External Image");

            Node jcrContent = node.getNode(JCR_CONTENT + IMAGE_NEW_NODE);
            String imagePath = jcrContent.getPath();
            Image image = new Image(getResourceResolver().getResource(imagePath));
            String href = image.getHref();
            String imagePathSplitted[] = href.split("/");
            String imgPath = imagePathSplitted[imagePathSplitted.length - 1];
            imgPath = imagePath + ".img" + image.getExtension() + "/" + imgPath;
            return imgPath;
        } else if (node.hasProperty(JCR_CONTENT + IMAGE_FILE_REF_PATH)) {
            logger.info("fileReference Node Present");
            return node.getProperty(JCR_CONTENT + IMAGE_FILE_REF_PATH).getValue().getString();
        }
        return StringUtils.EMPTY;
    }

    private String getRelativePublishedDate(Node node) throws RepositoryException {
        if (node.hasProperty(JCR_CONTENT + PUBLISHED_DATE_TIME_VALUE)) {
            DateTime lastModifiedDate = new DateTime(node.getProperty(JCR_CONTENT + PUBLISHED_DATE_TIME_VALUE).getValue().getDate());
            RelativeTimeFormat rtf = new RelativeTimeFormat("r");
            String difference = rtf.format(lastModifiedDate.getMillis(), true);
            if (difference.contains("minutes") || difference.contains("seconds")) {
                return "1 hour ago";
            }
            return difference;
        }
        return StringUtils.EMPTY;
    }

    public Map<String, String> getQueryMap() {

        Map<String, String> map = new HashMap<>();

        map.put("type", "cq:Page");
        map.put("path", PATH_TO_SEARCH);
        map.put("path.self", "false");
        map.put("1_property", "jcr:content/cq:tags");
        Tag[] tags = getCurrentPage().getTags();
        int i = 1;
        for (Tag tag : tags) {
            String property = "1_property." + i++ + "_value";
            String value = tag.getTagID();
            map.put(property, value);
        }
        map.put("2_property", "@jcr:created");
        map.put("2_property.operation", "exists");
        map.put("2_property.value", "true");
        map.put("3_property", "@jcr:content/jcr:title");
        map.put("3_property.value", getPageProperties().get("jcr:title").toString());
        map.put("3_property.operation", "unequals");


        map.put("orderby", "@jcr:created");
        map.put("orderby.sort", "desc"); // desc for descending , "" for ascending
        map.put("p.hits", "full"); // return all the properties in a node
        map.put(LIMIT, "-1");
        return map;
    }

    public List<ViewRelatedArticlesBean> getArticlesBean() {
        return articlesBean;
    }
}

