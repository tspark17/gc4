package helloworld.example.com.smartcane;

import android.app.Activity;

import java.util.ArrayList;

/**
 * Class for maintaining activities
 */
public class ActivityStacks {
    public static final ActivityStacks activityManager = new ActivityStacks();
    private ArrayList<Activity> listActivity = null;

    private ActivityStacks()
    {
        listActivity = new ArrayList();
    }

    /**
     *
     * @return
     */
    public static ActivityStacks getInstance()
    {
        return activityManager;
    }

    /**
     *
     * @param activity
     */
    public void addActivity(Activity activity)
    {
        listActivity.add(activity);
    }

    /**
     *
     * @param activity
     * @return
     */
    public boolean removeActivity(Activity activity)
    {
        return listActivity.remove(activity);
    }

    public void finishAllActivity()
    {
        for(Activity activity : listActivity)
        {
            activity.finish();
        }
    }

    /**
     *
     * @return
     */
    public ArrayList getListActivity()
    {
        return listActivity ;
    }

    /**
     * 
     * @param listActivity
     */
    public void setListActivity(ArrayList listActivity)
    {
        this.listActivity = listActivity ;
    }
}
