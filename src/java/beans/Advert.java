package beans;

import java.io.Serializable;
import java.util.Date;

public class Advert implements Serializable {

    private Long idadvert;
    private String title;
    private String description;
    private String text;
    private Date date;

    public Long getIdadvert() {
        return idadvert;
    }

    public void setIdadvert(Long idadvert) {
        this.idadvert = idadvert;
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
