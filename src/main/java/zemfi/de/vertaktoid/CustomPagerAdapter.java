package zemfi.de.vertaktoid;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.util.ArrayList;

import zemfi.de.vertaktoid.model.Facsimile;

/**
 * Created by eugen on 27.03.17.
 */

public class CustomPagerAdapter extends PagerAdapter {
    private FacsimileView facsimileView;
    private ArrayList<PageView> cashedViews;

    public CustomPagerAdapter(FacsimileView facsimileView) {
        this.facsimileView = facsimileView;
        cashedViews = new ArrayList<>();
    }

    @Override
    public int getCount() {
        if(facsimileView.document != null) {
            return facsimileView.document.pages.size();
        }
        return 0;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int page) {
        PageView pageView = new PageView(container.getContext(), facsimileView.document.pages.get(page),
                facsimileView, facsimileView.document);
        container.addView(pageView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        cashedViews.add(pageView);
        return pageView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        PageView pageView = (PageView) object;
        //pageView.recycle();
        cashedViews.remove(pageView);
        container.removeView(pageView);
        pageView = null;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    public void refresh() {
        for(PageView pageView : cashedViews) {
            pageView.refresh();
        }
    }

    public void recycle() {
        for(PageView pageView : cashedViews) {
            pageView.recycle();
        }
    }

    public void restore() {
        for(PageView pageView : cashedViews) {
            pageView.restore();
        }
    }

}
