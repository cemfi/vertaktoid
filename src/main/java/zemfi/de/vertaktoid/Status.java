package zemfi.de.vertaktoid;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import java.util.Date;

/**
 * Created by yevgen on 24.11.2016.
 */

public final class Status extends BaseObservable {
    private StatusStrings.StatusId status = StatusStrings.StatusId.UNKNOWN;
    private StatusStrings.ActionId action = StatusStrings.ActionId.UNKNOWN;
    private Date date = new Date();

    @Bindable
    public StatusStrings.ActionId getAction() {
        return action;
    }

    @Bindable
    public Date getDate() {
        return date;
    }

    @Bindable
    public StatusStrings.StatusId getStatus() {
        return status;
    }

    public void setAction(StatusStrings.ActionId action) {
        this.action = action;
        notifyPropertyChanged(BR.action);
    }

    public void setDate(Date date) {
        this.date = date;
        notifyPropertyChanged(BR.date);
    }

    public void setStatus(StatusStrings.StatusId status) {
        this.status = status;
        notifyPropertyChanged(BR.status);
    }
}
