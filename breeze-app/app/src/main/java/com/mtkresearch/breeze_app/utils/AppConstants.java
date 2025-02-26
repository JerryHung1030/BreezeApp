package com.mtkresearch.breeze_app.utils;

import android.content.Context;
import java.io.File;
import android.util.Log;

public class AppConstants {
    // Shared Preferences
    public static final String PREFS_NAME = "GAISettings";
    
    // Preference Keys
    public static final String KEY_HISTORY_LOOKBACK = "history_lookback";
    public static final String KEY_SEQUENCE_LENGTH = "sequence_length";
    public static final String KEY_DEFAULT_MODEL = "default_model";
    public static final String KEY_FIRST_LAUNCH = "first_launch";
    public static final String KEY_TEMPERATURE = "temperature";
    public static final String KEY_PREFERRED_BACKEND = "preferred_backend";
    public static final String DEFAULT_BACKEND = "cpu";  // Default to CPU backend
    
    // Service Enable Flags
    public static final boolean LLM_ENABLED = true;  // LLM is essential
    public static final boolean VLM_ENABLED = false; // VLM is experimental
    public static final boolean ASR_ENABLED = false; // ASR requires permission
    public static final boolean TTS_ENABLED = true;  // TTS is stable
    
    // Backend Constants
    public static final String BACKEND_NONE = "none";
    public static final String BACKEND_CPU = "cpu";
    public static final String BACKEND_MTK = "mtk";
    public static final String BACKEND_DEFAULT = BACKEND_CPU;  // Default to CPU backend since MTK is experimental
    
    // Backend Enable Flags
    public static final boolean MTK_BACKEND_ENABLED = false;  // Set to true to enable MTK backend
    public static volatile boolean MTK_BACKEND_AVAILABLE = false;  // Runtime state of MTK backend availability
    
    // Backend Initialization Constants
    public static final int MAX_MTK_INIT_ATTEMPTS = 5;
    public static final long MTK_CLEANUP_TIMEOUT_MS = 5000;  // 5 seconds timeout for cleanup
    public static final long MTK_NATIVE_OP_TIMEOUT_MS = 2000;  // 2 seconds timeout for native operations
    public static final long BACKEND_INIT_DELAY_MS = 200;    // Delay between backend initialization attempts
    public static final long BACKEND_CLEANUP_DELAY_MS = 100; // Delay for backend cleanup operations
    
    // LLM Service Constants
    public static final long LLM_INIT_TIMEOUT_MS = 300000;  // 5 minutes for initialization
    public static final long LLM_GENERATION_TIMEOUT_MS = Long.MAX_VALUE;  // No timeout for generation
    public static final long LLM_NATIVE_OP_TIMEOUT_MS = 10000;  // 10 seconds for native ops
    public static final long LLM_CLEANUP_TIMEOUT_MS = 10000;  // 10 seconds for cleanup
    public static final int LLM_MAX_MTK_INIT_ATTEMPTS = 3;
    
    // Model Files and Paths
    public static final String LLAMA_MODEL_FILE = "llama3_2.pte";
    public static final String BREEZE_MODEL_FILE = "Breeze-Tiny-Instruct-v0_1-2048.pte";
    public static final String LLAMA_MODEL_DIR = "/data/local/tmp/llama/";  // Legacy location
    public static final String APP_MODEL_DIR = "models";  // New path relative to app's private storage
    public static final String LLM_TOKENIZER_FILE = "tokenizer.bin";  // Add tokenizer filename constant
    public static final String LLM_TOKENIZER_PATH = "/data/user/0/com.mtkresearch.breeze_app.breeze/files/models/tokenizer.bin";  // Default to app's private storage
    public static final String MODEL_PATH = "/data/user/0/com.mtkresearch.breeze_app.breeze/files/models/" + BREEZE_MODEL_FILE;  // Default to app's private storage
    
    // Get absolute path to the app's model directory
    public static String getAppModelDir(Context context) {
        return new File(context.getFilesDir(), APP_MODEL_DIR).getAbsolutePath();
    }

    // Get the model path to use, prioritizing app's private storage
    public static String getModelPath(Context context) {
        // First check the app's private directory
        File appModelFile = new File(new File(context.getFilesDir(), APP_MODEL_DIR), BREEZE_MODEL_FILE);
        Log.d("AppConstants", "Checking app model path: " + appModelFile.getAbsolutePath());
        if (appModelFile.exists() && appModelFile.length() > 0) {
            Log.d("AppConstants", "Found model in app directory: " + appModelFile.getAbsolutePath());
            return appModelFile.getAbsolutePath();
        }

        // Then check the legacy location
        File legacyModelFile = new File(LLAMA_MODEL_DIR, BREEZE_MODEL_FILE);
        Log.d("AppConstants", "Checking legacy model path: " + legacyModelFile.getAbsolutePath());
        if (legacyModelFile.exists() && legacyModelFile.length() > 0) {
            Log.d("AppConstants", "Found model in legacy directory: " + legacyModelFile.getAbsolutePath());
            return legacyModelFile.getAbsolutePath();
        }

        // If neither exists, return the app's private path as default (for download dialog)
        Log.d("AppConstants", "No model found, returning app path: " + appModelFile.getAbsolutePath());
        return appModelFile.getAbsolutePath();
    }

    // Get the tokenizer path to use
    public static String getTokenizerPath(Context context) {
        // First check the app's private directory
        File appTokenizerFile = new File(new File(context.getFilesDir(), APP_MODEL_DIR), LLM_TOKENIZER_FILE);
        if (appTokenizerFile.exists() && appTokenizerFile.length() > 0) {
            return appTokenizerFile.getAbsolutePath();
        }

        // Then check the legacy location
        File legacyTokenizerFile = new File(LLAMA_MODEL_DIR, LLM_TOKENIZER_FILE);
        if (legacyTokenizerFile.exists() && legacyTokenizerFile.length() > 0) {
            return legacyTokenizerFile.getAbsolutePath();
        }

        // If neither exists, return the app's private path as default
        return appTokenizerFile.getAbsolutePath();
    }

    // Get the model file, checking both app-specific and legacy locations
    public static String findModelFile(Context context, String modelFileName) {
        // First check the app's private directory
        File appModelFile = new File(new File(context.getFilesDir(), APP_MODEL_DIR), modelFileName);
        Log.d("AppConstants", "Checking app model path: " + appModelFile.getAbsolutePath());
        if (appModelFile.exists() && appModelFile.length() > 0) {
            Log.d("AppConstants", "Found model in app directory: " + appModelFile.getAbsolutePath());
            return appModelFile.getAbsolutePath();
        }

        // Then check the legacy location
        File legacyModelFile = new File(LLAMA_MODEL_DIR, modelFileName);
        Log.d("AppConstants", "Checking legacy model path: " + legacyModelFile.getAbsolutePath());
        if (legacyModelFile.exists() && legacyModelFile.length() > 0) {
            Log.d("AppConstants", "Found model in legacy directory: " + legacyModelFile.getAbsolutePath());
            return legacyModelFile.getAbsolutePath();
        }

        // If neither exists, return the app's private path as default
        Log.d("AppConstants", "No model found, returning app path: " + appModelFile.getAbsolutePath());
        return appModelFile.getAbsolutePath();
    }

    // LLM Sequence Length Constants
    public static final int LLM_MAX_SEQ_LENGTH = MODEL_PATH.contains("2048") ? 2048 : 128;
    public static final int LLM_MIN_OUTPUT_LENGTH = MODEL_PATH.contains("2048") ? 512 : 32;
    public static final int LLM_MAX_INPUT_LENGTH = LLM_MAX_SEQ_LENGTH - LLM_MIN_OUTPUT_LENGTH;
    
    // LLM Response Messages
    public static final String LLM_ERROR_RESPONSE = "[!!!] LLM engine backend failed";
    public static final String LLM_DEFAULT_ERROR_RESPONSE = "I apologize, but I encountered an error generating a response. Please try again.";
    public static final String LLM_EMPTY_RESPONSE_ERROR = "I apologize, but I couldn't generate a proper response. Please try rephrasing your question.";
    public static final String LLM_INPUT_TOO_LONG_ERROR = "I apologize, but your input is too long. Please try breaking it into smaller parts.";
    
    // LLM Configuration
    public static final float LLM_TEMPERATURE = 0.0f;
    
    // When false: Send button always shows send icon and only sends messages
    // When true: Send button toggles between send and audio chat mode
    public static final boolean AUDIO_CHAT_ENABLED = false;

    // Conversation History Constants
    public static final int CONVERSATION_HISTORY_LOOKBACK = BREEZE_MODEL_FILE.contains("2048") ? 10 : 3;

    // Activity Request Codes
    public static final int PERMISSION_REQUEST_CODE = 123;
    public static final int PICK_IMAGE_REQUEST = 1;
    public static final int CAPTURE_IMAGE_REQUEST = 2;
    public static final int PICK_FILE_REQUEST = 3;

    // UI Constants
    public static final float ENABLED_ALPHA = 1.0f;
    public static final float DISABLED_ALPHA = 0.3f;

    // Get history lookback based on model sequence length
    public static int getConversationHistoryLookback(String modelName) {
        return modelName != null && modelName.contains("2048") ? 10 : 3;  // 10 messages for 2048 models, 3 for others
    }

    public static final int TAPS_TO_SHOW_MAIN = 7;
    public static final long TAP_TIMEOUT_MS = 3000;
    public static final int INIT_DELAY_MS = 1000;

    // Activity Tags
    public static final String CHAT_ACTIVITY_TAG = "ChatActivity";
    public static final String MAIN_ACTIVITY_TAG = "MainActivity";
    public static final String AUDIO_CHAT_ACTIVITY_TAG = "AudioChatActivity";

    // Model Download Constants
    private static final String MODEL_BASE_URL = "https://huggingface.co/MediaTek-Research/Breeze-Tiny-Instruct-v0_1-mobile/resolve/main/";
    private static final String HF_MIRROR_URL = "https://hf-mirror.com/MediaTek-Research/Breeze-Tiny-Instruct-v0_1-mobile/resolve/main/";
    
    public static final String[] MODEL_DOWNLOAD_URLS = {
        // Tokenizer - small file, use regular URL
        MODEL_BASE_URL + "tokenizer.bin?download=true",
        // Model file - try multiple reliable sources
        MODEL_BASE_URL + BREEZE_MODEL_FILE + "?download=true",
        HF_MIRROR_URL + BREEZE_MODEL_FILE + "?download=true"
    };

    // HTTP Headers
    public static final String[][] DOWNLOAD_HEADERS = {
        {"User-Agent", "BreezeApp/1.0"},
        {"Accept", "application/octet-stream"},
        {"Connection", "keep-alive"}
    };

    // Optimize buffer size for large files (8MB buffer)
    public static final int MODEL_DOWNLOAD_BUFFER_SIZE = 8 * 1024 * 1024;
    
    // More frequent progress updates for better UX
    public static final int MODEL_DOWNLOAD_PROGRESS_UPDATE_INTERVAL = 1;
    
    // Increase timeout for large files (30 minutes)
    public static final long MODEL_DOWNLOAD_TIMEOUT_MS = 1800000;
    
    // Required free space (8GB)
    public static final long MODEL_DOWNLOAD_MIN_SPACE_MB = 8192;
    
    // Disable parallel downloads since servers don't support it well
    public static final boolean MODEL_DOWNLOAD_PARALLEL = false;
    
    // Temporary extension for partial downloads
    public static final String MODEL_DOWNLOAD_TEMP_EXTENSION = ".part";
} 