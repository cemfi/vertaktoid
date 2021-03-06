package zemfi.de.vertaktoid;

import android.content.Context;
import android.widget.RelativeLayout;

import java.util.LinkedHashMap;

import zemfi.de.vertaktoid.model.Facsimile;
import zemfi.de.vertaktoid.model.Page;


public class PageView extends RelativeLayout {
    private PageImageView pageImageView;
    private Page page;
    private FacsimileView facsimileView;
    private Facsimile facsimile;

    public PageView(Context context, Page page, FacsimileView facsimileView, Facsimile facsimile) {
        super(context);
        this.page = page;
        this.facsimileView = facsimileView;
        this.facsimile = facsimile;
        pageImageView = new PageImageView(this, page, facsimileView, facsimile);
        this.addView(pageImageView,
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
    }

    public void recycle() {
        pageImageView.recycle();
    }

    public void restore() {
       pageImageView.init();
    }

    public void refresh() {
        pageImageView.refresh();
        pageImageView.postInvalidate();
    }
}
