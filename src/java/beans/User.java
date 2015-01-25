package beans;

import controllers.ReportController;
import java.io.Serializable;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;


@ManagedBean
@SessionScoped
public class User implements Serializable {

    private String username;//login 
    private String password; //пароль
    private String fio; //имя
    private String status; //должность
    private String contact; //контакт

    public User() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFio() {
        return fio;
    }

    public void setFio(String fio) {
        this.fio = fio;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String login() {
        try {

            try {
                Thread.sleep(1000);// имитация загрузки процесса
            } catch (InterruptedException ex) {
                Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
            }

            ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest()).login(username, password);

            return "main";
        } catch (ServletException ex) {
            ResourceBundle bundle = ResourceBundle.getBundle("locale.properties.languages", FacesContext.getCurrentInstance().getViewRoot().getLocale());
            Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
            FacesContext context = FacesContext.getCurrentInstance();
            FacesMessage message = new FacesMessage(bundle.getString("login_error"));
            message.setSeverity(FacesMessage.SEVERITY_ERROR);
            context.addMessage("login_form", message);

        }

        return "index";

    }

    public String logout() {
        String result = "/index.xhtml?faces-redirect=true";

        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        try {
            request.logout();
        } catch (ServletException e) {
            Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, e);
        }

        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();

        return result;
    }

}
