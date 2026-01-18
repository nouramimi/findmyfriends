package amiminourelhouda.grp2.findmyfriends;

public class Position {
    public int idposition;
    public String pseudo,numero,longitude,latitude;

    public Position(int idposition, String pseudo, String numero, String longitude, String latitude) {
        this.idposition = idposition;
        this.pseudo = pseudo;
        this.numero = numero;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public Position(String pseudo, String numero, String longitude, String latitude) {
        this.pseudo = pseudo;
        this.numero = numero;
        this.longitude = longitude;
        this.latitude = latitude;
    }

    @Override
    public String toString() {
        return "Position{" +
                "idposition=" + idposition +
                ", pseudo='" + pseudo + '\'' +
                ", numero='" + numero + '\'' +
                ", longitude='" + longitude + '\'' +
                ", latitude='" + latitude + '\'' +
                '}';
    }
}
