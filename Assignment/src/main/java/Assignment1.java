
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Iterator;
import java.util.Set;

public class Testing {


    private OkHttpClient client = new OkHttpClient();


    String run(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            return response.body().string();
        }
    }

    private static Connection getConnect(){
        Connection conn = null;
        try{
            Class.forName("org.postgresql.Driver");
            String url = "jdbc:postgresql://localhost:5432/user";
            conn = DriverManager.getConnection(url,"postgres","root");
        }catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }


    private static void searchFunc(String repo) throws JSONException {
        String url = "https://api.github.com/search/repositories?q=";
        url = url+ repo;
        //System.out.println(url+"--------------"+repo);

        Testing example = new Testing();
        String rate="";
        try {
            rate = example.run(url);
        }
        catch(IOException e){
            e.printStackTrace();
        }
        //System.out.println(rate);

        JSONObject obj = new JSONObject(rate);

        JSONArray allItems = obj.getJSONArray("items");

        for(int itr =0;itr< allItems.length(); itr++){
            JSONObject itrObj = (JSONObject)allItems.get(itr);
            String name = (String)itrObj.get("full_name");
            name = name.split("/")[0];

            System.out.printf("owner = %-20s repo name = %-20s star count = %-10s fork count = %-10s \n",name,itrObj.get("name"),itrObj.get("stargazers_count"),itrObj.get("forks"));

        }
        System.out.println("total: " + obj.get("total_count"));

    }


    private static  void importFunc(String ID) throws IOException, JSONException {
        String url1 = "https://api.github.com/repositories/" + ID;
        Testing example = new Testing();
        //retrieving the fullname/repositoryname

        System.out.println(url1);
        String repoDetails = example.run(url1);
        JSONObject obj = new JSONObject(repoDetails);
        repoDetails = (String) obj.get("full_name");

        //repoDetails = repoDetails.split("\"full_name\":")[1].split(",")[0].split("\"")[1].split("\"")[0];
        System.out.println(repoDetails);

        //7796523 for sample testing


        String url = "https://raw.githubusercontent.com/" + repoDetails + "/master/package.json";
        String rate = example.run(url);
        System.out.println(url);

        if (!rate.equals("404: Not Found\n")) {
            List<String> packages = new ArrayList<>();
            JSONObject packageObj = new JSONObject(rate);
            JSONObject packagesJSON = packageObj.getJSONObject("dependencies");
            Iterator keys = packagesJSON.keys();


            //Iterator a = keys.iterator();
            while(keys.hasNext()) {
                String key = (String)keys.next();
                packages.add(key);
                // loop to get the dynamic key
                String value = (String)packagesJSON.get(key);
                System.out.printf("key = %-25s value = %-25s \n",key,value);
            }



            //System.out.println("+++++"+packagesJSON+"+++++++");

            StringBuilder allPackages = new StringBuilder();
            for (String installed_package : packages) {
                allPackages.append(installed_package).append(",");
            }
            allPackages = new StringBuilder(allPackages.substring(0, allPackages.length() - 2));


            Connection cDB = getConnect();
            if (cDB != null) {
                Statement st = null;
                String sql = "INSERT INTO IMPORT_TABLE (REPOID, REPO_OWNER, REPO_NAME, REPO_PACKAGES ) VALUES(" + ID + ",'" + repoDetails.split("/")[0] + "','" + repoDetails.split("/")[1] + "','" + allPackages + "')";

                try {
                    st = cDB.createStatement();
                    st.executeUpdate(sql);
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                for (String installed_package : packages) {
                    sql = "SELECT PACKAGE_COUNT FROM PACKAGE_TABLE WHERE PACKAGE_NAME = '" + installed_package + "';";
                    try {
                        assert st != null;
                        ResultSet rs = st.executeQuery(sql);

                        int count = 0;
                        while (rs.next()) {
                            count = rs.getInt("PACKAGE_COUNT");
                        }


                        if (count == 0) {
                            sql = "INSERT INTO PACKAGE_TABLE (PACKAGE_NAME, PACKAGE_COUNT) VALUES('" + installed_package + "', 1);";
                            st.executeUpdate(sql);
                        } else {

                            count += 1;
                            sql = "UPDATE PACKAGE_TABLE SET PACKAGE_COUNT =" + count + " WHERE PACKAGE_NAME='" + installed_package + "';";
                            st.executeUpdate(sql);
                        }

                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                }
            } else {
                System.out.println("Entered Url is not valid");
            }
        }

    }


    private static void topPacks() {
        Connection cDB = getConnect();
        if(cDB != null){
            Statement st = null;
            String sql = "SELECT * FROM PACKAGE_TABLE ORDER BY PACKAGE_COUNT DESC LIMIT 10";
            try{
                st = cDB.createStatement();
                ResultSet rs = st.executeQuery(sql);
                while (rs.next()){
                    String package_name = rs.getString("package_name");
                    int package_count = rs.getInt("package_count");
                    System.out.printf("%-25s : %10d\n",package_name,package_count);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException, JSONException {
        while(true) {
            System.out.println("\n\nHi there\n Select 1 for Searching \n Select 2 for importing \n Select 3 for Toppacks \n Select 4 to Exit");

            Scanner in = new Scanner(System.in);
            int choice;
            choice = in.nextInt();
            if (choice == 1) {
                System.out.println("Please Enter the Keyword for searching");
                String searchWord = in.next();
                searchFunc(searchWord);
            } else if (choice == 2) {
                System.out.println("Please Enter the 'GitHub Repository id' to import the packages");
                String ID = in.next();
                importFunc(ID);
            } else if (choice == 3) {
                System.out.println("Here are your TopPacks");
                topPacks();

            } else if( choice == 4){
                System.out.println("Tata..See you again");
                break;
            } else {
                System.out.println("Enter a value among 1,2, 3 or 4");
            }
        }
    }

}


//7796523
