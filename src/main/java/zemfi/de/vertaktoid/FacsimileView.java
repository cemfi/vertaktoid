package zemfi.de.vertaktoid;

import android.app.Dialog;
import android.content.Context;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.CoordinatorLayout;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import java.util.ArrayList;

import zemfi.de.vertaktoid.commands.CommandManager;
import zemfi.de.vertaktoid.helpers.HSLColor;
import zemfi.de.vertaktoid.helpers.HSLColorsGenerator;
import zemfi.de.vertaktoid.model.Facsimile;

/**
 * Contains the presentation and user interaction functions (click on icon). Directs the UI layouts.
 * Extends the SubsamplingScaleImageView class.
 */

public class FacsimileView extends CoordinatorLayout {

    CommandManager commandManager;

    /**
     * Constructor
     * @param context android application context
     * @param attr attributes
     */
    public FacsimileView(Context context, AttributeSet attr) {
        super(context, attr);
        commandManager = new CommandManager();
        movementColors = new ArrayList<>();
    }

    public void generateColors() {
        int colorsToGenerate = document.movements.size() - movementColors.size();
        movementColors.addAll(HSLColorsGenerator.generateColorSet(colorsToGenerate, s, l, a));
    }

    /**
     * Constructor
     * @param context android application context
     */
    public FacsimileView(Context context) {
        super(context);
        commandManager = new CommandManager();
    }

    private float s = 100f;
    private float l = 30f;
    private float a = 1f;
    public int cutOverlapping = 0;
    public Facsimile document;
    public final ObservableInt pageNumber = new ObservableInt(-1);
    public int currentMovementNumber = 0;
    public final ObservableField<String> currentPath = new ObservableField<>();
    public final ObservableInt maxPageNumber = new ObservableInt(0);
    public boolean needToSave = false;
    public enum Action {DRAW, ERASE, ADJUST_MEASURE, ORTHOGONAL_CUT, PRECISE_CUT, ADJUST_MOVEMENT}
    public Action nextAction = Action.DRAW;
    public boolean isFirstPoint = true;
    public ArrayList<HSLColor> movementColors;


    /**
     * Stores the instance state.
     * @return parcelable object
     */
    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("instanceState", super.onSaveInstanceState());
        bundle.putParcelable("document", document);
        bundle.putInt("pageNumber", pageNumber.get());
        bundle.putInt("currentMovementNumber", currentMovementNumber);
        bundle.putInt("cutOverlapping", cutOverlapping);
        bundle.putParcelable("history", commandManager);
        bundle.putParcelableArrayList("colors", movementColors);
        return bundle;
    }

    /**
     * Restores the instance state.
     * @param state parcelable object
     */
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            document = (Facsimile) bundle.getParcelable("document");
            if(document == null) {
                return;
            }
            pageNumber.set(bundle.getInt("pageNumber"));
            currentMovementNumber = bundle.getInt("currentMovementNumber");
            cutOverlapping = bundle.getInt("cutOverlapping");
            setPage(pageNumber.get());
            commandManager = (CommandManager) bundle.getParcelable("history");
            maxPageNumber.set(document.pages.size());
            currentPath.set(document.dir.getName());
            movementColors = bundle.getParcelableArrayList("colors");
            super.onRestoreInstanceState(bundle.getParcelable("instanceState"));
            return;
        }

        super.onRestoreInstanceState(state);
    }

    public void setPage(int page) {
        if(document == null) {
            return;
        }
        if (page >= 0 && page < document.pages.size()) {
            pageNumber.set(page);
            CustomViewPager viewPager = (CustomViewPager) findViewById(R.id.view_pager);
            viewPager.setCurrentItem(page);
        }
    }

    /**
     * Sets Icons after actions to show possible undo and redo actions.
     */
    public void adjustHistoryNavigation() {
        if(commandManager.getUndoStackSize() > 0) {
            menu.findItem(R.id.action_undo).setIcon(R.drawable.undo_on);
        } else {
            menu.findItem(R.id.action_undo).setIcon(R.drawable.undo_off);
        }
        if(commandManager.getRedoStackSize() > 0) {
            menu.findItem(R.id.action_redo).setIcon(R.drawable.redo_on);
        } else {
            menu.findItem(R.id.action_redo).setIcon(R.drawable.redo_off);
        }
    }

    /**
     * Shows dialog and obtains the user defined next page number. Changes to the page with giving number.
     */
    public void gotoClicked(){
        resetState();
        final Dialog gotoDialog = new Dialog(getContext());
        Window window = gotoDialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.TOP;
        window.setAttributes(wlp);
        gotoDialog.setContentView(R.layout.dialog_goto);
        gotoDialog.setTitle(R.string.dialog_goto_titel);
        TextView gotoPageLabel = (TextView) gotoDialog.findViewById(R.id.dialog_goto_page_label);
        gotoPageLabel.setText(R.string.dialog_goto_page_label);
        final EditText gotoPageInput = (EditText) gotoDialog.findViewById(R.id.dialog_goto_page_input);
        gotoPageInput.setHint("1 - " + (document.pages.size()));
        gotoPageInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        Button gotoButtonNegative = (Button) gotoDialog.findViewById(R.id.dialog_goto_button_negative);
        gotoButtonNegative.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                resetMenu();
                resetState();
                invalidate();
                gotoDialog.cancel();
            }
        });

        Button gotoButtonPositive = (Button) gotoDialog.findViewById(R.id.dialog_goto_button_positive);
        gotoButtonPositive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int newPageNumber = Integer.parseInt(gotoPageInput.getText().toString()) - 1;
                    if(newPageNumber >= 0 && newPageNumber < document.pages.size()) {
                        pageNumber.set(newPageNumber);
                        setPage(newPageNumber);
                        refresh();
                    }
                }
                catch (NumberFormatException e) {

                }
                resetMenu();
                resetState();
                invalidate();
                gotoDialog.dismiss();
            }
        });

        gotoDialog.show();
    }

    /**
     * Shows the settings dialog and process the user input.
     * Currently obtains the horizontal overlapping parameter.
     */
    public void settingsClicked() {
        resetState();
        final Dialog settingsDialog = new Dialog(getContext());
        Window window = settingsDialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.TOP;
        window.setAttributes(wlp);
        settingsDialog.setContentView(R.layout.dialog_settings);
        settingsDialog.setTitle(R.string.dialog_settings_titel);
        final EditText settingsHoroverInput = (EditText) settingsDialog.findViewById(R.id.dialog_settings_horover_input);
        settingsHoroverInput.setHint("" + cutOverlapping);
        settingsHoroverInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final RadioGroup settingsHoroverType = (RadioGroup) settingsDialog.findViewById(R.id.dialog_settings_horover_type);
        settingsHoroverType.check(R.id.dialog_settings_horover_type_points);
        final EditText settingsUndosizeInput = (EditText) settingsDialog.findViewById(R.id.dialog_settings_undosize_input);
        settingsUndosizeInput.setHint("" + commandManager.getHistoryMaxSize());
        settingsUndosizeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        final RadioGroup settingsMEIType = (RadioGroup) settingsDialog.findViewById(R.id.dialog_settings_mei_type);
        if(document.nextAnnotationsType == Facsimile.AnnotationType.ORTHOGONAL_BOX) {
            settingsMEIType.check(R.id.dialog_settings_annotation_type_orthogonal);
        } else if(document.nextAnnotationsType == Facsimile.AnnotationType.ORIENTED_BOX) {
            settingsMEIType.check(R.id.dialog_settings_annotation_type_oriented);
        } else if(document.nextAnnotationsType == Facsimile.AnnotationType.POLYGON) {
            settingsMEIType.check(R.id.dialog_settings_annotation_type_polygon);
        }
        Button settingsButtonNegative = (Button) settingsDialog.findViewById(R.id.dialog_settings_button_negative);

        settingsButtonNegative.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsDialog.cancel();
                resetState();
                resetMenu();
                invalidate();
            }
        });

        Button settingsButtonPositive = (Button) settingsDialog.findViewById(R.id.dialog_settings_button_positive);
        settingsButtonPositive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    String historyMaxSize = settingsUndosizeInput.getText().toString();
                    if(!historyMaxSize.equals("")) {
                        commandManager.setHistoryMaxSize(Integer.parseInt(settingsUndosizeInput.getText().toString()));
                    }
                    String horover = settingsHoroverInput.getText().toString();
                    if(!horover.equals("")) {
                        if (settingsHoroverType.getCheckedRadioButtonId() == R.id.dialog_settings_horover_type_points) {
                            cutOverlapping = Integer.parseInt(settingsHoroverInput.getText().toString());
                        } else if (settingsHoroverType.getCheckedRadioButtonId() == R.id.dialog_settings_horover_type_percents) {
                            float percent = Float.parseFloat(settingsHoroverInput.getText().toString());
                            if (percent > 100) {
                                percent = percent % 100;
                            }
                            cutOverlapping = Math.round(document.pages.get(pageNumber.get()).imageWidth * percent / 100);
                        } else {
                            cutOverlapping = 0;
                        }
                    }
                    if(settingsMEIType.getCheckedRadioButtonId() == R.id.dialog_settings_annotation_type_orthogonal) {
                        document.nextAnnotationsType = Facsimile.AnnotationType.ORTHOGONAL_BOX;
                    } else if(settingsMEIType.getCheckedRadioButtonId() == R.id.dialog_settings_annotation_type_oriented) {
                        document.nextAnnotationsType = Facsimile.AnnotationType.ORIENTED_BOX;
                    }
                    else if(settingsMEIType.getCheckedRadioButtonId() == R.id.dialog_settings_annotation_type_polygon) {
                        document.nextAnnotationsType = Facsimile.AnnotationType.POLYGON;
                    }
                }
                catch (NumberFormatException e) {
                    // do nothing
                }
                resetState();
                resetMenu();
                invalidate();
                settingsDialog.dismiss();
            }
        });

        settingsDialog.show();
    }

    /**
     * Sets the current facsimile
     * @param facsimile facsimile
     */
    public void setFacsimile(Facsimile facsimile) {
        movementColors = new ArrayList<>();
        this.document = facsimile;
        pageNumber.set(0);
        currentMovementNumber = document.movements.size() - 1;
        maxPageNumber.set(document.pages.size());
        currentPath.set(document.dir.getName());
        HSLColorsGenerator.resetHueToDefault();
        generateColors();
        cutOverlapping = 0;
    }

    /**
     * Gets current facsimile.
     * @return facsimile
     */
    public Facsimile getFacsimile() {
        return this.document;
    }


    /**
     * Menu entry "brush" clicked.
     */
    public void brushClicked() {
        resetState();
    }

    /**
     * Menu entry "erase" clicked.
     */
    public void eraseClicked() {
        refresh();
        nextAction = Action.ERASE;
    }

    /**
     * Menu entry "type"
     */
    public void typeClicked() {
        refresh();
        nextAction = Action.ADJUST_MEASURE;
    }

    /**
     * Menu entry "vertical cut" clicked.
     */
    public void OrthogonalCutClicked() {
        refresh();
        nextAction = Action.ORTHOGONAL_CUT;
    }

    /**
     * Menu entry "horizontal cut" clicked.
     */
    public void PreciseCutClicked() {
        refresh();
        nextAction = Action.PRECISE_CUT;
    }

    /**
     * Menu entry "movement" clicked.
     */
    public void movementClicked(){
        refresh();
        nextAction = Action.ADJUST_MOVEMENT;
    }

    public void undoClicked() {
        int pageIndex = commandManager.undo();
        if(pageIndex != -1 && pageIndex != pageNumber.get()) {
            pageNumber.set(pageIndex);
            setPage(pageIndex);
        }
        adjustHistoryNavigation();
        refresh();
    }

    public void redoClicked() {
        int pageIndex = commandManager.redo();
        if(pageIndex != -1 && pageIndex != pageNumber.get()) {
            pageNumber.set(pageIndex);
            setPage(pageIndex);
        }
        adjustHistoryNavigation();
        refresh();
    }

    /**
     * Reset current menu state to default "brush" entry
     */
    void resetState() {
        nextAction = Action.DRAW;
        refresh();

    }

    public void refresh() {
        isFirstPoint = true;
        invalidate();
        CustomViewPager viewPager = (CustomViewPager) findViewById(R.id.view_pager);
        CustomPagerAdapter pagerAdapter = (CustomPagerAdapter) viewPager.getAdapter();
        // pageAdapter is null if opened the first time and no image is loaded
        if(pagerAdapter != null)
            pagerAdapter.refresh();
    }


    Menu menu;

    /**
     * Setter for menu.
     * @param menu menu
     */
    public void setMenu(Menu menu) {
        this.menu = menu;
    }

    /**
     * Reset menu icons.
     */
    void resetMenu() {
        for (int i = 0; i < menu.size(); i++) {
            // Set default icons
            if (menu.getItem(i).getItemId() == R.id.action_erase) {
                menu.getItem(i).setIcon(R.drawable.eraser_off);
            } else if (menu.getItem(i).getItemId() == R.id.action_type) {
                menu.getItem(i).setIcon(R.drawable.textbox_off);
            } else if (menu.getItem(i).getItemId() == R.id.action_orthogonal_cut) {
                menu.getItem(i).setIcon(R.drawable.orthogonal_cut_off);
            } else if (menu.getItem(i).getItemId() == R.id.action_precise_cut) {
                menu.getItem(i).setIcon(R.drawable.precise_cut_off);
            } else if (menu.getItem(i).getItemId() == R.id.action_brush) {
                menu.getItem(i).setIcon(R.drawable.brush_on);
            }
        }
    }

}
