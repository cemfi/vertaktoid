package zemfi.de.vertaktoid;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import java.util.Date;

/**
 * Represents bindable status that will be displayed after regular actions.
 */

public final class Status extends BaseObservable {
    // The status value.
    private StatusStrings.StatusId status = StatusStrings.StatusId.UNKNOWN;
    // The action value.
    private StatusStrings.ActionId action = StatusStrings.ActionId.UNKNOWN;
    // The date value.
    private Date date = new Date();

    /**
     * The bindable getter for action.
     * @return The action.
     */
    @Bindable
    public StatusStrings.ActionId getAction() {
        return action;
    }

    /**
     * The bindable getter for date.
     * @return The date.
     */
    @Bindable
    public Date getDate() {
        return date;
    }

    /**
     * The bindable getter for status.
     * @return The status.
     */
    @Bindable
    public StatusStrings.StatusId getStatus() {
        return status;
    }

    /**
     * The setter for action with notifyPropertyChanged call.
     * @param action The action.
     */
    public void setAction(StatusStrings.ActionId action) {
        this.action = action;
        notifyPropertyChanged(BR.action);
    }

    /**
     * The setter for date with notifyPropertyChanged call.
     * @param date The date.
     */
    public void setDate(Date date) {
        this.date = date;
        notifyPropertyChanged(BR.date);
    }

    /**
     * The setter for status with notifyPropertyChanged call.
     * @param status The status.
     */
    public void setStatus(StatusStrings.StatusId status) {
        this.status = status;
        notifyPropertyChanged(BR.status);
    }
}
