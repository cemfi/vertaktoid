package zemfi.de.vertaktoid;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import java.util.Date;

/**
 * Created by yevgen on 24.11.2016.
 */

public final class Status extends BaseObservable {
    private int status = R.string.status_unknown;
    private int action = R.string.action_unknown;
    private Date date = new Date();

    @Bindable
    public int getAction() {
        return action;
    }

    @Bindable
    public Date getDate() {
        return date;
    }

    @Bindable
    public int getStatus() {
        return status;
    }

    public void setAction(int action) {
        this.action = action;
        notifyPropertyChanged(BR.status);
    }

    public void setDate(Date date) {
        this.date = date;
        notifyPropertyChanged(BR.action);
    }

    public void setStatus(int status) {
        this.status = status;
        notifyPropertyChanged(BR.date);
    }
}
