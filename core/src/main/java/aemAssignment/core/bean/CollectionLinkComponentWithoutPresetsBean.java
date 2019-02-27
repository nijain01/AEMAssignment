package aemAssignment.core.bean;

import java.util.List;

/**
 * Created by vn78228 on 4/4/2017.
 */
public class CollectionLinkComponentWithoutPresetsBean {
    private String heading;
    private String pathTitle;
    private List<CollectionLinkComponentWithoutPresetsMetadataBean> metadataBeanList;

    public String getPathTitle() {
        return pathTitle;
    }

    public void setPathTitle(String pathTitle) {
        this.pathTitle = pathTitle;
    }

    public List<CollectionLinkComponentWithoutPresetsMetadataBean> getMetadataBeanList() {
        return metadataBeanList;
    }

    public void setMetadataBeanList(List<CollectionLinkComponentWithoutPresetsMetadataBean> metadataBeanList) {
        this.metadataBeanList = metadataBeanList;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }
}
