package io.agora.chatdemo;

import android.app.Activity;
import android.content.Context;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailabilityLight;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import io.agora.CallBack;
import io.agora.chat.ChatClient;
import io.agora.chat.ChatManager;
import io.agora.chat.ChatOptions;
import io.agora.chat.ChatRoomManager;
import io.agora.chat.ContactManager;
import io.agora.chat.GroupManager;
import io.agora.chat.PushManager;
import io.agora.chat.uikit.EaseUIKit;
import io.agora.chat.uikit.models.EaseUser;
import io.agora.chat.uikit.options.EaseAvatarOptions;
import io.agora.chat.uikit.provider.EaseUserProfileProvider;
import io.agora.chatdemo.general.db.DemoDbHelper;
import io.agora.chatdemo.general.manager.UserProfileManager;
import io.agora.chatdemo.general.models.DemoModel;
import io.agora.push.PushConfig;
import io.agora.push.PushHelper;
import io.agora.push.PushListener;
import io.agora.push.PushType;
import io.agora.util.EMLog;


/**
 * 作为hyphenate-sdk的入口控制类，获取sdk下的基础类均通过此类
 */
public class DemoHelper {
    private static final String TAG = DemoHelper.class.getSimpleName();

    public boolean isSDKInit;//SDK是否初始化
    private static DemoHelper mInstance;
    private DemoModel demoModel = null;
    private Map<String, EaseUser> contactList;
    private UserProfileManager userProManager;

    private DemoHelper() {}

    public static DemoHelper getInstance() {
        if(mInstance == null) {
            synchronized (DemoHelper.class) {
                if(mInstance == null) {
                    mInstance = new DemoHelper();
                }
            }
        }
        return mInstance;
    }

    public void init(Context context) {
        demoModel = new DemoModel(context);
        //初始化IM SDK
        if(initSDK(context)) {
            // debug mode, you'd better set it to false, if you want release your App officially.
            ChatClient.getInstance().setDebugMode(true);
            //初始化推送
            initPush(context);
            //注册call Receiver
            //initReceiver(context);
            //初始化ease ui相关
            initEaseUI(context);
        }

    }

    /**
     * 初始化SDK
     * @param context
     * @return
     */
    private boolean initSDK(Context context) {
        // 根据项目需求对SDK进行配置
        ChatOptions options = initChatOptions(context);
        //配置自定义的rest server和im server
        //options.setRestServer("a1-hsb.easemob.com");
        //options.setIMServer("106.75.100.247");
        //options.setImPort(6717);
        // 初始化SDK
        isSDKInit = EaseUIKit.getInstance().init(context, options);
        return isSDKInit();
    }


    /**
     * 判断是否之前登录过
     * @return
     */
    public boolean isLoggedIn() {
        return getChatClient().isLoggedInBefore();
    }

    /**
     * 获取IM SDK的入口类
     * @return
     */
    public ChatClient getChatClient() {
        return ChatClient.getInstance();
    }

    /**
     * 获取contact manager
     * @return
     */
    public ContactManager getContactManager() {
        return getChatClient().contactManager();
    }

    /**
     * 获取group manager
     * @return
     */
    public GroupManager getGroupManager() {
        return getChatClient().groupManager();
    }

    /**
     * 获取chatroom manager
     * @return
     */
    public ChatRoomManager getChatroomManager() {
        return getChatClient().chatroomManager();
    }


    /**
     * get EMChatManager
     * @return
     */
    public ChatManager getChatManager() {
        return getChatClient().chatManager();
    }

    /**
     * get push manager
     * @return
     */
    public PushManager getPushManager() {
        return getChatClient().pushManager();
    }

    public String getCurrentUser() {
        return getChatClient().getCurrentUser();
    }

    /**
     * @param context
     */
    private void initEaseUI(Context context) {
        //添加ChatPresenter,ChatPresenter中添加了网络连接状态监听，
        //EaseUIKit.getInstance().addChatPresenter(ChatPresenter.getInstance());
        EaseUIKit.getInstance()
                .setUserProvider(new EaseUserProfileProvider() {
                    @Override
                    public EaseUser getUser(String username) {
                        return getUserInfo(username);
                    }

                })
                .setAvatarOptions(getAvatarOptions());
    }

    /**
     * Unified Profile Picture Configuration
     * @return
     */
    private EaseAvatarOptions getAvatarOptions() {
        EaseAvatarOptions avatarOptions = new EaseAvatarOptions();
        avatarOptions.setAvatarShape(1);
        return avatarOptions;
    }

    public EaseUser getUserInfo(String username) {
        // To get instance of EaseUser, here we get it from the user list in memory
        // You'd better cache it if you get it from your server
        EaseUser user = null;
        if(username.equals(ChatClient.getInstance().getCurrentUser()))
            return getUserProfileManager().getCurrentUserInfo();
        user = getContactList().get(username);
        return user;
    }

    public UserProfileManager getUserProfileManager() {
        if (userProManager == null) {
            userProManager = new UserProfileManager();
        }
        return userProManager;
    }


    /**
     * Custom settings
     * @param context
     * @return
     */
    private ChatOptions initChatOptions(Context context){
        Log.d(TAG, "init Agora Chat Options");

        ChatOptions options = new ChatOptions();
        options.setAppKey( context.getString(R.string.ease_configure_app_key));
        // Sets whether to automatically accept friend invitations. Default is true
        options.setAcceptInvitationAlways(false);
        // Set whether read confirmation is required by the recipient
        options.setRequireAck(true);
        // Set whether confirmation of delivery is required by the recipient. Default: false
        options.setRequireDeliveryAck(false);

        /**
         * NOTE:You need to set up your own account to use the three-way push function, see the integration documentation
         */
        PushConfig.Builder builder = new PushConfig.Builder(context);

        builder.enableFCM("142290967082");
        options.setPushConfig(builder.build());

        // 设置是否允许聊天室owner离开并删除会话记录，意味着owner再不会受到任何消息
        options.allowChatroomOwnerLeave(demoModel.isChatroomOwnerLeaveAllowed());
        // 设置退出(主动和被动退出)群组时是否删除聊天消息
        options.setDeleteMessagesAsExitGroup(demoModel.isDeleteMessagesAsExitGroup());
        // 设置是否自动接受加群邀请
        options.setAutoAcceptGroupInvitation(demoModel.isAutoAcceptGroupInvitation());
        // 是否自动将消息附件上传到环信服务器，默认为True是使用环信服务器上传下载
        options.setAutoTransferMessageAttachments(demoModel.isSetTransferFileByUser());
        // 是否自动下载缩略图，默认是true为自动下载
        options.setAutoDownloadThumbnail(demoModel.isSetAutodownloadThumbnail());
        return options;
    }

    public void initPush(Context context) {
        if(EaseUIKit.getInstance().isMainProcess(context)) {
            PushHelper.getInstance().setPushListener(new PushListener() {
                @Override
                public void onError(PushType pushType, long errorCode) {
                    // TODO: 返回的errorCode仅9xx为环信内部错误，可从EMError中查询，其他错误请根据pushType去相应第三方推送网站查询。
                    EMLog.e("PushClient", "Push client occur a error: " + pushType + " - " + errorCode);
                }

                @Override
                public boolean isSupportPush(PushType pushType, PushConfig pushConfig) {
                    // 由外部实现代码判断设备是否支持FCM推送
                    if(pushType == PushType.FCM){
                        EMLog.d("FCM", "GooglePlayServiceCode:"+ GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context));
                        return GoogleApiAvailabilityLight.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
                    }
                    return super.isSupportPush(pushType, pushConfig);
                }
            });
        }
    }

    /**
     * logout
     *
     * @param unbindDeviceToken
     *            whether you need unbind your device token
     * @param callback
     *            callback
     */
    public void logout(boolean unbindDeviceToken, final CallBack callback) {
        Log.d(TAG, "logout: " + unbindDeviceToken);
        ChatClient.getInstance().logout(unbindDeviceToken, new CallBack() {

            @Override
            public void onSuccess() {
                logoutSuccess();
                //reset();
                if (callback != null) {
                    callback.onSuccess();
                }

            }

            @Override
            public void onProgress(int progress, String status) {
                if (callback != null) {
                    callback.onProgress(progress, status);
                }
            }

            @Override
            public void onError(int code, String error) {
                Log.d(TAG, "logout: onSuccess");
                //reset();
                if (callback != null) {
                    callback.onError(code, error);
                }
            }
        });
    }

    /**
     * 关闭当前进程
     */
    public void killApp() {
        List<Activity> activities = DemoApplication.getInstance().getLifecycleCallbacks().getActivityList();
        if(activities != null && !activities.isEmpty()) {
            for(Activity activity : activities) {
                activity.finish();
            }
        }
        Process.killProcess(Process.myPid());
        System.exit(0);
    }



    /**
     * 退出登录后，需要处理的业务逻辑
     */
    public void logoutSuccess() {
        Log.d(TAG, "logout: onSuccess");
        DemoDbHelper.getInstance(DemoApplication.getInstance()).closeDb();
    }

//    public EaseAvatarOptions getEaseAvatarOptions() {
//        return EaseUIKit.getInstance().getAvatarOptions();
//    }

    public DemoModel getModel(){
        if(demoModel == null) {
            demoModel = new DemoModel(DemoApplication.getInstance());
        }
        return demoModel;
    }

    public String getCurrentLoginUser() {
        return ChatClient.getInstance().getCurrentUser();
    }

    /**
     * get instance of EaseNotifier
     * @return
     */
//    public EaseNotifier getNotifier(){
//        return EaseUIKit.getInstance().getNotifier();
//    }

    /**
     * 获取本地标记，是否自动登录
     * @return
     */
    public boolean getAutoLogin() {
        return ChatClient.getInstance().getOptions().getAutoLogin();
    }

    /**
     * 设置SDK是否初始化
     * @param init
     */
    public void setSDKInit(boolean init) {
        isSDKInit = init;
    }

    public boolean isSDKInit() {
        return isSDKInit;
    }

    /**
     * 向数据库中插入数据
     * @param object
     */
    public void insert(Object object) {
        demoModel.insert(object);
    }

    /**
     * update
     * @param object
     */
    public void update(Object object) {
        demoModel.update(object);
    }


    /**
     * get contact list
     *
     * @return
     */
    public Map<String, EaseUser> getContactList() {
        if (isLoggedIn() && contactList == null) {
            contactList = demoModel.getAllUserList();
        }

        // return a empty non-null object to avoid app crash
        if(contactList == null){
            return new Hashtable<String, EaseUser>();
        }
        return contactList;
    }

    /**
     * update contact list
     */
    public void updateContactList() {
        if(isLoggedIn()) {
            contactList = demoModel.getContactList();
        }
    }

    /**
     * 删除联系人
     * @param username
     * @return
     */
    public synchronized int deleteContact(String username) {
        if(TextUtils.isEmpty(username)) {
            return 0;
        }
        DemoDbHelper helper = DemoDbHelper.getInstance(DemoApplication.getInstance());
        if(helper.getUserDao() == null) {
            return 0;
        }
        int num = helper.getUserDao().deleteUser(username);
        ChatClient.getInstance().chatManager().deleteConversation(username, false);
        getModel().deleteUsername(username, false);
        Log.e(TAG, "delete num = "+num);
        return num;
    }

    /**
     * Determine if it is from the current user account of another device
     * @param username
     * @return
     */
    public boolean isCurrentUserFromOtherDevice(String username) {
        if(TextUtils.isEmpty(username)) {
            return false;
        }
        if(username.contains("/") && username.contains(ChatClient.getInstance().getCurrentUser())) {
            return true;
        }
        return false;
    }

}