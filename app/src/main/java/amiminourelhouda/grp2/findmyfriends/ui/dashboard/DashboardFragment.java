package amiminourelhouda.grp2.findmyfriends.ui.dashboard;

import static android.app.Service.START_NOT_STICKY;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.Manifest;

import amiminourelhouda.grp2.findmyfriends.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {

    Button btn;
    private FragmentDashboardBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.sendSMSBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String tel = binding.edtelDashboard.getText().toString().trim();

                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS)
                        == PackageManager.PERMISSION_GRANTED) {
                    SmsManager manager = SmsManager.getDefault();
                    manager.sendTextMessage(tel, null, "send me your location", null, null);
                } else {
                    Toast.makeText(getContext(), "Permission SMS non accordée", Toast.LENGTH_SHORT).show();
                }

            }
        });

        final TextView textView = binding.textDashboard;
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}