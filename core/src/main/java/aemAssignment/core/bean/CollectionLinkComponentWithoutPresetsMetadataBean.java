package aemAssignment.core.bean;

/**
 * Created by vn78228 on 4/3/2017.
 */
public class CollectionLinkComponentWithoutPresetsMetadataBean {
    private String title;
    private String path;
    private String lastModifiedDate;
    private String shortDescription;
    private String shortText;
    private String shortTextForPdf;
    private long lLastModifiedDate;

    public String getShortTextForPdf() {
        return shortTextForPdf;
    }

    public void setShortTextForPdf(String shortTextForPdf) {
        this.shortTextForPdf = shortTextForPdf;
    }

    public String getShortText() {
        return shortText;
    }

    public void setShortText(String shortText) {
        this.shortText = shortText;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public long getlLastModifiedDate() {
        return lLastModifiedDate;
    }

    public void setlLastModifiedDate(long lLastModifiedDate) {
        this.lLastModifiedDate = lLastModifiedDate;
    }
}
