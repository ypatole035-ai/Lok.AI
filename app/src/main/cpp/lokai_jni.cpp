#include <jni.h>
#include <string>
#include <vector>
#include <thread>
#include <atomic>
#include <android/log.h>

#include "llama.h"

#define LOG_TAG "LokAI_JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static llama_model*   g_model   = nullptr;
static llama_context* g_ctx     = nullptr;
static std::atomic<bool> g_stop_flag{false};
static JavaVM* g_jvm = nullptr;

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    llama_backend_init();
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNI_OnUnload(JavaVM* vm, void* reserved) {
    llama_backend_free();
}

static std::string jstring_to_string(JNIEnv* env, jstring jstr) {
    if (!jstr) return "";
    const char* chars = env->GetStringUTFChars(jstr, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(jstr, chars);
    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_lokai_app_data_inference_LlamaEngine_loadModel(
        JNIEnv* env, jobject, jstring modelPath, jint threads, jint contextSize, jint gpuLayers) {
    if (g_ctx)   { llama_free(g_ctx);         g_ctx   = nullptr; }
    if (g_model) { llama_model_free(g_model); g_model = nullptr; }
    std::string path = jstring_to_string(env, modelPath);
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = (int)gpuLayers;
    g_model = llama_model_load_from_file(path.c_str(), model_params);
    if (!g_model) return JNI_FALSE;
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = (uint32_t)contextSize;
    ctx_params.n_threads = (uint32_t)threads;
    ctx_params.n_threads_batch = (uint32_t)threads;
    g_ctx = llama_init_from_model(g_model, ctx_params);
    if (!g_ctx) { llama_model_free(g_model); g_model = nullptr; return JNI_FALSE; }
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_lokai_app_data_inference_LlamaEngine_runInference(
        JNIEnv* env, jobject, jstring prompt, jint maxTokens, jfloat temperature, jobject callback) {
    if (!g_model || !g_ctx) {
        jclass c = env->GetObjectClass(callback);
        jmethodID m = env->GetMethodID(c, "onError", "(Ljava/lang/String;)V");
        env->CallVoidMethod(callback, m, env->NewStringUTF("Model not loaded"));
        return;
    }
    g_stop_flag.store(false);
    std::string prompt_str = jstring_to_string(env, prompt);
    std::vector<llama_token> tokens(prompt_str.size() + 64);
    int n_tokens = llama_tokenize(llama_model_get_vocab(g_model), prompt_str.c_str(), (int32_t)prompt_str.size(),
        tokens.data(), (int32_t)tokens.size(), true, false);
    if (n_tokens < 0) {
        jclass c = env->GetObjectClass(callback);
        jmethodID m = env->GetMethodID(c, "onError", "(Ljava/lang/String;)V");
        env->CallVoidMethod(callback, m, env->NewStringUTF("Tokenization failed"));
        return;
    }
    tokens.resize(n_tokens);
    llama_kv_cache_seq_rm(g_ctx, 0, -1, -1);
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
        llama_batch_free(batch);
        jclass c = env->GetObjectClass(callback);
        jmethodID m = env->GetMethodID(c, "onError", "(Ljava/lang/String;)V");
        env->CallVoidMethod(callback, m, env->NewStringUTF("Prompt evaluation failed"));
        return;
    }
    jclass cbClass = env->GetObjectClass(callback);
    jmethodID onToken    = env->GetMethodID(cbClass, "onToken",    "(Ljava/lang/String;)V");
    jmethodID onComplete = env->GetMethodID(cbClass, "onComplete", "()V");
    llama_sampler* sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(sampler, llama_sampler_init_temp((float)temperature));
    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
    const llama_vocab* vocab = llama_model_get_vocab(g_model);
    int n_cur = n_tokens, n_decode = 0;
    while (n_decode < (int)maxTokens) {
        if (g_stop_flag.load()) break;
        llama_token new_token = llama_sampler_sample(sampler, g_ctx, -1);
        if (llama_vocab_is_eog(vocab, new_token)) break;
        char buf[256] = {0};
        int len = llama_token_to_piece(vocab, new_token, buf, sizeof(buf)-1, 0, true);
        if (len < 0) len = 0;
        buf[len] = '\0';
        jstring tokenStr = env->NewStringUTF(buf);
        env->CallVoidMethod(callback, onToken, tokenStr);
        env->DeleteLocalRef(tokenStr);
        if (env->ExceptionCheck()) { env->ExceptionClear(); break; }
        llama_batch_free(batch);
        batch = llama_batch_init(1, 0, 1);
        batch.token[0] = new_token; batch.pos[0] = n_cur;
        batch.n_seq_id[0] = 1; batch.seq_id[0][0] = 0;
        batch.logits[0] = 1; batch.n_tokens = 1;
        if (llama_decode(g_ctx, batch) != 0) break;
        n_cur++; n_decode++;
    }
    llama_sampler_free(sampler);
    llama_batch_free(batch);
    env->CallVoidMethod(callback, onComplete);
}

extern "C" JNIEXPORT void JNICALL
Java_com_lokai_app_data_inference_LlamaEngine_stopInference(JNIEnv* env, jobject) {
    g_stop_flag.store(true);
}

extern "C" JNIEXPORT void JNICALL
Java_com_lokai_app_data_inference_LlamaEngine_unloadModel(JNIEnv* env, jobject) {
    g_stop_flag.store(true);
    if (g_ctx)   { llama_free(g_ctx);         g_ctx   = nullptr; }
    if (g_model) { llama_model_free(g_model); g_model = nullptr; }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_lokai_app_data_inference_LlamaEngine_getContextUsed(JNIEnv* env, jobject) {
    if (!g_ctx) return 0;
    return (jint)llama_n_ctx(g_ctx);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_lokai_app_data_inference_LlamaEngine_getContextMax(JNIEnv* env, jobject) {
    if (!g_ctx) return 0;
    return (jint)llama_n_ctx(g_ctx);
}
