package beans;

import java.util.Date;

public class News implements java.io.Serializable {

    private Long idnews;
    private String title;
    private String description;
    private String text;
    private Date date;

    public Long getIdnews() {
        return idnews;
    }

    public void setIdnews(Long idnews) {
        this.idnews = idnews;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}