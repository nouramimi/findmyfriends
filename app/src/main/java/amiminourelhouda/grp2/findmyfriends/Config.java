package amiminourelhouda.grp2.findmyfriends;

public class Config {
    public static String IP_Serveur="10.62.1.40:80";

    public static String URL_InsertLocation="http://"+IP_Serveur+"/servicephp/insert.php";
    public static String URL_GetALLLocations="http://"+IP_Serveur+"/servicephp/get_all.php";
    public static String URL_DeletePosition = "http://"+IP_Serveur+"/servicephp/deletePosition.php";
    public static String URL_UpdatePosition = "http://"+IP_Serveur+"/servicephp/updatePosition.php";
    public static String URL_GetPositionById = "http://"+IP_Serveur+"/servicephp/getPositionById.php";
    public static String URL_SearchLocationByNumero = "http://"+IP_Serveur+"/servicephp/searchLocationByNumero.php";
}
