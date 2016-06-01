package helloworld.example.com.smartcane;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import junit.framework.TestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by Owner on 2016-05-26.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SplashActivityTest {
    @Rule
    public ActivityTestRule<SplashActivity> testRule = new ActivityTestRule<SplashActivity>(SplashActivity.class);

    @Test
    public void checkHelloWorldText() {
        onView(withId(R.id.imageView)).check(matches(isDisplayed()));
        onView(withId(R.id.textView)).check(matches(isDisplayed()));
    }
}