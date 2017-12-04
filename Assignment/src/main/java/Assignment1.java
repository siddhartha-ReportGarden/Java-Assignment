package main.java;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;


public class Assignment1 {


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


    private static void searchFunc(String repo) throws IOException {
        String url = "https://api.github.com/search/repositories?q=";
        url = url+ repo;
        //System.out.println(url+"--------------"+repo);

        Assignment1 example = new Assignment1();
        String rate= example.run(url);
        System.out.println(rate);

        List<String> owners = new ArrayList<>();
        Arrays.stream(rate.split("\"full_name\":")).skip(1).map(l -> l.split(",")[0].split("/")[0].split("\"")[1]).forEach(owners::add);

        List<String> repoNames = new ArrayList<>();
        Arrays.stream(rate.split("\"full_name\":")).skip(1).map(l -> l.split(",")[0].split("/")[1].split("\"")[0]).forEach(repoNames::add);

        List<String> starCounts = new ArrayList<>();
        Arrays.stream(rate.split("\"stargazers_count\":")).skip(1).map(l -> l.split(",")[0]).forEach(starCounts::add);

        List<String> forkCounts = new ArrayList<>();
        Arrays.stream(rate.split("\"forks\":")).skip(1).map(l -> l.split(",")[0]).forEach(forkCounts::add);

        for(int i=0;i<owners.size();i++){
            System.out.println("owner = "+owners.get(i)+"\t repo name = "+repoNames.get(i)+"\t star count = "+starCounts.get(i)+"\t fork count = "+forkCounts.get(i));
        }
    }


    private static  void importFunc(String ID) throws IOException {
        String url1 = "https://api.github.com/repositories/" + ID;
        Assignment1 example = new Assignment1();
        //retrieving the fullname/repositoryname

        System.out.println(url1);
        String repoDetails = example.run(url1);

        repoDetails = repoDetails.split("\"full_name\":")[1].split(",")[0].split("\"")[1].split("\"")[0];
        System.out.println(repoDetails);

        //7796523 for sample testing


        String url = "https://raw.githubusercontent.com/" + repoDetails + "/master/package.json";
        String rate = example.run(url);

        if (!rate.equals("404: Not Found\n")) {
            List<String> packages = new ArrayList<>();
            String res = rate.split("\"dependencies\": ")[1];
            res = res.split("bugs")[0];

            res = res.substring(res.indexOf("{") + 1);
            res = res.substring(0, res.indexOf("}"));
            Arrays.stream(res.split(",")).map(l -> l.split(",")[0].split(":")[0]).forEach(packages::add);
            StringBuilder allPackages = new StringBuilder();
            for (String installed_package : packages) {
                allPackages.append(installed_package).append(",");
            }
            allPackages = new StringBuilder(allPackages.substring(0, allPackages.length() - 2));
            System.out.println("-----------------------------");
            for (String installed_package : packages) {
                System.out.print(installed_package + " ,");
            }
            System.out.println("\n-----------------------------");

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

                    installed_package = installed_package.split("\"")[1].split("\"")[0];
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
                    System.out.println(package_name+" : " + package_count);
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Hi there\n Select 1 for Searching \n Select 2 for importing \n Select 3 for Toppacks");

        Scanner in = new Scanner(System.in);
        int choice;
        choice = in.nextInt();
        if(choice == 1 ){
            System.out.println("Please Enter the Keyword for searching");
            String searchWord = in.next();
            searchFunc(searchWord);
        }
        else if(choice == 2){
            System.out.println("Please Enter the 'GitHub Repository id' to import the packages");
            String ID =in.next();
            importFunc(ID);
        }
        else if(choice == 3){
            System.out.println("Here are your TopPacks");
            topPacks();

        }
        else {
            System.out.println("Enter a value among 1,2 or 3");
        }
    }

}


//7796523