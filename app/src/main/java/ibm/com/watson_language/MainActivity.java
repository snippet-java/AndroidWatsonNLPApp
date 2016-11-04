package ibm.com.watson_language;

import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import com.ibm.watson.developer_cloud.language_translation.v2.LanguageTranslation;
import com.ibm.watson.developer_cloud.language_translation.v2.model.IdentifiableLanguage;
import com.ibm.watson.developer_cloud.language_translation.v2.model.TranslationModel;
import com.ibm.watson.developer_cloud.language_translation.v2.model.TranslationResult;
import com.ibm.watson.developer_cloud.service.exception.UnauthorizedException;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.AudioFormat;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ibm.com.watson_language.R.id.entrySpeechButton;
import static ibm.com.watson_language.R.id.resultSpeechButton;

import com.ibm.mobilefirstplatform.clientsdk.android.core.api.BMSClient;


public class MainActivity extends AppCompatActivity {
    private static final String EXCEPTION_NO_TRANSLATION = "noTranslation";
    private static final String EXCEPTION_NO_SPEECH = "noSpeech";
    private static final String EXCEPTION_NO_INTERNET = "noInternet";
    private static final String EXCEPTION_UNKNOWN = "unknown";
    private static final String VALID_CREDENTIALS = "valid";

    private TextToSpeech speechService;
    private LanguageTranslation translationService;
    private MediaPlayer mediaPlayer;
    private Spinner entryLanguageSpinner;
    private Spinner resultLanguageSpinner;
    private EditText entryEditText;
    private EditText resultEditText;

    // Data structures used to hold LanguageTranslation Data and Relationships
    private ArrayList<ArrayList<String>> supportedTranslationModels;
    private Map<String, String> shortToLongLanguageNameMap;
    private Map<String, String> longToShortLanguageNameMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mediaPlayer = new MediaPlayer();

        entryLanguageSpinner = (Spinner)findViewById(R.id.entryLanguageSpinner);
        resultLanguageSpinner = (Spinner)findViewById(R.id.resultLanguageSpinner);
        entryEditText = (EditText)findViewById(R.id.translationEntry);
        resultEditText = (EditText)findViewById(R.id.translationResult);

        // Core SDK must be initialized to interact with Bluemix mobile services
        BMSClient.getInstance().initialize(getApplicationContext(), BMSClient.REGION_US_SOUTH);


        //Instantiate Watson Services and Grab User Credentials
        speechService = new TextToSpeech();
        String textToSpeechUserName = getString(R.string.watson_text_to_speech_username);
        String textToSpeechPassword = getString(R.string.watson_text_to_speech_password);
        speechService.setUsernameAndPassword(textToSpeechUserName,textToSpeechPassword);


        translationService = new LanguageTranslation();
        String langaugeTranslatorUserName = getString(R.string.watson_language_translator_username);
        String langaugeTranslatorPassword = getString(R.string.watson_language_translator_password);
        translationService.setEndPoint("https://gateway.watsonplatform.net/language-translator/api");
        translationService.setUsernameAndPassword(langaugeTranslatorUserName,langaugeTranslatorPassword);


        //Validate User Credentials
        ValidateCredentialsTask vct = new ValidateCredentialsTask();
        vct.execute();

        // Buttons to be given onClick Listeners
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        ImageButton entrySpeechButton = (ImageButton)findViewById(R.id.entrySpeechButton);
        ImageButton resultSpeechButton = (ImageButton)findViewById(R.id.resultSpeechButton);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendUIDataToTranslationTask();
            }
        });

        entrySpeechButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String inputText = entryEditText.getText().toString();

                if (inputText.length() > 0) {
                    String inputLanguage = (String)entryLanguageSpinner.getSelectedItem();

                    String[] inputs = {inputText, inputLanguage};

                    SpeechTask speaker = new SpeechTask();
                    speaker.execute(inputs);
                }
            }
        });

        resultSpeechButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String inputText = resultEditText.getText().toString();

                if (inputText.length() > 0) {
                    String outputLanguage = (String)resultLanguageSpinner.getSelectedItem();

                    String[] inputs = {inputText, outputLanguage};

                    SpeechTask speaker = new SpeechTask();
                    speaker.execute(inputs);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        
        
        

        // We stop media player onStop() so recreate if we're resuming from a stop
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onStop() {
        super.onStop();

        mediaPlayer.release();
        mediaPlayer = null;
    }

    /**
     * Helper function that pulls all user input and sends it to the TranslationTask
     */
    private void sendUIDataToTranslationTask() {

        String inputText = entryEditText.getText().toString();
        if (inputText.length() > 0) {

            String inputLanguage = (String)entryLanguageSpinner.getSelectedItem();
            String outputLanguage = (String)resultLanguageSpinner.getSelectedItem();

            String[] inputValues = {inputText, inputLanguage, outputLanguage};

            TranslationTask translator = new TranslationTask();
            translator.execute(inputValues);
        }
    }

    /**
     * Asynchronous Task, called onCreate, used to validate Watson credentials and give pertinent
     * information if credentials are invalid or if the application cannot connect to Bluemix
     */
    private class ValidateCredentialsTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            // Testing credentials by attempting to obtain service tokens, switch on result
            try {
                translationService.getToken().execute();
            } catch (Exception e) {
                if (e.getClass().equals(UnauthorizedException.class) ||
                        e.getClass().equals(IllegalArgumentException.class))
                    return EXCEPTION_NO_TRANSLATION;
                else if (e.getCause().getClass().equals(UnknownHostException.class))
                    return EXCEPTION_NO_INTERNET;
                else {
                    e.printStackTrace();
                    return EXCEPTION_UNKNOWN;
                }
            }

            try {
                speechService.getToken().execute();
            } catch (Exception e) {
                if (e.getClass().equals(UnauthorizedException.class) ||
                        e.getClass().equals(IllegalArgumentException.class))
                    return EXCEPTION_NO_SPEECH;
                else if (e.getCause().getClass().equals(UnknownHostException.class))
                    return EXCEPTION_NO_INTERNET;
                else {
                    e.printStackTrace();
                    return EXCEPTION_UNKNOWN;
                }
            }

            // If we receive no exceptions we assume the credentials are valid
            return VALID_CREDENTIALS;
        }

        @Override
        protected void onPostExecute(String result) {

            if (result.equals(VALID_CREDENTIALS)) {
                // If credentials are fine then we can send requests to Language Translation and populate the UI
                FetchTranslationDataTask ftdt = new FetchTranslationDataTask();
                ftdt.execute();
            } else {

                //There's some credential/access error so I want to alert the user.
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();

                switch (result) {

                    case (EXCEPTION_NO_TRANSLATION):
                        alertDialog.setTitle("Invalid Translator Credentials");
                        alertDialog.setMessage("Failed to connect to LanguageTranslator due to invalid credentials.\n" +
                                "Please verify your credentials and rebuild. \n" +
                                "See the README for further assistance.");

                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Close Application",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int pos) {
                                        MainActivity.this.finish();
                                    }
                                });
                        break;

                    case (EXCEPTION_NO_SPEECH):
                        alertDialog.setTitle("Invalid Speech Credentials");
                        alertDialog.setMessage("Failed to connect to TextToSpeech due to invalid credentials.\n" +
                                "You may continue to use translation but Text To Speech will be disabled.\n" +
                                "See the README for further assistance.");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Okay",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int pos) {
                                        ImageButton entryButton = (ImageButton) findViewById(entrySpeechButton);
                                        ImageButton resultButton = (ImageButton) findViewById(resultSpeechButton);
                                        // Hide and disable the speech buttons since we don't have service
                                        entryButton.setEnabled(false);
                                        entryButton.setVisibility(View.INVISIBLE);
                                        resultButton.setEnabled(false);
                                        resultButton.setVisibility(View.INVISIBLE);
                                        // But continue to populate the UI with Translation Data
                                        FetchTranslationDataTask ftdt = new FetchTranslationDataTask();
                                        ftdt.execute();
                                    }
                                });
                        break;

                    case (EXCEPTION_NO_INTERNET):
                        alertDialog.setTitle("Cannot Connect to Bluemix");
                        alertDialog.setMessage("Failed to connect to Bluemix.\n" +
                                "Please verify your connection to the internet. \n" +
                                "See the README for further assistance.");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Okay",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int pos) {
                                        MainActivity.this.finish();
                                    }
                                });
                        break;

                    default:
                        alertDialog.setTitle("Unknown Error");
                        alertDialog.setMessage("Failed to Verify Credentials.\n" +
                                "Please see console output. \n" +
                                "See the README for further assistance.");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Close Application",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int pos) {
                                        MainActivity.this.finish();
                                    }
                                });
                        break;
                }
                alertDialog.show();
            }
        }
    }

    /**
     * Asynchronous Task, called onCreate, used to populate UI Spinners dynamically from Watson
     * LanguageTranslation and create mappings for Short to Long Language names and vice-versa
     */
    private class FetchTranslationDataTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            supportedTranslationModels = new ArrayList<ArrayList<String>>();
            shortToLongLanguageNameMap = new HashMap<String,String>();
            longToShortLanguageNameMap = new HashMap<String,String>();

            List<TranslationModel> allLanguages = translationService.getModels().execute();

            List<IdentifiableLanguage> allIdLanguages = translationService.getIdentifiableLanguages().execute();
            for (int i = 0; i < allIdLanguages.size(); i++) {
                shortToLongLanguageNameMap.put(allIdLanguages.get(i).getLanguage(), allIdLanguages.get(i).getName());
                longToShortLanguageNameMap.put(allIdLanguages.get(i).getName(), allIdLanguages.get(i).getLanguage());
            }

            //Watson Translation Models want "arz" whereas Identifiable Languages returns "az", short-term fix until a resolution
            shortToLongLanguageNameMap.put("arz", "Azerbaijan");
            longToShortLanguageNameMap.put("Azerbaijan", "arz");


            boolean alreadyContainsFlag;
            int entryLanguagePosition;

            for (int j = 0; j < allLanguages.size(); j++) {
                if(allLanguages.get(j).isDefaultModel()) {

                    entryLanguagePosition = -1;
                    alreadyContainsFlag = false;
                    String sourceLanguage = shortToLongLanguageNameMap.get(allLanguages.get(j).getSource());

                    for (int k = 0; k < supportedTranslationModels.size(); k++) {
                        if (supportedTranslationModels.get(k).get(0).equals(sourceLanguage)){
                            alreadyContainsFlag = true;
                            entryLanguagePosition = k;
                        }
                    }

                    if (!alreadyContainsFlag) {
                        ArrayList<String> tempList = new ArrayList<String>();
                        tempList.add(sourceLanguage);
                        supportedTranslationModels.add(tempList);
                        entryLanguagePosition = supportedTranslationModels.size() - 1;
                    }

                    String targetLanguage = shortToLongLanguageNameMap.get(allLanguages.get(j).getTarget());
                    supportedTranslationModels.get(entryLanguagePosition).add(targetLanguage);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(final Void result) {

            ArrayList<String> sourceLanguages = new ArrayList<String>();

            int englishLocation = 0; //I want to default to English input language

            for (int i = 0; i < supportedTranslationModels.size(); i++) {
                String supportedModelSource = supportedTranslationModels.get(i).get(0);

                if (supportedModelSource.equals("English"))
                    englishLocation = i;

                sourceLanguages.add(supportedModelSource);
            }

            ArrayAdapter<String> inputAdapter = new ArrayAdapter<String>(getApplicationContext(),
                    R.layout.spinner_item, sourceLanguages);

            inputAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            entryLanguageSpinner.setAdapter(inputAdapter);
            entryLanguageSpinner.setSelection(englishLocation);

            entryLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                    resultLanguageSpinner.setEnabled(true);

                    String entryLanguage = (String)parent.getItemAtPosition(position);
                    ArrayList<String> supportOutput = new ArrayList<String>();

                    for (int i = 0; i < supportedTranslationModels.size(); i++) {
                        if (supportedTranslationModels.get(i).get(0).equals(entryLanguage)){
                            supportOutput.addAll(supportedTranslationModels.get(i));
                            supportOutput.remove(0);
                        }
                    }

                    ArrayAdapter<String> outputAdapter = new ArrayAdapter<String>(getApplicationContext(),
                            R.layout.spinner_item, supportOutput);
                    outputAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    resultLanguageSpinner.setAdapter(outputAdapter);
                    resultLanguageSpinner.setSelection(0);
                    sendUIDataToTranslationTask();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    resultLanguageSpinner.setEnabled(false);
                }
            });

            resultLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    sendUIDataToTranslationTask();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
    }

    /**
     * Asynchronous Task used to consume watsonnlpTranslation Service given an input
     * language, output language, and input text. Shows result in the resultEditText
     */
    private class TranslationTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String inputText = params[0];
            String inputLanguage = params[1];
            String outputLanguage = params[2];

            // Translation Model Names are of the format shortName-shortName (so "en-ar")
            String tlModelName = longToShortLanguageNameMap.get(inputLanguage) + "-"
                    + longToShortLanguageNameMap.get(outputLanguage);

            TranslationResult result = translationService.translate(inputText, tlModelName).execute();

            return result.getFirstTranslation();
        }

        @Override
        protected void onPostExecute(String result) {
            resultEditText.setText(result);
        }
    }

    /**
     * Asynchronous Task used to consume Watson Text to Speech Service. Takes an input string and
     * either creates and plays an audio transcript of the string or displays a Toast for unsupported
     * languages. Unsupported languages are dynamically received from Watson Text to Speech.
     */
    private class SpeechTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            String inputText = params[0];
            String inputLanguage = longToShortLanguageNameMap.get(params[1]); //grab internal short-name

            List<Voice> allVoices = speechService.getVoices().execute();

            boolean voiceAvailable = false;
            Voice matchingVoice = new Voice();

            // Iterate through all available voices, if a voice in the correct language is found use it
            for (int i = 0; i < allVoices.size(); i++) {
                //Voice.language is of the form "en-English". We just want the short-name
                String languageShortName = allVoices.get(i).getLanguage().substring(0, 2);

                if (languageShortName.equals(inputLanguage)) {
                    voiceAvailable = true;
                    matchingVoice = allVoices.get(i);
                    break;
                }
            }

            // If we can't find a voice we'll inform the user
            if (!voiceAvailable) {
                return false;
            }

            // Synthesize returns an inputStream, we have to write to a file before playback can occur
            InputStream in = speechService.synthesize(inputText, matchingVoice,
                    AudioFormat.OGG_VORBIS).execute();

            try {
                File tempTranslation = File.createTempFile("translation", ".ogg", getCacheDir());
                FileOutputStream out = new FileOutputStream(tempTranslation);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
                out.close();
                in.close();

                FileInputStream fis = new FileInputStream(tempTranslation);

                mediaPlayer.reset();
                mediaPlayer.setDataSource(fis.getFD());

                fis.close();

                mediaPlayer.prepare();
                mediaPlayer.start();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                String unspeakableLanguage = resultLanguageSpinner.getSelectedItem().toString();

                Toast.makeText(getApplicationContext(), "Speech-to-Text does not currently support "
                        + unspeakableLanguage + " playback.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
