package controllers;

import beans.Report;
import beans.User;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import serviceclass.Database;

@ManagedBean(eager = true)
@SessionScoped
public final class ReportController implements Serializable {

    //<editor-fold defaultstate="collapsed" desc="fields">
    private ArrayList<Report> reportList; // текущий список отчетов
    private String currentUser; // id текущего пользователя
    private String userFio; // фио текущего пользователя
    private ArrayList<User> users;
    private Map<String, Object> performerList; // список испольнителей (+все)
    private Map<String, Object> performerList2; // список испольнителей 
    private Map<String, Object> equipUserList; // список пользователей
    private Map<String, Object> equipModelList; // список моделей оборудований
    private Map<String, Object> equipInventList; // список инвентарных номеров
    private Map<String, Object> dateCreateList; // список даты отчетов 
    private Map<String, Object> dateCreateListForUsers; // список даты отчетов 
    //поля для вставки в базу
    private String insertDataBegin; //
    private String insertEqUser; //
    private String insertModel; //
    private String insertInvent; //
    private String insertSerial; //
    private String insertDescr; //
    private String insertDecision; //
    private String insertDateEnd; //
    private String insertPerformer; //
    private String insertDepartment; //
    private boolean edit;
    // поля для формы
    private String selectedPerformer = "all"; // выбранный performer
    private String selectedDate = "all"; // выбранный дата отчета
    private String selectedStatus = "all"; // выбранный состояние отчета
    private String selectedDepartment = "all"; // выбранный отчет отдела
    private String sqlCriterion; // запрос с критерием
    private String sqlCriterionForUser; // запрос с критерием
    //для pagination
    private boolean requestFromPager;  // true если запрос из pagera
    private int reportsOnPage = 10; // количество отчетов на странице
    private long selectedPageNumber = 1; // выбранный номер pagera
    private long totalReportsCount; // общее количество отчетов
    private ArrayList<Integer> pageNumbers = new ArrayList<Integer>(); // список номеров pagera
    private String currentSql; // текущий sql запрос без лимита
    private int pagesCount;

    //</editor-fold>
    //<editor-fold defaultstate="collapsed" desc="service methods">
    public ReportController() // конструктор 
    {
        fillDatesBySQLForUsers();
        fillReportsAll();
        fillPerformersBySQL();
        fillPerformersForSubmit();
        fillDatesBySQL();
        fillEquipUsersForSubmit();
        fillEquipModelsForSubmit();
        fillEquipInventForSubmit();
        ResourceBundle bundle = ResourceBundle.getBundle("locale.properties.languages", FacesContext.getCurrentInstance().getViewRoot().getLocale());
    }

    public void fillReportsAll() // выборка всех отчетов
    {
        selectedPageNumber = 1;
        requestFromPager = false;
        fillReportBySQL("select * from report where (status = 1) order by id desc");
    }

    public String fillReportsByCriterion() // выборка отчетов по критерию из формы
    {
        selectedPageNumber = 1;
        requestFromPager = false;
        sqlCriterionBuild();
        fillReportBySQL(sqlCriterion);
        return "cabinet";
    }

    public String fillReportsByCriterionForUser() // выборка отчетов по критерию из формы
    {
        selectedPageNumber = 1;
        requestFromPager = false;
        sqlCriterionBuildByUser();
        fillReportBySQL(sqlCriterionForUser);
        return "cabinet";
    }

    public void selectPage()// выборка из номера pagera
    {
        Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();
        selectedPageNumber = Integer.valueOf(params.get("page_number"));
        requestFromPager = true;
        System.out.println(currentSql);
        fillReportBySQL(currentSql);
    }

    private void fillReportBySQL(String sql) // выборка отчетов по sql 
    {
        StringBuilder sqlBuilder = new StringBuilder(sql);

        currentSql = sql;

        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;

        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();
            System.out.println(currentSql);
            if (!requestFromPager) {
                rs = stmt.executeQuery(sqlBuilder.toString());
                rs.last();

                totalReportsCount = rs.getRow();
                fillPageNumbers(totalReportsCount, reportsOnPage);
            }

            if (totalReportsCount > reportsOnPage) {
                sqlBuilder.append(" limit ").append(selectedPageNumber * reportsOnPage - reportsOnPage).append(",").append(reportsOnPage);
            }

            rs = stmt.executeQuery(sqlBuilder.toString());

            reportList = new ArrayList<Report>();

            while (rs.next()) {

                Report rep = new Report();
                rep.setId(rs.getLong("id"));
                rep.setDateBegin(rs.getString("date_begin"));
                rep.setUser(rs.getString("user"));
                rep.setModel(rs.getString("model"));
                rep.setInventory(rs.getString("inventory"));
                rep.setSerial(rs.getString("serial"));
                rep.setDescription(rs.getString("description"));
                rep.setDesicion(rs.getString("desicion"));
                rep.setDateEnd(rs.getString("date_end"));
                rep.setPerformer(rs.getString("performer"));
                rep.setDateCreate(rs.getString("date_create"));
                rep.setStatus(rs.getInt("status"));
                rep.setDepartment(rs.getString("department"));
                reportList.add(rep);
            }

        } catch (SQLException ex) {
            Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void fillPerformersBySQL() // выборка списка испольнителей
    {

        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;

        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();

            rs = stmt.executeQuery("select distinct(performer) from Report order by performer");

            performerList = new LinkedHashMap<String, Object>();
            performerList.put("Все", "all");
            while (rs.next()) {
                performerList.put(rs.getString("performer"), rs.getString("performer")); //label, value
            }
        } catch (SQLException ex) {
            Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void fillPerformersForSubmit() // выборка списка испольнителей
    {

        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;

        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();

            rs = stmt.executeQuery("select distinct(performer) from Report order by performer");

            performerList2 = new LinkedHashMap<String, Object>();
            while (rs.next()) {
                performerList2.put(rs.getString("performer"), rs.getString("performer")); //label, value
            }
        } catch (SQLException ex) {
            Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void fillEquipUsersForSubmit() // выборка списка пользователей
    {

        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;

        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();

            rs = stmt.executeQuery("select user_name from sp_eq_user order by user_name");

            equipUserList = new LinkedHashMap<String, Object>();
            while (rs.next()) {
                equipUserList.put(rs.getString("user_name"), rs.getString("user_name")); //label, value
            }
        } catch (SQLException ex) {
            Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void fillEquipModelsForSubmit() // выборка списка моделей оборудований
    {

        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;

        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();

            rs = stmt.executeQuery("select model_name from sp_model");

            equipModelList = new LinkedHashMap<String, Object>();
            while (rs.next()) {
                equipModelList.put(rs.getString("model_name"), rs.getString("model_name")); //label, value
            }
        } catch (SQLException ex) {
            Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void fillEquipInventForSubmit() // выборка списка инвентарных номеров
    {

        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;

        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();

            rs = stmt.executeQuery("SELECT inventory FROM equipment");

            equipInventList = new LinkedHashMap<String, Object>();
            while (rs.next()) {
                equipInventList.put(rs.getString("inventory"), rs.getString("inventory")); //label, value
            }
        } catch (SQLException ex) {
            Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private void fillDatesBySQL() // выборка всех даты отчетов
    {

        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;

        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();

            rs = stmt.executeQuery("select distinct(date_create) from Report order by date_create desc");

            dateCreateList = new LinkedHashMap<String, Object>();
            dateCreateList.put("Все", "all");
            while (rs.next()) {
                dateCreateList.put(rs.getDate("date_create").toString(), rs.getDate("date_create").toString());
            }
        } catch (Exception ex) {
            Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
                Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void fillDatesBySQLForUsers() // выборка всех даты отчетов
    {

        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;

        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();

            rs = stmt.executeQuery("select distinct(date_create) from Report where (performer = '" + userFio + "') order by date_create desc");

            dateCreateListForUsers = new LinkedHashMap<String, Object>();
            dateCreateListForUsers.put("Все", "all");
            while (rs.next()) {
                dateCreateListForUsers.put(rs.getDate("date_create").toString(), rs.getDate("date_create").toString());
            }
        } catch (Exception ex) {
            Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception ex) {
                Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void sqlCriterionBuild() // создание sql строки по значению из формы
    {
        StringBuilder query = new StringBuilder();
        query.append("select *from report ");

        if ((selectedPerformer.equalsIgnoreCase("all"))) {
            query.append("");
        } else {
            query.append("where (performer = '").append(selectedPerformer).append("') ");
        }


        if (!(selectedPerformer.equalsIgnoreCase("all")) && !(selectedStatus.equalsIgnoreCase("all"))) {
            query.append(" and (status = '").append(selectedStatus).append("') ");
        } else if ((selectedPerformer.equalsIgnoreCase("all")) && !(selectedStatus.equalsIgnoreCase("all"))) {
            query.append(" where (status = '").append(selectedStatus).append("') ");
        } else {
            query.append("");
        }


        if (!(selectedDepartment.equalsIgnoreCase("all"))) {

            if (selectedPerformer.equalsIgnoreCase("all") && selectedStatus.equalsIgnoreCase("all")) {
                query.append(" where (department = '").append(selectedDepartment).append("')");
            } else {
                query.append(" and (department = '").append(selectedDepartment).append("')");
            }
        } else {
            query.append("");
        }


        if (!(selectedDate.equalsIgnoreCase("all"))) {

            if (selectedPerformer.equalsIgnoreCase("all") && selectedStatus.equalsIgnoreCase("all") && selectedDepartment.equalsIgnoreCase("all")) {
                query.append(" where (date_create = '").append(selectedDate).append("') ");
            } else {
                query.append(" and (date_create = '").append(selectedDate).append("') ");
            }
        } else {
            query.append("");
        }

        query.append(" order by id desc ");

        sqlCriterion = query.toString();
    }

    private void fillPageNumbers(long totalReportsCount, int reportsOnPage) // заполнение коллекции номерами страницы
    {
        int pageCount = 0;

        if (totalReportsCount % reportsOnPage == 0) {
            pageCount = reportsOnPage > 0 ? (int) (totalReportsCount / reportsOnPage) : 0;
        } else {
            pageCount = reportsOnPage > 0 ? (int) (totalReportsCount / reportsOnPage) + 1 : 0;
        }

        pageNumbers.clear();
        for (int i = 1; i <= pageCount; i++) {
            pageNumbers.add(i);

        }
        pagesCount = pageNumbers.size();

    }

    public String getFioOnID() // выборка фио пользователья
    {

        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;

        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();

            Map<String, String> params = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap();

            String userid = params.get("username").toString();
            currentUser = userid;

            rs = stmt.executeQuery("select * from users where (userid = '" + currentUser + "')");

            while (rs.next()) {

                userFio = rs.getString("fio");
                System.out.println("userFIO =" + userFio);
            }

        } catch (SQLException ex) {
            Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException ex) {
                Logger.getLogger(ReportController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return "cabinet";
    }

    private void sqlCriterionBuildByUser() // создание sql строки по значению из формы
    {


        StringBuilder query = new StringBuilder();
        query.append("select *from report where (performer = '").append(userFio).append("')");

        if (!(selectedStatus.equalsIgnoreCase("all"))) {

            query.append(" and (status = '").append(selectedStatus).append("')");
        } else {
            query.append("");
        }


        if (!(selectedDate.equalsIgnoreCase("all"))) {

            query.append(" and (date_create = '").append(selectedDate).append("') ");
        } else {
            query.append("");
        }

        query.append(" order by id desc ");

        sqlCriterionForUser = query.toString();
    }
//<editor-fold defaultstate="collapsed" desc="UPDATE методы">

    public String insertReport() {

        Statement stmt = null;
        ResultSet rs = null;
        Connection conn = null;

        try {
            conn = Database.getConnection();
            stmt = conn.createStatement();
            String insert = "INSERT INTO `report` (`date_begin`, `user`, `model`, `inventory`, `serial`, "
                    + " `description`, `desicion`, `date_end`, `performer`, `date_create`,  `status`, `department`) VALUES ( "
                    + " '" + insertDataBegin + "', '" + insertEqUser + "', '" + insertModel + "', '" + insertInvent + "', '" + insertSerial + "',"
                    + " '" + insertDescr + "', '" + insertDecision + " ', '" + insertDateEnd + "', '" + insertPerformer + "', '" + insertDateEnd + "', '0', '" + insertDepartment + "') ";
            stmt.executeUpdate(insert);



        } catch (SQLException ex) {
            Logger.getLogger(ReportController.class
                    .getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();


                }
            } catch (SQLException ex) {
                Logger.getLogger(ReportController.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }

        return "cabinet";
    }

    public String deleteReportOnId() {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        Connection conn = null;

        try {
            conn = Database.getConnection();
            prepStmt = conn.prepareStatement(" DELETE FROM report WHERE id=? ");

            for (Report rep : reportList) {
                if (!rep.isEdit()) {
                    continue;
                }
                prepStmt.setLong(1, rep.getId());
                prepStmt.addBatch();
            }


            prepStmt.executeBatch();




        } catch (SQLException ex) {
            Logger.getLogger(ReportController.class
                    .getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (prepStmt != null) {
                    prepStmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();


                }
            } catch (SQLException ex) {
                Logger.getLogger(ReportController.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }
        cancelEdit();
        return "cabinet";
    }

    public void booksOnPageChanged(ValueChangeEvent e) {
        requestFromPager = false;
        reportsOnPage = Integer.valueOf(e.getNewValue().toString()).intValue();
        selectedPageNumber = 1;
        fillReportBySQL(currentSql);
    }

    public String updateReports() {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        Connection conn = null;

        try {
            conn = Database.getConnection();
            prepStmt = conn.prepareStatement("update report set date_begin=?, user=?, model=?, inventory=?, serial=?, "
                    + " description=?, desicion=?, date_end=?, performer=?, date_create=?, department=?  where id=?");


            for (Report rep : reportList) {
                if (!rep.isEdit()) {
                    continue;
                }
                prepStmt.setString(1, rep.getDateBegin());
                prepStmt.setString(2, rep.getUser());
                prepStmt.setString(3, rep.getModel());
                prepStmt.setString(4, rep.getInventory());
                prepStmt.setString(5, rep.getSerial());
                prepStmt.setString(6, rep.getDescription());
                prepStmt.setString(7, rep.getDesicion());
                prepStmt.setString(8, rep.getDateEnd());
                prepStmt.setString(9, rep.getPerformer());
                prepStmt.setString(10, rep.getDateEnd());
                prepStmt.setString(11, rep.getDepartment());
                prepStmt.setLong(12, rep.getId());
                prepStmt.addBatch();
            }


            prepStmt.executeBatch();




        } catch (SQLException ex) {
            Logger.getLogger(ReportController.class
                    .getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (prepStmt != null) {
                    prepStmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();


                }
            } catch (SQLException ex) {
                Logger.getLogger(ReportController.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }

        cancelEdit();

        return "cabinet";
    }

    public String saveReport() {

        PreparedStatement prepStmt = null;
        ResultSet rs = null;
        Connection conn = null;

        try {
            conn = Database.getConnection();
            prepStmt = conn.prepareStatement("update report set status ='1' where id=?");


            for (Report rep : reportList) {
                if (!rep.isEdit()) {
                    continue;
                }
                prepStmt.setLong(1, rep.getId());
                prepStmt.addBatch();
            }


            prepStmt.executeBatch();




        } catch (SQLException ex) {
            Logger.getLogger(ReportController.class
                    .getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (prepStmt != null) {
                    prepStmt.close();
                }
                if (rs != null) {
                    rs.close();
                }
                if (conn != null) {
                    conn.close();


                }
            } catch (SQLException ex) {
                Logger.getLogger(ReportController.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
        }

        cancelEdit();

        return "cabinet";
    }

    public boolean isEditMode() {
        return edit;
    }

    public void showEdit() {
        edit = true;
    }

    public String cancelEdit() {
        edit = false;
        for (Report report : reportList) {
            report.setEdit(false);
        }
        return "cabinet";
    }
    //</editor-fold>

//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public String getSelectedPerformer() {
        return selectedPerformer;
    }

    public Map<String, Object> getPerformerList() {
        return performerList;
    }

    public void setReportList(ArrayList<Report> reportList) {
        this.reportList = reportList;
    }

    public void setPerformerList(Map<String, Object> performerList) {
        this.performerList = performerList;
    }

    public void setSelectedPerformer(String selectedPerformer) {
        this.selectedPerformer = selectedPerformer;
    }

    public Map<String, Object> getDateCreateList() {
        return dateCreateList;
    }

    public void setDateCreateList(Map<String, Object> dateCreateList) {
        this.dateCreateList = dateCreateList;
    }

    public ArrayList<Report> getReportList() {
        return reportList;
    }

    public String getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(String selectedDate) {
        this.selectedDate = selectedDate;
    }

    public String getSelectedStatus() {
        return selectedStatus;
    }

    public void setSelectedStatus(String selectedStatus) {
        this.selectedStatus = selectedStatus;
    }

    public String getSelectedDepartment() {
        return selectedDepartment;
    }

    public void setSelectedDepartment(String selectedDepartment) {
        this.selectedDepartment = selectedDepartment;
    }

    public String getSqlCriterion() {
        return sqlCriterion;
    }

    public void setSqlCriterion(String sqlCriterion) {
        this.sqlCriterion = sqlCriterion;
    }

    public boolean isRequestFromPager() {
        return requestFromPager;
    }

    public void setRequestFromPager(boolean requestFromPager) {
        this.requestFromPager = requestFromPager;
    }

    public int getReportsOnPage() {
        return reportsOnPage;
    }

    public void setReportsOnPage(int reportsOnPage) {
        this.reportsOnPage = reportsOnPage;
    }

    public long getSelectedPageNumber() {
        return selectedPageNumber;
    }

    public void setSelectedPageNumber(long selectedPageNumber) {
        this.selectedPageNumber = selectedPageNumber;
    }

    public long getTotalReportsCount() {
        return totalReportsCount;
    }

    public void setTotalReportsCount(long totalReportsCount) {
        this.totalReportsCount = totalReportsCount;
    }

    public ArrayList<Integer> getPageNumbers() {
        return pageNumbers;
    }

    public void setPageNumbers(ArrayList<Integer> pageNumbers) {
        this.pageNumbers = pageNumbers;
    }

    public String getCurrentSql() {
        return currentSql;
    }

    public void setCurrentSql(String currentSql) {
        this.currentSql = currentSql;
    }

    public Map<String, Object> getPerformerList2() {
        return performerList2;
    }

    public void setPerformerList2(Map<String, Object> performerList2) {
        this.performerList2 = performerList2;
    }

    public Map<String, Object> getEquipUserList() {
        return equipUserList;
    }

    public void setEquipUserList(Map<String, Object> equipUserList) {
        this.equipUserList = equipUserList;
    }

    public Map<String, Object> getEquipModelList() {
        return equipModelList;
    }

    public void setEquipModelList(Map<String, Object> equipModelList) {
        this.equipModelList = equipModelList;
    }

    public Map<String, Object> getEquipInventList() {
        return equipInventList;
    }

    public void setEquipInventList(Map<String, Object> equipInventList) {
        this.equipInventList = equipInventList;
    }

    public String getInsertDataBegin() {
        return insertDataBegin;
    }

    public void setInsertDataBegin(String insertDataBegin) {
        this.insertDataBegin = insertDataBegin;
    }

    public String getInsertEqUser() {
        return insertEqUser;
    }

    public void setInsertEqUser(String insertEqUser) {
        this.insertEqUser = insertEqUser;
    }

    public String getInsertModel() {
        return insertModel;
    }

    public void setInsertModel(String insertModel) {
        this.insertModel = insertModel;
    }

    public String getInsertInvent() {
        return insertInvent;
    }

    public void setInsertInvent(String insertInvent) {
        this.insertInvent = insertInvent;
    }

    public String getInsertSerial() {
        return insertSerial;
    }

    public void setInsertSerial(String insertSerial) {
        this.insertSerial = insertSerial;
    }

    public String getInsertDescr() {
        return insertDescr;
    }

    public void setInsertDescr(String insertDescr) {
        this.insertDescr = insertDescr;
    }

    public String getInsertDecision() {
        return insertDecision;
    }

    public void setInsertDecision(String insertDecision) {
        this.insertDecision = insertDecision;
    }

    public String getInsertDateEnd() {
        return insertDateEnd;
    }

    public void setInsertDateEnd(String insertDateEnd) {
        this.insertDateEnd = insertDateEnd;
    }

    public String getInsertPerformer() {
        return insertPerformer;
    }

    public void setInsertPerformer(String insertPerformer) {
        this.insertPerformer = insertPerformer;
    }

    public String getInsertDepartment() {
        return insertDepartment;
    }

    public void setInsertDepartment(String insertDepartment) {
        this.insertDepartment = insertDepartment;
    }

    public boolean isEdit() {
        return edit;
    }

    public void setEdit(boolean edit) {
        this.edit = edit;
    }

    public int getPagesCount() {
        return pagesCount;
    }

    public void setPagesCount(int pagesCount) {
        this.pagesCount = pagesCount;
    }
    //</editor-fold>

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }

    public String getUserFio() {
        return userFio;
    }

    public void setUserFio(String userFio) {
        this.userFio = userFio;
    }

    public ArrayList<User> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }

    public String getSqlCriterionForUser() {
        return sqlCriterionForUser;
    }

    public void setSqlCriterionForUser(String sqlCriterionForUser) {
        this.sqlCriterionForUser = sqlCriterionForUser;
    }

    public Map<String, Object> getDateCreateListForUsers() {
        return dateCreateListForUsers;
    }

    public void setDateCreateListForUsers(Map<String, Object> dateCreateListForUsers) {
        this.dateCreateListForUsers = dateCreateListForUsers;
    }
}
