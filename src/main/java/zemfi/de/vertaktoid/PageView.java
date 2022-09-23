package zemfi.de.vertaktoid;

import android.content.Context;
import android.widget.RelativeLayout;

import zemfi.de.vertaktoid.model.Facsimile;
import zemfi.de.vertaktoid.model.Page;


public class PageView extends RelativeLayout {
    private final PageImageView pageImageView;
    private final Page page;
    private final FacsimileView facsimileView;
    private final Facsimile facsimile;

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
