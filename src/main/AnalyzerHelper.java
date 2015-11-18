package main;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by fab on 11/17/2015
 * Singleton class
 */

public class AnalyzerHelper {

    public enum ApplicationType {
        Communication,
        Education,
        Entertainment,
        Game,
        Family,
        HealthFitness,
        Medical,
        Music,
        MediaPhotographyVideo,
        Social,
        Sports,
        Transportation,
        Others
    }

    private static AnalyzerHelper analyzerHelper;

    /*
     * MANIFEST PERMISSIONS TO WATCH FOR
     * Based on a list of dangerous permissions provided by Google
     * https://developer.android.com/guide/topics/security/permissions.html
     */
    private static String READ_CALENDAR = "android.permission.READ_CALENDAR";
    private static String WRITE_CALENDAR = "android.permission.WRITE_CALENDAR";
    private static String CAMERA = "android.permission.CAMERA";
    private static String READ_CONTACTS = "android.permission.READ_CONTACTS";
    private static String WRITE_CONTACTS = "android.permission.WRITE_CONTACTS";
    private static String GET_ACCOUNTS = "android.permission.GET_ACCOUNTS";
    private static String ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION";
    private static String ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";
    private static String RECORD_AUDIO = "android.permission.RECORD_AUDIO";
    private static String READ_PHONE_STATE = "android.permission.READ_PHONE_STATE";
    private static String CALL_PHONE = "android.permission.CALL_PHONE";
    private static String READ_CALL_LOG = "android.permission.READ_CALL_LOG";
    private static String WRITE_CALL_LOG = "android.permission.WRITE_CALL_LOG";
    private static String ADD_VOICEMAIL = "com.android.voicemail.permission.ADD_VOICEMAIL";
    private static String USE_SIP = "android.permission.USE_SIP";
    private static String PROCESS_OUTGOING_CALLS = "android.permission.PROCESS_OUTGOING_CALLS";
    private static String BODY_SENSORS = "android.permission.BODY_SENSORS";
    private static String SEND_SMS = "android.permission.SEND_SMS";
    private static String RECEIVE_SMS = "android.permission.RECEIVE_SMS";
    private static String READ_SMS = "android.permission.READ_SMS";
    private static String RECEIVE_WAP_PUSH = "android.permission.RECEIVE_WAP_PUSH";
    private static String RECEIVE_MMS = "android.permission.RECEIVE_MMS";
    private static String READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
    private static String WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";

    /*
     * MANIFEST BROADCAST ACTIONS TO WATCH FOR
     * https://developer.android.com/reference/android/content/Intent.html -> Standard Broadcast Actions
     */
    private static String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
    private static String ACTION_PACKAGE_ALL = "android.intent.action.PACKAGE_";

    /*
     * SMS MANAGER METHOD CALLS
     * https://developer.android.com/reference/android/telephony/SmsManager.html
     */
    private static String SMS_SENDING_CODE = "sendTextMessage";
    private static String SMS_MULTIPART_SENDING_CODE = "sendMultipartTextMessage";
    private static String MMS_SENDING_CODE = "sendMultimediaMessage";
    private static String DATA_SENDING_CODE = "sendDataMessage";
    private static String DOWNLOAD_SMS_CODE = "downloadMultimediaMessage";
    private static String GET_USER_PHONE_NUMBER_CODE = "SubscriptionId";

    /*
     * NFC related
     */
    private static String NFC_CODE = "nfcManager";

    /*
     * BLUETOOTH
     */
    private static String BLUETOOTH_CODE = "bluetoothAdapter";

    /*
     * TELEPHONY MANAGER
     * https://developer.android.com/reference/android/telephony/TelephonyManager.html
     */
    private static String DEVICE_ID_CODE = "getDeviceId";
    private static String USER_PHONE_NUMBER_CODE = "getLine1Number";
    private static String NETWORK_INFO_CODE = "getNetworkCountryIso";
    private static String NETWORK_OPERATOR_CODE = "getNetworkOperator";
    private static String SIM_INFO_CODE = "getSimOperator";
    private static String SUBSCRIBER_ID_CODE = "getSubscriberId";
    private static String VOICEMAIL_NUMBER_CODE = "getVoiceMail";
    private static String VOICEMAIL_NUMBER_CHANGE_CODE = "setVoiceMailNumber";

    /*
     * PHONE STATE LISTENER
     * https://developer.android.com/reference/android/telephony/PhoneStateListener.html
     */
    private static String PHONE_STATE_LISTENER_CODE = "PhoneStateListener";

    /*
     * SUBSCRIPTION MANAGER
     * https://developer.android.com/reference/android/telephony/SubscriptionManager.html
     */
    private static String SUBSCRIPTION_MANAGER_CODE = "SubscriptionManager";

    /*
     * ACCOUNT MANAGER
     * https://developer.android.com/reference/android/accounts/AccountManager.html
     */
    private static String GET_ACCOUNTS_CODE = "getAccounts";

    /*
     * LOCATION
     * https://developer.android.com/reference/android/location/Location.html
     */
    private static String GET_ALTITUDE_CODE = "getAltitude";
    private static String GET_LATITUDE_CODE = "getLatitude";
    private static String GET_LONGITUDE_CODE = "getLongitude";

    /*
     * LOCATION MANAGER
     * https://developer.android.com/reference/android/location/LocationManager.html
     */
    private static String GET_LAST_LOCATION_CODE = "getLastKnownLocation";
    private static String GPS_STATUS_LISTENER_CODE = "addGpsStatusListener";
    private static String GET_LOCATION_UPDATES_CODE = "requestLocationUpdates";

    /*
     * AUDIO MANAGER
     * https://developer.android.com/reference/android/media/AudioManager.html
     */
    private static String AUDIO_MANAGER_CODE = "AudioManager";

    /*
     * AUDIO RECORD
     * https://developer.android.com/reference/android/media/AudioRecord.html
     */
    private static String AUDIO_RECORD_CODE = "AudioRecord";

    /*
     * MEDIA RECORDER
     * https://developer.android.com/reference/android/media/MediaRecorder.html
     */
    private static String MEDIA_RECORDER_CODE = "MediaRecorder";

    /*
     * MEDIA STORE
     * https://developer.android.com/reference/android/provider/MediaStore.html#ACTION_IMAGE_CAPTURE
     */
    private static String ACTION_IMAGE_CAPTURE_CODE = "android.media.action.IMAGE_CAPTURE";
    private static String ACTION_IMAGE_CAPTURE_SECURE_CODE = "android.media.action.IMAGE_CAPTURE_SECURE";
    private static String ACTION_VIDEO_CAPTURE_CODE = "android.media.action.VIDEO_CAPTURE";

    /*
     * Trackers so we do not count the score twice
     * They should all add up to 100
     */
    private boolean smsSendingAlreadyCounted;           // 10 - SMS_SENDING_CODE, SMS_MULTIPART_SENDING_CODE, MMS_SENDING_CODE
    private boolean dataSendingAlreadyCounted;          // 5 - DATA_SENDING_CODE
    private boolean downloadSmsAlreadyCounted;          // 5 - DOWNLOAD_SMS_CODE
    private boolean userPhoneNumberAlreadyCounted;      // 10 - GET_USER_PHONE_NUMBER_CODE, USER_PHONE_NUMBER_CODE
    private boolean nfcAlreadyCounted;                  // 5 - NFC_CODE
    private boolean bluetoothAlreadyCounted;            // 5 - BLUETOOTH_CODE
    private boolean deviceIdAlreadyCounted;             // 5 - DEVICE_ID_CODE
    private boolean networkInfoAlreadyCounted;          // 5 - NETWORK_INFO_CODE, NETWORK_OPERATOR_CODE
    private boolean simInfoAlreadyCounted;              // 5 - SIM_INFO_CODE
    private boolean subscriberIdAlreadyCounted;         // 5 - SUBSCRIBER_ID_CODE
    private boolean voicemailAccessAlreadyCounted;      // 10 - VOICEMAIL_NUMBER_CODE, VOICEMAIL_NUMBER_CHANGE_CODE
    private boolean phoneStateListenerAlreadyCounted;   // 5 - PHONE_STATE_LISTENER_CODE
    private boolean subscriptionManagerAlreadyCounted;  // 5 - SUBSCRIPTION_MANAGER_CODE
    private boolean getAccountsAlreadyCounted;          // 10 - GET_ACCOUNTS_CODE
    private boolean getLocationAlreadyCounted;          // 10 - GET_ALTITUDE_CODE, GET_LATITUDE_CODE, GET_LONGITUDE_CODE, GET_LAST_LOCATION_CODE
    private boolean getLocationUpdateAlreadyCounted;    // 10 - GPS_STATUS_LISTENER_CODE, GET_LOCATION_UPDATES_CODE
    private boolean getAudioManagerAlreadyCounted;      // 5 - AUDIO_MANAGER_CODE
    private boolean getAudioRecordAlreadyCounted;       // 10 - AUDIO_RECORD_CODE
    private boolean getMediaRecordAlreadyCounted;       // 10 - MEDIA_RECORDER_CODE
    private boolean getMediaCaptureAlreadyCounted;      // 10 - ACTION_IMAGE_CAPTURE_CODE, ACTION_IMAGE_CAPTURE_SECURE_CODE, ACTION_VIDEO_CAPTURE_CODE

    private ApplicationType currentApplicationType; // If time permits use that to better classify permission requests an api method calls
    private int malwareLikelihood;

    private AnalyzerHelper() {
    }

    public static AnalyzerHelper getInstance() {
        if (analyzerHelper == null) {
            analyzerHelper = new AnalyzerHelper();
        }
        return analyzerHelper;
    }

    public void setApplicationType(ApplicationType applicationType) {
        analyzerHelper.currentApplicationType = applicationType;
    }

    public int getMalwareLikelihood() {
        System.out.println("--------------------------------------------------------------------------------------");
        System.out.println("Malware/Spyware/Trojan likelihood score: " + malwareLikelihood);
        return malwareLikelihood;
    }

    /**
     * Reads the manifest file and returns us a score
     */
    public void analyzeManifestPermissions(String manifestPath) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        Document document;
        NodeList nodeList;
        Node node;

        try {
            builder = factory.newDocumentBuilder();
            document = builder.parse(new File(manifestPath));
            nodeList = document.getElementsByTagName("*");

            System.out.println("--------------------------------------------------------------------------------------");
            System.out.println("Starting static analysis of Manifest.xml" + "\n");
            for (int i = 0; i < nodeList.getLength(); i++) {
                node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("uses-permission")) {
                    analyzerHelper.manifestPermissionsMatcher(node.getAttributes().getNamedItem("android:name").getNodeValue());
                } else if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals("action")) {
                    analyzerHelper.manifestIntentActionMatcher(node.getAttributes().getNamedItem("android:name").getNodeValue());
                }
            }

        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    }

    //TODO: Adjust the malware score per permission if time permits
    private void manifestPermissionsMatcher(String nodeValue) {
        if (nodeValue.toLowerCase().contains(READ_CALENDAR.toLowerCase())) {
            System.out.println("The app requires read calendar permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(WRITE_CALENDAR.toLowerCase())) {
            System.out.println("The app requires write calendar permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(CAMERA.toLowerCase())) {
            System.out.println("The app requires camera permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(READ_CONTACTS.toLowerCase())) {
            System.out.println("The app requires read contacts permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(WRITE_CONTACTS.toLowerCase())) {
            System.out.println("The app requires write contacts permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(GET_ACCOUNTS.toLowerCase())) {
            System.out.println("The app requires get accounts permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(ACCESS_FINE_LOCATION.toLowerCase())) {
            System.out.println("The app requires access fine location permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(ACCESS_COARSE_LOCATION.toLowerCase())) {
            System.out.println("The app requires access coarse location permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(RECORD_AUDIO.toLowerCase())) {
            System.out.println("The app requires record audio permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(READ_PHONE_STATE.toLowerCase())) {
            System.out.println("The app requires read phone state permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(CALL_PHONE.toLowerCase())) {
            System.out.println("The app requires call phone permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(READ_CALL_LOG.toLowerCase())) {
            System.out.println("The app requires read call log permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(WRITE_CALL_LOG.toLowerCase())) {
            System.out.println("The app requires write call log permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(ADD_VOICEMAIL.toLowerCase())) {
            System.out.println("The app requires add voicemail permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(USE_SIP.toLowerCase())) {
            System.out.println("The app requires use sip permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(PROCESS_OUTGOING_CALLS.toLowerCase())) {
            System.out.println("The app requires process outgoing calls permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(BODY_SENSORS.toLowerCase())) {
            System.out.println("The app requires body sensors permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(SEND_SMS.toLowerCase())) {
            System.out.println("The app requires send sms permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(RECEIVE_SMS.toLowerCase())) {
            System.out.println("The app requires calendar permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(READ_SMS.toLowerCase())) {
            System.out.println("The app requires read sms permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(RECEIVE_WAP_PUSH.toLowerCase())) {
            System.out.println("The app requires receive wap push permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(RECEIVE_MMS.toLowerCase())) {
            System.out.println("The app requires receive mms permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(READ_EXTERNAL_STORAGE.toLowerCase())) {
            System.out.println("The app requires read external storage permission");
            malwareLikelihood += 4;
        } else if (nodeValue.toLowerCase().contains(WRITE_EXTERNAL_STORAGE.toLowerCase())) {
            System.out.println("The app requires write external storage permission");
            malwareLikelihood += 4;
        }
    }

    private void manifestIntentActionMatcher(String nodeValue) {
        if (nodeValue.toLowerCase().contains(ACTION_BOOT_COMPLETED.toLowerCase())) {
            System.out.println("The app listens to phone start actions event");
            malwareLikelihood += 3;
        } else if (nodeValue.toLowerCase().contains(ACTION_PACKAGE_ALL.toLowerCase())) {
            System.out.println("The app listens to package change actions event");
            malwareLikelihood += 3;
        }
    }

    /**
     * Reads the java files, and returns us a score
     */
    public void analyzeJavaSources(String decompiledDirectory) {
        File folder = new File(decompiledDirectory);
        BufferedReader br;
        String line;

        System.out.println("--------------------------------------------------------------------------------------");
        System.out.println("Starting static analysis of Java source files" + "\n");
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isFile()) {
                try {
                    br = new BufferedReader(new FileReader(fileEntry.getAbsolutePath()));

                    while ((line = br.readLine()) != null) {
                        analyzerHelper.javaSourceMatcher(fileEntry.getName(), line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void javaSourceMatcher(String currentClass, String currentLine) {

        //Search and calculate the score
        if (currentLine.toLowerCase().contains(SMS_SENDING_CODE.toLowerCase())) {
            System.out.println("The app sends text messages from: " + currentClass);
            if (!smsSendingAlreadyCounted) {
                smsSendingAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        } else if (currentLine.toLowerCase().contains(SMS_MULTIPART_SENDING_CODE.toLowerCase())) {
            System.out.println("The app sends multipart text messages from: " + currentClass);
            if (!smsSendingAlreadyCounted) {
                smsSendingAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        } else if (currentLine.toLowerCase().contains(MMS_SENDING_CODE.toLowerCase())) {
            System.out.println("The app sends mms text messages from: " + currentClass);
            if (!smsSendingAlreadyCounted) {
                smsSendingAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        } else if (currentLine.toLowerCase().contains(DATA_SENDING_CODE.toLowerCase())) {
            System.out.println("The app sends data messages from: " + currentClass);
            if (!dataSendingAlreadyCounted) {
                dataSendingAlreadyCounted = true;
                malwareLikelihood += 5;
            }
        } else if (currentLine.toLowerCase().contains(DOWNLOAD_SMS_CODE.toLowerCase())) {
            System.out.println("The app downloads text messages from: " + currentClass);
            if (!downloadSmsAlreadyCounted) {
                downloadSmsAlreadyCounted = true;
                malwareLikelihood += 5;
            }
        } else if (currentLine.toLowerCase().contains(GET_USER_PHONE_NUMBER_CODE.toLowerCase())) {
            System.out.println("The app gathers the user phone number in: " + currentClass);
            if (!userPhoneNumberAlreadyCounted) {
                userPhoneNumberAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        } else if (currentLine.toLowerCase().contains(USER_PHONE_NUMBER_CODE.toLowerCase())) {
            System.out.println("The app gathers the user phone number in: " + currentClass);
            if (!userPhoneNumberAlreadyCounted) {
                userPhoneNumberAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        } else if (currentLine.toLowerCase().contains(NFC_CODE.toLowerCase())) {
            System.out.println("The app makes use of NFC in: " + currentClass);
            if (!nfcAlreadyCounted) {
                nfcAlreadyCounted = true;
                malwareLikelihood += 5;
            }
        } else if (currentLine.toLowerCase().contains(BLUETOOTH_CODE.toLowerCase())) {
            System.out.println("The app makes use of Bluetooth in: " + currentClass);
            if (!bluetoothAlreadyCounted) {
                bluetoothAlreadyCounted = true;
                malwareLikelihood += 5;
            }
        } else if (currentLine.toLowerCase().contains(DEVICE_ID_CODE.toLowerCase())) {
            System.out.println("The app gathers the phone device id from: " + currentClass);
            if (!deviceIdAlreadyCounted) {
                deviceIdAlreadyCounted = true;
                malwareLikelihood += 5;
            }
        } else if (currentLine.toLowerCase().contains(NETWORK_INFO_CODE.toLowerCase())) {
            System.out.println("The app gathers network information from: " + currentClass);
            if (!networkInfoAlreadyCounted) {
                networkInfoAlreadyCounted = true;
                malwareLikelihood += 5;
            }
        } else if (currentLine.toLowerCase().contains(NETWORK_OPERATOR_CODE.toLowerCase())) {
            System.out.println("The app gathers network information from: " + currentClass);
            if (!networkInfoAlreadyCounted) {
                networkInfoAlreadyCounted = true;
                malwareLikelihood += 5;
            }
        } else if (currentLine.toLowerCase().contains(SIM_INFO_CODE.toLowerCase())) {
            System.out.println("The app gathers the sim information from: " + currentClass);
            if (!simInfoAlreadyCounted) {
                simInfoAlreadyCounted = true;
                malwareLikelihood += 5;
            }
        } else if (currentLine.toLowerCase().contains(SUBSCRIBER_ID_CODE.toLowerCase())) {
            System.out.println("The app gathers the subscriber id from: " + currentClass);
            if (!subscriberIdAlreadyCounted) {
                subscriberIdAlreadyCounted = true;
                malwareLikelihood += 5;
            }
        } else if (currentLine.toLowerCase().contains(VOICEMAIL_NUMBER_CODE.toLowerCase())) {
            System.out.println("The app access the user voicemail from: " + currentClass);
            if (!voicemailAccessAlreadyCounted) {
                voicemailAccessAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        } else if (currentLine.toLowerCase().contains(VOICEMAIL_NUMBER_CHANGE_CODE.toLowerCase())) {
            System.out.println("The app changes the user voicemail number from: " + currentClass);
            if (!voicemailAccessAlreadyCounted) {
                voicemailAccessAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        } else if (currentLine.toLowerCase().contains(PHONE_STATE_LISTENER_CODE.toLowerCase())) {
            System.out.println("The app listens to the phone state from: " + currentClass);
            if (!phoneStateListenerAlreadyCounted) {
                phoneStateListenerAlreadyCounted = true;
                malwareLikelihood += 5;
            }
        } else if (currentLine.toLowerCase().contains(SUBSCRIPTION_MANAGER_CODE.toLowerCase())) {
            System.out.println("The app subscribes to the subscription manager events from: " + currentClass);
            if (!subscriptionManagerAlreadyCounted) {
                subscriptionManagerAlreadyCounted = true;
                malwareLikelihood += 5;
            }
        } else if (currentLine.toLowerCase().contains(GET_ACCOUNTS_CODE.toLowerCase())) {
            System.out.println("The app accesses the user's accounts from: " + currentClass);
            if (!getAccountsAlreadyCounted) {
                getAccountsAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        } else if (currentLine.toLowerCase().contains(GET_ALTITUDE_CODE.toLowerCase())) {
            System.out.println("The app accesses the user altitude from: " + currentClass);
            if (!getLocationAlreadyCounted) {
                getLocationAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        } else if (currentLine.toLowerCase().contains(GET_LATITUDE_CODE.toLowerCase())) {
            System.out.println("The app accesses the user latitude from: " + currentClass);
            if (!getLocationAlreadyCounted) {
                getLocationAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        }  else if (currentLine.toLowerCase().contains(GET_LONGITUDE_CODE.toLowerCase())) {
            System.out.println("The app accesses the user longitude from: " + currentClass);
            if (!getLocationAlreadyCounted) {
                getLocationAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        } else if (currentLine.toLowerCase().contains(GET_LAST_LOCATION_CODE.toLowerCase())) {
            System.out.println("The app accesses the user location from: " + currentClass);
            if (!getLocationAlreadyCounted) {
                getLocationAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        } else if (currentLine.toLowerCase().contains(GPS_STATUS_LISTENER_CODE.toLowerCase())) {
            System.out.println("The app listens to the gps status from: " + currentClass);
            if (!getLocationUpdateAlreadyCounted) {
                getLocationUpdateAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        } else if (currentLine.toLowerCase().contains(GET_LOCATION_UPDATES_CODE.toLowerCase())) {
            System.out.println("The app listens to location updates from: " + currentClass);
            if (!getLocationUpdateAlreadyCounted) {
                getLocationUpdateAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        } else if (currentLine.toLowerCase().contains(AUDIO_MANAGER_CODE.toLowerCase())) {
            System.out.println("The app access the audio manager from: " + currentClass);
            if (!getAudioManagerAlreadyCounted) {
                getAudioManagerAlreadyCounted = true;
                malwareLikelihood += 5;
            }
        } else if (currentLine.toLowerCase().contains(AUDIO_RECORD_CODE.toLowerCase())) {
            System.out.println("The app records audio from: " + currentClass);
            if (!getAudioRecordAlreadyCounted) {
                getAudioRecordAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        } else if (currentLine.toLowerCase().contains(MEDIA_RECORDER_CODE.toLowerCase())) {
            System.out.println("The app accesses the media recorder from: " + currentClass);
            if (!getMediaRecordAlreadyCounted) {
                getMediaRecordAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        } else if (currentLine.toLowerCase().contains(ACTION_IMAGE_CAPTURE_CODE.toLowerCase())) {
            System.out.println("The app calls for an image capture from: " + currentClass);
            if (!getMediaCaptureAlreadyCounted) {
                getMediaCaptureAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        } else if (currentLine.toLowerCase().contains(ACTION_IMAGE_CAPTURE_SECURE_CODE.toLowerCase())) {
            System.out.println("The app calls for an image capture from: " + currentClass);
            if (!getMediaCaptureAlreadyCounted) {
                getMediaCaptureAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        } else if (currentLine.toLowerCase().contains(ACTION_VIDEO_CAPTURE_CODE.toLowerCase())) {
            System.out.println("The app calls for a video capture from: " + currentClass);
            if (!getMediaCaptureAlreadyCounted) {
                getMediaCaptureAlreadyCounted = true;
                malwareLikelihood += 10;
            }
        }

    }

}
