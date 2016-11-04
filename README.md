<img src="https://bluemixassets.eu-gb.mybluemix.net/api/Products/image/logos/watson-text-to-speech.svg?key=[starter-watson-language]&event=readme-image-view" alt="Watson Text To Speech Logo" width="200px"/>

## Watson Language
Bluemix Mobile Starter for Watson Language in Java

[![](https://img.shields.io/badge/bluemix-powered-blue.svg)](https://bluemix.net)
[![](https://img.shields.io/badge/platform-android-lightgrey.svg?style=flat)](https://developer.android.com/index.html)

### Table of Contents
* [Summary](#summary)
* [Requirements](#requirements)
* [Configuration](#configuration)
* [Run](#run)
* [License](#license)

### Summary
The Bluemix Mobile Starter for Watson Language showcases the Text To Speech and Language Translator services from Watson and gives you integration points for each of the Bluemix Mobile services.

### Requirements
* A [Bluemix](http://bluemix.net) Account
* [Android Studio](https://developer.android.com/studio/index.html) and [Gradle](https://gradle.org/gradle-download/)
* [LanguageTranslator](https://console.ng.bluemix.net/catalog/language-translator) and [TextToSpeech](https://console.ng.bluemix.net/catalog/services/text-to-speech/) service instances obtained from the [Bluemix Catalog](https://console.ng.bluemix.net/catalog/)

### Configuration
* Open the project in Android Studio and perform a Gradle Sync.
* Navigate to `res/values/watson_credentials.xml` and input your Username and Password for both Text to Speech (`watson_text_to_speech`) and LanguageTranslator (`watson_language_translator`).
 
```HTML
<resources>
    <string name="watson_language_translator_password">xxxxxxxxxxxx</string>
    <string name="watson_language_translator_username">xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</string>
    <string name="watson_text_to_speech_password">xxxxxxxxxxxx</string>
    <string name="watson_text_to_speech_username">xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx</string>
</resources>
```

Obtain these credentials from your Bluemix Service Dashboard for these two services:
![LanguageTranslationCredentials](README_Images/LanguageTranslatorCredentials.png)
![TextToSpeechCredentials](README_Images/TextToSpeechCredentials.png)


### Run
* You can now build and run the application from Android Studio!

![En-fr Translation](README_Images/watsonlanguageandroid.png)

The application allows you to do Language Translator and Text To Speech for multiple input and output languages. Choose your input language, type a message in the corresponding text box, and then click the text to speech or translation button.

### License 
This package contains code licensed under the Apache License, Version 2.0 (the "License"). You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 and may also view the License in the LICENSE file within this package.
