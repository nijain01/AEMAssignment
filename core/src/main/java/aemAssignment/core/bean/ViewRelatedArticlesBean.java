package aemAssignment.core.bean;

/**
 * Created by vn78228 on 4/14/2017.
 */
public class ViewRelatedArticlesBean {
    private String title;
    private String path;
    private String relativePublishedDate;
    private String imagePath;
    private String name;

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

    public String getRelativePublishedDate() {
        return relativePublishedDate;
    }

    public void setRelativePublishedDate(String relativePublishedDate) {
        this.relativePublishedDate = relativePublishedDate;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
