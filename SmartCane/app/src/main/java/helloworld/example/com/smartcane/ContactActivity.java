package helloworld.example.com.smartcane;

import android.app.ListActivity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

/**
 * Called from BluetoothChat
 * Avail editing emergency contacts
 */

public class ContactActivity extends ListActivity {

    // Debugging
    private static final String TAG = "ContactActivity";
    private static final boolean D = true;

    // SharedPreferences
    SharedPreferences pnum;
    SharedPreferences.Editor editor;
    SharedPreferences contact;
    SharedPreferences.Editor c_editor;
    private String remove[] = new String[5];
    private int rnext = 0;

    // Contents for ListView
    String[] contactList = new String[5];
    Button comp;

    /**
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);
        sharedPreferences();

        // Set up the window layout
        comp = (Button) findViewById(R.id.complete);
        comp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor = pnum.edit();
                c_editor = contact.edit();
                for(int i = 0; i < rnext; i++) {
                    editor.remove(remove[i]);
                    c_editor.remove(remove[i]);
                    BluetoothChat.pnext--;
                }
                editor.commit();
                c_editor.commit();
                int cnt = 0;
                for(int i = 0; i < 5; i++) {
                    if (!(contact.getString("p" + i, "").equals(""))) {
                        String tmp = pnum.getString("p" + i, "");
                        String ctmp = contact.getString("p" + i, "");
                        editor = pnum.edit();
                        c_editor = contact.edit();
                        editor.remove("p" + i);
                        c_editor.remove("p" + i);
                        editor.commit();
                        c_editor.commit();
                        editor = pnum.edit();
                        c_editor = contact.edit();
                        editor.putString("p" + cnt, tmp);
                        c_editor.putString("p" + cnt, ctmp);
                        editor.commit();
                        c_editor.commit();
                        cnt++;
                    }
                }
                finish();
            }
        });
        ListView lv = getListView();
        lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        for (int i = 0; i < 5; i++) {
            String idx = "p" + i;
            contactList[i] = contact.getString(idx, "");
        }
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_checked, contactList));
    }

    /**
     *
     * @param parent
     * @param v
     * @param position
     * @param id
     */
    public void onListItemClick(
            ListView parent, View v, int position, long id) {
        remove[rnext++] = "p" + position;
    }

    public void sharedPreferences() {
        if (pnum == null)
            pnum = getSharedPreferences("pnum", Context.MODE_MULTI_PROCESS);
        if(contact == null)
            contact  = getSharedPreferences("contact", Context.MODE_MULTI_PROCESS);
    }
}
