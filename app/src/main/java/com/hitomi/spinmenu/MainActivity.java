package com.hitomi.spinmenu;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.hitomi.smlibrary.OnSpinMenuStateChangeListener;
import com.hitomi.smlibrary.SpinMenu;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private SpinMenu spinMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinMenu = (SpinMenu) findViewById(R.id.spin_menu);

        // Set page title
        List<String> hintStrList = new ArrayList<>();
        hintStrList.add("Messengers");
        hintStrList.add("E-mail");
        hintStrList.add("Phone");

        spinMenu.setHintTextStrList(hintStrList);
        spinMenu.setHintTextColor(Color.parseColor("#FFFFFF"));
        spinMenu.setHintTextSize(14);

        // Set the startup gesture to open the menu
        spinMenu.setEnableGesture(true);

        // Set page adapter
        final List<Fragment> fragmentList = new ArrayList<>();
        fragmentList.add(Fragment1.newInstance());
        fragmentList.add(Fragment2.newInstance());
        fragmentList.add(Fragment3.newInstance());

        FragmentPagerAdapter fragmentPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return fragmentList.get(position);
            }

            @Override
            public int getCount() {
                return fragmentList.size();
            }
        };
        spinMenu.setFragmentAdapter(fragmentPagerAdapter);

        // Set the listener when the menu state changes
        spinMenu.setOnSpinMenuStateChangeListener(new OnSpinMenuStateChangeListener() {
            @Override
            public void onMenuOpened() {
                Toast.makeText(MainActivity.this, "SpinMenu opened", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMenuClosed() {
                Toast.makeText(MainActivity.this, "SpinMenu closed", Toast.LENGTH_SHORT).show();
            }
        });



    }
}
