package zemfi.de.vertaktoid;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.io.Console;

/**
 * Created by eugen on 30.03.17.
 */

public class CustomViewPager extends ViewPager {
    public CustomViewPager(Context context) {
        super(context);
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(ev.getToolType(0) != MotionEvent.TOOL_TYPE_STYLUS) {
            return super.onInterceptTouchEvent(ev);
        }
        return false;
    }

    public void recycle(){
        ((CustomPagerAdapter)getAdapter()).recycle();
    }

    public void restore(){
        CustomPagerAdapter adapter = ((CustomPagerAdapter)getAdapter());
        if(adapter != null) {
            ((CustomPagerAdapter) getAdapter()).restore();
        }
    }
}
