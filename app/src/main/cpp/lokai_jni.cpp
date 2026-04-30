#include <jni.h>
#include <string>
#include <thread>
#include <atomic>
#include <android/log.h>

#include "llama.h"

#define LOG_TAG "LokAI_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ─── Global state ─────────────────────────────────────────────────────────────
static llama_model*   g_model   = nullptr;
static llama_context* g_ctx     = nullptr;
static std::atomic<bool> g_stop_flag{false};
static JavaVM* g_jvm = nullptr;

// ─── JNI_OnLoad ───────────────────────────────────────────────────────────────
JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    llama_backend_init();
    LOGI("llama.cpp backend initialized");
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved) {
    llama_backend_free();
    LOGI("llama.cpp backend freed");
}

// ─── Helper: jstring → std::string ───────────────────────────────────────────
static std::string jstring_to_string(JNIEnv* env, jstring jstr) {
    if (!jstr) return "";
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return result;
}

// ─── loadModel ────────────────────────────────────────────────────────────────
extern "C" JNIEXPORT jboolean JNICALL
Java_com_lokai_app_data_inference_LlamaEngine_loadModel(
        JNIEnv* env,
        jobject /* thiz */,
        jstring modelPath,
        jint    threads,
        jint    contextSize,
        jint    gpuLayers)    // always 0 on Android
{
    // Unload any existing model first
    if (g_ctx)   { llama_free(g_ctx);         g_ctx   = nullptr; }
    if (g_model) { llama_free_model(g_model); g_model = nullptr; }

    std::string path = jstring_to_string(env, modelPath);
    LOGI("Loading model: %s  threads=%d  ctx=%d  gpu_layers=%d",
         path.c_str(), (int)threads, (int)contextSize, (int)gpuLayers);

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = (int)gpuLayers;  // 0 on Android

    g_model = llama_load_model_from_file(path.c_str(), model_params);
    if (!g_model) {
        LOGE("Failed to load model from: %s", path.c_str());
        return JNI_FALSE;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx      = (uint32_t)contextSize;
    ctx_params.n_threads  = (uint32_t)threads;
    ctx_params.n_threads_batch = (uint32_t)threads;

    g_ctx = llama_new_context_with_model(g_model, ctx_params);
    if (!g_ctx) {
        LOGE("Failed to create llama context");
        llama_free_model(g_model);
        g_model = nullptr;
        return JNI_FALSE;
    }

    LOGI("Model loaded successfully");
    return JNI_TRUE;
}

// ─── runInference ─────────────────────────────────────────────────────────────
// callback interface: com.lokai.app.data.inference.TokenCallback
// void onToken(String token)
// void onComplete()
// void onError(String message)
extern "C" JNIEXPORT void JNICALL
Java_com_lokai_app_data_inference_LlamaEngine_runInference(
        JNIEnv* env,
        jobject /* thiz */,
        jstring prompt,
        jint    maxTokens,
        jfloat  temperature,
        jobject callback)
{
    if (!g_model || !g_ctx) {
        LOGE("runInference called but model not loaded");
        jclass cbClass = env->GetObjectClass(callback);
        jmethodID onError = env->GetMethodID(cbClass, "onError", "(Ljava/lang/String;)V");
        jstring errMsg = env->NewStringUTF("Model not loaded");
        env->CallVoidMethod(callback, onError, errMsg);
        env->DeleteLocalRef(errMsg);
        return;
    }

    g_stop_flag.store(false);

    std::string prompt_str = jstring_to_string(env, prompt);
    LOGI("runInference: prompt_len=%zu  max_tokens=%d  temp=%.2f",
         prompt_str.size(), (int)maxTokens, (float)temperature);

    // Tokenize prompt
    std::vector<llama_token> tokens(prompt_str.size() + 64);
    int n_tokens = llama_tokenize(
        g_model,
        prompt_str.c_str(),
        (int32_t)prompt_str.size(),
        tokens.data(),
        (int32_t)tokens.size(),
        /*add_special=*/true,
        /*parse_special=*/false
    );

    if (n_tokens < 0) {
        LOGE("Tokenization failed, buffer too small");
        jclass cbClass = env->GetObjectClass(callback);
        jmethodID onError = env->GetMethodID(cbClass, "onError", "(Ljava/lang/String;)V");
        jstring errMsg = env->NewStringUTF("Tokenization failed");
        env->CallVoidMethod(callback, onError, errMsg);
        env->DeleteLocalRef(errMsg);
        return;
    }
    tokens.resize(n_tokens);

    // Clear KV cache and evaluate prompt
    llama_kv_cache_clear(g_ctx);

    // Build batch for prompt tokens
    llama_batch batch = llama_batch_init(512, 0, 1);

    for (int i = 0; i < n_tokens; i++) {
        batch.token[batch.n_tokens]     = tokens[i];
        batch.pos[batch.n_tokens]       = i;
        batch.n_seq_id[batch.n_tokens]  = 1;
        batch.seq_id[batch.n_tokens][0] = 0;
        batch.logits[batch.n_tokens]    = (i == n_tokens - 1) ? 1 : 0;
        batch.n_tokens++;
    }

    if (llama_decode(g_ctx, batch) != 0) {
        LOGE("llama_decode failed for prompt");
        llama_batch_free(batch);
        jclass cbClass = env->GetObjectClass(callback);
        jmethodID onError = env->GetMethodID(cbClass, "onError", "(Ljava/lang/String;)V");
        jstring errMsg = env->NewStringUTF("Prompt evaluation failed");
        env->CallVoidMethod(callback, onError, errMsg);
        env->DeleteLocalRef(errMsg);
        return;
    }

    // Get callback method IDs
    jclass cbClass   = env->GetObjectClass(callback);
    jmethodID onToken    = env->GetMethodID(cbClass, "onToken",    "(Ljava/lang/String;)V");
    jmethodID onComplete = env->GetMethodID(cbClass, "onComplete", "()V");
    jmethodID onError    = env->GetMethodID(cbClass, "onError",    "(Ljava/lang/String;)V");

    // Sampling
    llama_sampler* sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_temp((float)temperature));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

    int n_cur = n_tokens;
    int n_decode = 0;

    while (n_decode < (int)maxTokens) {
        if (g_stop_flag.load()) {
            LOGI("Inference stopped by user");
            break;
        }

        llama_token new_token = llama_sampler_sample(sampler, g_ctx, -1);

        if (llama_token_is_eog(g_model, new_token)) {
            LOGI("EOG token reached after %d tokens", n_decode);
            break;
        }

        // Decode token to string
        char buf[256] = {0};
        int len = llama_token_to_piece(g_model, new_token, buf, sizeof(buf) - 1, 0, true);
        if (len < 0) len = 0;
        buf[len] = '\0';

        // Emit token to Kotlin
        jstring tokenStr = env->NewStringUTF(buf);
        env->CallVoidMethod(callback, onToken, tokenStr);
        env->DeleteLocalRef(tokenStr);

        // Check for Java exceptions
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            LOGE("Java exception during onToken callback");
            break;
        }

        // Prepare next batch (single token)
        llama_batch_free(batch);
        batch = llama_batch_init(1, 0, 1);
        batch.token[0]     = new_token;
        batch.pos[0]       = n_cur;
        batch.n_seq_id[0]  = 1;
        batch.seq_id[0][0] = 0;
        batch.logits[0]    = 1;
        batch.n_tokens     = 1;

        if (llama_decode(g_ctx, batch) != 0) {
            LOGE("llama_decode failed during generation");
            break;
        }

        n_cur++;
        n_decode++;
    }

    llama_sampler_free(sampler);
    llama_batch_free(batch);

    LOGI("Inference complete: %d tokens generated", n_decode);
    env->CallVoidMethod(callback, onComplete);
}

// ─── stopInference ────────────────────────────────────────────────────────────
extern "C" JNIEXPORT void JNICALL
Java_com_lokai_app_data_inference_LlamaEngine_stopInference(
        JNIEnv* env,
        jobject /* thiz */)
{
    LOGI("stopInference called");
    g_stop_flag.store(true);
}

// ─── unloadModel ──────────────────────────────────────────────────────────────
extern "C" JNIEXPORT void JNICALL
Java_com_lokai_app_data_inference_LlamaEngine_unloadModel(
        JNIEnv* env,
        jobject /* thiz */)
{
    LOGI("unloadModel called");
    g_stop_flag.store(true);

    if (g_ctx) {
        llama_free(g_ctx);
        g_ctx = nullptr;
    }
    if (g_model) {
        llama_free_model(g_model);
        g_model = nullptr;
    }
    LOGI("Model unloaded");
}

// ─── getContextUsed ───────────────────────────────────────────────────────────
extern "C" JNIEXPORT jint JNICALL
Java_com_lokai_app_data_inference_LlamaEngine_getContextUsed(
        JNIEnv* env,
        jobject /* thiz */)
{
    if (!g_ctx) return 0;
    return (jint)llama_get_kv_cache_used_cells(g_ctx);
}

// ─── getContextMax ────────────────────────────────────────────────────────────
extern "C" JNIEXPORT jint JNICALL
Java_com_lokai_app_data_inference_LlamaEngine_getContextMax(
        JNIEnv* env,
        jobject /* thiz */)
{
    if (!g_ctx) return 0;
    return (jint)llama_n_ctx(g_ctx);
}
