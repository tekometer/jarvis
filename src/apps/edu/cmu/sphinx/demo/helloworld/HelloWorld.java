/*
 * Copyright 1999-2004 Carnegie Mellon University.
 * Portions Copyright 2004 Sun Microsystems, Inc.
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.demo.helloworld;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import java.util.regex.Pattern;

import javax.speech.Central;
import javax.speech.Engine;
import javax.speech.EngineList;
import javax.speech.EngineException;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerModeDesc;
import javax.speech.synthesis.SynthesizerProperties;
import javax.speech.synthesis.Voice;

import edu.cmu.sphinx.frontend.util.Microphone;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple HelloWorld demo showing a simple speech application built using Sphinx-4. This application uses the Sphinx-4
 * endpointer, which automatically segments incoming audio into utterances and silences.
 */
public class HelloWorld {
    Synthesizer synthesizer;
    Map<String, String> responseMap = new HashMap<String, String>();


    public void addResponse(String input, String output){
        responseMap.put(input, output);
    }

    public String getResponse(String input){
        return responseMap.get(input);
    }

    /**
     * Returns a "no synthesizer" message, and asks 
     * the user to check if the "speech.properties" file is
     * at <code>user.home</code> or <code>java.home/lib</code>.
     *
     * @return a no synthesizer message
     */
    static private String noSynthesizerMessage() {
        String message =
            "No synthesizer created.  This may be the result of any\n" +
            "number of problems.  It's typically due to a missing\n" +
            "\"speech.properties\" file that should be at either of\n" +
            "these locations: \n\n";
        message += "user.home    : " + System.getProperty("user.home") + "\n";
        message += "java.home/lib: " + System.getProperty("java.home") +
        File.separator + "lib\n\n" +
            "Another cause of this problem might be corrupt or missing\n" +
            "voice jar files in the freetts lib directory.  This problem\n" +
            "also sometimes arises when the freetts.jar file is corrupt\n" +
            "or missing.  Sorry about that.  Please check for these\n" +
            "various conditions and then try again.\n";
        return message;
    }


    /** 
     * Speak the given text string
     */
    public void speakText(String textToSpeak){
        if (textToSpeak != null) {
            synthesizer.speakPlainText(textToSpeak, null);
        } else {
            // throw new IllegalArgumentException("Bad time format");
        System.out.println("Unable to speak the text");
        }
    }
        

    public void initSpeechEngine(){
            String _voice = "kevin16";
            try {
            /* Find a synthesizer that has the general domain voice
                 * we are looking for.  NOTE:  this uses the Central class
                 * of JSAPI to find a Synthesizer.  The Central class
                 * expects to find a speech.properties file in user.home
                 * or java.home/lib.
                 *
                 * If your situation doesn't allow you to set up a
                 * speech.properties file, you can circumvent the Central
                 * class and do a very non-JSAPI thing by talking to
                 * FreeTTSEngineCentral directly.  See the WebStartClock
                 * demo for an example of how to do this.
                 */
            SynthesizerModeDesc desc = new SynthesizerModeDesc(
                    null,          // engine name
                    "general",        // mode name
                    Locale.US,     // locale
                    null,          // running
                    null);         // voice

            synthesizer = Central.createSynthesizer(desc);

                /* Just an informational message to guide users that didn't
                 * set up their speech.properties file. 
                 */
            if (synthesizer == null) {
                System.err.println(noSynthesizerMessage());
                System.exit(1);
            }

            /* Get the synthesizer ready to speak
                 */
            synthesizer.allocate();
            synthesizer.resume();

                /* Choose the voice.
                 */
                desc = (SynthesizerModeDesc) synthesizer.getEngineModeDesc();
                Voice[] voices = desc.getVoices();
                Voice voice = null;
                for (int i = 0; i < voices.length; i++) {
                    if (voices[i].getName().equals(_voice)) {
                        voice = voices[i];
                        break;
                    }
                }
                if (voice == null) {
                    System.err.println(
                        "Synthesizer does not have a voice named " + _voice + ".");
                    System.exit(1);
                }
                synthesizer.getSynthesizerProperties().setVoice(voice);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Example of how to list all the known voices for a specific
     * mode using just JSAPI.  FreeTTS maps the domain name to the
     * JSAPI mode name.  The currently supported domains are
     * "general," which means general purpose synthesis for tasks
     * such as reading e-mail, and "time" which means a domain that's
     * only good for speaking the time of day. 
     */
    public static void listAllVoices(String modeName) {
        
        System.out.println();
        System.out.println(
            "All " + modeName + " Mode JSAPI Synthesizers and Voices:");

        /* Create a template that tells JSAPI what kind of speech
         * synthesizer we are interested in.  In this case, we're
         * just looking for a general domain synthesizer for US
         * English.
         */ 
        SynthesizerModeDesc required = new SynthesizerModeDesc(
            null,      // engine name
            modeName,  // mode name
            Locale.US, // locale
            null,      // running
            null);     // voices

        /* Contact the primary entry point for JSAPI, which is
         * the Central class, to discover what synthesizers are
         * available that match the template we defined above.
         */
        EngineList engineList = Central.availableSynthesizers(required);
        for (int i = 0; i < engineList.size(); i++) {
            
            SynthesizerModeDesc desc = (SynthesizerModeDesc) engineList.get(i);
            System.out.println("    " + desc.getEngineName()
                               + " (mode=" + desc.getModeName()
                               + ", locale=" + desc.getLocale() + "):");
            Voice[] voices = desc.getVoices();
            for (int j = 0; j < voices.length; j++) {
                System.out.println("        " + voices[j].getName());
            }
        }
    }



    public static void main(String[] args) {
        ConfigurationManager cm;

        if (args.length > 0) {
            cm = new ConfigurationManager(args[0]);
        } else {
            cm = new ConfigurationManager(HelloWorld.class.getResource("helloworld.config.xml"));
        }

        Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
        recognizer.allocate();

        // start the microphone or exit if the programm if this is not possible
        Microphone microphone = (Microphone) cm.lookup("microphone");
        if (!microphone.startRecording()) {
            System.out.println("Cannot start microphone.");
            recognizer.deallocate();
            System.exit(1);
        }

        System.out.println("Say: (Good morning | Hello) ( Bhiksha | Evandro | Paul | Philip | Rita | Will )");

        HelloWorld helloWorld = new HelloWorld();
        helloWorld.addResponse("how is mandy", "Jarvis thinks mandy is wonderful");
        helloWorld.addResponse("how is dang", "Jarvis thinks dang loves red zone");
        helloWorld.addResponse("jarvis power off tv", "Okay now go away");
        helloWorld.addResponse("what is mandy doing", "she might be dreaming about cats again");
        helloWorld.addResponse("who is mandy", "she is your girl friend don't be an ass");
        helloWorld.addResponse("thank you jarvis", "this is my job now");
        helloWorld.addResponse("cats", "mandy wants you to meow");
        //helloWorld.addResponse("hello dang", "");

        listAllVoices("general");
        
        helloWorld.initSpeechEngine();
        //TextToSpeech textToSpeech = new TextToSpeech("kevin");
        //textToSpeech.initSpeechEngine();

        // loop the recognition until the programm exits.
        while (true) {
            System.out.println("Start speaking. Press Ctrl-C to quit.\n");

            Result result = recognizer.recognize();

            if (result != null) {
                String resultText = result.getBestFinalResultNoFiller();
                System.out.println("You said: " + resultText + '\n');
                String responseText = helloWorld.getResponse(resultText);
                if(responseText != null){
                    helloWorld.speakText(responseText);
                }
                //else{
                    //helloWorld.speakText("Rich I don't know what you are talking about");
                //}
            } else {
                System.out.println("I can't hear what you said.\n");
            }
        }
    }
}
