// Extension lib defines
#define EXTENSION_NAME apkx
#define LIB_NAME "apkx"
#define MODULE_NAME LIB_NAME

// Defold SDK
#include <dmsdk/sdk.h>

#if defined(DM_PLATFORM_ANDROID)

static JNIEnv* Attach()
{
    JNIEnv* env;
    JavaVM* vm = dmGraphics::GetNativeAndroidJavaVM();
    vm->AttachCurrentThread(&env, NULL);
    return env;
}

static bool Detach(JNIEnv* env)
{
    bool exception = (bool) env->ExceptionCheck();
    env->ExceptionClear();
    JavaVM* vm = dmGraphics::GetNativeAndroidJavaVM();
    vm->DetachCurrentThread();
    return !exception;
}

namespace {
struct AttachScope
{
    JNIEnv* m_Env;
    AttachScope() : m_Env(Attach())
    {
    }
    ~AttachScope()
    {
        Detach(m_Env);
    }
};
}

static jclass GetClass(JNIEnv* env, const char* classname)
{
    jclass activity_class = env->FindClass("android/app/NativeActivity");
    jmethodID get_class_loader = env->GetMethodID(activity_class,"getClassLoader", "()Ljava/lang/ClassLoader;");
    jobject cls = env->CallObjectMethod(dmGraphics::GetNativeAndroidActivity(), get_class_loader);
    jclass class_loader = env->FindClass("java/lang/ClassLoader");
    jmethodID find_class = env->GetMethodID(class_loader, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");

    jstring str_class_name = env->NewStringUTF(classname);
    jclass outcls = (jclass)env->CallObjectMethod(cls, find_class, str_class_name);
    env->DeleteLocalRef(str_class_name);
    return outcls;
}

static int get_expansion_apk_file_path(lua_State* L) {
    DM_LUA_STACK_CHECK(L, 1);
    AttachScope attachscope;
    JNIEnv* env = attachscope.m_Env;

    jboolean mainFile = (jboolean)lua_toboolean(L, 1);
    jint version = (jint)luaL_checkint(L, 2);

    jclass cls = GetClass(env, "me.petcu.defoldapkx.DefoldInterface");
    jmethodID method = env->GetStaticMethodID(cls, "getExpansionAPKFilePath", "(Landroid/content/Context;ZI)Ljava/lang/String;");

    jstring return_value = (jstring)env->CallStaticObjectMethod(cls, method,
        dmGraphics::GetNativeAndroidActivity(), mainFile, version
    );
    lua_pushstring(L, env->GetStringUTFChars(return_value, 0));
    env->DeleteLocalRef(return_value);
    return 1;
}

static int get_downloader_string_from_state(lua_State* L) {
    DM_LUA_STACK_CHECK(L, 1);
    AttachScope attachscope;
    JNIEnv* env = attachscope.m_Env;

    jint state = (jint)luaL_checkint(L, 1);

    jclass cls = GetClass(env, "com.google.android.vending.expansion.downloader.Helpers");
    jmethodID method = env->GetStaticMethodID(cls, "getDownloaderStringFromState", "(I)Ljava/lang/String;");

    jstring return_value = (jstring)env->CallStaticObjectMethod(cls, method, state);
    lua_pushstring(L, env->GetStringUTFChars(return_value, 0));
    env->DeleteLocalRef(return_value);
    return 1;
}

static int configure_download_service(lua_State* L) {
    DM_LUA_STACK_CHECK(L, 0);
    AttachScope attachscope;
    JNIEnv* env = attachscope.m_Env;

    lua_getfield(L, 1, "public_key");
    const char* public_key_ptr = luaL_checkstring(L, -1);
    lua_pop(L, 1);

    jstring public_key = env->NewStringUTF(public_key_ptr);

    lua_getfield(L, 1, "salt");
    size_t salt_len;
    const char* salt_ptr = luaL_checklstring(L, -1, &salt_len);
    lua_pop(L, 1);

    jbyteArray salt = env->NewByteArray(salt_len);
    env->SetByteArrayRegion(salt, 0, salt_len, (const jbyte*)salt_ptr);

    jclass cls = GetClass(env, "me.petcu.defoldapkx.DefoldInterface");
    jmethodID method = env->GetStaticMethodID(cls, "configureDownloadService", "(Landroid/content/Context;Ljava/lang/String;[B)V");

    env->CallStaticVoidMethod(cls, method, dmGraphics::GetNativeAndroidActivity(), public_key, salt);
    return 0;
}

static int start_download_service_if_required(lua_State* L) {
    DM_LUA_STACK_CHECK(L, 1);
    AttachScope attachscope;
    JNIEnv* env = attachscope.m_Env;

    jclass cls = GetClass(env, "me.petcu.defoldapkx.DefoldInterface");
    jmethodID method = env->GetStaticMethodID(cls, "startDownloadServiceIfRequired", "(Landroid/app/Activity;)Z");

    jboolean return_value = env->CallStaticBooleanMethod(cls, method, dmGraphics::GetNativeAndroidActivity());
    lua_pushboolean(L, return_value);
    return 1;
}

// Functions exposed to Lua
static const luaL_reg Module_methods[] =
{
    {"get_expansion_apk_file_path", get_expansion_apk_file_path},
    {"get_downloader_string_from_state", get_downloader_string_from_state},
    {"configure_download_service", configure_download_service},
    {"start_download_service_if_required", start_download_service_if_required},
    {0, 0}
};

static void LuaInit(lua_State* L)
{
    int top = lua_gettop(L);

    // Register lua names
    luaL_register(L, MODULE_NAME, Module_methods);

    #define set_enum(field, value) lua_pushnumber(L, value); lua_setfield(L, -2, field)
    set_enum("STATE_IDLE", 1);
    set_enum("STATE_FETCHING_URL", 2);
    set_enum("STATE_CONNECTING", 3);
    set_enum("STATE_DOWNLOADING", 4);
    set_enum("STATE_COMPLETED", 5);
    set_enum("STATE_PAUSED_NETWORK_UNAVAILABLE", 6);
    set_enum("STATE_PAUSED_BY_REQUEST", 7);
    set_enum("STATE_PAUSED_WIFI_DISABLED_NEED_CELLULAR_PERMISSION", 8);
    set_enum("STATE_PAUSED_NEED_CELLULAR_PERMISSION", 9);
    set_enum("STATE_PAUSED_WIFI_DISABLED", 10);
    set_enum("STATE_PAUSED_NEED_WIFI", 11);
    set_enum("STATE_PAUSED_ROAMING", 12);
    set_enum("STATE_PAUSED_NETWORK_SETUP_FAILURE", 13);
    set_enum("STATE_PAUSED_SDCARD_UNAVAILABLE", 14);
    set_enum("STATE_FAILED_UNLICENSED", 15);
    set_enum("STATE_FAILED_FETCHING_URL", 16);
    set_enum("STATE_FAILED_SDCARD_FULL", 17);
    set_enum("STATE_FAILED_CANCELED", 18);
    set_enum("STATE_FAILED", 19);

    lua_pop(L, 1);
    assert(top == lua_gettop(L));
}

static dmExtension::Result AppInitializeExtension(dmExtension::AppParams* params)
{
    return dmExtension::RESULT_OK;
}

static dmExtension::Result InitializeExtension(dmExtension::Params* params)
{
    LuaInit(params->m_L);
    return dmExtension::RESULT_OK;
}

static dmExtension::Result AppFinalizeExtension(dmExtension::AppParams* params)
{
    return dmExtension::RESULT_OK;
}

static dmExtension::Result FinalizeExtension(dmExtension::Params* params)
{
    return dmExtension::RESULT_OK;
}

static void OnEventExtension(dmExtension::Params* params, const dmExtension::Event* event)
{
    if (event->m_Event == dmExtension::EVENT_ID_ACTIVATEAPP) {
        AttachScope attachscope;
        JNIEnv* env = attachscope.m_Env;

        jclass cls = GetClass(env, "me.petcu.defoldapkx.DefoldInterface");
        jmethodID method = env->GetStaticMethodID(cls, "onActivateApp", "(Landroid/content/Context;)V");
        env->CallStaticVoidMethod(cls, method, dmGraphics::GetNativeAndroidActivity());

    } else if (event->m_Event == dmExtension::EVENT_ID_DEACTIVATEAPP) {
        AttachScope attachscope;
        JNIEnv* env = attachscope.m_Env;

        jclass cls = GetClass(env, "me.petcu.defoldapkx.DefoldInterface");
        jmethodID method = env->GetStaticMethodID(cls, "onDeactivateApp", "(Landroid/content/Context;)V");
        env->CallStaticVoidMethod(cls, method, dmGraphics::GetNativeAndroidActivity());
    }
}

#else

static dmExtension::Result AppInitializeExtension(dmExtension::AppParams* params)
{
    return dmExtension::RESULT_OK;
}

static dmExtension::Result InitializeExtension(dmExtension::Params* params)
{
    return dmExtension::RESULT_OK;
}

static dmExtension::Result AppFinalizeExtension(dmExtension::AppParams* params)
{
    return dmExtension::RESULT_OK;
}

static dmExtension::Result FinalizeExtension(dmExtension::Params* params)
{
    return dmExtension::RESULT_OK;
}

#define OnEventExtension 0

#endif

DM_DECLARE_EXTENSION(EXTENSION_NAME, LIB_NAME, AppInitializeExtension, AppFinalizeExtension, InitializeExtension, 0, OnEventExtension, FinalizeExtension)