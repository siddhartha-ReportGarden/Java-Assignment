import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Iterator;

public class Assignment {


  private OkHttpClient client = new OkHttpClient();

  private Connection getConnect() {
    Connection conn = null;
    try {
      Class.forName("org.postgresql.Driver");
      String url = "jdbc:postgresql://localhost:5432/user";
      conn = DriverManager.getConnection(url, "postgres", "root");
    } catch (ClassNotFoundException | SQLException e) {
      e.printStackTrace();
    }
    return conn;
  }

  private String Run(String url) throws IOException {
    Request request = new Request.Builder()
        .url(url)
        .build();

      Response response = client.newCall(request).execute();
      return response.body().string();
  }


  private void SearchFunc(String repo, int page, int pageLimit) {
    if(repo.equals("")){
      System.out.println("Enter a valid search keyword");
      return;
    }
    String url = "https://api.github.com/rate_limit";

    Assignment example = new Assignment();
    String rate;
    try {
      rate = example.Run(url);
      JSONObject obj = new JSONObject(rate);

      JSONObject rateValue = obj.getJSONObject("resources");
      rateValue = rateValue.getJSONObject("search");

      int remaining = rateValue.getInt("remaining");
      if (remaining == 0) {
        System.out.println("The rate limit is completed. Please try after a while");
        return;
      }
    } catch (IOException | JSONException e) {
      System.out.println(e);
      return ;
    }

    url = "https://api.github.com/search/repositories?q=";
    url = url + repo+"&page="+page+"&per_page="+pageLimit;
    System.out.println(url);
    try {
      rate = example.Run(url);
      JSONObject obj = new JSONObject(rate);
      JSONArray allItems = obj.getJSONArray("items");

      for (int itr = 0; itr < allItems.length(); itr++) {
        JSONObject itrObj = (JSONObject) allItems.get(itr);
        String name = (String) itrObj.get("full_name");
        name = name.split("/")[0];
        System.out
            .printf("owner = %-20s repo name = %-20s star count = %-10s fork count = %-10s \n",
                name, itrObj.get("name"), itrObj.get("stargazers_count"), itrObj.get("forks"));
      }

    } catch (IOException | JSONException e) {
      System.out.println(e);
    }

    //System.out.println(rate);

  }

  private void ImportFunc(String id) {
    if(id.equals("")){
      System.out.println("Enter a valid repo ID");
      return;
    }

    String url = "https://api.github.com/rate_limit";

    Assignment example = new Assignment();
    String rate;
    try {
      rate = example.Run(url);
      JSONObject obj = new JSONObject(rate);

      JSONObject rateValue = obj.getJSONObject("resources");
      rateValue = rateValue.getJSONObject("core");

      int remaining = rateValue.getInt("remaining");
      if (remaining == 0) {
        System.out.println("The rate limit is completed. Please try after a while");
        return;
      }
    } catch (IOException | JSONException e) {
      System.out.println(e);
      return ;
    }

    String url1 = "https://api.github.com/repositories/" + id;

    System.out.println(url1);
    String repoDetails = "";
    try {
      repoDetails = example.Run(url1);
    } catch (IOException e) {
      System.out.println(e);
      return;
    }
    try {
      JSONObject obj = new JSONObject(repoDetails);
      repoDetails = (String) obj.get("full_name");
    } catch (JSONException e) {
      System.out.println(e);
      return;
    }

    //repoDetails = repoDetails.split("\"full_name\":")[1].split(",")[0].split("\"")[1].split("\"")[0];
    System.out.println(repoDetails);

    //7796523 for sample testing

    url = "https://raw.githubusercontent.com/" + repoDetails + "/master/package.json";
    try {
      rate = example.Run(url);
    } catch (IOException e) {
      System.out.println(e);
      return;
    }

    System.out.println(url);

    if (!rate.equals("404: Not Found\n")) {
      StringBuilder allPackages = new StringBuilder();
      List<String> packages = new ArrayList<>();
      try {
        JSONObject packageObj = new JSONObject(rate);
        JSONObject packagesJSON = packageObj.getJSONObject("dependencies");

        Iterator keys = packagesJSON.keys();
        //Iterator a = keys.iterator();
        while (keys.hasNext()) {
          String key = (String) keys.next();
          packages.add(key);
          // loop to get the dynamic key
          String value = (String) packagesJSON.get(key);
          System.out.printf("key = %-25s value = %-25s \n", key, value);
          allPackages.append(key).append(",");
        }

      } catch (JSONException e) {
        System.out.println(e);
        return;
      }

      allPackages = new StringBuilder(allPackages.substring(0, allPackages.length() - 2));

      Connection cDB = getConnect();
      if (cDB != null) {
        Statement st = null;
        String sql =
            "INSERT INTO IMPORT_TABLE (REPOID, REPO_OWNER, REPO_NAME, REPO_PACKAGES ) VALUES(" + id
                + ",'" + repoDetails.split("/")[0] + "','" + repoDetails.split("/")[1] + "','"
                + allPackages + "')";
        try {
          st = cDB.createStatement();
          st.executeUpdate(sql);
        } catch (SQLException e) {
          e.printStackTrace();
          return;
        }

        for (String installedPackage : packages) {
          sql = "SELECT PACKAGE_COUNT FROM PACKAGE_TABLE WHERE PACKAGE_NAME = '" + installedPackage
              + "';";
          try {

              ResultSet rs = st.executeQuery(sql);
              System.out.println("Error establishing connection to database");

              int count = 0;
              while (rs.next()) {
                count = rs.getInt("PACKAGE_COUNT");
              }

              if (count == 0) {
                sql = "INSERT INTO PACKAGE_TABLE (PACKAGE_NAME, PACKAGE_COUNT) VALUES('"
                    + installedPackage + "', 1);";
                st.executeUpdate(sql);
              } else {
                count += 1;
                sql = "UPDATE PACKAGE_TABLE SET PACKAGE_COUNT =" + count + " WHERE PACKAGE_NAME='"
                    + installedPackage + "';";
                st.executeUpdate(sql);
              }

            } catch (SQLException e) {
              e.printStackTrace();
              return;
            }

        }
      } else {
        System.out.println("Entered Url is not valid");
      }
    }

  }

  private  void TopPacks() {
    Connection cDB = getConnect();
    if (cDB != null) {
      Statement st = null;
      String sql = "SELECT * FROM PACKAGE_TABLE ORDER BY PACKAGE_COUNT DESC LIMIT 10";
      try {
        st = cDB.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
          String package_name = rs.getString("package_name");
          int package_count = rs.getInt("package_count");
          System.out.printf("%-25s : %10d\n", package_name, package_count);
        }

      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] args) {
    Assignment assignment = new Assignment();
    while (true) {
      System.out.println(
          "\n\nHi there\n Select 1 for Searching \n Select 2 for importing \n Select 3 for Toppacks \n Select 4 to Exit");
      Scanner in = new Scanner(System.in);
      int choice;
      choice = in.nextInt();
      if (choice == 1) {
        System.out.println("Please Enter the Keyword for searching");
        String searchWord = in.next();
        int page = 1;
        int pageLimit;
        System.out.println("Please enter the number of entries you want per page");
        pageLimit = in.nextInt();
        assignment.SearchFunc(searchWord,page,pageLimit);
        String moreResults;

        label:
        while (true) {
          System.out.println("press Y for more results. Else click N");
          moreResults = in.next();
          switch (moreResults) {
            case "Y":
            case "y":
              page += 1;
              assignment.SearchFunc(searchWord, page, pageLimit);
              break;
            case "N":
            case "n":
              break label;
            default:
              System.out.println("Please enter a valid answer. Either Y or N");
              break;
          }
        }
        //SearchFunc(searchWord);
      } else if (choice == 2) {
        System.out.println("Please Enter the 'GitHub Repository id' to import the packages");
        String id = in.next();
        assignment.ImportFunc(id);
      } else if (choice == 3) {
        System.out.println("Here are your TopPacks");
        assignment.TopPacks();
      } else if (choice == 4) {
        System.out.println("Tata..See you again");
        break;
      } else {
        System.out.println("Enter a value among 1,2, 3 or 4");
      }
    }
  }
}

//7796523
//9852918
