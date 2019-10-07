package com.example.myapplication.receivers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.widget.Toast;

import com.example.myapplication.R;
import com.example.myapplication.utils.ContactsAccessHelper;
import com.example.myapplication.utils.Notifications;
import com.example.myapplication.utils.Utils;

import static com.example.myapplication.utils.SMSSendHelper.DELIVERY;
import static com.example.myapplication.utils.SMSSendHelper.MESSAGE_ID;
import static com.example.myapplication.utils.SMSSendHelper.MESSAGE_PARTS;
import static com.example.myapplication.utils.SMSSendHelper.MESSAGE_PART_ID;
import static com.example.myapplication.utils.SMSSendHelper.PHONE_NUMBER;

/**
 * Receives the results of SMS sending/delivery
 */

public class SMSSendResultBroadcastReceiver extends BroadcastReceiver {
    public static final String SMS_SENT = "com.example.blacklist.SMS_SENT";
    public static final String SMS_DELIVERY = "com.example.blacklist.SMS_DELIVERY";

    @Override
    public void onReceive(Context context, Intent intent) {
        // check action
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case SMS_SENT:
                    onSMSSent(context, intent, getResultCode());
                    break;
                case SMS_DELIVERY:
                    onSMSDelivery(context, intent, getResultCode());
                    break;
            }
        }
    }

    // Is calling on SMS sending result received
    private void onSMSSent(Context context, Intent intent, int result) {
        boolean failed = true;
        int stringId = R.string.Unknown_error;
        switch (result) {
            case Activity.RESULT_OK:
                stringId = R.string.SMS_is_sent;
                failed = false;
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                stringId = R.string.Generic_failure;
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                stringId = R.string.No_service;
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                stringId = R.string.Null_PDU;
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                stringId = R.string.Radio_off;
                break;
        }

        // get SMS parameters
        String phoneNumber = intent.getStringExtra(PHONE_NUMBER);
        if (phoneNumber != null) {
            int messagePartId = intent.getIntExtra(MESSAGE_PART_ID, 0);
            int messageParts = intent.getIntExtra(MESSAGE_PARTS, 0);
            long messageId = intent.getLongExtra(MESSAGE_ID, 0);
            boolean delivery = intent.getBooleanExtra(DELIVERY, false);

            // if the last part of the message was sent
            if (messageParts > 0 && messagePartId == messageParts) {
                // notify the user about the message sending
                String message = phoneNumber + "\n" + context.getString(stringId);
                Utils.showToast(context, message, Toast.LENGTH_SHORT);

                if (messageId >= 0) {
                    // update written message as sent
                    ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
                    db.updateSMSMessageOnSent(context, messageId, delivery, failed);
                }

                // send internal event message
                InternalEventBroadcast.sendSMSWasWritten(context, phoneNumber);
            }
        }
    }

    // Is calling on SMS delivery result received
    private void onSMSDelivery(Context context, Intent intent, int result) {
        boolean failed = true;
        int stringId = R.string.Unknown_error;
        switch (result) {
            case Activity.RESULT_OK:
                stringId = R.string.SMS_is_delivered;
                failed = false;
                break;
            case Activity.RESULT_CANCELED:
                stringId = R.string.SMS_is_not_delivered;
                break;
        }

        // get SMS parameters
        String phoneNumber = intent.getStringExtra(PHONE_NUMBER);
        if (phoneNumber != null) {
            int messagePartId = intent.getIntExtra(MESSAGE_PART_ID, 0);
            int messageParts = intent.getIntExtra(MESSAGE_PARTS, 0);
            long messageId = intent.getLongExtra(MESSAGE_ID, 0);

            // if the last part of the message was delivered
            if (messageParts > 0 && messagePartId == messageParts) {
                // notify the user about the message delivery
                String message = context.getString(stringId);
                Notifications.onSmsDelivery(context, phoneNumber, message);

                if (messageId >= 0) {
                    // update written message as delivered
                    ContactsAccessHelper db = ContactsAccessHelper.getInstance(context);
                    db.updateSMSMessageOnDelivery(context, messageId, failed);
                }

                // send internal event message
                InternalEventBroadcast.sendSMSWasWritten(context, phoneNumber);
            }
        }
    }
}
