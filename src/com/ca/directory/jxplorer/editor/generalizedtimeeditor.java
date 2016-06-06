package com.ca.directory.jxplorer.editor;

import com.ca.commons.cbutil.*;
import com.ca.directory.jxplorer.HelpIDs;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The GeneralizedTimeEditor is used for editing generalizedTime attributes within the
 * directory.  It is initiated when the user clicks on a generalizedTime attribute in
 * the Table Editor of JXplorer or when the user selects a generalizedTime attribute in
 * the search dialog.
 * <p/>
 * The GeneralizedTimeEditor  displays a date/time dialog with combo boxes for the following:
 * <p/>
 * Milli seconds 				(possible values: 0-999).
 * Seconds of the minute		(possible values: 0-59).
 * Minutes of the hour			(possible values: 0-59).
 * Hour of the day				(possible values: 0-23).
 * Day of the month 			(possible values: --, 1-31).
 * Month of the year			(possible values: --, January - Decemeber inclusive).
 * <p/>
 * Note: it is possible to enter the following generalizedTime 000000000000, this is why a value of -- (0) is
 * included in day of the month and month of the year combos.
 * <p/>
 * The year is displayed and edited through a restricted (whole number) field.  Year can only be of 4 characters.
 * The UTC (or GTM or zulu) is selected via a check box.
 * <p/>
 * This class follows the generalizedTime format of year-month-day-hour-minute (yyyymmddhhmm) as manditory.
 * Optional values in the generalizedTime are second-millisecond-UTC (ss.mmmZ).  Milliseconds require seconds
 * to be added.  Examples of a correct generalizeTime are
 * <P>
 * 20011231235959.999Z
 * 20011231235959.999
 * 20011231235959.99Z
 * 20011231235959.99
 * 20011231235959.9Z
 * 20011231235959.9
 * 19960229235959
 * 19960229235959Z
 * 199702282359
 * 199702282359Z
 * <p/>
 * The dialog has a 'Now' button for displaying the current date/time of the user's machine.
 * <p/>
 * The dialog enforces date constraints such as leap years.  For example if a leap year is entered in the
 * year field and february is selected, the days displayed will be 0-29.  The correct days for the month that
 * is selected will be displayed also.  For example if April is selected the days will be 0-30.
 * @author Trudi.
 */
public class generalizedtimeeditor extends CBDialog
        implements abstractstringeditor
{
    /**
     * Index of feb.
     */
    private static final int FEBRUARY = 2;

    /**
     * List of months (first element is '--').
     */
    private String months[] = {"--", CBIntText.get("January"), CBIntText.get("February"),
                       CBIntText.get("March"), CBIntText.get("April"), CBIntText.get("May"),
                       CBIntText.get("June"), CBIntText.get("July"), CBIntText.get("August"),
                       CBIntText.get("September"), CBIntText.get("October"),
                       CBIntText.get("November"), CBIntText.get("December")};

    /**
     * Max days in a month, in order of months.
     */
    private int daysInMonth[] = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    /**
     * The date and time right now.
     */
    private Calendar rightNow = Calendar.getInstance();

    /**
     * The time value (from the directory).
     */
    private editablestring editableString = null;

    /**
     * The Button that when clicked loads the current date and time into the display.
     */
    private CBButton btnNow = new CBButton(CBIntText.get("Now"), CBIntText.get("Display the present date & time."));

    /**
     * If checked will add a 'Z' to the generalized time (20010918235959.999Z).
     */
    private JCheckBox checkBox = new JCheckBox(CBIntText.get("UTC"));

    /**
     * Year label.
     */
    private JLabel yearLabel = new JLabel(CBIntText.get("Year")+":");

    /**
     * Year field.
     */
    private WholeNumberField yearTextField = new WholeNumberField(2001, 4);   // was 5 columns? -CB

    /**
     * Month label.
     */
    private JLabel monthLabel = new JLabel(CBIntText.get("Month")+":");

    /**
     * Month combo box.
     */
    private CBJComboBox monthCombo = new CBJComboBox();

    /**
     * Day label.
     */
    private JLabel dayLabel = new JLabel(CBIntText.get("Day")+":");

    /**
     * Day combo box.
     */
    private CBJComboBox dayCombo = new CBJComboBox();

    /**
     * Hour label.
     */
    private JLabel hourLabel = new JLabel(CBIntText.get("Hour")+":");

    /**
     * Hour combo box.
     */
    private CBJComboBox hourCombo = new CBJComboBox();

    /**
     * Minute label.
     */
    private JLabel minuteLabel = new JLabel(CBIntText.get("Minute")+":");

    /**
     * Minuet combo box.
     */
    private CBJComboBox minuteCombo = new CBJComboBox();

    /**
     * Second label.
     */
    private JLabel secondLabel = new JLabel(CBIntText.get("Second")+":");

    /**
     * Second combo box.
     */
    private CBJComboBox secondCombo = new CBJComboBox();

    /**
     * Millisecond label.
     */
    private JLabel milliSecondLabel = new JLabel(CBIntText.get("Millisecond")+":");

    /**
     * Millisecond combo box.
     */
    private CBJComboBox milliSecondCombo = new CBJComboBox();

    /**
     * Month as entered by the user.
     * Possible values: 0-12.
     */
    private int userMonth;

    /**
     * Year as entered by the user.
     * Example: 2001.
     */
    private int userYear;

    /**
     * Date as entered by the user.
     * Possible values: 0-31.
     */
    private int userDay;

    /**
     * Hour as entered by the user.
     * Possible values: 0-23.
     */
    private int userHour;

    /**
     * Minute as entered by the user.
     * Possible values: 0-59.
     */
    private int userMinute;

    /**
     * Second as entered by the user.
     * Possible values: 0-59.
     */
    private int userSecond;

    /**
     * Millisecond as entered by the user.
     * Possible values: 0-999.
     */
    private int userMilliSecond;

    /**
     * Number of days in month.
     * Possible values: 0, 28, 29, 30 or 31.
     */
    private int numDays = 0;

    /**
     * A backup of the selected index in the date combo.
     */
    private int dateIndexBackup = 0;

    /**
     * Attribute value as a string.
     */
    private String value = null;

    /**
     * This class is normally used by the table editor.  If used elsewhere
     * set this to false and get the time value from the getTime() method.
     */
    private boolean isTableEditor = true;

    /**
     * Used if isTableEditor = false to return the set time via getTime().
     */
    private String time = "";


    private final static Logger log = Logger.getLogger(generalizedtimeeditor.class.getName());

    /**
     * Sets up the dialog components, displaying the supplied attribute value.  If there is
     * no value to display, the fields are all set to zero.
     * @param owner the parent frame, usually JXplorer.
     * @param value the value of the generalized time attribute.
     * @param isTableEditor this class is normally used by the table editor.
     * If used elsewhere set this to false and get the time value from the getTime() method.
     */
    public generalizedtimeeditor(Frame owner, String value, boolean isTableEditor)
    {
        super(owner, CBIntText.get("Generalized Time"), HelpIDs.ATTR_TIME);

        this.isTableEditor = isTableEditor;
        this.value = value;

        // Padding at top...
        display.addln(new JLabel(" "));
        display.makeLight();

        // Initiate the date and time (integer) variables with the
        // attribute values (or 0 if value is not present)...
        initDateTimeVariables();

        // Populate the combos...
        initMilliSeconds();
        initSeconds();
        initMinutes();
        initHours();
        initDays(31);
        initMonths();

        // Sets the state of the check box & sets the tooltip of the check box...
        if (value.indexOf("Z") != -1)
            checkBox.setSelected(true);
        checkBox.setToolTipText(CBIntText.get("Adds a 'Z' to the end of the generalized time."));

        // Adds an action listener to the 'Now' button...
        btnNow.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                displayCurrent();
            }
        });

        /**
         * Listener for month changes. Updates the day combo depending on month selected.
         */
        monthCombo.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                updateDayCombo();
            }
        });

        // Adds a document listener on the year field to do a leap year
        // check if feb is selected in month combo...
        MyDocumentListener myDocumentListener = new MyDocumentListener();
        yearTextField.getDocument().addDocumentListener(myDocumentListener);

        // New panel for drop down boxes...
        CBPanel dateTimePanel = new CBPanel();

        // Add DAY components...
        dateTimePanel.add(dayLabel);
        dateTimePanel.add(dayCombo);

        // Add MONTH components...
        dateTimePanel.add(monthLabel);
        dateTimePanel.addWide(monthCombo, 2);

        // Add YEAR components...
        dateTimePanel.add(yearLabel);
        dateTimePanel.addWide(yearTextField, 2);
        dateTimePanel.addln(new JLabel(" "));
        dateTimePanel.addln(new JLabel(" "));
        dateTimePanel.newLine();

        // Add HOUR components...
        dateTimePanel.add(hourLabel);
        dateTimePanel.add(hourCombo);

        // Add MINUTE components...
        dateTimePanel.add(minuteLabel);
        dateTimePanel.add(minuteCombo);
        dateTimePanel.add(new JLabel(" "));

        // Add SECOND components...
        dateTimePanel.add(secondLabel);
        dateTimePanel.add(secondCombo);
        dateTimePanel.add(new JLabel(" "));

        // Add MILLISECOND components...
        dateTimePanel.add(milliSecondLabel);
        dateTimePanel.add(milliSecondCombo);

        // Add border...
        dateTimePanel.setBorder(new TitledBorder(CBIntText.get("Date/Time")));

        // Add 'Now' button and a check box to a new panel...
        CBPanel btnPanel = new CBPanel();
        btnPanel.makeLight();
        btnPanel.addln(btnNow);
        btnPanel.addln(checkBox);

        // Set selected indexes and field text...
        setSelectedIndexes();

        // Add panels to main display panel...
        display.makeHeavy();
        display.add(dateTimePanel);
        display.makeHigh();
        display.add(btnPanel);
        display.newLine();
        display.makeHeavy();

        // Padding at bottom...
        display.addln(new JLabel("  "));

        setSize(500, 200);

        // Get the number of days in the selected month...
        numDays = getDaysInMonth(userMonth);
    }

    /**
     * Initialises the date and time variables depending on the current attribute value.
     * For example: 20010920235959.999 would display...
     * year 		- 2001
     * month 		- September
     * day 		    - 20
     * hour 		- 23 (11pm)
     * minute 		- 59
     * second 		- 59
     * millisecond - 999
     * If there is no value the date and time variables default to zero (0).
     */
    protected void initDateTimeVariables()
    {
        int size = 0;

        try
        {
            size = value.length();
        }
        catch (Exception e)
        {
            size = 0;
        }

        try
        {
            // Init month...
            userMonth = Integer.parseInt(value.substring(4, 6));

            // Init year...
            userYear = Integer.parseInt(value.substring(0, 4));

            // Init day...
            userDay = Integer.parseInt(value.substring(6, 8));

            // Init hour...
            userHour = Integer.parseInt(value.substring(8, 10));

            // Init minute...
            userMinute = Integer.parseInt(value.substring(10, 12));

            // Init second...
            if (size > 13)
                userSecond = Integer.parseInt(value.substring(12, 14));

            // Init milli-seconds...
            if (value.indexOf(".") == 14)
            {
                // Where is the Z?...
                int zIndex = value.indexOf("Z");

                String milli = null;

                if (zIndex == 16)
                    milli = value.substring(15, 16) + "00";				// .9Z.
                else if (zIndex == 17)
                    milli = value.substring(15, 17) + "0";				// .99Z.
                else if (size == 18 || zIndex == 18)
                    milli = value.substring(15, 18);					// .999 or .999Z.
                else if (size == 17)
                    milli = value.substring(15, 17) + "0";				// .99.
                else if (size == 16)
                    milli = value.substring(15, 16) + "00";				// .9.

                userMilliSecond = Integer.parseInt(milli);
            }
        }
        catch (Exception e)
        {
            userMonth = 0;
            userYear = 0;
            userDay = 0;
            userHour = 0;
            userMinute = 0;
            userSecond = 0;
            userMilliSecond = 0;
        }
    }

    /**
     * Returns the days in the month that is supplied.
     * @param month an integer representation of the month
     * for example: 1 = Jan, 2 = Feb...12 = Dec.
     * 0 = an unspecified month which has 0 days.
     * @return the days in the month for example:
     * Jan = 31, Feb = 28 (29 if leap) etc.
     */
    protected int getDaysInMonth(int month)
    {
        return daysInMonth[month] + ((isLeapYear(getUserYear()) &&
                (userMonth == FEBRUARY)) ? 1 : 0);
    }

    /**
     * Gets the current date and time from the user's machine and displays it.
     */
    protected void displayCurrent()
    {
        // Get the date and time of right now...
        rightNow = Calendar.getInstance();

        userYear = rightNow.get(Calendar.YEAR);

        // Jan is meant to be 1 but is it actually is 0 and so on dec = 11...
        userMonth = rightNow.get(Calendar.MONTH) + 1;
        userDay = rightNow.get(Calendar.DAY_OF_MONTH);
        userHour = rightNow.get(Calendar.HOUR_OF_DAY);
        userMinute = rightNow.get(Calendar.MINUTE);
        userSecond = rightNow.get(Calendar.SECOND);
        userMilliSecond = rightNow.get(Calendar.MILLISECOND);

        yearTextField.setText(String.valueOf(userYear));
        monthCombo.setSelectedIndex(userMonth);
        dayCombo.setSelectedIndex(userDay);
        hourCombo.setSelectedIndex(userHour);
        minuteCombo.setSelectedIndex(userMinute);
        secondCombo.setSelectedIndex(userSecond);
        milliSecondCombo.setSelectedIndex(userMilliSecond);
    }

    /**
     * Sets the index of the combo boxes and the year field to the already initiated
     * date an time variables.
     */
    protected void setSelectedIndexes()
    {
        // Year...
        yearTextField.setText(String.valueOf(userYear));

        // Month...
        if (userMonth <= 13)
            monthCombo.setSelectedIndex(userMonth);
        else
            userMonth = 0;

        // Day...
        if (userDay <= getDaysInMonth(userMonth))
            dayCombo.setSelectedIndex(userDay);

        // Hour...
        if (userHour <= 23)
            hourCombo.setSelectedIndex(userHour);

        // Minute...
        if (userMinute <= 59)
            minuteCombo.setSelectedIndex(userMinute);

        // Second...
        if (userSecond <= 59)
            secondCombo.setSelectedIndex(userSecond);

        // Millisecond...
        if (userMilliSecond <= 999)
            milliSecondCombo.setSelectedIndex(userMilliSecond);
    }

    /**
     * Initialises the milli-second combo with integers from 0-999.
     */
    protected void initMilliSeconds()
    {
        for (int i = 0; i < 1000; i++)
            milliSecondCombo.addItem(new Integer(i));
    }

    /**
     * Initialises the second combo with integers from 0-59.
     */
    protected void initSeconds()
    {
        for (int i = 0; i < 60; i++)
            secondCombo.addItem(new Integer(i));
    }

    /**
     * Initialises the minute combo with integers from 0-59.
     */
    protected void initMinutes()
    {
        for (int i = 0; i < 60; i++)
            minuteCombo.addItem(new Integer(i));
    }

    /**
     * Initialises the hour combo with integers from 0-23.
     */
    protected void initHours()
    {
        for (int i = 0; i < 24; i++)
            hourCombo.addItem(new Integer(i));
    }

    /**
     * Initialises the second combo with integers from 0-31, 0-30, 0-29 or 0-28
     * depending on the number of days supplied.
     * @param days the maximum number of days to display.  Can be 0, 28, 29, 30 ,31.
     */
    protected void initDays(int days)
    {
        for (int i = 0; i <= days; i++)
        {
            if (i == 0)
                dayCombo.addItem("--");
            else
                dayCombo.addItem(new Integer(i));
        }
    }

    /**
     * Initialises the month combo with months of the year.
     * For example: --, January, Febuarary...December.
     */
    protected void initMonths()
    {
        for (int i = 0; i < 13; i++)
            monthCombo.addItem(months[i]);
    }

    /**
     * Re-populates the day combo to reflect the number of
     * days of the month that is selected in the month combo.
     * Accounts for leap year also.
     */
    protected void updateDayCombo()
    {
        // Remember the selected position in the day combo...
        int dayPos = dayCombo.getSelectedIndex();

        // Get the index of the selected month...
        userMonth = monthCombo.getSelectedIndex();

        // Empty the day combo...
        dayCombo.removeAllItems();

        // Get the days in the month (account for leap year)...
        numDays = getDaysInMonth(userMonth);

        // Add the days to the day combo...
        initDays(numDays);

        if (dayPos > -1)
        {
            setDayComboSelectedIndex(dayPos);

            // Make a backup of the last day index incase something goes wrong......
            dateIndexBackup = dayPos;
        }
        else
        {
            // Something has gone wrong so set the day combo to the backup index...
            setDayComboSelectedIndex(dateIndexBackup);
        }

        dayCombo.revalidate();
    }

    /**
     * Returns the year from the year field as an integer.  If there is a problem
     * parsing the value (which there shouldn't be because the year field is restricted to
     * whole numbers), the year is set to zero (0).
     * @return the year as displayed in the year field.
     */
    protected int getUserYear()
    {
        int userYearInt = 0;

        try
        {
            // Get the year from the text field and parse it...
            userYearInt = Integer.parseInt(yearTextField.getText(), 10);
        }
        catch (Exception e)
        {
            // If the year is something weird (other than blank) show the message...
            if (!(yearTextField.getText().length() == 0))
                JOptionPane.showMessageDialog(this, CBIntText.get("Please enter a valid year."),
                        CBIntText.get("Invalid Year"), JOptionPane.INFORMATION_MESSAGE);
            userYear = 0;
            yearTextField.setText(String.valueOf(userYear));
        }

        if (userYearInt > 1581)
            userYear = userYearInt;

        return userYear;
    }

    /**
     * Displays the value in the day combo at the index supplied.
     * If there is a problem with the index being out of bounds (or the like),
     * the index displayed is the last available in the combo.
     * @param index the index of the value to display in the day combo.
     */
    protected void setDayComboSelectedIndex(int index)
    {
        try
        {
            // Set the position of the day combo box...
            dayCombo.setSelectedIndex(index);
        }
        catch (Exception e)
        {
            int items = dayCombo.getItemCount();

            dayCombo.setSelectedIndex(items - 1);
        }
    }

    /**
     * Determines if given year is a leap year.
     * @param year = given year after 1582 (start of the Gregorian calendar).
     * @return TRUE if given year is leap year, FALSE if not.
     */
    public boolean isLeapYear(int year)
    {
        // If multiple of 100, leap year if multiple of 400...
        if ((year % 100) == 0)
            return ((year % 400) == 0);

        // Otherwise leap year iff multiple of 4...
        return ((year % 4) == 0);
    }

    /**
     * Returns a string representation of the year as entered in the year field.
     * The year can only have four characters therefore any characters after the
     * first for are ignored.  If there is less than four characters the space(s)
     * to the left are filled with a zero (0).
     * @return the year as a string for example 2001.
     */
    protected String getSelectedYear()
    {
        String year = yearTextField.getText();

        int len = year.length();

        switch (len)
        {
            case 0:
                year = "0000";
                break;
            case 1:
                year = "000" + year;
                break;
            case 2:
                year = "00" + year;
                break;
            case 3:
                year = "0" + year;
                break;
            case 4:
                break;  // year = year;
            default:
                year = year.substring(0, 4);
        }
        return year;
    }

    /**
     * Returns a string representation of the month as entered in the month combo.
     * @return the month as a string for example 01 (January).
     */
    protected String getSelectedMonth()
    {
        int selection = monthCombo.getSelectedIndex();

        String month = Integer.toString(selection);

        if (selection < 10)
            month = "0" + month;

        return month;
    }

    /**
     * Returns a string representation of the day as entered in the day combo.
     * @return the day as a string for example 31.
     */
    protected String getSelectedDay()
    {
        int selection = dayCombo.getSelectedIndex();

        String day = Integer.toString(selection);

        if (selection < 10)
            day = "0" + day;

        return day;
    }

    /**
     * Returns a string representation of the hour as entered in the hour combo.
     * @return the hour as a string for example 23.
     */
    protected String getSelectedHour()
    {
        int selection = hourCombo.getSelectedIndex();

        String hour = Integer.toString(selection);

        if (selection < 10)
            hour = "0" + hour;

        return hour;
    }

    /**
     * Returns a string representation of the minute as entered in the minute combo.
     * @return the minute as a string for example 59.
     */
    protected String getSelectedMinute()
    {
        int selection = minuteCombo.getSelectedIndex();

        String minute = Integer.toString(selection);

        if (selection < 10)
            minute = "0" + minute;

        return minute;
    }

    /**
     * Returns a string representation of the second as entered in the second combo.
     * @return the second as a string for example 59.
     */
    protected String getSelectedSecond()
    {
        int selection = secondCombo.getSelectedIndex();

        String second = Integer.toString(selection);

        if (selection < 10)
            second = "0" + second;

        return second;
    }

    /**
     * Returns a string representation of the milliSecond as entered in the milliSecond combo.
     * @return the milliSecond as a string for example '.999', or an empty string if milliseconds equals 0.
     */
    protected String getSelectedMilliSecond()
    {
        int selection = milliSecondCombo.getSelectedIndex();

        // special handling for '0' milliseconds - some directories do not like milliseconds
        // DDDDDDDDDD.mmm format, and cannot handle the decimal '.mmm' bit - to allow users to still
        // get by, we'll leave it out altogether if milliseconds are unspecified.
        if (selection == 0)
            return "";

        String milliSecond = Integer.toString(selection);

        if (selection < 10)
            milliSecond = "00" + milliSecond;

        if (selection < 100 && selection > 9)
            milliSecond = "0" + milliSecond;

        return "." + milliSecond;
    }

    /**
     * Sets the EditableString of this class to the supplied EditableString.
     * @param editString
     */
    public void setStringValue(editablestring editString)
    {
        editableString = editString;
    }

    /**
     * Sets the changed (or unchanged) attribute value from the attribute editor in the table.
     * Disposes of the dialog.
     */
    public void doOK()
    {
        String date = getSelectedYear() + getSelectedMonth() +
                getSelectedDay() + getSelectedHour() + getSelectedMinute() +
                getSelectedSecond() + getSelectedMilliSecond();

        // Add the Z if check box is selected...
        if (checkBox.isSelected())
            date = date + "Z";

        if (isTableEditor)
            // Sets the attribute value in the table editor to reflect the changes made...
            editableString.setStringValue(date);
        else
            time = date;

        setVisible(false);
        dispose();
    }

    /**
     * When the user hits 'cancel', the window is shut down.
     * Sets the 'time' (accessable via getTime()) to the initial 'value'.
     */
    public void doCancel()
    {
        if (!isTableEditor)
            time = value;
        super.doCancel();
    }

    /**
     * Returns the time set in the GUI.  This is a hack so that the Search dialog can
     * use this editor for time attributes.  Instead of setting the value
     * via editableString.setStringValue(), a global variable should have
     * been set with the date/time.
     * @return the date/time set by the user.
     */
    public String getTime()
    {
        return time;
    }

    /**
     * Listener that monitors any insert or removal updates of a document.
     * This is used on the year text field to spawn a leap year check if
     * february   is selected in the month combo.
     * @author Trudi.
     */
    class MyDocumentListener implements DocumentListener
    {
        /**
         * Does a date check if the user inserts a value in to the document (year field).
         */
        public void insertUpdate(DocumentEvent e)
        {
            checkDate(e);
        }

        /**
         * Does a date check if the user removes a value from the document (year field).
         */
        public void removeUpdate(DocumentEvent e)
        {
            checkDate(e);
        }

        public void changedUpdate(DocumentEvent e)
        {
            /* we won't ever get this with PlainDocument */
        }

        /**
         * Triggers an update in the day combo if the month selected in the month combo is february.
         * Basically it is checking that the correct days are displayed if the year is a leap or not.
         */
        private void checkDate(DocumentEvent e)
        {
            try
            {
                int index = monthCombo.getSelectedIndex();

                // If the month that is selected is feb, make sure the number of days
                // in the day combo is correct (leap year)...
                if (index == 2)
                    updateDayCombo();
            }
            catch (Exception ee)
            {
                log.log(Level.WARNING, "Problem getting the selected month from the drop down box in the Generalized Time editor. ", e);
            }
        }
    }

    /**
     * This class extends JTextField to only allow whole
     * numbers to be entered into the text field.
     */
    class WholeNumberField extends JTextField
    {
        private NumberFormat integerFormatter;

        public WholeNumberField(int value, int columns)
        {
            super(columns);
            integerFormatter = NumberFormat.getNumberInstance(Locale.getDefault());
            integerFormatter.setParseIntegerOnly(true);
            integerFormatter.setGroupingUsed(false);  // prevents error where formatter turns 2007 into '2,007'

            setValue(value);
        }

        public int getValue()
        {
            int retVal = 0;
            try
            {
                retVal = integerFormatter.parse(getText()).intValue();
            }
            catch (ParseException e)
            {
                // This should never happen because insertString allows
                // only properly formatted data to get in the field.
            }
            return retVal;
        }

        public void setValue(int value)
        {
            setText(integerFormatter.format(value));
        }

        protected Document createDefaultModel()
        {
            return new WholeNumberDocument();
        }

        protected class WholeNumberDocument extends PlainDocument
        {
            public void insertString(int offs, String str, AttributeSet a)
                    throws BadLocationException
            {
                char[] source = str.toCharArray();
                char[] result = new char[source.length];
                int j = 0;

                for (int i = 0; i < result.length; i++)
                {
                    if (Character.isDigit(source[i]))
                        result[j++] = source[i];
                    else
                        log.warning("Invalid data, you can't enter '" + source[i] + "' (from " + str + ") into a year field.");
                }
                super.insertString(offs, new String(result, 0, j), a);
            }
        }
    }
}