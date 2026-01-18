package amiminourelhouda.grp2.findmyfriends;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class MyRecyclerPositionAdapter extends RecyclerView.Adapter<MyRecyclerPositionAdapter.MyViewHolder> {

    Context con;
    ArrayList<Position> data;
    ArrayList<Position> dataFull;
    OnPositionClickListener listener;

    public interface OnPositionClickListener {
        void onShowMapClick(Position position);
        void onPositionDeleted();
        void onPositionUpdated();
    }

    public MyRecyclerPositionAdapter(Context con, ArrayList<Position> data, OnPositionClickListener listener) {
        this.con = con;
        this.data = new ArrayList<>(data);
        this.dataFull = new ArrayList<>(data);
        this.listener = listener;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(con).inflate(R.layout.view_position, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Position p = data.get(position);
        holder.tvPseudo.setText(p.pseudo);
        holder.tvNumero.setText(p.numero);
        holder.tvCoordinates.setText(p.latitude + ", " + p.longitude);

        // Set avatar initial (first letter of pseudo)
        if (p.pseudo != null && !p.pseudo.isEmpty()) {
            holder.tvAvatarInitial.setText(String.valueOf(p.pseudo.charAt(0)).toUpperCase());
        } else {
            holder.tvAvatarInitial.setText("?");
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void filter(String text) {
        text = text.toLowerCase().trim();
        data.clear();

        if (text.isEmpty()) {
            data.addAll(dataFull);
        } else {
            for (Position p : dataFull) {
                if (p.pseudo.toLowerCase().contains(text) ||
                        p.numero.toLowerCase().contains(text)) {
                    data.add(p);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void updateData(ArrayList<Position> newData) {
        this.data.clear();
        this.data.addAll(newData);
        this.dataFull.clear();
        this.dataFull.addAll(newData);
        notifyDataSetChanged();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvPseudo, tvNumero, tvCoordinates, tvAvatarInitial;
        ImageView iconShowMap, iconCall, iconEdit, iconDelete, iconTrack;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views from new layout
            tvAvatarInitial = itemView.findViewById(R.id.tv_avatar_initial);
            tvPseudo = itemView.findViewById(R.id.tv_pseudo_view_position);
            tvNumero = itemView.findViewById(R.id.tv_numero_view_position);
            tvCoordinates = itemView.findViewById(R.id.tv_coordinates_view_position);
            iconShowMap = itemView.findViewById(R.id.icon_show_map_view_position);
            iconCall = itemView.findViewById(R.id.icon_call_view_position);
            iconEdit = itemView.findViewById(R.id.icon_edit_view_position);
            iconDelete = itemView.findViewById(R.id.icon_delete_view_position);
            iconTrack = itemView.findViewById(R.id.icon_track_view_position);

            // Show on map click
            iconShowMap.setOnClickListener(v -> {
                int indice = getAdapterPosition();
                if (indice != RecyclerView.NO_POSITION && listener != null) {
                    Position p = data.get(indice);
                    listener.onShowMapClick(p);
                }
            });

            // Call click
            iconCall.setOnClickListener(v -> {
                int indice = getAdapterPosition();
                if (indice != RecyclerView.NO_POSITION) {
                    Position p = data.get(indice);
                    String numero = p.numero;
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:" + numero));

                    if (ContextCompat.checkSelfPermission(con, Manifest.permission.CALL_PHONE)
                            == PackageManager.PERMISSION_GRANTED) {
                        con.startActivity(intent);
                    } else {
                        Toast.makeText(con, "Permission d'appel requise", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // Track click - NEW FEATURE
            iconTrack.setOnClickListener(v -> {
                int indice = getAdapterPosition();
                if (indice != RecyclerView.NO_POSITION) {
                    Position p = data.get(indice);
                    Intent intent = new Intent(con, TrackFriendActivity.class);
                    intent.putExtra("idposition", p.idposition);
                    intent.putExtra("pseudo", p.pseudo);
                    intent.putExtra("numero", p.numero);
                    intent.putExtra("longitude", p.longitude);
                    intent.putExtra("latitude", p.latitude);
                    con.startActivity(intent);
                }
            });

            // Edit click
            iconEdit.setOnClickListener(v -> {
                int indice = getAdapterPosition();
                if (indice != RecyclerView.NO_POSITION) {
                    Position p = data.get(indice);
                    showEditDialog(p, indice);
                }
            });

            // Delete click
            iconDelete.setOnClickListener(v -> {
                int indice = getAdapterPosition();
                if (indice != RecyclerView.NO_POSITION) {
                    Position p = data.get(indice);
                    showDeleteDialog(p, indice);
                }
            });
        }

        /*private void showEditDialog(Position position, int indice) {
            AlertDialog.Builder builder = new AlertDialog.Builder(con);
            builder.setCancelable(true);
            builder.setTitle("Modifier la position");

            LayoutInflater inflater = LayoutInflater.from(con);
            View dialogView = inflater.inflate(R.layout.dialog_edit_position, null);
            builder.setView(dialogView);

            EditText edPseudo = dialogView.findViewById(R.id.ed_pseudo_edit);
            EditText edNumero = dialogView.findViewById(R.id.ed_numero_edit);
            EditText edLongitude = dialogView.findViewById(R.id.ed_longitude_edit);
            EditText edLatitude = dialogView.findViewById(R.id.ed_latitude_edit);
            Button btnValider = dialogView.findViewById(R.id.btn_valider_edit);
            Button btnAnnuler = dialogView.findViewById(R.id.btn_annuler_edit);

            edPseudo.setText(position.pseudo);
            edNumero.setText(position.numero);
            edLongitude.setText(position.longitude);
            edLatitude.setText(position.latitude);

            AlertDialog dialog = builder.create();
            dialog.show();

            btnValider.setOnClickListener(view -> {
                String pseudo = edPseudo.getText().toString().trim();
                String numero = edNumero.getText().toString().trim();
                String longitude = edLongitude.getText().toString().trim();
                String latitude = edLatitude.getText().toString().trim();

                if (pseudo.isEmpty() || numero.isEmpty() || longitude.isEmpty() || latitude.isEmpty()) {
                    Toast.makeText(con, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Execute update task
                new UpdatePositionTask(position.idposition, pseudo, numero, longitude, latitude, indice, dialog).execute();
            });

            btnAnnuler.setOnClickListener(view -> dialog.dismiss());
        }*/
        private void showEditDialog(Position position, int indice) {
            AlertDialog.Builder builder = new AlertDialog.Builder(con);
            builder.setCancelable(true);
            builder.setTitle("Modifier la position");

            LayoutInflater inflater = LayoutInflater.from(con);
            View dialogView = inflater.inflate(R.layout.dialog_edit_position, null);
            builder.setView(dialogView);

            EditText edPseudo = dialogView.findViewById(R.id.ed_pseudo_edit);
            EditText edNumero = dialogView.findViewById(R.id.ed_numero_edit);
            EditText edLongitude = dialogView.findViewById(R.id.ed_longitude_edit);
            EditText edLatitude = dialogView.findViewById(R.id.ed_latitude_edit);
            Button btnValider = dialogView.findViewById(R.id.btn_valider_edit);
            Button btnAnnuler = dialogView.findViewById(R.id.btn_annuler_edit);

            Log.e("ShowEditDialog", "Position - Pseudo: " + position.pseudo +
                    ", Numero: " + position.numero +
                    ", Longitude: " + position.longitude +
                    ", Latitude: " + position.latitude);

            edPseudo.setText(position.pseudo);
            edNumero.setText(position.numero);
            edLongitude.setText(position.longitude);
            edLatitude.setText(position.latitude);

            // Make longitude and latitude read-only
            edLongitude.setEnabled(false);
            edLatitude.setEnabled(false);

            AlertDialog dialog = builder.create();
            dialog.show();

            btnValider.setOnClickListener(view -> {
                String pseudo = edPseudo.getText().toString().trim();
                String numero = edNumero.getText().toString().trim();
                String longitude = edLongitude.getText().toString().trim();
                String latitude = edLatitude.getText().toString().trim();

                if (pseudo.isEmpty() || numero.isEmpty()) {
                    Toast.makeText(con, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Execute update task - keep original coordinates
                new UpdatePositionTask(position.idposition, pseudo, numero, longitude, latitude, indice, dialog).execute();
            });

            btnAnnuler.setOnClickListener(view -> dialog.dismiss());
        }

        private void showDeleteDialog(Position position, int indice) {
            new AlertDialog.Builder(con)
                    .setTitle("Suppression")
                    .setMessage("Voulez-vous vraiment supprimer cette position ?")
                    .setPositiveButton("Oui", (dialog, which) -> {
                        new DeletePositionTask(position.idposition, indice).execute();
                    })
                    .setNegativeButton("Non", null)
                    .show();
        }

        // AsyncTask for deleting position
        class DeletePositionTask extends AsyncTask<Void, Void, Boolean> {
            int idposition;
            int indice;
            AlertDialog progressDialog;

            DeletePositionTask(int idposition, int indice) {
                this.idposition = idposition;
                this.indice = indice;
            }

            @Override
            protected void onPreExecute() {
                AlertDialog.Builder builder = new AlertDialog.Builder(con);
                builder.setTitle("Suppression en cours...");
                builder.setMessage("Veuillez patienter...");
                builder.setCancelable(false);
                progressDialog = builder.create();
                progressDialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    JSONParser parser = new JSONParser();
                    HashMap<String, String> params = new HashMap<>();
                    params.put("idposition", String.valueOf(idposition));

                    JSONObject response = parser.makeHttpRequest(
                            Config.URL_DeletePosition,
                            "POST",
                            params
                    );

                    Log.e("DeleteResponse", response.toString());

                    int success = response.getInt("success");
                    return success == 1;

                } catch (Exception e) {
                    Log.e("DeleteError", e.getMessage());
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                if (success) {
                    data.remove(indice);
                    dataFull.removeIf(p -> p.idposition == idposition);
                    notifyDataSetChanged();
                    Toast.makeText(con, "Position supprimée avec succès", Toast.LENGTH_SHORT).show();

                    if (listener != null) {
                        listener.onPositionDeleted();
                    }
                } else {
                    Toast.makeText(con, "Échec de la suppression", Toast.LENGTH_SHORT).show();
                }
            }
        }

        // AsyncTask for updating position
        class UpdatePositionTask extends AsyncTask<Void, Void, Boolean> {
            int idposition;
            String pseudo, numero, longitude, latitude;
            int indice;
            AlertDialog editDialog;

            UpdatePositionTask(int idposition, String pseudo, String numero, String longitude, String latitude, int indice, AlertDialog editDialog) {
                this.idposition = idposition;
                this.pseudo = pseudo;
                this.numero = numero;
                this.longitude = longitude;
                this.latitude = latitude;
                this.indice = indice;
                this.editDialog = editDialog;
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    JSONParser parser = new JSONParser();
                    HashMap<String, String> params = new HashMap<>();
                    params.put("idposition", String.valueOf(idposition));
                    params.put("pseudo", pseudo);
                    params.put("numero", numero);
                    params.put("longitude", longitude);
                    params.put("latitude", latitude);

                    JSONObject response = parser.makeHttpRequest(
                            Config.URL_UpdatePosition,
                            "POST",
                            params
                    );

                    Log.e("UpdateResponse", response.toString());

                    int success = response.getInt("success");
                    return success == 1;

                } catch (Exception e) {
                    Log.e("UpdateError", e.getMessage());
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    Position p = data.get(indice);
                    p.pseudo = pseudo;
                    p.numero = numero;
                    p.longitude = longitude;
                    p.latitude = latitude;

                    // Update in dataFull as well
                    for (Position pos : dataFull) {
                        if (pos.idposition == idposition) {
                            pos.pseudo = pseudo;
                            pos.numero = numero;
                            pos.longitude = longitude;
                            pos.latitude = latitude;
                            break;
                        }
                    }

                    notifyItemChanged(indice);
                    Toast.makeText(con, "Position modifiée avec succès", Toast.LENGTH_SHORT).show();
                    editDialog.dismiss();

                    if (listener != null) {
                        listener.onPositionUpdated();
                    }
                } else {
                    Toast.makeText(con, "Échec de la modification", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}