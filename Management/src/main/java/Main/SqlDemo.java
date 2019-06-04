package csi311;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.derby.jdbc.EmbeddedDriver;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SqlDemo {

    private static final String DB_URL = "jdbc:derby:csi311-testdb1;create=true";
    private Connection conn = null;
    private PreparedStatement stmt = null;
    private Map<String, Order> orders = new HashMap<String, Order>();
    MachineSpec statemachine;
    int flagIndex = 1;

    public SqlDemo() {
    }

    private void insertStateMachine(int id, String json) {		// Insert the state_machine data into table
        try {
            stmt = conn.prepareStatement("insert into state_machines (tenant_id, json_string) values (? , ?)");
            stmt.setInt(1, id);
            stmt.setString(2, json);
            stmt.executeUpdate();

            stmt.close();
        } catch (SQLException sqlExcept) {
            sqlExcept.printStackTrace();
        }
    }


    public void loadStateMachine(String JsonFilePath) throws Exception {	// Load the state machine into the SQL database
        createConnection();
        delTables();
        createStateMachineTable();
        String json = processStateFile(JsonFilePath);
        statemachine = parseJson(json);
        int id = statemachine.getTenantId();
        insertStateMachine(id, json);

        shutdown();
    }


    void delTables() {	// Drops the tables 
        try {
            String sql = "DROP TABLE state_machines";
            String sql1 = "DROP TABLE order_table";

            stmt = conn.prepareStatement(sql);
            stmt.execute();

            stmt = conn.prepareStatement(sql1);
            stmt.execute();

            stmt.close();
        } catch (SQLException ex) {

        }
    }


    public void loadOrdersFile(String ordersFilePath) throws Exception {	// Load the orders file into the SQL database
        createConnection();
        createOrderTable();
        processOrderFile(ordersFilePath);

        shutdown();
    }


    public void generateReport(int tenantID) throws Exception {		// Generates the entire report similar to project 2
        System.out.println("Tru State");

        Map<String, Integer> countOfMap = new HashMap<String, Integer>();
        Map<String, Float> valueOfMap = new HashMap<String, Float>();
        Integer flaggedCount = 0;

        createConnection();
        getStateMachine(tenantID);
        getOrders(tenantID);

        for (String key : orders.keySet()) {
            Order o = orders.get(key);
            if (!countOfMap.containsKey(o.getState())) {
                countOfMap.put(o.getState(), 0);
                valueOfMap.put(o.getState(), 0.0f);
            }
            if (o.isFlagged()) 
                flaggedCount++;
            
            else {
                countOfMap.put(o.getState(), countOfMap.get(o.getState()) + 1);
                valueOfMap.put(o.getState(), valueOfMap.get(o.getState()) + o.getCost());
            }
        }

        for (String state : countOfMap.keySet()) {
            Float costOf = valueOfMap.get(state);
            if (costOf == null) {
                costOf = 0.0f;
            }
            String terminal = "";
            if (MachineSpec.isTerminalState(statemachine, state)) {
                terminal = "(terminal)";
            }

            String cst = String.format("%.2f", costOf);
            System.out.println(state + " " + countOfMap.get(state) + " $" + cst + " " + terminal);
        }

        System.out.println("flagged " + flaggedCount);

        shutdown();
    }


    private void createConnection() {		// Database creation 
        try {
            Driver derbyEmbeddedDriver = new EmbeddedDriver();
            DriverManager.registerDriver(derbyEmbeddedDriver);
            conn = DriverManager.getConnection(DB_URL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void createStateMachineTable() {		// State machine table is being created into the  SQL database
        int max_chars = 1000;
        try {
            String statement_string
                    = "create table state_machines (tenant_id INT NOT NULL, json_string varchar(" + max_chars + ") not null)";

            stmt = conn.prepareStatement(statement_string);
            stmt.execute();

            stmt.close();
        } 
        catch (SQLException sqlExcept) {
            if (!tableAlreadyExists(sqlExcept)) 
                sqlExcept.printStackTrace();
            
        }
    }


    private String getStateMachine(int tenantId) {		// Gets the data from the state machine table from the SQL database
        String json_string = null;
        try {
            stmt = conn.prepareStatement("select json_string from state_machines where tenant_id = " + tenantId);
            ResultSet results = stmt.executeQuery();

            while (results.next()) {
                json_string = results.getString("json_string");
            }
            statemachine = parseJson(json_string);

            results.close();
            stmt.close();
        } catch (SQLException sqlExcept) {
            sqlExcept.printStackTrace();
        }

        statemachine = parseJson(json_string);

        return json_string;
    }


    private String processStateFile(String fileName) throws Exception {		// Turn the machine file into json format
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        String json = "";
        String line;
        while ((line = br.readLine()) != null) {
            json += " " + line;
        }
        br.close();
        return json.replaceAll("\n", " ").replaceAll("\t", " ").replaceAll("\r", " ");
    }


    private void processOrderFile(String oFileName) throws Exception {		// Read the orders file 
        BufferedReader br = new BufferedReader(new FileReader(oFileName));
        String line = null;
        while ((line = br.readLine()) != null) {
            processOrder(line);
        }
        br.close();
    }
    

    private void createOrderTable() {	// Creates the order table into the SQL database
        int max_chars = 1000;
        try {
            String statement_string
                    = "create table order_table (tenant_id INT NOT NULL, timestamp varchar(" + max_chars + ") not null,"
                    + " order_id varchar(" + max_chars + ") not null,"
                    + " customer_id varchar(" + max_chars + ") not null,"
                    + " order_state varchar(" + max_chars + ") not null,"
                    + " description varchar(" + max_chars + ") not null,"
                    + " quantity INT not null,"
                    + " cost FLOAT not null"
                    + ")";

            stmt = conn.prepareStatement(statement_string);
            stmt.execute();

            stmt.close();
        } catch (SQLException sqlExcept) {
            if (!tableAlreadyExists(sqlExcept)) {
                sqlExcept.printStackTrace();
            }
        }
    }



    private void processOrder(String line) {	// Process the order file
        try {
            // Parse the line item.
            String[] tok = line.split(",");
            Order order = new Order();
            order.setTenant(Integer.valueOf(tok[0].trim()));
            order.setTimeMs(tok[1].trim());
            order.setOrderId(tok[2].trim());
            order.setCustomerId(tok[3].trim());
            order.setState(tok[4].trim().toLowerCase());
            order.setDescription(tok[5].trim());
            order.setQuantity(Integer.valueOf(tok[6].trim()));
            order.setCost(Float.valueOf(tok[7].trim()));

            insertOrders(order);
        } 
        
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void currentOrder(Order newOrder) {		// Validates the given order
        boolean isNew = false;
        if (!orders.containsKey(newOrder.getOrderId())) {
            orders.put(newOrder.getOrderId(), newOrder);
            isNew = true;
        }

        Order olderOrder = orders.get(newOrder.getOrderId());

        if ((!newOrder.validateOrderFields()) ||
                (Long.valueOf(newOrder.getTimeMs()) < Long.valueOf(olderOrder.getTimeMs())) ||
                (!newOrder.getCustomerId().equals(olderOrder.getCustomerId())) ||
                (!MachineSpec.isValidTransition(statemachine, olderOrder.getState(), newOrder.getState(), isNew))) {

            System.out.println("Flagging order " + newOrder.getOrderId());

            olderOrder.setFlagged(true);
            newOrder.setFlagged(olderOrder.isFlagged());
        }

        orders.put(olderOrder.getOrderId(), newOrder);
    }
    

    private MachineSpec parseJson(String json) {		// Returns the machineSpec 
        ObjectMapper mapper = new ObjectMapper();
        try {
            MachineSpec machineSpec = mapper.readValue(json, MachineSpec.class);
            return machineSpec;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean tableAlreadyExists(SQLException e) {
        boolean exists;
        if (e.getSQLState().equals("X0Y32")) {
            exists = true;
        } else {
            exists = false;
        }
        return exists;
    }


    private void insertOrders(Order order) {		// Insert data into the order table
        try {
            stmt = conn.prepareStatement("insert into order_table (tenant_id, timestamp, order_id, customer_id, order_state, description,"
                    + " quantity, cost) values (? , ?, ?, ?, ? , ?, ?, ?)");
            stmt.setInt(1, order.getTenant());
            stmt.setString(2, order.getTimeMs());
            stmt.setString(3, order.getOrderId());
            stmt.setString(4, order.getCustomerId());
            stmt.setString(5, order.getState());
            stmt.setString(6, order.getDescription());
            stmt.setInt(7, order.getQuantity());
            stmt.setFloat(8, order.getCost());
            stmt.execute();

            stmt.close();
        } catch (SQLException sqlExcept) {
            sqlExcept.printStackTrace();
        }
    }


    private void getOrders(int tenantId) throws SQLException {		// Gets the order data 
        createConnection();
        stmt = conn.prepareStatement("select * from order_table where tenant_id = " + tenantId);
        ResultSet resultSet = stmt.executeQuery();
        ResultSetMetaData rsmd = resultSet.getMetaData();
        int columnsNumber = rsmd.getColumnCount();
        orders.clear();
        //System.out.println(resultSet.getFetchSize());
        while (resultSet.next()) {
            Order order = new Order();

            order.setTenant(resultSet.getInt(1));
            order.setTimeMs(resultSet.getString(2));
            order.setOrderId(resultSet.getString(3));
            order.setCustomerId(resultSet.getString(4));
            order.setState(resultSet.getString(5));
            order.setDescription(resultSet.getString(6));
            order.setQuantity(resultSet.getInt(7));
            order.setCost(resultSet.getFloat(8));

            currentOrder(order);
        }
    }

    private void shutdown() {
        try {
            if (stmt != null) {
                stmt.close();
            }
            if (conn != null) {
                DriverManager.getConnection(DB_URL + ";shutdown=true");
                conn.close();
            }
        } catch (SQLException sqlExcept) {
        }
    }

    
    public static void main(String[] args) {	// Takes the arguments from command line and runs according to its implementation
        SqlDemo theApp = new SqlDemo();
        String mode = null;

        //Verifying that only two arguments have been passed
        if (args.length == 2) {
            mode = args[0];
            try {
                if (mode.equals("--state")) {
                    String JsonFilePath = args[1];
                    theApp.loadStateMachine(JsonFilePath);
                } 
                else if (mode.equals("--order")) {
                    String orderFilePath = args[1];
                    theApp.loadOrdersFile(orderFilePath);
                } 
                else if (mode.equals("--report")) {
                    String tenantID = args[1];
                    //Using regex to verify that tenant ID has 5 digits
                    if (tenantID.matches("[0-9]{5}")) {
                        theApp.generateReport(Integer.parseInt(tenantID));
                    } 
                    else 
                        System.out.println("Malformed tenant ID");                   
                } 
                else 
                    System.out.println("Invalid mode option");
                
            } 
            catch (Exception e) {
                e.printStackTrace();
            }
        } 
        else 
            System.out.println("Wrong number of arguments");
        
    }
}
